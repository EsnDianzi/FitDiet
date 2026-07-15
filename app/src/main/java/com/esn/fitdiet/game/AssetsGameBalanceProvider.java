package com.esn.fitdiet.game;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 从 assets/game_balance.json 加载数值配置。
 * 任何读取/解析失败都回退到 {@link GameBalanceConfig#fromDefault()}，保证永不崩溃。
 */
public class AssetsGameBalanceProvider implements GameBalanceProvider {

    private static final String TAG = "GameBalance";
    private static final String ASSET_PATH = "game_balance.json";

    private final Context context;
    private GameBalanceConfig cached;

    public AssetsGameBalanceProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public GameBalanceConfig get() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    private GameBalanceConfig load() {
        try (InputStream is = context.getAssets().open(ASSET_PATH);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            GameBalanceConfig cfg = new Gson().fromJson(reader, GameBalanceConfig.class);
            if (cfg != null) {
                Log.i(TAG, "loaded game_balance.json");
                return cfg;
            }
        } catch (Exception e) {
            Log.w(TAG, "asset load failed, fallback to defaults", e);
        }
        return GameBalanceConfig.fromDefault();
    }
}
