package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.AiConversation;
import com.produsoft.workflow.domain.AiMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AiConversationMapper {

    public AiConversationSummaryResponse toSummary(AiConversation conversation, Optional<AiMessage> lastMessage) {
        String preview = lastMessage.map(AiMessage::getContent)
            .map(content -> content.length() > 120 ? content.substring(0, 117) + "..." : content)
            .orElse(null);
        return new AiConversationSummaryResponse(
            conversation.getId(),
            conversation.getTitle(),
            conversation.getCreatedAt(),
            conversation.getUpdatedAt(),
            preview
        );
    }

    public AiConversationResponse toResponse(AiConversation conversation) {
        List<AiMessageResponse> messages = conversation.getMessages().stream()
            .sorted(Comparator.comparing(AiMessage::getCreatedAt))
            .map(message -> new AiMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()))
            .toList();
        return new AiConversationResponse(
            conversation.getId(),
            conversation.getTitle(),
            conversation.getCreatedAt(),
            conversation.getUpdatedAt(),
            messages
        );
    }
}
