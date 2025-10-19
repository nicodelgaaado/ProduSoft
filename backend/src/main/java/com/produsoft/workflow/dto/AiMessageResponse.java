package com.produsoft.workflow.dto;

import com.produsoft.workflow.domain.AiMessageRole;
import java.time.Instant;

public record AiMessageResponse(
    Long id,
    AiMessageRole role,
    String content,
    Instant createdAt
) {
}
