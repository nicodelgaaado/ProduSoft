package com.produsoft.workflow.dto;

import java.util.List;

public record WipSummaryResponse(
    long totalOrders,
    long completedOrders,
    long exceptionOrders,
    List<StageSummaryResponse> stages
) {}
