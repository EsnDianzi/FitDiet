package com.esn.fitdiet.domain.model;

/** Origin of a food log entry. */
public enum FoodSource {
    MANUAL("手动录入"),
    AI("AI识别");

    private final String label;

    FoodSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
