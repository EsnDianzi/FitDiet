package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.ActivityLevel;

/** Total Daily Energy Expenditure = BMR * activity factor. */
public final class TdeeCalculator {

    private TdeeCalculator() { }

    public static double calculate(double bmr, ActivityLevel level) {
        return bmr * level.getFactor();
    }
}
