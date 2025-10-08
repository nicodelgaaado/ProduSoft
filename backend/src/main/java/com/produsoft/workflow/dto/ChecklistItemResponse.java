package com.produsoft.workflow.dto;

public record ChecklistItemResponse(
    String id,
    String label,
    boolean required,
    boolean completed
) {}
