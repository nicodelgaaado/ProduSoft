package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConversationTitleRequest(
    @NotBlank(message = "title must not be blank")
    @Size(max = 255, message = "title must be 255 characters or fewer")
    String title
) {
}
