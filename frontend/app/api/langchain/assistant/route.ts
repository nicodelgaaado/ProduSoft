import { NextRequest, NextResponse } from 'next/server';
import { ChatOllama } from '@langchain/ollama';
import type { BaseMessageLike } from '@langchain/core/messages';
import { z } from 'zod';
import type {
  OrderResponse,
  OrderStageStatus,
  StageState,
  StageType,
} from '@/types/api';

const MAX_CONTEXT_ORDERS = 10;
const OLLAMA_MODEL = process.env.LANGCHAIN_OLLAMA_MODEL ?? 'gpt-oss:20b-cloud';
const BACKEND_API_BASE_URL =
  process.env.WORKFLOW_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  'https://produsoft.onrender.com';
const OLLAMA_BASE_URL =
  process.env.LANGCHAIN_OLLAMA_ENDPOINT ??
  process.env.OLLAMA_ENDPOINT ??
  'https://ollama.com';
const OLLAMA_API_KEY =
  process.env.OLLAMA_API_KEY ?? process.env.LANGCHAIN_OLLAMA_API_KEY;
const STAGE_ORDER: StageType[] = ['PREPARATION', 'ASSEMBLY', 'DELIVERY'];
const STAGE_STATES: StageState[] = [
  'BLOCKED',
  'PENDING',
  'IN_PROGRESS',
  'COMPLETED',
  'EXCEPTION',
  'SKIPPED',
  'REWORK',
];
const STAGE_RANK = new Map<StageType, number>(
  STAGE_ORDER.map((stage, index) => [stage, index]),
);
const ACTION_NAMES = [
  'list_orders',
  'get_order_details',
  'create_order',
  'update_order_priority',
  'claim_stage',
  'complete_stage',
  'flag_stage_exception',
  'approve_stage_skip',
] as const;

type ActionName = (typeof ACTION_NAMES)[number];
type Role = 'OPERATOR' | 'SUPERVISOR';

type AgentActionResult = {
  name: ActionName | string;
  status: 'success' | 'error' | 'skipped';
  summary: string;
  data?: unknown;
  error?: string;
};

const PlanActionSchema = z.object({
  name: z.enum(ACTION_NAMES),
  rationale: z.string().optional(),
  arguments: z.record(z.any()).default({}),
});
const PlanSchema = z.object({
  intent: z.string().default(''),
  reasoning: z.string().optional(),
  notes: z.string().optional(),
  actions: z.array(PlanActionSchema).default([]),
});
type AgentPlan = z.infer<typeof PlanSchema>;

type ExecutionContext = {
  token: string;
  username: string;
};

type ActionDefinition = {
  name: ActionName;
  description: string;
  parameterSummary: string;
  roles: Role[];
  schema: z.ZodTypeAny;
  handler: (args: unknown, ctx: ExecutionContext) => Promise<AgentActionResult>;
};

const ACTION_DEFINITIONS: ActionDefinition[] = [
  {
    name: 'list_orders',
    description: 'Retrieve up to 25 live orders for situational awareness.',
    parameterSummary:
      'limit (optional number 1-25), stage (PREPARATION/ASSEMBLY/DELIVERY), states (array of workflow states).',
    roles: ['OPERATOR', 'SUPERVISOR'],
    schema: z.object({
      limit: z.number().int().min(1).max(25).optional(),
      stage: z.enum(STAGE_ORDER).optional(),
      states: z.array(z.enum(STAGE_STATES)).optional(),
    }),
    handler: async (args, ctx) => {
      const orders = await fetchOrders(ctx.token);
      let filtered = orders;
      if (args.stage) {
        filtered = filtered.filter(
          (order) => order.currentStage === args.stage,
        );
      }
      if (args.states?.length) {
        const set = new Set(args.states);
        filtered = filtered.filter((order) => set.has(order.overallState));
      }
      const limit = args.limit ?? Math.min(filtered.length, 10);
      return {
        name: 'list_orders',
        status: 'success',
        summary: `Retrieved ${filtered.length} orders, returning ${limit}.`,
        data: filtered.slice(0, limit),
      };
    },
  },
  {
    name: 'get_order_details',
    description:
      'Pull a single order with all stage details before taking action.',
    parameterSummary: 'orderId (number).',
    roles: ['OPERATOR', 'SUPERVISOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
    }),
    handler: async (args, ctx) => {
      const data = await fetchWithAuthJson<OrderResponse>(
        `/api/orders/${args.orderId}`,
        { method: 'GET' },
        ctx.token,
      );
      return {
        name: 'get_order_details',
        status: 'success',
        summary: `Pulled current data for order ${data.orderNumber}.`,
        data,
      };
    },
  },
  {
    name: 'create_order',
    description:
      'Create a brand-new work order when supervisors request new work.',
    parameterSummary:
      'orderNumber (string), priority (optional integer), notes (optional string).',
    roles: ['SUPERVISOR'],
    schema: z.object({
      orderNumber: z.string().min(3).max(64),
      priority: z.number().int().min(0).max(999).optional(),
      notes: z.string().max(2000).optional(),
    }),
    handler: async (args, ctx) => {
      const body = {
        orderNumber: args.orderNumber,
        priority: args.priority ?? null,
        notes: args.notes ?? null,
      };
      const data = await fetchWithAuthJson<OrderResponse>(
        '/api/orders',
        {
          method: 'POST',
          body: JSON.stringify(body),
        },
        ctx.token,
      );
      return {
        name: 'create_order',
        status: 'success',
        summary: `Created order ${data.orderNumber} (id ${data.id}).`,
        data,
      };
    },
  },
  {
    name: 'update_order_priority',
    description: 'Supervisors can adjust queue priority on any order.',
    parameterSummary: 'orderId (number), priority (integer).',
    roles: ['SUPERVISOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
      priority: z.number().int().min(0).max(999),
    }),
    handler: async (args, ctx) => {
      const data = await fetchWithAuthJson<OrderResponse>(
        `/api/orders/${args.orderId}/priority`,
        {
          method: 'PATCH',
          body: JSON.stringify({ priority: args.priority }),
        },
        ctx.token,
      );
      return {
        name: 'update_order_priority',
        status: 'success',
        summary: `Updated order ${data.orderNumber} priority to ${data.priority}.`,
        data,
      };
    },
  },
  {
    name: 'claim_stage',
    description: 'Operators claim a stage before doing work.',
    parameterSummary:
      'orderId (number), stage (PREPARATION/ASSEMBLY/DELIVERY), assignee (optional string defaults to current user).',
    roles: ['OPERATOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
      stage: z.enum(STAGE_ORDER),
      assignee: z.string().min(2).max(64).optional(),
    }),
    handler: async (args, ctx) => {
      const payload = {
        assignee: args.assignee ?? ctx.username,
      };
      const data = await fetchWithAuthJson<OrderStageStatus>(
        `/api/operator/orders/${args.orderId}/stages/${args.stage}/claim`,
        {
          method: 'POST',
          body: JSON.stringify(payload),
        },
        ctx.token,
      );
      return {
        name: 'claim_stage',
        status: 'success',
        summary: `Stage ${data.stage} claimed by ${data.assignee ?? ctx.username}.`,
        data,
      };
    },
  },
  {
    name: 'complete_stage',
    description:
      'Operators mark a stage complete once the work and notes are captured.',
    parameterSummary:
      'orderId (number), stage, serviceTimeMinutes (optional int), notes (optional string).',
    roles: ['OPERATOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
      stage: z.enum(STAGE_ORDER),
      serviceTimeMinutes: z.number().int().min(1).max(600).optional(),
      notes: z.string().max(2000).optional(),
    }),
    handler: async (args, ctx) => {
      const payload = {
        assignee: ctx.username,
        serviceTimeMinutes: args.serviceTimeMinutes ?? null,
        notes: args.notes ?? null,
      };
      const data = await fetchWithAuthJson<OrderStageStatus>(
        `/api/operator/orders/${args.orderId}/stages/${args.stage}/complete`,
        {
          method: 'POST',
          body: JSON.stringify(payload),
        },
        ctx.token,
      );
      return {
        name: 'complete_stage',
        status: 'success',
        summary: `Stage ${data.stage} completed (state ${data.state}).`,
        data,
      };
    },
  },
  {
    name: 'flag_stage_exception',
    description:
      'Operators document blocking issues so supervisors can follow up.',
    parameterSummary:
      'orderId (number), stage, exceptionReason (string), notes (optional string).',
    roles: ['OPERATOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
      stage: z.enum(STAGE_ORDER),
      exceptionReason: z.string().min(3).max(500),
      notes: z.string().max(2000).optional(),
    }),
    handler: async (args, ctx) => {
      const payload = {
        assignee: ctx.username,
        exceptionReason: args.exceptionReason,
        notes: args.notes ?? null,
      };
      const data = await fetchWithAuthJson<OrderStageStatus>(
        `/api/operator/orders/${args.orderId}/stages/${args.stage}/flag-exception`,
        {
          method: 'POST',
          body: JSON.stringify(payload),
        },
        ctx.token,
      );
      return {
        name: 'flag_stage_exception',
        status: 'success',
        summary: `Flagged stage ${data.stage} with exception "${data.exceptionReason}".`,
        data,
      };
    },
  },
  {
    name: 'approve_stage_skip',
    description:
      'Supervisors can approve stage skips or resequencing when justified.',
    parameterSummary: 'orderId (number), stage, notes (optional string).',
    roles: ['SUPERVISOR'],
    schema: z.object({
      orderId: z.number().int().positive(),
      stage: z.enum(STAGE_ORDER),
      notes: z.string().max(2000).optional(),
    }),
    handler: async (args, ctx) => {
      const payload = {
        approver: ctx.username,
        notes: args.notes ?? null,
      };
      const data = await fetchWithAuthJson<OrderStageStatus>(
        `/api/supervisor/orders/${args.orderId}/stages/${args.stage}/approve-skip`,
        {
          method: 'POST',
          body: JSON.stringify(payload),
        },
        ctx.token,
      );
      return {
        name: 'approve_stage_skip',
        status: 'success',
        summary: `Approved skip for stage ${data.stage}.`,
        data,
      };
    },
  },
];

const ACTION_MAP = new Map<ActionName, ActionDefinition>(
  ACTION_DEFINITIONS.map((action) => [action.name, action]),
);

export async function POST(request: NextRequest) {
  if (!OLLAMA_BASE_URL) {
    return NextResponse.json(
      {
        message:
          'LANGCHAIN_OLLAMA_ENDPOINT (or OLLAMA_ENDPOINT) is not configured on the server.',
      },
      { status: 500 },
    );
  }

  let payload: AgentRequest;
  try {
    payload = (await request.json()) as AgentRequest;
  } catch {
    return NextResponse.json({ message: 'Invalid JSON payload.' }, { status: 400 });
  }

  const question = payload.question?.trim();
  if (!question) {
    return NextResponse.json({ message: 'question is required.' }, { status: 400 });
  }
  if (!payload.token) {
    return NextResponse.json(
      { message: 'Authentication token is required for autonomous actions.' },
      { status: 401 },
    );
  }

  try {
    const trimmedQuestion = question.slice(0, 4000);
    const { summary: contextSummary, error: contextError } = await buildWorkflowContext(
      payload.token,
    );
    const userProfile = await fetchUserProfile(payload.token);
    const normalizedRoles = normalizeRoles(userProfile.roles);
    const allowedActions = ACTION_DEFINITIONS.filter((action) =>
      action.roles.some((role) => normalizedRoles.has(role)),
    );

    const model = new ChatOllama({
      model: OLLAMA_MODEL,
      temperature: 0.2,
      baseUrl: OLLAMA_BASE_URL,
      headers: OLLAMA_API_KEY
        ? new Headers({ Authorization: `Bearer ${OLLAMA_API_KEY}` })
        : undefined,
    });

    let agentPlan: AgentPlan | null = null;
    let actionResults: AgentActionResult[] = [];

    if (allowedActions.length > 0) {
      agentPlan = await generatePlan(
        model,
        trimmedQuestion,
        contextSummary,
        allowedActions,
        userProfile,
      );
      if (agentPlan.actions.length > 0) {
        actionResults = await executePlan(agentPlan.actions, {
          token: payload.token,
          username: userProfile.username,
        });
      }
    }

    const finalResponse = await buildFinalAnswer(
      model,
      trimmedQuestion,
      contextSummary,
      agentPlan,
      actionResults,
    );

    return NextResponse.json({
      answer: finalResponse.answer,
      model: finalResponse.model,
      contextSummary: contextSummary || 'No contextual data was available.',
      contextWarning: contextError ?? undefined,
      plan: agentPlan,
      actions: actionResults,
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Failed to run the LangChain agent.';
    return NextResponse.json({ message }, { status: 502 });
  }
}

type AgentRequest = {
  question?: string;
  token?: string | null;
};

async function generatePlan(
  model: ChatOllama,
  question: string,
  contextSummary: string,
  allowedActions: ActionDefinition[],
  profile: { username: string; roles: string[] },
): Promise<AgentPlan> {
  const toolDescription = allowedActions
    .map(
      (action) =>
        `- ${action.name}: ${action.description} Params: ${action.parameterSummary}`,
    )
    .join('\n');

  const messages: BaseMessageLike[] = [
    [
      'system',
      `You are ProduSoft's autonomous workflow orchestrator. Plan ONLY with the actions explicitly listed. Return STRICT JSON (no prose) matching schema { "intent": string, "reasoning": string, "notes": string?, "actions": [ { "name": string, "rationale": string, "arguments": object } ] }. If no action is needed, return an empty actions array.`,
    ],
    [
      'human',
      [
        `User: ${profile.username}`,
        `Roles: ${profile.roles.join(', ') || 'unknown'}`,
        `Available actions:\n${toolDescription}`,
        `Context:\n${contextSummary}`,
        `Objective:\n${question}`,
      ].join('\n\n'),
    ],
  ];

  const response = await model.invoke(messages);
  const planText = normalizeContent(response.content);
  const planJson = extractJson(planText);
  if (!planJson) {
    throw new Error('Agent plan was not valid JSON.');
  }
  const parsed = PlanSchema.safeParse(JSON.parse(planJson));
  if (!parsed.success) {
    throw new Error('Agent plan schema validation failed.');
  }
  return parsed.data;
}

async function executePlan(
  actions: z.infer<typeof PlanActionSchema>[],
  ctx: ExecutionContext,
): Promise<AgentActionResult[]> {
  const results: AgentActionResult[] = [];
  for (const action of actions) {
    const definition = ACTION_MAP.get(action.name);
    if (!definition) {
      results.push({
        name: action.name,
        status: 'error',
        summary: `Action ${action.name} is not supported by this environment.`,
      });
      continue;
    }
    const parsed = definition.schema.safeParse(action.arguments ?? {});
    if (!parsed.success) {
      results.push({
        name: action.name,
        status: 'error',
        summary: `Invalid arguments supplied for ${action.name}.`,
        error: parsed.error.message,
      });
      continue;
    }
    try {
      const result = await definition.handler(parsed.data, ctx);
      results.push(result);
    } catch (error) {
      results.push({
        name: action.name,
        status: 'error',
        summary: `Failed to execute ${action.name}.`,
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }
  return results;
}

async function buildFinalAnswer(
  model: ChatOllama,
  question: string,
  contextSummary: string,
  plan: AgentPlan | null,
  actionResults: AgentActionResult[],
) {
  const executionLog =
    actionResults.length === 0
      ? 'No autonomous actions were executed.'
      : actionResults
          .map(
            (result) =>
              `- ${result.name}: ${result.status.toUpperCase()} — ${result.summary}${
                result.error ? ` (error: ${result.error})` : ''
              }`,
          )
          .join('\n');

  const planSummary = plan
    ? JSON.stringify(plan, null, 2)
    : 'No plan generated (information-only response).';

  const messages: BaseMessageLike[] = [
    [
      'system',
      `You are ProduSoft's workflow assistant. You MUST reflect the real execution results that were already performed. If something failed, explain why and recommend next steps. Never invent actions outside the execution log.`,
    ],
    ['system', `Operational context:\n${contextSummary}`],
    ['system', `Agent plan:\n${planSummary}`],
    ['system', `Execution log:\n${executionLog}`],
    ['human', question],
  ];

  const response = await model.invoke(messages);
  return {
    answer: normalizeContent(response.content),
    model: (response.response_metadata as { model?: string })?.model ?? OLLAMA_MODEL,
  };
}

async function buildWorkflowContext(token: string) {
  try {
    const orders = await fetchOrders(token);
    if (!orders.length) {
      return { summary: 'No orders are currently available to the signed-in user.', error: null };
    }
    const limited = orders
      .slice()
      .sort(compareOrders)
      .slice(0, MAX_CONTEXT_ORDERS);
    const summary =
      `Total orders available: ${orders.length}. Showing top ${limited.length} by priority and creation.\n` +
      limited.map(formatOrder).join('\n');
    return { summary, error: null };
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Unable to read workflow context from the backend.';
    return { summary: '', error: message };
  }
}

async function fetchOrders(token: string): Promise<OrderResponse[]> {
  return fetchWithAuthJson(`/api/orders`, { method: 'GET' }, token);
}

async function fetchWithAuthJson<T>(
  path: string,
  init: RequestInit,
  token: string,
): Promise<T> {
  const response = await fetchWithAuth(path, init, token);
  return (await response.json()) as T;
}

async function fetchWithAuth(path: string, init: RequestInit, token: string) {
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Basic ${token}`);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(`${BACKEND_API_BASE_URL}${path}`, {
    ...init,
    headers,
  });
  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message);
  }
  return response;
}

async function fetchUserProfile(token: string) {
  return fetchWithAuthJson<{ username: string; roles: string[] }>(
    '/auth/me',
    { method: 'GET' },
    token,
  );
}

function normalizeRoles(roles: string[] = []) {
  return new Set<Role>(
    roles
      .map((role) => role.replace(/^ROLE_/, '').toUpperCase())
      .filter((role): role is Role => role === 'OPERATOR' || role === 'SUPERVISOR'),
  );
}

function compareOrders(a: OrderResponse, b: OrderResponse) {
  const priorityDiff = (b.priority ?? 0) - (a.priority ?? 0);
  if (priorityDiff !== 0) {
    return priorityDiff;
  }
  const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
  const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
  return dateB - dateA;
}

function formatOrder(order: OrderResponse) {
  const header = `Order ${order.orderNumber ?? 'unknown'} (id=${order.id}) priority ${
    order.priority ?? 'n/a'
  } - current stage ${order.currentStage.toLowerCase()} / overall ${order.overallState.toLowerCase()}`;
  const stageSummaries = order.stages
    .slice()
    .sort((a, b) => rankStage(a.stage) - rankStage(b.stage))
    .map((stage) => formatStage(stage))
    .join('; ');
  return `${header}. Stage details: ${stageSummaries || 'no recorded stages.'}`;
}

function rankStage(stage: StageType) {
  return STAGE_RANK.get(stage) ?? Number.MAX_SAFE_INTEGER;
}

function formatStage(stage: OrderStageStatus) {
  const parts = [
    `${stage.stage.toLowerCase()}: ${stage.state.toLowerCase()}`,
    stage.assignee ? `assignee ${stage.assignee}` : null,
    stage.exceptionReason ? `exception ${stage.exceptionReason}` : null,
    stage.notes ? `notes ${stage.notes}` : null,
  ].filter(Boolean);
  return parts.join(' | ');
}

function normalizeContent(content: unknown): string {
  if (typeof content === 'string') {
    return content;
  }
  if (Array.isArray(content)) {
    return content
      .map((entry) => {
        if (typeof entry === 'string') {
          return entry;
        }
        if (typeof entry === 'object' && entry !== null && 'text' in entry) {
          const value = (entry as { text?: unknown }).text;
          return typeof value === 'string' ? value : '';
        }
        return '';
      })
      .filter(Boolean)
      .join('\n');
  }
  return '';
}

async function readErrorMessage(response: Response) {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload?.message || response.statusText;
  } catch {
    return response.statusText;
  }
}

function extractJson(text: string) {
  const trimmed = text.trim();
  const codeFenceMatch = trimmed.match(/```json([\s\S]*?)```/i);
  if (codeFenceMatch) {
    return codeFenceMatch[1].trim();
  }
  const start = trimmed.indexOf('{');
  const end = trimmed.lastIndexOf('}');
  if (start === -1 || end === -1 || end <= start) {
    return null;
  }
  return trimmed.slice(start, end + 1);
}
