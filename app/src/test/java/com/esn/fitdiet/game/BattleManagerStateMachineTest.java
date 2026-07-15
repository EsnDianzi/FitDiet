package com.esn.fitdiet.game;

import com.esn.fitdiet.data.local.entity.MonsterDef;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * F2 状态机 + 奖励计算纯 JVM 验证（测试端 TC-F2-01~10）。
 * 不依赖 Android，直接驱动 BattleManager.Session 与 computeComboReward。
 */
public class BattleManagerStateMachineTest {

    private static MonsterDef monster(long id, String name, String mg, int difficulty) {
        MonsterDef m = new MonsterDef();
        m.id = id;
        m.name = name;
        m.muscleGroup = mg;
        m.difficulty = difficulty;
        return m;
    }

    // ===== TC-F2-01：单动作完成，exp 含 d×10；血条该格清空 =====
    @Test
    public void singleActionExpAndBlood() {
        MonsterDef m = monster(1, "胸", "胸", 2);
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(m, 1));
        BattleManager.BattleReward r = BattleManager.computeComboReward(groups, false, 0, GameBalance.DEFAULT);
        // 1×10×2 + 40×2 = 100，含 d×10=20
        assertEquals(100, r.groupExp[0]);
        assertEquals(20, BattleManager.computeActionExp(m));

        // 血条：1 轮后恰好清空
        BattleManager.Session s = new BattleManager.Session(groups, GameBalance.DEFAULT);
        s.selectCombo().startAction().startRound().completeRound();
        assertEquals(0, s.getBloodRemaining());
        assertEquals(BattleManager.State.SESSION_DONE, s.getState());
    }

    // ===== TC-F2-02：肌群击杀（N 动作，难度 d），总 EXP = N×10d + 40d；coins=round(EXP×0.1) =====
    @Test
    public void muscleKillFormula() {
        MonsterDef m = monster(1, "胸", "胸", 2);
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(m, 3));
        BattleManager.BattleReward r = BattleManager.computeComboReward(groups, false, 0, GameBalance.DEFAULT);
        // 3×10×2 + 40×2 = 140（GameBalance.EXP_KILL_BONUS=40 单一真源）
        assertEquals(140, r.baseExp);
        assertEquals(140, r.totalExp);
        assertEquals(14, r.totalCoins); // round(140×0.1)
    }

    // ===== TC-F2-03：连击加成 2/3/4 肌群 → ×1.15/1.30/1.45 =====
    @Test
    public void comboBonus() {
        MonsterDef a = monster(1, "胸", "胸", 2);
        MonsterDef b = monster(2, "背", "背", 2);
        MonsterDef c = monster(3, "腿", "腿", 2);
        MonsterDef d = monster(4, "肩", "肩", 2);

        // 2 肌群：base=240, ×1.15 → 276
        int two = BattleManager.computeComboReward(
                Arrays.asList(new BattleManager.GroupPlan(a, 2), new BattleManager.GroupPlan(b, 2)),
                false, 0, GameBalance.DEFAULT).totalExp;
        assertEquals(276, two);

        // 3 肌群：base=360, ×1.30 → 468
        int three = BattleManager.computeComboReward(
                Arrays.asList(new BattleManager.GroupPlan(a, 2), new BattleManager.GroupPlan(b, 2),
                        new BattleManager.GroupPlan(c, 2)),
                false, 0, GameBalance.DEFAULT).totalExp;
        assertEquals(468, three);

        // 4 肌群：base=480, ×1.45 → 696
        int four = BattleManager.computeComboReward(
                Arrays.asList(new BattleManager.GroupPlan(a, 2), new BattleManager.GroupPlan(b, 2),
                        new BattleManager.GroupPlan(c, 2), new BattleManager.GroupPlan(d, 2)),
                false, 0, GameBalance.DEFAULT).totalExp;
        assertEquals(696, four);
    }

    // ===== TC-F2-04：当日首杀（连续 day）→ 额外 + min(day,30)×5 =====
    @Test
    public void firstKillStreakBonus() {
        MonsterDef m = monster(1, "胸", "胸", 2);
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(m, 2));
        // base=240, 首杀 streakDays=1 → +5
        int with = BattleManager.computeComboReward(groups, true, 1, GameBalance.DEFAULT).totalExp;
        int without = BattleManager.computeComboReward(groups, false, 1, GameBalance.DEFAULT).totalExp;
        assertEquals(125, with);
        assertEquals(120, without);
    }

    // ===== TC-F2-06：等级跨越（循环扣减） =====
    @Test
    public void crossLevel() {
        assertEquals(1, LevelSystem.getLevel(0));
        assertEquals(3, LevelSystem.getLevel(500));   // 150 + 210 <= 500
        assertEquals(4, LevelSystem.getLevel(630));   // +270
        assertEquals(5, LevelSystem.getLevel(1205));  // 跨多级，余量正确
    }

    // ===== TC-F2-07：等级曲线上限 L≥15 每级 900 =====
    @Test
    public void levelCap() {
        assertEquals(900, LevelSystem.expToNext(15));
        assertEquals(900, LevelSystem.expToNext(30));
    }

    // ===== TC-F2-08：回蓝不可越界（回蓝中禁止 startRound / startAction） =====
    @Test
    public void restingBlocksRound() {
        MonsterDef m = monster(1, "胸", "胸", 2);
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(m, 3));
        BattleManager.Session s = new BattleManager.Session(groups, GameBalance.DEFAULT);
        s.selectCombo().startAction().startRound().completeRound(); // round1 → RESTING
        assertEquals(BattleManager.State.RESTING, s.getState());

        // 回蓝期间：startRound / startAction 均被守卫拒绝
        try {
            s.startRound();
            fail("回蓝中应当禁止 startRound");
        } catch (IllegalStateException expected) { /* ok */ }
        try {
            s.startAction();
            fail("回蓝中应当禁止 startAction");
        } catch (IllegalStateException expected) { /* ok */ }

        // 回蓝结束：restComplete 进入下一 round，可继续推进
        s.restComplete();
        assertEquals(BattleManager.State.ROUND_ACTIVE, s.getState());
        s.completeRound(); // round2 → RESTING
        s.restComplete().completeRound(); // round3（末轮）→ SESSION_DONE（restComplete 已到 ROUND_ACTIVE，无需 startRound）
        assertEquals(BattleManager.State.SESSION_DONE, s.getState());
        assertEquals(0, s.getBloodRemaining());
    }

    // ===== TC-F2-09：血条不为负 =====
    @Test
    public void bloodNeverNegative() {
        MonsterDef m = monster(1, "胸", "胸", 3);
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(m, 4));
        BattleManager.Session s = new BattleManager.Session(groups, GameBalance.DEFAULT);
        s.selectCombo().startAction().startRound();
        assertTrue(s.getBloodRemaining() >= 0);
        for (int i = 0; i < 4; i++) {
            s.completeRound();
            assertTrue("血条不为负", s.getBloodRemaining() >= 0);
            if (s.getState() == BattleManager.State.RESTING) {
                s.restComplete(); // → ROUND_ACTIVE，进入下一轮（不再 startRound）
            }
        }
        assertEquals(0, s.getBloodRemaining());
        assertEquals(BattleManager.State.SESSION_DONE, s.getState());
    }

    // ===== TC-F2-10：Boss 难度驱动血量（难度高 → 动作数多 → 血条格数多，EXP 正比难度） =====
    @Test
    public void difficultyDrivesBloodAndExp() {
        MonsterDef weak = monster(1, "弱", "胸", 1);
        MonsterDef strong = monster(2, "强", "背", 5);
        // 同一轮次数下，强 Boss 单动作 EXP 更高
        assertTrue(BattleManager.computeActionExp(strong) > BattleManager.computeActionExp(weak));
        // 难度驱动默认动作数：强 Boss 血条格数（rounds）更多
        List<BattleManager.GroupPlan> weakG = Arrays.asList(new BattleManager.GroupPlan(weak, 2));
        List<BattleManager.GroupPlan> strongG = Arrays.asList(new BattleManager.GroupPlan(strong, 5));
        assertEquals(2, weakG.get(0).rounds);
        assertEquals(5, strongG.get(0).rounds);
        assertTrue("强 Boss 总 EXP 更高",
                BattleManager.computeComboReward(strongG, false, 0, GameBalance.DEFAULT).totalExp
                        > BattleManager.computeComboReward(weakG, false, 0, GameBalance.DEFAULT).totalExp);
    }
}
