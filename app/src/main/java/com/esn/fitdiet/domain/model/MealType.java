package com.esn.fitdiet.domain.model;

/** Meal category for a food log entry. */
public enum MealType {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐");

    private final String label;

    MealType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
