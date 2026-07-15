package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.NutritionTarget;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * F1 营养计算参数化全量断言（测试端 TC-F1-01~10）。
 * 期望值由公式推导（bmr/protein/fat 容差 0.5；carbs 下限 0）。
 */
@RunWith(Parameterized.class)
public class NutritionCalculatorTest {

    @Parameterized.Parameter(0)
    public String name;
    @Parameterized.Parameter(1)
    public Gender gender;
    @Parameterized.Parameter(2)
    public int age;
    @Parameterized.Parameter(3)
    public double height;
    @Parameterized.Parameter(4)
    public double weight;
    @Parameterized.Parameter(5)
    public ActivityLevel activity;
    @Parameterized.Parameter(6)
    public Goal goal;
    @Parameterized.Parameter(7)
    public boolean boost;
    @Parameterized.Parameter(8)
    public double expectBmr;
    @Parameterized.Parameter(9)
    public double expectProtein;
    @Parameterized.Parameter(10)
    public double expectFat;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // TC-F1-01 / TC-F1-02
                {"男35/179/72.5/减脂保肌/促睾", Gender.MALE, 35, 179.0, 72.5, ActivityLevel.MODERATE, Goal.RECOMP, true, 1673.75, 145.0, 72.5},
                // TC-F1-03
                {"女28/165/55/减脂保肌/关", Gender.FEMALE, 28, 165.0, 55.0, ActivityLevel.LIGHT, Goal.RECOMP, false, 1280.25, 99.0, 40.57},
                // TC-F1-04
                {"男30/175/80/增肌/促睾", Gender.MALE, 30, 175.0, 80.0, ActivityLevel.ACTIVE, Goal.BULK, true, 1748.75, 136.0, 80.0},
                // TC-F1-05
                {"女30/175/80/增肌/关", Gender.FEMALE, 30, 175.0, 80.0, ActivityLevel.ACTIVE, Goal.BULK, false, 1582.75, 144.0, 84.17},
                // TC-F1-06
                {"男40/180/90/维持/关", Gender.MALE, 40, 180.0, 90.0, ActivityLevel.SEDENTARY, Goal.MAINTAIN, false, 1830.0, 126.0, 61.0},
                // TC-F1-07
                {"男20/140/40/减脂/关/极高", Gender.MALE, 20, 140.0, 40.0, ActivityLevel.VERY_ACTIVE, Goal.CUT, false, 1180.0, 80.0, 48.39},
                // TC-F1-08
                {"男60/210/150/减脂保肌/促睾", Gender.MALE, 60, 210.0, 150.0, ActivityLevel.MODERATE, Goal.RECOMP, true, 2517.5, 300.0, 150.0},
                // TC-F1-09
                {"女25/160/45/减脂/关", Gender.FEMALE, 25, 160.0, 45.0, ActivityLevel.MODERATE, Goal.CUT, false, 1164.0, 81.0, 36.23},
                // TC-F1-10 同 F1-01 但关促睾 → 脂肪改用 目标×25%
                {"男35/179/72.5/减脂保肌/关", Gender.MALE, 35, 179.0, 72.5, ActivityLevel.MODERATE, Goal.RECOMP, false, 1673.75, 145.0, 63.73},
        });
    }

    @Test
    public void compute_matchesFormula() {
        NutritionTarget t = NutritionCalculator.compute(gender, age, height, weight, activity, goal, boost);
        assertEquals(name + " BMR", expectBmr, t.getBmr(), 0.5);
        assertEquals(name + " protein", expectProtein, t.getProteinG(), 0.5);
        assertEquals(name + " fat", expectFat, t.getFatG(), 0.5);
        assertTrue(name + " carbs>=0", t.getCarbsG() >= 0);
    }
}
