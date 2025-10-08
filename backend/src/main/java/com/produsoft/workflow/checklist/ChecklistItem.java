package com.produsoft.workflow.checklist;

public record ChecklistItem(
    String id,
    String label,
    boolean required,
    boolean completed
) {}
