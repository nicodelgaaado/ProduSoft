package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotNull;

public record ReprioritizeOrderRequest(
    @NotNull(message = "Priority is required")
    Integer priority
) {}
