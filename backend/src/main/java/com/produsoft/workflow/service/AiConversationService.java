package com.produsoft.workflow.service;

import com.produsoft.workflow.domain.AiConversation;
import com.produsoft.workflow.domain.AiMessage;
import com.produsoft.workflow.domain.AiMessageRole;
import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.dto.AiChatResponse;
import com.produsoft.workflow.dto.AiConversationMapper;
import com.produsoft.workflow.dto.AiConversationSummaryResponse;
import com.produsoft.workflow.dto.CreateConversationRequest;
import com.produsoft.workflow.dto.SendMessageRequest;
import com.produsoft.workflow.exception.AiClientException;
import com.produsoft.workflow.exception.InvalidStageActionException;
import com.produsoft.workflow.exception.NotFoundException;
import com.produsoft.workflow.repository.AiConversationRepository;
import com.produsoft.workflow.repository.AiMessageRepository;
import com.produsoft.workflow.dto.AiConversationResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Transactional
public class AiConversationService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiChatService aiChatService;
    private final AiConversationMapper mapper;
    private final AiContextService contextService;
    private final TransactionTemplate transactionTemplate;

    public AiConversationService(AiConversationRepository conversationRepository,
                                 AiMessageRepository messageRepository,
                                 AiChatService aiChatService,
                                 AiConversationMapper mapper,
                                 AiContextService contextService,
                                 PlatformTransactionManager transactionManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiChatService = aiChatService;
        this.mapper = mapper;
        this.contextService = contextService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public List<AiConversationSummaryResponse> listConversations(String username) {
        return conversationRepository.findByCreatedByOrderByUpdatedAtDesc(username).stream()
            .map(conversation -> mapper.toSummary(
                conversation,
                messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())))
            .toList();
    }

    public AiConversationResponse getConversation(String username, Long conversationId) {
        AiConversation conversation = conversationRepository.findWithMessagesByIdAndCreatedBy(conversationId, username)
            .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        return mapper.toResponse(conversation);
    }

    public AiConversationResponse createConversation(String username, CreateConversationRequest request) {
        AiConversation conversation = new AiConversation();
        conversation.setCreatedBy(username);
        conversation.setTitle(normalizeTitle(request.title()));
        conversationRepository.save(conversation);

        if (StringUtils.hasText(request.initialMessage())) {
            SendMessageRequest sendMessageRequest = new SendMessageRequest(request.initialMessage());
            return sendMessage(username, conversation.getId(), sendMessageRequest);
        }

        return mapper.toResponse(conversation);
    }

    public AiConversationResponse renameConversation(String username, Long conversationId, String newTitle) {
        AiConversation conversation = conversationRepository.findWithMessagesByIdAndCreatedBy(conversationId, username)
            .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        conversation.setTitle(normalizeTitle(newTitle));
        conversation.touch();
        return mapper.toResponse(conversation);
    }

    public AiConversationResponse sendMessage(String username, Long conversationId, SendMessageRequest request) {
        AiConversation conversation = conversationRepository.findWithMessagesByIdAndCreatedBy(conversationId, username)
            .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        String content = request.content().trim();
        if (content.length() > 4000) {
            throw new InvalidStageActionException("Message is too long. Limit to 4000 characters.");
        }

        AiMessage userMessage = new AiMessage();
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(content);
        conversation.addMessage(userMessage);
        messageRepository.save(userMessage);

        if (conversation.getTitle() == null) {
            conversation.setTitle(deriveTitle(content));
        }

        List<AiChatRequest.Message> history = buildHistoryMessages(conversation);
        List<AiChatRequest.Message> messages = new ArrayList<>();
        messages.add(contextService.buildContextMessage());
        messages.addAll(history);
        AiChatResponse aiResponse = aiChatService.chat(new AiChatRequest(
            null,
            messages,
            Boolean.FALSE
        ));

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setRole(AiMessageRole.ASSISTANT);
        assistantMessage.setContent(aiResponse.content());
        conversation.addMessage(assistantMessage);
        messageRepository.save(assistantMessage);

        conversationRepository.save(conversation);
        return mapper.toResponse(conversation);
    }

    public SseEmitter streamMessage(String username, Long conversationId, SendMessageRequest request) {
        AiConversation conversation = conversationRepository.findWithMessagesByIdAndCreatedBy(conversationId, username)
            .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        String content = request.content().trim();
        if (content.length() > 4000) {
            throw new InvalidStageActionException("Message is too long. Limit to 4000 characters.");
        }

        AiMessage userMessage = new AiMessage();
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(content);
        conversation.addMessage(userMessage);
        messageRepository.save(userMessage);

        if (conversation.getTitle() == null) {
            conversation.setTitle(deriveTitle(content));
        }

        conversationRepository.save(conversation);

        List<AiChatRequest.Message> history = buildHistoryMessages(conversation);
        List<AiChatRequest.Message> messages = new ArrayList<>();
        messages.add(contextService.buildContextMessage());
        messages.addAll(history);

        AiChatRequest chatRequest = new AiChatRequest(
            null,
            messages,
            Boolean.TRUE
        );

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean emitterOpen = new AtomicBoolean(true);
        emitter.onCompletion(() -> emitterOpen.set(false));
        emitter.onTimeout(() -> emitterOpen.set(false));
        emitter.onError(error -> emitterOpen.set(false));

        CompletableFuture.runAsync(() -> {
            StringBuilder assistantBuilder = new StringBuilder();
            try {
                String assistantReply = aiChatService.chatStream(chatRequest, delta -> {
                    if (delta == null) {
                        return;
                    }
                    assistantBuilder.append(delta);
                    safeSendToken(emitter, emitterOpen, delta);
                });
                if (!StringUtils.hasText(assistantReply)) {
                    throw new AiClientException("Received empty response from Ollama.");
                }
                AiConversationResponse response = transactionTemplate.execute(status -> {
                    AiConversation refreshed = conversationRepository
                        .findWithMessagesByIdAndCreatedBy(conversationId, username)
                        .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
                    AiMessage assistantMessage = new AiMessage();
                    assistantMessage.setRole(AiMessageRole.ASSISTANT);
                    assistantMessage.setContent(assistantReply);
                    refreshed.addMessage(assistantMessage);
                    messageRepository.save(assistantMessage);
                    conversationRepository.save(refreshed);
                    return mapper.toResponse(refreshed);
                });
                safeSendConversation(emitter, emitterOpen, response);
                completeQuietly(emitter);
            } catch (Exception ex) {
                if (emitterOpen.get()) {
                    handleStreamingError(emitter, ex);
                    return;
                }
                completeQuietly(emitter);
            }
        });

        return emitter;
    }

    public void deleteConversation(String username, Long conversationId) {
        AiConversation conversation = conversationRepository.findByIdAndCreatedBy(conversationId, username)
            .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        conversationRepository.delete(conversation);
    }

    private List<AiChatRequest.Message> buildHistoryMessages(AiConversation conversation) {
        List<AiMessage> sortedMessages = conversation.getMessages().stream()
            .sorted(Comparator.comparing(AiMessage::getCreatedAt))
            .toList();

        int fromIndex = Math.max(0, sortedMessages.size() - MAX_HISTORY_MESSAGES);
        List<AiChatRequest.Message> history = new ArrayList<>();
        for (AiMessage message : sortedMessages.subList(fromIndex, sortedMessages.size())) {
            history.add(new AiChatRequest.Message(
                switch (message.getRole()) {
                    case USER -> "user";
                    case ASSISTANT -> "assistant";
                    case SYSTEM -> "system";
                },
                message.getContent()
            ));
        }
        return history;
    }

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        return title.trim();
    }

    private String deriveTitle(String message) {
        if (!StringUtils.hasText(message)) {
            return "Conversation";
        }
        String trimmed = message.trim();
        return trimmed.length() <= 60 ? trimmed : trimmed.substring(0, 57) + "...";
    }

    private void safeSendToken(SseEmitter emitter, AtomicBoolean emitterOpen, String delta) {
        if (!emitterOpen.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("token").data(Map.of("delta", delta)));
        } catch (IOException | IllegalStateException ex) {
            emitterOpen.set(false);
        }
    }

    private void safeSendConversation(SseEmitter emitter, AtomicBoolean emitterOpen, AiConversationResponse response) {
        if (!emitterOpen.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("conversation").data(response));
        } catch (IOException | IllegalStateException ex) {
            emitterOpen.set(false);
        }
    }

    private void handleStreamingError(SseEmitter emitter, Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Streaming chat failed.";
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
        } catch (IOException | IllegalStateException ignored) {
            // If we cannot notify the client, there's nothing else to do.
        }
        emitter.completeWithError(ex);
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // Already completed by the client.
        }
    }
}
