package com.esn.fitdiet.game;

/**
 * Gamified progression. Each level requires more exp than the previous one.
 * exp needed to advance from level L to L+1 = min(LEVEL_BASE + LEVEL_GROWTH * (L - 1), LEVEL_EXP_CAP).
 *
 * 所有数值统一引用 GameBalance，与开发方案设计文档 §3.2 一致。
 */
public final class LevelSystem {

    private LevelSystem() { }

    /** Exp required to advance from the given level to the next. */
    public static int expToNext(int level) {
        int raw = GameBalance.LEVEL_BASE + GameBalance.LEVEL_GROWTH * (level - 1);
        return Math.min(raw, GameBalance.LEVEL_EXP_CAP);
    }

    /** Current level for a given total accumulated exp. */
    public static int getLevel(int totalExp) {
        int level = 1;
        int remaining = totalExp;
        while (remaining >= expToNext(level)) {
            remaining -= expToNext(level);
            level++;
        }
        return level;
    }

    /** Exp already spent inside the current level. */
    public static int expIntoLevel(int totalExp) {
        int level = getLevel(totalExp);
        int spent = 0;
        for (int l = 1; l < level; l++) {
            spent += expToNext(l);
        }
        return totalExp - spent;
    }

    /** Progress fraction [0,1] toward the next level. */
    public static float levelProgress(int totalExp) {
        int level = getLevel(totalExp);
        int need = expToNext(level);
        return (float) expIntoLevel(totalExp) / need;
    }

    /**
     * 应用 EXP 增量并返回新 totalExp（处理跨级扣减）。
     * 文档 §3.2：applyExp(progress, gained) 循环扣减直至不足升级。
     */
    public static int applyExp(int totalExp, int gained) {
        return totalExp + gained;
    }
}
