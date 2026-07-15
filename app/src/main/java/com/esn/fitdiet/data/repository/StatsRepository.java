package com.esn.fitdiet.data.repository;

import androidx.lifecycle.LiveData;

import com.esn.fitdiet.data.local.dao.DailySummaryDao;
import com.esn.fitdiet.data.local.dao.ExerciseLogDao;
import com.esn.fitdiet.data.local.dao.LevelProgressDao;
import com.esn.fitdiet.data.local.dao.UserProfileDao;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计 / 成就仓储（开发方案 §1.3 / F6 趋势）。
 *
 * <p>基于 {@code DailySummary}（历史摄入/消耗）、{@code ExerciseLog}（训练历史）、
 * {@code LevelProgress}（等级/连胜/击杀/肌群）做轻聚合：本周净热量缺口、体重曲线、训练历史、成就解锁。
 * 纯查询，便于单测。
 */
public class StatsRepository {

    private final DailySummaryDao summaryDao;
    private final LevelProgressDao levelProgressDao;
    private final ExerciseLogDao exerciseLogDao;
    private final UserProfileDao userProfileDao;

    public StatsRepository(DailySummaryDao summaryDao,
                          LevelProgressDao levelProgressDao,
                          ExerciseLogDao exerciseLogDao,
                          UserProfileDao userProfileDao) {
        this.summaryDao = summaryDao;
        this.levelProgressDao = levelProgressDao;
        this.exerciseLogDao = exerciseLogDao;
        this.userProfileDao = userProfileDao;
    }

    public LiveData<List<DailySummary>> observeRecent(int limit) {
        return summaryDao.observeRecent(limit);
    }

    public DailySummary getToday(String date) {
        return summaryDao.getByDate(date);
    }

    /**
     * 本周净热量缺口：从本周一（含）到今天（含）所有 DailySummary 的 netCalories 累计求和。
     * 负数 = 热量缺口（摄入 < 消耗），正数 = 热量盈余。
     */
    public double weeklyDeficit() {
        String start = DateUtil.startOfWeek();
        String end = DateUtil.today();
        List<DailySummary> list = summaryDao.getByDateRange(start, end);
        if (list == null || list.isEmpty()) return 0.0;
        double sum = 0;
        for (DailySummary s : list) sum += s.netCalories;
        return sum;
    }

    /** LiveData: 体重曲线数据点（按日期升序，只包含已记录体重的日期）。 */
    public LiveData<List<DailySummary>> observeWeightPoints() {
        return summaryDao.observeRecent(60); // 取近 60 天供 UI 自行过滤
    }

    /**
     * 同步取体重曲线点（date, weightKg）。
     * 优先从 DailySummary 读取历史体重点；如果为空，回退到 UserProfile 建档体重作为初始点。
     */
    public List<WeightPoint> getWeightPoints() {
        List<WeightPoint> out = new ArrayList<>();
        List<DailySummary> recent = summaryDao.getRecent(60);
        if (recent != null) {
            // getRecent 是 DESC，需反转成 ASC
            for (int i = recent.size() - 1; i >= 0; i--) {
                DailySummary s = recent.get(i);
                if (s.weightKg != null && s.weightKg > 0) {
                    out.add(new WeightPoint(s.date, s.weightKg));
                }
            }
        }
        // Fallback：历史体重未记录时，使用 UserProfile 的初始体重作为曲线起点
        if (out.isEmpty()) {
            UserProfile profile = userProfileDao.get();
            if (profile != null && profile.weightKg > 0) {
                out.add(new WeightPoint(DateUtil.today(), profile.weightKg));
            }
        }
        return out;
    }

    /** 最近 N 条训练记录 LiveData。 */
    public LiveData<List<ExerciseLog>> observeRecentExercise(int limit) {
        return exerciseLogDao.observeRecent(limit);
    }

    /** 同步取最近 N 条训练记录。 */
    public List<ExerciseLog> getRecentExercise(int limit) {
        return exerciseLogDao.getRecent(limit);
    }

    public LevelProgress getProgress() {
        return levelProgressDao.get();
    }

    /** F6 成就列表（基于 LevelProgress 快照）。 */
    public List<Achievement> computeAchievements(LevelProgress lp) {
        int level = (lp != null) ? lp.level : 1;
        int streak = (lp != null) ? lp.streakDays : 0;
        int kills = (lp != null) ? lp.killCount : 0;
        int muscleKinds = (lp != null && lp.muscleKills != null) ? lp.muscleKills.size() : 0;

        List<Achievement> out = new ArrayList<>();
        out.add(new Achievement("lv1", "初出茅庐", level >= 1, "达到 1 级"));
        out.add(new Achievement("lv3", "渐入佳境", level >= 3, "达到 3 级"));
        out.add(new Achievement("lv5", "健身狂人", level >= 5, "达到 5 级"));
        out.add(new Achievement("streak7", "连胜七日", streak >= 7, "连续打卡 7 天"));
        out.add(new Achievement("kill100", "百战百胜", kills >= 100, "累计击杀 100"));
        out.add(new Achievement("muscle6", "肌群征服者", muscleKinds >= 6, "征服 6 大肌群"));
        return out;
    }

    /** 成就项。 */
    public static final class Achievement {
        public final String id;
        public final String title;
        public final boolean unlocked;
        public final String desc;

        public Achievement(String id, String title, boolean unlocked, String desc) {
            this.id = id;
            this.title = title;
            this.unlocked = unlocked;
            this.desc = desc;
        }
    }

    /** 体重曲线数据点。 */
    public static final class WeightPoint {
        public final String date;
        public final double weightKg;

        public WeightPoint(String date, double weightKg) {
            this.date = date;
            this.weightKg = weightKg;
        }
    }
}
