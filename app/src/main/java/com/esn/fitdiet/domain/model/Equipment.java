package com.esn.fitdiet.domain.model;

/** 训练器械枚举，与 MonsterDef.equipmentNeeded / UserProfile.equipment 对应。 */
public enum Equipment {
    BODYWEIGHT("无器械/徒手"),
    DUMBBELL("哑铃"),
    BARBELL("杠铃"),
    BAND("弹力带"),
    KETTLEBELL("壶铃"),
    MACHINE("器械");

    private final String label;

    Equipment(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 由内置素材/PRD 食材表风格的字符串映射回枚举，未知值回退徒手。 */
    public static Equipment fromLabel(String s) {
        if (s == null) return BODYWEIGHT;
        for (Equipment e : values()) {
            if (e.label.equals(s) || e.name().equalsIgnoreCase(s)) return e;
        }
        return BODYWEIGHT;
    }
}
