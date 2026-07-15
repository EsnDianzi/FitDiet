package com.esn.fitdiet.data.local.entity;

import androidx.room.Ignore;

/** Aggregated nutrition sums for a given day (from food logs). */
public class NutritionTotals {
    public double calories;
    public double protein;
    public double carbs;
    public double fat;

    public NutritionTotals() { }

    @Ignore
    public NutritionTotals(double calories, double protein, double carbs, double fat) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }
}
