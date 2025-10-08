package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.StageState;
import com.produsoft.workflow.domain.StageType;
import java.time.Instant;
import java.util.List;

public record OrderStageStatusResponse(
    Long id,
    StageType stage,
    StageState state,
    String assignee,
    Instant claimedAt,
    Instant startedAt,
    Instant completedAt,
    Long serviceTimeMinutes,
    String notes,
    String exceptionReason,
    String supervisorNotes,
    String approvedBy,
    Instant updatedAt,
    List<ChecklistItemResponse> checklist
) {}
