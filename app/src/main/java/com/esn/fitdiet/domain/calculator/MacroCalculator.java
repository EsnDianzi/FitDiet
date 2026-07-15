package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;

/**
 * Splits a target calorie budget into protein / carbohydrate / fat grams.
 *
 * Rules (scientific, gender x goal aware):
 * - Protein (g/kg): 减脂保肌/减脂 男2.0 女1.8; 增肌 男1.6-1.8(取1.7) 女1.6-2.0(取1.8); 维持 1.4
 * - Fat: 促睾强化 => 0.9-1.1 g/kg(男) / 0.8-1.0 g/kg(女), 取中值; 否则 目标热量x25% / 9
 * - Carbs: (目标热量 - 蛋白x4 - 脂肪x9) / 4, 下限 0
 */
public final class MacroCalculator {

    private static final double FAT_CALORIE_RATIO = 0.25;
    private static final double KCAL_PER_G_FAT = 9.0;
    private static final double KCAL_PER_G_CARB = 4.0;

    private MacroCalculator() { }

    public static MacroResult calculate(double targetCalories, double weightKg,
                                         Gender gender, Goal goal, boolean boostTestosterone) {
        double proteinG = proteinPerKg(gender, goal) * weightKg;
        double proteinKcal = proteinG * KCAL_PER_G_CARB;

        double fatG;
        // 文档 §3.1：仅男性促睾使用 1.0×体重；女性促睾应被忽略（TC-F1-11）
        if (boostTestosterone && gender == Gender.MALE) {
            fatG = 1.0 * weightKg;
        } else {
            fatG = (targetCalories * FAT_CALORIE_RATIO) / KCAL_PER_G_FAT;
        }
        double fatKcal = fatG * KCAL_PER_G_FAT;

        double remainingKcal = targetCalories - proteinKcal - fatKcal;
        double carbsG = remainingKcal > 0 ? remainingKcal / KCAL_PER_G_CARB : 0;

        return new MacroResult(proteinG, carbsG, fatG);
    }

    /** Protein target in g per kg of body weight, by gender x goal. */
    private static double proteinPerKg(Gender gender, Goal goal) {
        if (gender == Gender.MALE) {
            switch (goal) {
                case BULK:    return 1.7;   // 1.6-1.8 中值
                case MAINTAIN:return 1.4;
                default:      return 2.0;   // 减脂保肌 / 减脂
            }
        } else {
            switch (goal) {
                case BULK:    return 1.8;   // 1.6-2.0 中值
                case MAINTAIN:return 1.4;
                default:      return 1.8;   // 减脂保肌 / 减脂（文档 §3.1）
            }
        }
    }

    public static class MacroResult {
        private final double proteinG;
        private final double carbsG;
        private final double fatG;

        public MacroResult(double proteinG, double carbsG, double fatG) {
            this.proteinG = proteinG;
            this.carbsG = carbsG;
            this.fatG = fatG;
        }

        public double getProteinG() { return proteinG; }
        public double getCarbsG() { return carbsG; }
        public double getFatG() { return fatG; }
    }
}
