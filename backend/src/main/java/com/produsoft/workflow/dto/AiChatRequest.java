package com.produsoft.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AiChatRequest(
    String model,
    @NotEmpty(message = "messages must not be empty")
    List<@Valid Message> messages,
    Boolean stream
) {

    public AiChatRequest {
        messages = List.copyOf(messages == null ? List.of() : messages);
    }

    public boolean streamRequested() {
        return Boolean.TRUE.equals(stream);
    }

    public record Message(
        @NotBlank(message = "role must not be blank")
        String role,
        @NotBlank(message = "content must not be blank")
        String content
    ) {
    }
}
