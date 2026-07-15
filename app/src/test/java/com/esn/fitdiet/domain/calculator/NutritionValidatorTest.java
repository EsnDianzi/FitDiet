package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 输入校验层（测试端 TC-F1-12）：非法年龄/身高/体重应抛 IllegalArgumentException。
 */
public class NutritionValidatorTest {

    @Test
    public void acceptsValidBoundary() {
        // 边界合法值不应抛错
        NutritionCalculator.validateInputs(Gender.MALE, NutritionCalculator.MIN_AGE, 160, 60);
        NutritionCalculator.validateInputs(Gender.FEMALE, 50, NutritionCalculator.MAX_HEIGHT_CM, 70);
        NutritionCalculator.validateInputs(Gender.MALE, 30, 175, NutritionCalculator.MIN_WEIGHT_KG);
    }

    @Test
    public void rejectsInvalidAge() {
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 0, 175, 70));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 13, 175, 70));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 200, 175, 70));
    }

    @Test
    public void rejectsInvalidHeight() {
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 0, 70));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 119, 70));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 300, 70));
    }

    @Test
    public void rejectsInvalidWeight() {
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 175, 0));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 175, 10));
        assertThrows(() -> NutritionCalculator.validateInputs(Gender.MALE, 30, 175, 999));
    }

    @Test
    public void computePropagatesValidation() {
        try {
            NutritionCalculator.compute(Gender.MALE, 0, 175, 70, ActivityLevel.MODERATE, Goal.CUT, false);
            fail("应抛出非法输入异常");
        } catch (IllegalArgumentException expected) {
            assertEquals(IllegalArgumentException.class, expected.getClass());
        }
    }

    private static void assertThrows(ThrowingRunnable r) {
        try {
            r.run();
            fail("未抛出异常");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private interface ThrowingRunnable {
        void run();
    }
}
