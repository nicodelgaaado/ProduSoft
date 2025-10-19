package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
    @NotBlank(message = "content must not be blank")
    String content
) {
}
