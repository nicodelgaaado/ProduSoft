package com.produsoft.workflow.checklist;

public record ChecklistTaskDefinition(
    String id,
    String label,
    boolean required
) {}
