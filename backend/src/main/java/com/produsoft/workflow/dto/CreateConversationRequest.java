package com.produsoft.workflow.dto;

import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
    @Size(max = 255, message = "title must be 255 characters or fewer")
    String title,
    @Size(max = 4000, message = "initialMessage must be 4000 characters or fewer")
    String initialMessage
) {
}
