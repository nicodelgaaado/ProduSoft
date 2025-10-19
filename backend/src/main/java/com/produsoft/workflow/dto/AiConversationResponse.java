package com.produsoft.workflow.dto;

import java.time.Instant;
import java.util.List;

public record AiConversationResponse(
    Long id,
    String title,
    Instant createdAt,
    Instant updatedAt,
    List<AiMessageResponse> messages
) {
}
