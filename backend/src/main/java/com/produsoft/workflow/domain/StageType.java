package com.produsoft.workflow.domain;

import java.util.Optional;

public enum StageType {
    PREPARATION,
    ASSEMBLY,
    DELIVERY;

    private static final StageType[] VALUES = values();

    public Optional<StageType> next() {
        int idx = ordinal();
        return idx < VALUES.length - 1 ? Optional.of(VALUES[idx + 1]) : Optional.empty();
    }

    public Optional<StageType> previous() {
        int idx = ordinal();
        return idx > 0 ? Optional.of(VALUES[idx - 1]) : Optional.empty();
    }

    public static StageType fromString(String value) {
        return StageType.valueOf(value.toUpperCase());
    }
}
