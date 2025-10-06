package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record FlagStageExceptionRequest(
    @NotBlank(message = "Assignee is required")
    String assignee,
    @NotBlank(message = "Exception reason is required")
    String exceptionReason,
    String notes
) {}
