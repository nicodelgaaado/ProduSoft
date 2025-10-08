package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import java.time.Instant;
import java.util.List;

public record WorkQueueItemResponse(
    Long orderId,
    String orderNumber,
    Integer priority,
    StageType stage,
    StageState stageState,
    StageType currentStage,
    StageState overallState,
    String assignee,
    Instant claimedAt,
    Instant updatedAt,
    String exceptionReason,
    String notes,
    List<ChecklistItemResponse> checklist
) {}
