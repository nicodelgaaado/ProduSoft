package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteStageRequest(
    @NotBlank(message = "Assignee is required")
    String assignee,
    Long serviceTimeMinutes,
    String notes
) {}
