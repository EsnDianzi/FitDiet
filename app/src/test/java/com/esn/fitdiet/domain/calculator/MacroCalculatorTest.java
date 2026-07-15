package com.esn.fitdiet.domain.calculator;

import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 宏量计算断言：女性促睾应被忽略（测试端 TC-F1-11，文档 §3.1）。
 */
public class MacroCalculatorTest {

    @Test
    public void femaleBoostIsIgnored() {
        double target = 2000.0;
        double weight = 72.5;

        MacroCalculator.MacroResult boosted =
                MacroCalculator.calculate(target, weight, Gender.FEMALE, Goal.RECOMP, true);
        MacroCalculator.MacroResult normal =
                MacroCalculator.calculate(target, weight, Gender.FEMALE, Goal.RECOMP, false);

        // 女性无论促睾与否，脂肪均走 目标×25%÷9，蛋白一致
        assertEquals("女性蛋白不受促睾影响", normal.getProteinG(), boosted.getProteinG(), 0.0001);
        assertEquals("女性脂肪不受促睾影响", normal.getFatG(), boosted.getFatG(), 0.0001);

        // 明确验证女性不走 1.0×体重
        assertEquals("女性脂肪=目标×25%÷9", target * 0.25 / 9.0, boosted.getFatG(), 0.0001);
    }

    @Test
    public void maleBoostUsesWeight() {
        double target = 2000.0;
        double weight = 72.5;
        MacroCalculator.MacroResult r =
                MacroCalculator.calculate(target, weight, Gender.MALE, Goal.RECOMP, true);
        assertEquals("男性促睾脂肪=1.0×体重", weight, r.getFatG(), 0.0001);
    }

    @Test
    public void carbsLowerBoundProtected() {
        // 极端：极低目标 + 高体重蛋白，碳水不应为负
        MacroCalculator.MacroResult r =
                MacroCalculator.calculate(800.0, 150.0, Gender.MALE, Goal.RECOMP, false);
        assertEquals("蛋白=2.0×150", 300.0, r.getProteinG(), 0.0001);
        assertEquals(0.0, r.getCarbsG(), 0.0001);
    }
}
