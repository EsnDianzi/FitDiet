package com.esn.fitdiet.game;

import com.google.gson.annotations.SerializedName;

/**
 * GameBalance 的可加载实例（数值配置）。
 * 由 {@link GameBalanceProvider} 从 assets/game_balance.json 加载；
 * 加载失败时回退到 {@link #fromDefault()}（= GameBalance 编译期常量）。
 */
public class GameBalanceConfig {

    @SerializedName("expPerAction")
    public int expPerAction;
    @SerializedName("expKillBonus")
    public int expKillBonus;
    @SerializedName("comboBonusPerExtraGroup")
    public double comboBonusPerExtraGroup;
    @SerializedName("comboBonusMax")
    public double comboBonusMax;
    @SerializedName("streakBonusPerDay")
    public int streakBonusPerDay;
    @SerializedName("streakBonusCap")
    public int streakBonusCap;
    @SerializedName("coinPerExp")
    public double coinPerExp;
    @SerializedName("levelBase")
    public int levelBase;
    @SerializedName("levelGrowth")
    public int levelGrowth;
    @SerializedName("levelExpCap")
    public int levelExpCap;
    @SerializedName("restRoundDefaultS")
    public int restRoundDefaultS;
    @SerializedName("restActionDefaultS")
    public int restActionDefaultS;

    public GameBalanceConfig() {
    }

    /** 复制 GameBalance 编译期默认常量作为离线兜底。 */
    public static GameBalanceConfig fromDefault() {
        GameBalanceConfig c = new GameBalanceConfig();
        c.expPerAction = GameBalance.EXP_PER_ACTION;
        c.expKillBonus = GameBalance.EXP_KILL_BONUS;
        c.comboBonusPerExtraGroup = GameBalance.COMBO_BONUS_PER_EXTRA_GROUP;
        c.comboBonusMax = GameBalance.COMBO_BONUS_MAX;
        c.streakBonusPerDay = GameBalance.STREAK_BONUS_PER_DAY;
        c.streakBonusCap = GameBalance.STREAK_BONUS_CAP;
        c.coinPerExp = GameBalance.COIN_PER_EXP;
        c.levelBase = GameBalance.LEVEL_BASE;
        c.levelGrowth = GameBalance.LEVEL_GROWTH;
        c.levelExpCap = GameBalance.LEVEL_EXP_CAP;
        c.restRoundDefaultS = GameBalance.REST_ROUND_DEFAULT_S;
        c.restActionDefaultS = GameBalance.REST_ACTION_DEFAULT_S;
        return c;
    }
}
