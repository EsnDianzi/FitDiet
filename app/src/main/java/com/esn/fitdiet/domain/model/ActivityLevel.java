package com.esn.fitdiet.domain.model;

/** Physical activity level, mapped to a TDEE multiplier. */
public enum ActivityLevel {
    SEDENTARY(1.2, "久坐（几乎不运动）"),
    LIGHT(1.375, "轻度（每周1-3次）"),
    MODERATE(1.55, "中度（每周3-5次）"),
    ACTIVE(1.725, "活跃（每周6-7次）"),
    VERY_ACTIVE(1.9, "极高（体力劳动/运动员）");

    private final double factor;
    private final String label;

    ActivityLevel(double factor, String label) {
        this.factor = factor;
        this.label = label;
    }

    public double getFactor() {
        return factor;
    }

    public String getLabel() {
        return label;
    }
}
