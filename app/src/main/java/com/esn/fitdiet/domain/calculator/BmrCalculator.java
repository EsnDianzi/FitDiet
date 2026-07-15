package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.Gender;

/**
 * Basal Metabolic Rate using the Mifflin-St Jeor equation.
 * Male:   10*weight + 6.25*height - 5*age + 5
 * Female: 10*weight + 6.25*height - 5*age - 161
 */
public final class BmrCalculator {

    private BmrCalculator() { }

    public static double calculate(Gender gender, int age, double heightCm, double weightKg) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return gender == Gender.MALE ? base + 5 : base - 161;
    }
}
