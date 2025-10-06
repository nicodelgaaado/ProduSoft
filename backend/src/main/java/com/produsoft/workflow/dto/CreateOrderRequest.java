package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
    @NotBlank(message = "Order number is required")
    String orderNumber,
    Integer priority,
    String notes
) {}
