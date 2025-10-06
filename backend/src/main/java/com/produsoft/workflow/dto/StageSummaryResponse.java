package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.StageType;

public record StageSummaryResponse(
    StageType stage,
    long pending,
    long inProgress,
    long exceptions,
    long completed
) {}
