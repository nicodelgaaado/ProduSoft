'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { RequireAuth } from '@/components/RequireAuth';
import { Modal } from '@/components/Modal';
import { StageBadge } from '@/components/StageBadge';
import { useAuth } from '@/hooks/useAuth';
import { WorkflowApi } from '@/lib/api';
import type { StageType, WorkQueueItem } from '@/types/api';

const stageOptions: StageType[] = ['PREPARATION', 'ASSEMBLY', 'DELIVERY'];

type ModalState =
  | { type: 'claim'; item: WorkQueueItem }
  | { type: 'complete'; item: WorkQueueItem }
  | { type: 'exception'; item: WorkQueueItem }
  | null;

export default function OperatorConsole() {
  return (
    <RequireAuth allowedRoles={['OPERATOR']}>
      <OperatorView />
    </RequireAuth>
  );
}

function OperatorView() {
  const { user, token } = useAuth();
  const [stage, setStage] = useState<StageType>('PREPARATION');
  const [queue, setQueue] = useState<WorkQueueItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [modalState, setModalState] = useState<ModalState>(null);
  const [serviceTime, setServiceTime] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [exceptionReason, setExceptionReason] = useState<string>('');
  const [exceptionNotes, setExceptionNotes] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);

  const loadQueue = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const response = await WorkflowApi.operatorQueue(stage, token);
      setQueue(response);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch queue';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [stage, token]);

  useEffect(() => {
    loadQueue().catch((err) => console.error(err));
  }, [loadQueue]);

  const closeModal = () => {
    setModalState(null);
    setServiceTime('');
    setNotes('');
    setExceptionReason('');
    setExceptionNotes('');
    setSubmitting(false);
  };

  const refreshAfterAction = async () => {
    await loadQueue();
    closeModal();
  };

  const handleClaimConfirm = async (item: WorkQueueItem) => {
    if (!token || !user) return;
    setSubmitting(true);
    try {
      await WorkflowApi.claimStage(item.orderId, item.stage, user.username, token);
      await refreshAfterAction();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to claim stage';
      setError(message);
      setSubmitting(false);
    }
  };

  const handleComplete = async (item: WorkQueueItem) => {
    if (!token || !user) return;
    setSubmitting(true);
    try {
      await WorkflowApi.completeStage(
        item.orderId,
        item.stage,
        {
          assignee: user.username,
          serviceTimeMinutes: serviceTime ? Number(serviceTime) : null,
          notes,
        },
        token,
      );
      await refreshAfterAction();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to complete stage';
      setError(message);
      setSubmitting(false);
    }
  };

  const handleException = async (item: WorkQueueItem) => {
    if (!token || !user) return;
    setSubmitting(true);
    try {
      await WorkflowApi.flagException(
        item.orderId,
        item.stage,
        {
          assignee: user.username,
          exceptionReason,
          notes: exceptionNotes || undefined,
        },
        token,
      );
      await refreshAfterAction();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to flag exception';
      setError(message);
      setSubmitting(false);
    }
  };

  const orderedQueue = useMemo(
    () =>
      [...queue].sort((a, b) => {
        const priorityA = a.priority ?? 0;
        const priorityB = b.priority ?? 0;
        if (priorityA === priorityB) {
          return (a.orderNumber ?? '').localeCompare(b.orderNumber ?? '');
        }
        return priorityB - priorityA;
      }),
    [queue],
  );

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Operator Console</h1>
          <p>Claim, complete, or flag staged work items sequentially.</p>
        </div>
        <div className="stage-tabs" role="tablist">
          {stageOptions.map((option) => (
            <button
              key={option}
              type="button"
              role="tab"
              aria-selected={stage === option}
              className={stage === option ? 'active' : ''}
              onClick={() => setStage(option)}
            >
              {option.toLowerCase()}
            </button>
          ))}
        </div>
      </header>

      {error && <div className="page-alert">{error}</div>}

      <section className="card">
        <header className="card__header">
          <h2>
            Stage queue <span className="muted">({orderedQueue.length})</span>
          </h2>
          <button type="button" className="link-button" onClick={loadQueue} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </header>
        <div className="table">
          <div className="table__head">
            <span>Order</span>
            <span>Priority</span>
            <span>Status</span>
            <span>Assignee</span>
            <span>Updated</span>
            <span>Actions</span>
          </div>
          <div className="table__body">
            {loading && orderedQueue.length === 0 && <div className="table__empty">Loading queue…</div>}
            {!loading && orderedQueue.length === 0 && <div className="table__empty">No items ready for this stage.</div>}
            {orderedQueue.map((item) => (
              <article key={`${item.orderId}-${item.stage}`} className="table__row">
                <span>
                  <strong>{item.orderNumber}</strong>
                </span>
                <span>{item.priority ?? '—'}</span>
                <span>
                  <StageBadge state={item.stageState} />
                  {item.exceptionReason && <small className="muted"> {item.exceptionReason}</small>}
                </span>
                <span>{item.assignee ?? 'Unassigned'}</span>
                <span>{item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '—'}</span>
                <span className="table__actions">
                  {(item.stageState === 'PENDING' || item.stageState === 'REWORK') && (
                    <button type="button" onClick={() => setModalState({ type: 'claim', item })}>
                      Claim
                    </button>
                  )}
                  {item.stageState === 'IN_PROGRESS' && item.assignee === user?.username && (
                    <>
                      <button type="button" onClick={() => setModalState({ type: 'complete', item })}>
                        Complete
                      </button>
                      <button type="button" className="danger" onClick={() => setModalState({ type: 'exception', item })}>
                        Flag exception
                      </button>
                    </>
                  )}
                </span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <Modal
        open={modalState?.type === 'claim'}
        title="Claim stage"
        onClose={closeModal}
        footer={
          <div className="modal-actions">
            <button type="button" onClick={closeModal} className="ghost">
              Cancel
            </button>
            {modalState?.type === 'claim' && (
              <button
                type="button"
                onClick={() => modalState && handleClaimConfirm(modalState.item)}
                disabled={submitting}
              >
                {submitting ? 'Claiming…' : 'Claim stage'}
              </button>
            )}
          </div>
        }
      >
        {modalState?.type === 'claim' && (
          <p>
            Confirm claim of <strong>{modalState.item.orderNumber}</strong> at the{' '}
            <strong>{modalState.item.stage.toLowerCase()}</strong> stage.
          </p>
        )}
      </Modal>

      <Modal
        open={modalState?.type === 'complete'}
        title="Complete stage"
        onClose={closeModal}
        footer={
          <div className="modal-actions">
            <button type="button" onClick={closeModal} className="ghost">
              Cancel
            </button>
            {modalState?.type === 'complete' && (
              <button
                type="button"
                onClick={() => modalState && handleComplete(modalState.item)}
                disabled={submitting}
              >
                {submitting ? 'Completing…' : 'Complete stage'}
              </button>
            )}
          </div>
        }
      >
        {modalState?.type === 'complete' && (
          <form className="modal-form" onSubmit={(event) => event.preventDefault()}>
            <p>
              Wrap up <strong>{modalState.item.orderNumber}</strong> and capture optional service details.
            </p>
            <label htmlFor="serviceTime">Service time (minutes)</label>
            <input
              id="serviceTime"
              type="number"
              min="0"
              step="1"
              value={serviceTime}
              onChange={(event) => setServiceTime(event.target.value)}
              placeholder="45"
            />
            <label htmlFor="completionNotes">Completion notes</label>
            <textarea
              id="completionNotes"
              rows={3}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Details captured while closing the task"
            />
          </form>
        )}
      </Modal>

      <Modal
        open={modalState?.type === 'exception'}
        title="Flag exception"
        onClose={closeModal}
        footer={
          <div className="modal-actions">
            <button type="button" onClick={closeModal} className="ghost">
              Cancel
            </button>
            {modalState?.type === 'exception' && (
              <button
                type="button"
                onClick={() => modalState && handleException(modalState.item)}
                disabled={submitting || !exceptionReason}
                className="danger"
              >
                {submitting ? 'Submitting…' : 'Flag exception'}
              </button>
            )}
          </div>
        }
      >
        {modalState?.type === 'exception' && (
          <form className="modal-form" onSubmit={(event) => event.preventDefault()}>
            <p>
              Provide context for the exception on <strong>{modalState.item.orderNumber}</strong>.
            </p>
            <label htmlFor="exceptionReason">Exception reason</label>
            <input
              id="exceptionReason"
              type="text"
              value={exceptionReason}
              onChange={(event) => setExceptionReason(event.target.value)}
              placeholder="Waiting on supplier"
              required
            />
            <label htmlFor="exceptionNotes">Notes</label>
            <textarea
              id="exceptionNotes"
              rows={3}
              value={exceptionNotes}
              onChange={(event) => setExceptionNotes(event.target.value)}
              placeholder="Add optional context for supervisors"
            />
          </form>
        )}
      </Modal>
    </section>
  );
}

