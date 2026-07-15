package com.esn.fitdiet.domain.model;

/**
 * Core fitness goal. Defines the calorie delta applied to TDEE.
 * Protein / fat split is resolved by MacroCalculator using goal x gender x boostTestosterone.
 */
public enum Goal {
    RECOMP("减脂保肌", -300),
    CUT("减脂", -500),
    BULK("增肌", 300),
    MAINTAIN("维持", 0);

    private final String label;
    /** Calorie adjustment relative to TDEE (can be negative). */
    private final int calorieDelta;

    Goal(String label, int calorieDelta) {
        this.label = label;
        this.calorieDelta = calorieDelta;
    }

    public String getLabel() {
        return label;
    }

    public int getCalorieDelta() {
        return calorieDelta;
    }
}
