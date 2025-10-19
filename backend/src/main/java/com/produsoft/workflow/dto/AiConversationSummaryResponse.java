package com.produsoft.workflow.dto;

import java.time.Instant;

public record AiConversationSummaryResponse(
    Long id,
    String title,
    Instant createdAt,
    Instant updatedAt,
    String lastMessagePreview
) {
}
