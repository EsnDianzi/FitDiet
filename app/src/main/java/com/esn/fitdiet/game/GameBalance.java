package com.esn.fitdiet.game;

/**
 * 游戏化数值常量单一真源。
 * 调平衡只需改本类常量，不动结算逻辑。
 */
public final class GameBalance {

    // EXP / 金币
    public static final int EXP_PER_ACTION = 10;
    public static final int EXP_KILL_BONUS = 40;
    public static final double COMBO_BONUS_PER_EXTRA_GROUP = 0.15; // 每多一个肌群 +15%
    public static final double COMBO_BONUS_MAX = 0.45;               // 4 肌群封顶
    public static final int STREAK_BONUS_PER_DAY = 5;
    public static final int STREAK_BONUS_CAP = 30;
    public static final double COIN_PER_EXP = 0.1;

    // 等级曲线
    public static final int LEVEL_BASE = 150;
    public static final int LEVEL_GROWTH = 60;
    public static final int LEVEL_EXP_CAP = 900;

    // 回蓝默认时长（秒）
    public static final int REST_ROUND_DEFAULT_S = 60;
    public static final int REST_ACTION_DEFAULT_S = 90;

    /** 编译期默认配置实例（= 上面常量），供 Provider 回退与状态机使用。 */
    public static final GameBalanceConfig DEFAULT = GameBalanceConfig.fromDefault();

    private GameBalance() { }
}
