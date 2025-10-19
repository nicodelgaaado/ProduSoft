'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Button, ContainedList, ContainedListItem, InlineLoading, InlineNotification, Layer, Stack, TextArea, Tile } from '@carbon/react';
import { Add, Edit, Renew, Send, TrashCan } from '@carbon/icons-react';
import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useAuth } from '@/hooks/useAuth';
import { WorkflowApi } from '@/lib/api';
import type {
  AiConversationResponse,
  AiConversationSummaryResponse,
  AiMessageResponse,
} from '@/types/api';
import styles from './AiChatPanel.module.css';

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
  const [autoSelectEnabled, setAutoSelectEnabled] = useState<boolean>(false);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

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
      setMessageInput('');
      setAutoSelectEnabled(false);
      return;
    }
    setActiveConversation(null);
    setSelectedConversationId(null);
    setMessageInput('');
    setAutoSelectEnabled(false);
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
    const container = messagesContainerRef.current;
    if (!container) {
      return;
    }
    container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
  }, [messageCount]);

  const handleNewConversation = () => {
    setActiveConversation(null);
    setSelectedConversationId(null);
    setMessageInput('');
    setAutoSelectEnabled(false);
  };

  const handleRefresh = () => {
    void loadConversations();
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
    <Layer level={0}>
      <Tile className={styles.panel}>
        <Stack gap={6}>
          <div className={styles.header}>
            <div>
              <h2 className="cds--heading-05">AI Assistant</h2>
              <p className={styles.subtitle}>
                Ask for operational insights or guidance using your live data.
              </p>
            </div>
            <Button kind="ghost" renderIcon={Add} onClick={handleNewConversation} type="button">
              New chat
            </Button>
          </div>

          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              title="There was a problem"
              subtitle={error}
              onClose={() => setError(null)}
            />
          )}

          <div className={styles.layout}>
            <Layer level={1}>
              <Tile className={styles.conversationTile}>
                <Stack gap={5}>
                  <div className={styles.conversationHeader}>
                    <div>
                      <h3 className={`${styles.conversationTitle} cds--heading-04`}>
                        {activeConversation ? conversationTitle : 'Start a conversation'}
                      </h3>
                      <p className={styles.conversationMeta}>
                        {activeConversation
                          ? `Updated ${formatRelative(activeConversation.updatedAt)}`
                          : 'Ask about workloads, bottlenecks, or next steps to receive AI suggestions.'}
                      </p>
                    </div>
                    {activeConversation && (
                      <div className={styles.actions}>
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={Edit}
                          onClick={handleRename}
                          type="button"
                        >
                          Rename
                        </Button>
                        <Button
                          kind="danger--ghost"
                          size="sm"
                          renderIcon={TrashCan}
                          onClick={handleDelete}
                          type="button"
                        >
                          Delete
                        </Button>
                      </div>
                    )}
                  </div>

                  <div
                    className={styles.messages}
                    role="log"
                    aria-live="polite"
                    ref={messagesContainerRef}
                  >
                    {loadingConversation && (
                      <div className={styles.placeholder}>
                        <InlineLoading status="active" description="Loading conversation" />
                      </div>
                    )}
                    {!loadingConversation && activeConversation && activeConversation.messages.length === 0 && (
                      <div className={styles.placeholder}>Send a question to begin.</div>
                    )}
                    {!loadingConversation && !activeConversation && (
                      <div className={styles.placeholder}>
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
                  </div>

                  <form className={styles.composer} onSubmit={handleComposerSubmit}>
                    <TextArea
                      labelText="Message"
                      hideLabel
                      value={messageInput}
                      onChange={(event) => setMessageInput(event.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder={
                        hasToken
                          ? 'Ask the assistant for help prioritising, summarising, or investigating work...'
                          : 'Sign in to chat with the assistant.'
                      }
                      disabled={!hasToken || sending}
                      rows={4}
                    />
                    <div className={styles.composerActions}>
                      <Button
                        type="submit"
                        kind="primary"
                        renderIcon={Send}
                        disabled={!canSend}
                      >
                        {sending ? 'Sending…' : 'Send'}
                      </Button>
                    </div>
                  </form>
                </Stack>
              </Tile>
            </Layer>

            <Layer level={1}>
              <Tile className={styles.sidebar}>
                <div className={styles.sidebarHeader}>
                  <h4 className={`${styles.sidebarTitle} cds--heading-03`}>Conversation history</h4>
                  <p className={styles.sidebarMeta}>
                    {loadingList
                      ? 'Loading conversations...'
                      : `${conversations.length} ${conversations.length === 1 ? 'thread' : 'threads'}`}
                  </p>
                </div>
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={handleRefresh}
                  type="button"
                  disabled={loadingList}
                  className={styles.refresh}
                >
                  Refresh
                </Button>
                {loadingList && conversations.length === 0 ? (
                  <div className={styles.placeholder}>
                    <InlineLoading status="active" description="Loading conversations" />
                  </div>
                ) : !loadingList && conversations.length === 0 ? (
                  <div className={styles.emptyState}>
                    Start a new chat to capture questions and AI responses for later reference.
                  </div>
                ) : (
                  <ContainedList kind="on-page" className={styles.list}>
                    {conversations.map((conversation) => {
                      const isActive = conversation.id === selectedConversationId;
                      const title = conversation.title?.trim() || 'Untitled conversation';
                      return (
                        <ContainedListItem
                          key={conversation.id}
                          onClick={() => handleSelectConversation(conversation.id)}
                          className={`${styles.listItem} ${isActive ? styles.listItemActive : ''}`}
                        >
                          <div className={styles.listItemContent}>
                            <span className={styles.listItemTitle}>{title}</span>
                            {conversation.lastMessagePreview && (
                              <span className={styles.listItemPreview}>{conversation.lastMessagePreview}</span>
                            )}
                            <span className={styles.listItemTime}>
                              {formatRelative(conversation.updatedAt)}
                            </span>
                          </div>
                        </ContainedListItem>
                      );
                    })}
                  </ContainedList>
                )}
              </Tile>
            </Layer>
          </div>
        </Stack>
      </Tile>
    </Layer>
  );
}

function ChatMessage({ message, currentUser }: { message: AiMessageResponse; currentUser: string }) {
  const author =
    message.role === 'ASSISTANT' ? 'AI Assistant' : message.role === 'USER' ? currentUser : 'System';
  const roleClass =
    message.role === 'ASSISTANT'
      ? styles.messageAssistant
      : message.role === 'USER'
        ? styles.messageUser
        : styles.messageSystem;

  return (
    <article className={`${styles.message} ${roleClass}`}>
      <header className={styles.messageHeader}>
        <span className={styles.messageAuthor}>{author}</span>
        <span className={styles.messageTimestamp}>{formatTimestamp(message.createdAt)}</span>
      </header>
      <div className={styles.messageBody}>
        <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
          {message.content}
        </ReactMarkdown>
      </div>
    </article>
  );
}

const markdownComponents: Components = {
  a(props) {
    return <a {...props} target="_blank" rel="noopener noreferrer" />;
  },
};

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
