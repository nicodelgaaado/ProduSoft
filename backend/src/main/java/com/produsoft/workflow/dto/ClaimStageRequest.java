package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimStageRequest(
    @NotBlank(message = "Assignee is required")
    String assignee
) {}
