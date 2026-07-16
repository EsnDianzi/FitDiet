package com.esn.fitdiet.data.repository;

import androidx.lifecycle.LiveData;

import com.esn.fitdiet.data.local.dao.DailySummaryDao;
import com.esn.fitdiet.data.local.dao.ExerciseLogDao;
import com.esn.fitdiet.data.local.dao.FoodLogDao;
import com.esn.fitdiet.data.local.dao.UserProfileDao;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.NutritionTotals;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * 每日汇总仓储（开发方案 §1.3 / §3.4 / §6.4）。
 *
 * <p>把 {@code DailySummaryWorker} 内的聚合逻辑收敛为纯函数 {@link #computeForDate}，
 * 供 Worker 与 {@code SummaryIntegrityChecker} 共用；并提供「缺失/过期补算」
 * （{@link #findMissingOrStale} + {@link #backfillIfNeeded}）实现 TC-X-11。
 */
public class SummaryRepository {

    /** 用户未手动填写时的日常活动消耗默认值（方案 §3.4）。 */
    public static final double DEFAULT_ACTIVITY_CALORIES = 300.0;
    /** 无建档时的 BMR 回退值（方案 §3.4）。 */
    public static final double DEFAULT_BMR = 1600.0;

    private final DailySummaryDao summaryDao;
    private final FoodLogDao foodLogDao;
    private final ExerciseLogDao exerciseLogDao;
    private final UserProfileDao userProfileDao;
    private final AppExecutors executors;

    public SummaryRepository(DailySummaryDao summaryDao, FoodLogDao foodLogDao,
                            ExerciseLogDao exerciseLogDao, UserProfileDao userProfileDao,
                            AppExecutors executors) {
        this.summaryDao = summaryDao;
        this.foodLogDao = foodLogDao;
        this.exerciseLogDao = exerciseLogDao;
        this.userProfileDao = userProfileDao;
        this.executors = executors;
    }

    public LiveData<DailySummary> observeByDate(String date) {
        return summaryDao.observeByDate(date);
    }

    public LiveData<List<DailySummary>> observeRecent(int limit) {
        return summaryDao.observeRecent(limit);
    }

    public DailySummary getByDate(String date) {
        return summaryDao.getByDate(date);
    }

    /**
     * 纯聚合：按日期汇总摄入 / 运动 / BMR / 日常活动，计算净热量。
     * 仅计算不写库，便于单测与 Worker / Checker 复用。
     */
    public DailySummary computeForDate(String date) {
        NutritionTotals intake = foodLogDao.sumByDate(date);
        if (intake == null) intake = new NutritionTotals();

        double exerciseCal = exerciseLogDao.sumCalories(date);

        // BMR 优先取 UserProfile（方案 §3.4），无建档回退默认值
        double bmr = DEFAULT_BMR;
        UserProfile profile = userProfileDao.get();
        if (profile != null && profile.bmr > 0) {
            bmr = profile.bmr;
        }

        // 保留用户已填的 dailyActivityCalories
        DailySummary existing = summaryDao.getByDate(date);
        double dailyActivity = (existing != null)
                ? existing.dailyActivityCalories : DEFAULT_ACTIVITY_CALORIES;

        double totalBurned = bmr + exerciseCal + dailyActivity;

        DailySummary summary = new DailySummary();
        summary.date = date;
        summary.intakeCalories = intake.calories;
        summary.intakeProtein = intake.protein;
        summary.intakeCarbs = intake.carbs;
        summary.intakeFat = intake.fat;
        summary.bmrCalories = bmr;
        summary.exerciseCalories = exerciseCal;
        summary.dailyActivityCalories = dailyActivity;
        summary.totalBurned = totalBurned;
        summary.netCalories = summary.intakeCalories - totalBurned;
        // 保留已有的体重快照，避免 refreshForDate() 覆盖历史体重点导致曲线丢失
        if (existing != null && existing.weightKg != null && existing.weightKg > 0) {
            summary.weightKg = existing.weightKg;
        }
        summary.updatedAt = System.currentTimeMillis();
        return summary;
    }

    /** 聚合并落库（REPLACE 幂等）。 */
    public DailySummary generateForDate(String date) {
        DailySummary summary = computeForDate(date);
        summaryDao.insert(summary);
        return summary;
    }

    /** 若当日汇总缺失则生成；返回是否生成了新行。 */
    public boolean ensureForDate(String date) {
        if (summaryDao.getByDate(date) == null) {
            generateForDate(date);
            return true;
        }
        return false;
    }

    /**
     * 强制重算指定日期的 DailySummary（无论是否存在）。
     * 适用场景：FoodLog / ExerciseLog 写入后立即刷新摄入/消耗。
     * REPLACE 写入幂等，updatedAt 会被刷新，触发 observeByDate LiveData 推送。
     */
    public void refreshForDate(String date) {
        generateForDate(date);
    }

    /**
     * 找出缺失或过期（updatedAt 早于该日最新 FoodLog/ExerciseLog 写入时间）的日期。
     * 支撑 TC-X-11 补算。
     */
    public List<String> findMissingOrStale(String[] dates) {
        List<String> out = new ArrayList<>();
        for (String date : dates) {
            DailySummary existing = summaryDao.getByDate(date);
            if (existing == null) {
                out.add(date);
                continue;
            }
            long foodMax = foodLogDao.maxCreatedAt(date);
            long exerciseMax = exerciseLogDao.maxCreatedAt(date);
            long latest = Math.max(foodMax, exerciseMax);
            if (latest > existing.updatedAt) {
                out.add(date); // 有更新但摘要未重算
            }
        }
        return out;
    }

    /** 对缺失/过期日期补算（幂等，重复执行结果一致）。 */
    public void backfillIfNeeded(String[] dates) {
        List<String> need = findMissingOrStale(dates);
        for (String date : need) {
            generateForDate(date);
        }
    }
}
