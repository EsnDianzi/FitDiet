package com.esn.fitdiet.domain.model;

/** Computed daily nutrition targets derived from the user profile. */
public class NutritionTarget {
    private final double bmr;
    private final double tdee;
    private final double targetCalories;
    private final double proteinG;
    private final double carbsG;
    private final double fatG;

    public NutritionTarget(double bmr, double tdee, double targetCalories,
                           double proteinG, double carbsG, double fatG) {
        this.bmr = bmr;
        this.tdee = tdee;
        this.targetCalories = targetCalories;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
    }

    public double getBmr() { return bmr; }
    public double getTdee() { return tdee; }
    public double getTargetCalories() { return targetCalories; }
    public double getProteinG() { return proteinG; }
    public double getCarbsG() { return carbsG; }
    public double getFatG() { return fatG; }
}
