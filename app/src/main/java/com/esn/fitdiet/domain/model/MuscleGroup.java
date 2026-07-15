package com.esn.fitdiet.domain.model;

/** 肌群枚举，对应 ExerciseLog.muscleGroup / MonsterDef.muscleGroup 文案。 */
public enum MuscleGroup {
    CHEST("胸"),
    BACK("背"),
    LEGS("腿"),
    SHOULDERS("肩"),
    ARMS("手臂"),
    CORE("核心"),
    FULL_BODY("全身");

    private final String label;

    MuscleGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 由中文文案映射回枚举（用于预置 Boss 的肌群字段）。 */
    public static MuscleGroup fromLabel(String s) {
        if (s == null) return null;
        for (MuscleGroup m : values()) {
            if (m.label.equals(s)) return m;
        }
        return null;
    }
}
