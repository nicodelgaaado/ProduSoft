'use client';

import type { StageState } from '@/types/api';

const stateLabels: Record<StageState, string> = {
  BLOCKED: 'Blocked',
  PENDING: 'Pending',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  EXCEPTION: 'Exception',
  SKIPPED: 'Skipped',
  REWORK: 'Rework',
};

export function StageBadge({ state }: { state: StageState }) {
  return <span className={`badge badge-${state.toLowerCase()}`}>{stateLabels[state]}</span>;
}

