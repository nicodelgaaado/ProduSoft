package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChecklistItemRequest(
    @NotBlank(message = "Task id is required")
    String taskId,
    boolean completed
) {}
