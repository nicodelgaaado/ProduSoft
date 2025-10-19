package com.produsoft.workflow.dto;

public record AiChatResponse(
    String model,
    String role,
    String content
) {
}
