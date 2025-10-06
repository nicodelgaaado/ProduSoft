package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    Integer priority,
    StageType currentStage,
    StageState overallState,
    Instant createdAt,
    Instant updatedAt,
    String notes,
    List<OrderStageStatusResponse> stages
) {}
