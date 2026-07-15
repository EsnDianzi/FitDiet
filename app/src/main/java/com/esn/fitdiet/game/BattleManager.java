package com.esn.fitdiet.game;

import com.esn.fitdiet.data.local.entity.MonsterDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves rewards for the segmented "boss battle" model and drives the combo session state machine.
 *
 * <p>所有数值统一引用 {@link GameBalance}（单一真源）。静态方法使用 {@link GameBalance#DEFAULT}；
 * 带 {@link GameBalanceConfig} 的重载允许从 assets 热加载配置，行为一致。
 *
 * <p>状态机（开发方案 §3.2 / 测试端 §3.1）：
 * IDLE → COMBO_SELECTED → ACTION_ACTIVE → ROUND_ACTIVE → RESTING → ... → BOSS_DOWN → SESSION_DONE。
 * 每次 completeRound 推进对应肌群 Boss 当前格血量（扣 1/rounds）；
 * actionDone 血条 −1/N 格并结算动作 EXP；bossDown 结算击杀奖励；
 * sessionDone 结算连击加成 + 当日首杀（连续天数）。
 */
public final class BattleManager {

    private BattleManager() { }

    // ===== 静态奖励（兼容既有测试，使用默认配置） =====

    /** Exp for finishing one exercise (one blood-bar segment). */
    public static int computeActionExp(MonsterDef monster) {
        return computeActionExp(monster, GameBalance.DEFAULT);
    }

    /** Bonus exp awarded when the boss is fully defeated. */
    public static int computeKillBonus(MonsterDef monster) {
        return computeKillBonus(monster, GameBalance.DEFAULT);
    }

    /** Coins earned for defeating the monster = round(EXP * COIN_PER_EXP). */
    public static int computeCoins(MonsterDef monster) {
        return computeCoins(monster, GameBalance.DEFAULT);
    }

    // ===== 配置重载（支持 assets 热加载） =====

    public static int computeActionExp(MonsterDef monster, GameBalanceConfig cfg) {
        return cfg.expPerAction * monster.difficulty;
    }

    public static int computeKillBonus(MonsterDef monster, GameBalanceConfig cfg) {
        return cfg.expKillBonus * monster.difficulty;
    }

    public static int computeCoins(MonsterDef monster, GameBalanceConfig cfg) {
        int totalExp = computeActionExp(monster, cfg) + computeKillBonus(monster, cfg);
        return (int) Math.round(totalExp * cfg.coinPerExp);
    }

    // ===== 连击 / 首杀辅助 =====

    /** 连击倍率：基于本组合已击杀肌群数。1 个肌群 = 1.0；每多 1 个 +perExtra，封顶 max。 */
    public static double comboMultiplier(int groupsDefeated, GameBalanceConfig cfg) {
        if (groupsDefeated <= 1) return 1.0;
        double bonus = Math.min((groupsDefeated - 1) * cfg.comboBonusPerExtraGroup, cfg.comboBonusMax);
        return 1.0 + bonus;
    }

    /** 连击加成 EXP（四舍五入到整）。用整数百分比避免浮点误差（0.15×base 精确为 .0/.5）。 */
    public static int comboBonusExp(int baseExp, int groupsDefeated, GameBalanceConfig cfg) {
        if (groupsDefeated <= 1) return 0;
        int perExtraPct = (int) Math.round(cfg.comboBonusPerExtraGroup * 100); // 15
        int maxPct = (int) Math.round(cfg.comboBonusMax * 100);                 // 45
        int steps = Math.min(groupsDefeated - 1, maxPct / perExtraPct);        // 封顶步数
        int totalPct = steps * perExtraPct;                                   // 15 / 30 / 45
        return (int) Math.round(baseExp * totalPct / 100.0);
    }

    /** 首杀连续天数加成 EXP：仅当日首次击杀时生效。 */
    public static int streakBonusExp(boolean firstKillToday, int streakDays, GameBalanceConfig cfg) {
        if (!firstKillToday) return 0;
        int effective = Math.min(streakDays, cfg.streakBonusCap);
        return effective * cfg.streakBonusPerDay;
    }

    // ===== 组合奖励汇总 =====

    /** 单个肌群计划：怪物定义 + 该肌群总动作数（= 血条格数）。 */
    public static final class GroupPlan {
        public final MonsterDef monster;
        public final int rounds;

        public GroupPlan(MonsterDef monster, int rounds) {
            this.monster = monster;
            this.rounds = Math.max(1, rounds);
        }
    }

    /** 一次组合的完整奖励结果。 */
    public static final class BattleReward {
        public final int[] groupExp;
        public final int[] groupCoins;
        public final int baseExp;
        public final int comboBonusExp;
        public final int streakBonusExp;
        public final int totalExp;
        public final int totalCoins;
        public final int groupsDefeated;

        public BattleReward(int[] groupExp, int[] groupCoins, int baseExp,
                            int comboBonusExp, int streakBonusExp, int totalExp,
                            int totalCoins, int groupsDefeated) {
            this.groupExp = groupExp;
            this.groupCoins = groupCoins;
            this.baseExp = baseExp;
            this.comboBonusExp = comboBonusExp;
            this.streakBonusExp = streakBonusExp;
            this.totalExp = totalExp;
            this.totalCoins = totalCoins;
            this.groupsDefeated = groupsDefeated;
        }
    }

    /**
     * 计算一次完整组合的奖励（动作 EXP + 击杀奖励 + 连击加成 + 首杀连续天数）。
     * 奖励只依赖各肌群的最终完成轮次，与中途是否暂停无关（支持断点续练）。
     */
    public static BattleReward computeComboReward(List<GroupPlan> groups,
                                                  boolean firstKillToday,
                                                  int streakDays,
                                                  GameBalanceConfig cfg) {
        int n = groups.size();
        int[] groupExp = new int[n];
        int[] groupCoins = new int[n];
        int baseExp = 0;
        int baseCoins = 0;

        for (int i = 0; i < n; i++) {
            MonsterDef m = groups.get(i).monster;
            int rounds = groups.get(i).rounds;
            int actionExp = cfg.expPerAction * m.difficulty * rounds;
            int killBonus = cfg.expKillBonus * m.difficulty;
            int gExp = actionExp + killBonus;
            int gCoins = (int) Math.round(gExp * cfg.coinPerExp);
            groupExp[i] = gExp;
            groupCoins[i] = gCoins;
            baseExp += gExp;
            baseCoins += gCoins;
        }

        int cBonus = comboBonusExp(baseExp, n, cfg);
        int sBonus = streakBonusExp(firstKillToday, streakDays, cfg);
        int totalExp = baseExp + cBonus + sBonus;
        int sessionBonusCoins = (int) Math.round((cBonus + sBonus) * cfg.coinPerExp);
        int totalCoins = baseCoins + sessionBonusCoins;

        return new BattleReward(groupExp, groupCoins, baseExp, cBonus, sBonus,
                totalExp, totalCoins, n);
    }

    // ===== 状态机（血条 / 回蓝 / 流转，供单测覆盖 TC-F2-08~10、BC-IT-03） =====

    public enum State {
        IDLE, COMBO_SELECTED, ACTION_ACTIVE, ROUND_ACTIVE, RESTING, ACTION_DONE, BOSS_DOWN, SESSION_DONE
    }

    /** 一次组合会话的状态机。逐动作逐 round 推进，管理血条与回蓝。 */
    public static final class Session {
        private final List<GroupPlan> groups;
        private final GameBalanceConfig cfg;

        private State state = State.IDLE;
        private int actionIndex = 0;
        private int roundInAction = 0;      // 当前动作内已完成轮次（0-based）
        private int bloodRemaining = 0;     // 当前 Boss 剩余血条格
        private long restUntilMs = 0;
        private final int[] completedRounds; // 各肌群已完成轮次（断点续练用）
        private final boolean[] defeated;    // 各肌群是否已击杀（防重复计数）
        private int groupsDefeated = 0;

        public Session(List<GroupPlan> groups, GameBalanceConfig cfg) {
            this.groups = new ArrayList<>(groups);
            this.cfg = cfg;
            this.completedRounds = new int[groups.size()];
            this.defeated = new boolean[groups.size()];
        }

        public State getState() { return state; }
        public int getBloodRemaining() { return bloodRemaining; }
        public int getActionIndex() { return actionIndex; }
        public int getGroupsDefeated() { return groupsDefeated; }
        public int getRoundInAction() { return roundInAction; }
        public int[] getCompletedRounds() { return completedRounds.clone(); }

        public Session selectCombo() {
            require(state == State.IDLE, "selectCombo");
            state = State.COMBO_SELECTED;
            return this;
        }

        public Session startAction() {
            require(state == State.COMBO_SELECTED || state == State.BOSS_DOWN
                    || state == State.ACTION_DONE, "startAction");
            // 进入新动作时清除上一段回蓝计时，避免误判仍在回蓝
            restUntilMs = 0;
            // 续练时保留已完成的轮次，仅补算剩余血条
            bloodRemaining = Math.max(0, groups.get(actionIndex).rounds - roundInAction);
            state = State.ACTION_ACTIVE;
            return this;
        }

        public Session startRound() {
            require(state == State.ACTION_ACTIVE || state == State.RESTING, "startRound");
            // 使用实时时钟判断回蓝是否结束
            if (System.currentTimeMillis() < restUntilMs) {
                throw new IllegalStateException("回蓝中禁止开始 round");
            }
            state = State.ROUND_ACTIVE;
            return this;
        }

        public Session completeRound() {
            require(state == State.ROUND_ACTIVE || state == State.RESTING, "completeRound");
            roundInAction++;
            bloodRemaining = Math.max(0, bloodRemaining - 1); // TC-F2-09：血条不为负
            completedRounds[actionIndex] = roundInAction;

            boolean isLastRound = roundInAction >= groups.get(actionIndex).rounds;
            if (isLastRound) {
                if (!defeated[actionIndex]) {
                    defeated[actionIndex] = true;
                    groupsDefeated++;
                }
                boolean hasMore = actionIndex < groups.size() - 1;
                if (hasMore) {
                    roundInAction = 0;   // 进入下一肌群时重置当前轮次
                    actionIndex++;
                    state = State.BOSS_DOWN; // 等待 startAction 进入下一肌群
                } else {
                    state = State.SESSION_DONE;
                }
            } else {
                // 进入 round 间回蓝：使用实时时钟
                restUntilMs = System.currentTimeMillis() + cfg.restRoundDefaultS * 1000L;
                state = State.RESTING;
            }
            return this;
        }

        /** 回蓝结束：推进到下一 round（同一动作内）。 */
        public Session restComplete() {
            require(state == State.RESTING, "restComplete");
            boolean isLastRound = roundInAction >= groups.get(actionIndex).rounds;
            if (isLastRound) {
                throw new IllegalStateException("末 round 不应在回蓝后继续");
            }
            state = State.ROUND_ACTIVE;
            return this;
        }

        /** 由已完成轮次重建会话进度（断点续练 BC-IT-03）。 */
        public static Session restore(List<GroupPlan> groups, int[] doneRounds,
                                       GameBalanceConfig cfg) {
            Session s = new Session(groups, cfg);
            int idx = 0;
            for (; idx < groups.size(); idx++) {
                int done = (doneRounds != null && doneRounds.length > idx) ? doneRounds[idx] : 0;
                if (done >= groups.get(idx).rounds) {
                    s.defeated[idx] = true;
                    s.groupsDefeated++;
                    s.completedRounds[idx] = done;
                } else {
                    break;
                }
            }
            s.actionIndex = Math.min(idx, groups.size() - 1);
            s.roundInAction = (idx < groups.size())
                    ? ((doneRounds != null && doneRounds.length > idx) ? doneRounds[idx] : 0) : 0;
            s.bloodRemaining = (idx < groups.size())
                    ? Math.max(0, groups.get(idx).rounds - s.roundInAction) : 0;
            s.state = (idx >= groups.size()) ? State.SESSION_DONE : State.COMBO_SELECTED;
            return s;
        }

        private void require(boolean condition, String evt) {
            if (!condition) {
                throw new IllegalStateException("非法事件 " + evt + " 于状态 " + state);
            }
        }
    }
}
