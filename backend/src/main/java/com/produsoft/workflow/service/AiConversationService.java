package com.produsoft.workflow.service;

import com.produsoft.workflow.domain.AiConversation;
import com.produsoft.workflow.domain.AiMessage;
import com.produsoft.workflow.domain.AiMessageRole;
import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.dto.AiChatResponse;
import com.produsoft.workflow.dto.AiConversationMapper;
import com.produsoft.workflow.dto.AiConversationResponse;
import com.produsoft.workflow.dto.AiConversationSummaryResponse;
import com.produsoft.workflow.dto.CreateConversationRequest;
import com.produsoft.workflow.dto.SendMessageRequest;
import com.produsoft.workflow.exception.InvalidStageActionException;
import com.produsoft.workflow.exception.NotFoundException;
import com.produsoft.workflow.repository.AiConversationRepository;
import com.produsoft.workflow.repository.AiMessageRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AiConversationService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiChatService aiChatService;
    private final AiConversationMapper mapper;
    private final AiContextService contextService;

    public AiConversationService(AiConversationRepository conversationRepository,
                                 AiMessageRepository messageRepository,
                                 AiChatService aiChatService,
                                 AiConversationMapper mapper,
                                 AiContextService contextService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiChatService = aiChatService;
        this.mapper = mapper;
        this.contextService = contextService;
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
}
