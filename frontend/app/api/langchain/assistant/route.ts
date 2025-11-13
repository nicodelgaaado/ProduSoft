import { NextRequest, NextResponse } from 'next/server';
import { ChatOllama } from '@langchain/ollama';
import type { BaseMessageLike } from '@langchain/core/messages';
import type { OrderResponse, OrderStageStatus, StageType } from '@/types/api';

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
const OLLAMA_API_KEY = process.env.OLLAMA_API_KEY ?? process.env.LANGCHAIN_OLLAMA_API_KEY;
const STAGE_ORDER: StageType[] = ['PREPARATION', 'ASSEMBLY', 'DELIVERY'];
const STAGE_RANK = new Map<StageType, number>(STAGE_ORDER.map((stage, index) => [stage, index]));

type AgentRequest = {
  question?: string;
  token?: string | null;
};

type AgentResponse = {
  answer: string;
  model: string;
  contextSummary: string;
};

export async function POST(request: NextRequest) {
  if (!OLLAMA_BASE_URL) {
    return NextResponse.json(
      { message: 'LANGCHAIN_OLLAMA_ENDPOINT (or OLLAMA_ENDPOINT) is not configured on the server.' },
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

  const trimmedQuestion = question.slice(0, 4000);

  const { summary: contextSummary, error: contextError } = await buildWorkflowContext(payload.token);

  const messages: BaseMessageLike[] = [
    [
      'system',
      `You are ProduSoft's workflow copilot. Answer only with operational data you can trace inside the provided context block. If the context lacks the answer, explain what is missing and suggest the user check the workflow dashboard.`,
    ],
    [
      'system',
      `Operational context:\n${contextSummary || 'No orders are accessible for the current user.'}`,
    ],
    ['human', trimmedQuestion],
  ];

  try {
    const model = new ChatOllama({
      model: OLLAMA_MODEL,
      temperature: 0.2,
      baseUrl: OLLAMA_BASE_URL,
      headers: OLLAMA_API_KEY ? new Headers({ Authorization: `Bearer ${OLLAMA_API_KEY}` }) : undefined,
    });
    const response = await model.invoke(messages);
    const answer = normalizeContent(response.content);
    const body: AgentResponse = {
      answer: answer || 'The model did not return any content.',
      model: response.response_metadata?.model ?? OLLAMA_MODEL,
      contextSummary: contextSummary || 'No contextual data was available.',
    };
    if (contextError) {
      return NextResponse.json(
        {
          ...body,
          contextWarning: contextError,
        },
        { status: 200 },
      );
    }
    return NextResponse.json(body, { status: 200 });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to query the LangChain agent.';
    return NextResponse.json({ message }, { status: 502 });
  }
}

async function buildWorkflowContext(token?: string | null) {
  if (!token) {
    return { summary: '', error: 'No workflow token supplied; context is empty.' };
  }
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
  const response = await fetch(`${BACKEND_API_BASE_URL}/api/orders`, {
    headers: {
      Accept: 'application/json',
      Authorization: `Basic ${token}`,
    },
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`Backend responded with ${response.status} while building the LangChain context.`);
  }
  return (await response.json()) as OrderResponse[];
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
