import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { WorkflowApi } from '@/lib/api';
import type {
  AiConversationResponse,
  AiConversationSummaryResponse,
  AiMessageResponse,
} from '@/types/api';

export function AiChatPanel() {
  const { token, user } = useAuth();
  const [conversations, setConversations] = useState<AiConversationSummaryResponse[]>([]);
  const [selectedConversationId, setSelectedConversationId] = useState<number | null>(null);
  const [activeConversation, setActiveConversation] = useState<AiConversationResponse | null>(null);
  const [loadingList, setLoadingList] = useState<boolean>(false);
  const [loadingConversation, setLoadingConversation] = useState<boolean>(false);
  const [messageInput, setMessageInput] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState<boolean>(false);
  const [autoSelectEnabled, setAutoSelectEnabled] = useState<boolean>(true);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  const hasToken = Boolean(token);
  const messageCount = activeConversation?.messages.length ?? 0;

  const loadConversations = useCallback(async () => {
    if (!token) {
      setConversations([]);
      return;
    }
    setLoadingList(true);
    setError(null);
    try {
      const response = await WorkflowApi.listAiConversations(token);
      setConversations(response);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load conversations';
      setError(message);
    } finally {
      setLoadingList(false);
    }
  }, [token]);

  const openConversation = useCallback(
    async (conversationId: number) => {
      if (!token) {
        return;
      }
      setLoadingConversation(true);
      setError(null);
      try {
        const response = await WorkflowApi.getAiConversation(conversationId, token);
        setSelectedConversationId(conversationId);
        setActiveConversation(response);
        setAutoSelectEnabled(true);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load conversation';
        setError(message);
      } finally {
        setLoadingConversation(false);
      }
    },
    [token],
  );

  useEffect(() => {
    if (!token) {
      setConversations([]);
      setActiveConversation(null);
      setSelectedConversationId(null);
      return;
    }
    loadConversations().catch((err) => console.error(err));
  }, [token, loadConversations]);

  useEffect(() => {
    if (!token || !autoSelectEnabled || selectedConversationId !== null) {
      return;
    }
    const first = conversations[0];
    if (first) {
      openConversation(first.id).catch((err) => console.error(err));
    }
  }, [conversations, token, selectedConversationId, autoSelectEnabled, openConversation]);

  useEffect(() => {
    if (messageCount === 0) {
      return;
    }
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messageCount]);

  const handleNewConversation = () => {
    setActiveConversation(null);
    setSelectedConversationId(null);
    setMessageInput('');
    setAutoSelectEnabled(false);
  };

  const handleSend = useCallback(async () => {
    if (!token) {
      setError('You must be signed in to chat with the assistant.');
      return;
    }
    const trimmed = messageInput.trim();
    if (!trimmed) {
      return;
    }
    setSending(true);
    setError(null);
    try {
      let response: AiConversationResponse;
      if (selectedConversationId) {
        response = await WorkflowApi.sendAiMessage(selectedConversationId, trimmed, token);
      } else {
        response = await WorkflowApi.createAiConversation({ initialMessage: trimmed }, token);
      }
      setActiveConversation(response);
      setSelectedConversationId(response.id);
      setAutoSelectEnabled(true);
      setMessageInput('');
      await loadConversations();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to send message';
      setError(message);
    } finally {
      setSending(false);
    }
  }, [messageInput, selectedConversationId, token, loadConversations]);

  const handleComposerSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void handleSend();
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void handleSend();
    }
  };

  const handleRename = async () => {
    if (!token || !activeConversation) {
      return;
    }
    const currentTitle = activeConversation.title ?? '';
    const nextTitle = window.prompt('Conversation title', currentTitle) ?? undefined;
    if (nextTitle === undefined) {
      return;
    }
    const trimmed = nextTitle.trim();
    if (!trimmed) {
      setError('Title cannot be empty.');
      return;
    }
    setError(null);
    try {
      const response = await WorkflowApi.renameAiConversation(activeConversation.id, trimmed, token);
      setActiveConversation(response);
      await loadConversations();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to rename conversation';
      setError(message);
    }
  };

  const handleDelete = async () => {
    if (!token || !activeConversation) {
      return;
    }
    const confirmed = window.confirm('Delete this conversation?');
    if (!confirmed) {
      return;
    }
    setError(null);
    try {
      await WorkflowApi.deleteAiConversation(activeConversation.id, token);
      setActiveConversation(null);
      setSelectedConversationId(null);
      setMessageInput('');
      setAutoSelectEnabled(true);
      await loadConversations();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete conversation';
      setError(message);
    }
  };

  const handleSelectConversation = (conversationId: number) => {
    setAutoSelectEnabled(true);
    openConversation(conversationId).catch((err) => console.error(err));
  };

  const canSend = useMemo(
    () => hasToken && Boolean(messageInput.trim()) && !sending,
    [hasToken, messageInput, sending],
  );

  const conversationTitle = activeConversation?.title?.trim() || 'Untitled conversation';

  return (
    <section className="card chat-card">
      <header className="card__header">
        <div>
          <h2>AI Assistant</h2>
          <p className="muted">Ask for operational insights or guidance using your live data.</p>
        </div>
        <button type="button" className="ghost" onClick={handleNewConversation}>
          New chat
        </button>
      </header>

      {error && <div className="page-alert">{error}</div>}

      <div className="chat-panel">
        <aside className="chat-panel__sidebar">
          <div className="chat-panel__sidebar-header">
            <span className="muted">
              {loadingList
                ? 'Loading conversations...'
                : `${conversations.length} ${conversations.length === 1 ? 'thread' : 'threads'}`}
            </span>
          </div>
          <div className="chat-panel__list">
            {loadingList && conversations.length === 0 && <div className="chat-panel__empty">Loading...</div>}
            {!loadingList && conversations.length === 0 && (
              <div className="chat-panel__empty">
                Start a new chat to capture questions and AI responses for later reference.
              </div>
            )}
            {conversations.map((conversation) => (
              <button
                key={conversation.id}
                type="button"
                className={`chat-list__item ${conversation.id === selectedConversationId ? 'active' : ''}`}
                onClick={() => handleSelectConversation(conversation.id)}
              >
                <span className="chat-list__title">{conversation.title?.trim() || 'Untitled conversation'}</span>
                {conversation.lastMessagePreview && (
                  <span className="chat-list__preview">{conversation.lastMessagePreview}</span>
                )}
                <span className="chat-list__time">{formatRelative(conversation.updatedAt)}</span>
              </button>
            ))}
          </div>
        </aside>

        <div className="chat-panel__body">
          <div className="chat-panel__body-header">
            <div>
              <h3>{activeConversation ? conversationTitle : 'Start a conversation'}</h3>
              <p className="muted">
                {activeConversation
                  ? `Updated ${formatRelative(activeConversation.updatedAt)}`
                  : 'Ask about workloads, bottlenecks, or next steps to receive AI suggestions.'}
              </p>
            </div>
            {activeConversation && (
              <div className="chat-panel__actions">
                <button type="button" className="ghost" onClick={handleRename}>
                  Rename
                </button>
                <button type="button" className="ghost danger" onClick={handleDelete}>
                  Delete
                </button>
              </div>
            )}
          </div>

          <div className="chat-panel__messages">
            {loadingConversation && <div className="chat-panel__empty">Loading conversation...</div>}
            {!loadingConversation && activeConversation && activeConversation.messages.length === 0 && (
              <div className="chat-panel__empty">Send a question to begin.</div>
            )}
            {!loadingConversation && !activeConversation && (
              <div className="chat-panel__empty">
                Compose a message below to auto-create a new conversation. Your exchanges will stay available here.
              </div>
            )}
            {!loadingConversation &&
              activeConversation?.messages.map((message) => (
                <ChatMessage
                  key={message.id}
                  message={message}
                  currentUser={user?.username ?? 'You'}
                />
              ))}
            <div ref={messagesEndRef} />
          </div>

          <form className="chat-composer" onSubmit={handleComposerSubmit}>
            <textarea
              value={messageInput}
              onChange={(event) => setMessageInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={
                hasToken
                  ? 'Ask the assistant for help prioritising, summarising, or investigating work...'
                  : 'Sign in to chat with the assistant.'
              }
              disabled={!hasToken || sending}
            />
            <button type="submit" disabled={!canSend}>
              {sending ? 'Sending...' : 'Send'}
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}

function ChatMessage({ message, currentUser }: { message: AiMessageResponse; currentUser: string }) {
  const roleClass = message.role.toLowerCase();
  const author =
    message.role === 'ASSISTANT' ? 'AI Assistant' : message.role === 'USER' ? currentUser : 'System';

  return (
    <article className={`chat-message chat-message--${roleClass}`}>
      <header className="chat-message__meta">
        <span className="chat-message__author">{author}</span>
        <span className="chat-message__timestamp">{formatTimestamp(message.createdAt)}</span>
      </header>
      <p>{message.content}</p>
    </article>
  );
}

function formatRelative(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const diff = Date.now() - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < minute) {
    return 'Just now';
  }
  if (diff < hour) {
    const value = Math.round(diff / minute);
    return `${value} min ago`;
  }
  if (diff < day) {
    const value = Math.round(diff / hour);
    return `${value} hr${value === 1 ? '' : 's'} ago`;
  }
  return date.toLocaleDateString();
}

function formatTimestamp(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

