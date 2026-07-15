package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.NutritionTarget;

/**
 * Orchestrates BMR -> TDEE -> target calories -> macro split.
 * Pure functions, no Android dependencies, easy to unit test.
 */
public final class NutritionCalculator {

    // 合法输入边界（文档 §3.1 / 测试端 TC-F1-12）：钳制或抛错防脏数据
    public static final int MIN_AGE = 14;
    public static final int MAX_AGE = 120;
    public static final double MIN_HEIGHT_CM = 120.0;
    public static final double MAX_HEIGHT_CM = 250.0;
    public static final double MIN_WEIGHT_KG = 30.0;
    public static final double MAX_WEIGHT_KG = 400.0;

    private NutritionCalculator() { }

    /** 输入校验：非法年龄/身高/体重抛 IllegalArgumentException（TC-F1-12）。 */
    public static void validateInputs(Gender gender, int age, double heightCm, double weightKg) {
        if (gender == null) throw new IllegalArgumentException("gender 不能为空");
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException("年龄应在 " + MIN_AGE + "~" + MAX_AGE + " 之间");
        }
        if (heightCm < MIN_HEIGHT_CM || heightCm > MAX_HEIGHT_CM) {
            throw new IllegalArgumentException("身高应在 " + MIN_HEIGHT_CM + "~" + MAX_HEIGHT_CM + " cm 之间");
        }
        if (weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException("体重应在 " + MIN_WEIGHT_KG + "~" + MAX_WEIGHT_KG + " kg 之间");
        }
    }

    public static NutritionTarget compute(Gender gender, int age, double heightCm,
                                           double weightKg, ActivityLevel level, Goal goal,
                                           boolean boostTestosterone) {
        validateInputs(gender, age, heightCm, weightKg);
        double bmr = BmrCalculator.calculate(gender, age, heightCm, weightKg);
        double tdee = TdeeCalculator.calculate(bmr, level);
        double targetCalories = tdee + goal.getCalorieDelta();

        MacroCalculator.MacroResult macros =
                MacroCalculator.calculate(targetCalories, weightKg, gender, goal, boostTestosterone);

        return new NutritionTarget(bmr, tdee, targetCalories,
                macros.getProteinG(), macros.getCarbsG(), macros.getFatG());
    }
}
