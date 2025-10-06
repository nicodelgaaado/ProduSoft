package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record SupervisorDecisionRequest(
    @NotBlank(message = "Approver name is required")
    String approver,
    String notes
) {}
