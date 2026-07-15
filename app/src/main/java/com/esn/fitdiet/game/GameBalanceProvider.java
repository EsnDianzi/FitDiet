package com.esn.fitdiet.game;

/**
 * 游戏数值配置提供方接口。
 * 当前实现 {@link AssetsGameBalanceProvider}（本地 assets）；
 * 若后续需远程下发，新增 RemoteGameBalanceProvider 即可无缝替换，
 * 无需改动 BattleManager / LevelSystem（开发方案设计文档 §3.2）。
 */
public interface GameBalanceProvider {
    GameBalanceConfig get();
}
