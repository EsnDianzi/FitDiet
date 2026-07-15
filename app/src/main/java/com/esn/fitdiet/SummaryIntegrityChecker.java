package com.esn.fitdiet;

import android.content.Context;
import android.util.Log;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.repository.SummaryRepository;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动 / MainActivity onResume 时回填缺失或过期的 DailySummary。
 * 对齐 开发方案设计文档 §6.4（TC-X-11 补算）。
 *
 * <p>核心逻辑委托 {@link SummaryRepository#backfillIfNeeded}：对「昨日 + 近 N 日」中
 * 摘要缺失或过期（updatedAt 早于该日最新 FoodLog/ExerciseLog）的日期重新聚合并覆盖写入，
 * 幂等（重复执行结果一致，updatedAt 防重复覆盖）。
 */
public final class SummaryIntegrityChecker {

    private static final String TAG = "IntegrityCheck";
    private static final int LOOKBACK_DAYS = 7;

    private SummaryIntegrityChecker() { }

    /** 昨日汇总完整性检查（方案 §6.4 触发点）。 */
    public static void checkYesterday(Context context) {
        io().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                SummaryRepository repo = new SummaryRepository(
                        db.dailySummaryDao(), db.foodLogDao(), db.exerciseLogDao(),
                        db.userProfileDao(), AppExecutors.getInstance());

                // 1) 昨日完整性（缺失或过期 → 补算）
                repo.backfillIfNeeded(new String[]{DateUtil.yesterday()});

                // 2) 近 N 日历史回填（闹钟失败/跨日遗漏补偿）
                repo.backfillIfNeeded(DateUtil.lastDays(LOOKBACK_DAYS));

                // 3) 确保今天至少有一次聚合
                repo.ensureForDate(DateUtil.today());
            } catch (Exception e) {
                Log.e(TAG, "integrity check failed", e);
            }
        });
    }

    /** 兼容别名：全量检查（含昨日 + 历史回填 + 今日保证）。 */
    public static void check(Context context) {
        checkYesterday(context);
    }

    private static ExecutorService io() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FitDiet-integrity");
            t.setDaemon(true);
            return t;
        });
    }
}
