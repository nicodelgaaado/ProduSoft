import type {
  AuthUser,
  OrderResponse,
  StageState,
  StageType,
  WipSummaryResponse,
  WorkQueueItem,
  OrderStageStatus,
} from '@/types/api';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

const defaultHeaders = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  Object.entries(defaultHeaders).forEach(([key, value]) => {
    if (!headers.has(key)) {
      headers.set(key, value);
    }
  });
  if (token) {
    headers.set('Authorization', `Basic ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = response.statusText;
    try {
      const payload = (await response.json()) as { message?: string };
      if (payload?.message) {
        errorMessage = payload.message;
      }
    } catch {
      // ignore json parse issues
    }
    throw new Error(errorMessage || 'Request failed');
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const WorkflowApi = {
  me: (token: string) => apiFetch<AuthUser>('/auth/me', { method: 'GET' }, token),
  listOrders: (token: string) => apiFetch<OrderResponse[]>('/api/orders', { method: 'GET' }, token),
  getOrder: (orderId: number, token: string) =>
    apiFetch<OrderResponse>(`/api/orders/${orderId}`, { method: 'GET' }, token),
  createOrder: (req: { orderNumber: string; priority?: number | null; notes?: string | null }, token: string) =>
    apiFetch<OrderResponse>(
      '/api/orders',
      {
        method: 'POST',
        body: JSON.stringify(req),
      },
      token,
    ),
  updatePriority: (orderId: number, priority: number, token: string) =>
    apiFetch<OrderResponse>(
      `/api/orders/${orderId}/priority`,
      {
        method: 'PATCH',
        body: JSON.stringify({ priority }),
      },
      token,
    ),
  operatorQueue: (stage: StageType, token: string, states?: StageState[]) => {
    const params = new URLSearchParams({ stage });
    states?.forEach((state) => params.append('states', state));
    return apiFetch<WorkQueueItem[]>(`/api/operator/queue?${params.toString()}`, { method: 'GET' }, token);
  },
  claimStage: (orderId: number, stage: StageType, assignee: string, token: string) =>
    apiFetch<OrderStageStatus>(`/api/operator/orders/${orderId}/stages/${stage}/claim`, {
      method: 'POST',
      body: JSON.stringify({ assignee }),
    }, token),
  completeStage: (
    orderId: number,
    stage: StageType,
    payload: { assignee: string; serviceTimeMinutes?: number | null; notes?: string | null },
    token: string,
  ) =>
    apiFetch<OrderStageStatus>(`/api/operator/orders/${orderId}/stages/${stage}/complete`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token),
  flagException: (
    orderId: number,
    stage: StageType,
    payload: { assignee: string; exceptionReason: string; notes?: string | null },
    token: string,
  ) =>
    apiFetch<OrderStageStatus>(`/api/operator/orders/${orderId}/stages/${stage}/flag-exception`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token),
  updateChecklistItem: (
    orderId: number,
    stage: StageType,
    payload: { taskId: string; completed: boolean },
    token: string,
  ) =>
    apiFetch<OrderStageStatus>(`/api/operator/orders/${orderId}/stages/${stage}/checklist`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }, token),
  wipSummary: (token: string) => apiFetch<WipSummaryResponse>('/api/supervisor/wip', { method: 'GET' }, token),
  approveSkip: (orderId: number, stage: StageType, payload: { approver: string; notes?: string | null }, token: string) =>
    apiFetch(`/api/supervisor/orders/${orderId}/stages/${stage}/approve-skip`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token),
  requestRework: (orderId: number, stage: StageType, payload: { approver: string; notes?: string | null }, token: string) =>
    apiFetch(`/api/supervisor/orders/${orderId}/stages/${stage}/request-rework`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }, token),
};
