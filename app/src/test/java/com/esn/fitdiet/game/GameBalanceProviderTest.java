package com.esn.fitdiet.game;

import com.google.gson.Gson;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * GameBalanceProvider / GameBalanceConfig 纯 JVM 验证（无需 Android Context）。
 * - fromDefault() 与 GameBalance 编译期常量一致（离线兜底）
 * - assets/game_balance.json 可被正常解析且与默认值一致
 * - 连击倍率 2/3/4 肌群 → 1.15/1.30/1.45
 */
public class GameBalanceProviderTest {

    @Test
    public void defaultConfigMatchesConstants() {
        GameBalanceConfig c = GameBalanceConfig.fromDefault();
        assertEquals(GameBalance.EXP_PER_ACTION, c.expPerAction);
        assertEquals(GameBalance.EXP_KILL_BONUS, c.expKillBonus);
        assertEquals(GameBalance.COMBO_BONUS_PER_EXTRA_GROUP, c.comboBonusPerExtraGroup, 0.0001);
        assertEquals(GameBalance.COMBO_BONUS_MAX, c.comboBonusMax, 0.0001);
        assertEquals(GameBalance.STREAK_BONUS_PER_DAY, c.streakBonusPerDay);
        assertEquals(GameBalance.STREAK_BONUS_CAP, c.streakBonusCap);
        assertEquals(GameBalance.COIN_PER_EXP, c.coinPerExp, 0.0001);
        assertEquals(GameBalance.LEVEL_BASE, c.levelBase);
        assertEquals(GameBalance.LEVEL_GROWTH, c.levelGrowth);
        assertEquals(GameBalance.LEVEL_EXP_CAP, c.levelExpCap);
    }

    @Test
    public void assetJsonIsConsistent() throws Exception {
        // 测试工作目录为模块根 app/，asset 位于 src/main/assets
        File json = new File("src/main/assets/game_balance.json");
        assertTrue("asset exists: " + json.getAbsolutePath(), json.exists());
        String text = new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
        GameBalanceConfig c = new Gson().fromJson(text, GameBalanceConfig.class);
        GameBalanceConfig def = GameBalanceConfig.fromDefault();
        assertEquals(def.expPerAction, c.expPerAction);
        assertEquals(def.expKillBonus, c.expKillBonus);
        assertEquals(def.coinPerExp, c.coinPerExp, 0.0001);
        assertEquals(def.levelBase, c.levelBase);
    }

    @Test
    public void comboMultiplierSteps() {
        GameBalanceConfig c = GameBalance.DEFAULT;
        assertEquals(1.0, BattleManager.comboMultiplier(1, c), 0.0001);
        assertEquals(1.15, BattleManager.comboMultiplier(2, c), 0.0001);
        assertEquals(1.30, BattleManager.comboMultiplier(3, c), 0.0001);
        assertEquals(1.45, BattleManager.comboMultiplier(4, c), 0.0001);
        // 封顶：5 肌群仍为 1.45
        assertEquals(1.45, BattleManager.comboMultiplier(5, c), 0.0001);
    }

    @Test
    public void streakBonusOnlyOnFirstKill() {
        GameBalanceConfig c = GameBalance.DEFAULT;
        assertEquals(0, BattleManager.streakBonusExp(false, 5, c));
        assertEquals(5, BattleManager.streakBonusExp(true, 1, c));    // min(1,30)*5
        assertEquals(150, BattleManager.streakBonusExp(true, 30, c)); // min(30,30)*5
        assertEquals(150, BattleManager.streakBonusExp(true, 99, c)); // 封顶 30→150
    }
}
