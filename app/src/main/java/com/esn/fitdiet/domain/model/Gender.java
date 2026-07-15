package com.esn.fitdiet.domain.model;

/** User biological sex, used in BMR calculation. */
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
