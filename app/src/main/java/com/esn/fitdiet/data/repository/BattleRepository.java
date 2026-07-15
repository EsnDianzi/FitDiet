package com.esn.fitdiet.data.repository;

import com.esn.fitdiet.data.local.dao.ExerciseLogDao;
import com.esn.fitdiet.data.local.dao.LevelProgressDao;
import com.esn.fitdiet.data.local.dao.MonsterDefDao;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.local.entity.MonsterDef;
import com.esn.fitdiet.game.BattleManager;
import com.esn.fitdiet.game.GameBalance;
import com.esn.fitdiet.game.LevelSystem;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战斗/训练仓储：封装 ExerciseLogDao + MonsterDefDao + LevelProgressDao + BattleManager + LevelSystem。
 * 一次组合会话（多个肌群 Boss）通过 {@link #completeCombo} 落地，正确计入连击加成与首杀连续天数。
 */
public class BattleRepository {

    private final ExerciseLogDao exerciseLogDao;
    private final MonsterDefDao monsterDefDao;
    private final LevelProgressDao levelProgressDao;
    private final AppExecutors executors;

    public BattleRepository(ExerciseLogDao exerciseLogDao,
                            MonsterDefDao monsterDefDao,
                            LevelProgressDao levelProgressDao,
                            AppExecutors executors) {
        this.exerciseLogDao = exerciseLogDao;
        this.monsterDefDao = monsterDefDao;
        this.levelProgressDao = levelProgressDao;
        this.executors = executors;
    }

    /** 异步插入训练日志。 */
    public void insert(ExerciseLog log) {
        executors.singleThread().execute(() -> exerciseLogDao.insert(log));
    }

    /** 同步获取怪物定义（可在任意线程调用）。 */
    public MonsterDef getMonsterById(long id) {
        return monsterDefDao.getById(id);
    }

    /** 同步获取等级进度（供 ViewModel 暴露等级）。 */
    public LevelProgress getProgress() {
        return levelProgressDao.get();
    }

    /** 完成一段动作：写入日志 + 累加进度。 */
    public void completeRound(ExerciseLog log) {
        executors.singleThread().execute(() -> {
            log.completedRounds++;
            exerciseLogDao.insert(log);
        });
    }

    /** 击败单个 Boss：标记完成 + 计算奖励（连击=1，含首杀规则）。 */
    public void defeatBoss(ExerciseLog log, MonsterDef monster) {
        List<BattleManager.GroupPlan> groups = new ArrayList<>();
        groups.add(new BattleManager.GroupPlan(monster, Math.max(1, log.totalRounds)));
        completeCombo(groups, log.date);
    }

    /** 异步：完成一次多肌群组合会话，写入 ExerciseLog + 更新 LevelProgress。 */
    public void completeCombo(List<BattleManager.GroupPlan> groups, String date) {
        executors.singleThread().execute(() -> completeComboSync(groups, date));
    }

    /**
     * 同步核心逻辑（供单测直接调用，使用注入的 DAO）：
     * 逐肌群写入 ExerciseLog（动作 EXP + 击杀奖励），并在 LevelProgress 累加连击加成与首杀连续天数。
     */
    void completeComboSync(List<BattleManager.GroupPlan> groups, String date) {
        LevelProgress lp = levelProgressDao.get();
        if (lp == null) {
            lp = new LevelProgress();
            lp.id = 1;
            lp.level = 1;
            lp.totalExp = 0;
            lp.coins = 0;
            lp.killCount = 0;
            lp.muscleKills = new HashMap<>();
            lp.streakDays = 0;
            lp.lastCheckInDate = "";
        }

        // 当日首杀判定：今日尚无训练记录即为首次击杀
        boolean firstKillToday = exerciseLogDao.getByDate(date).isEmpty();
        int newStreak;
        if (firstKillToday) {
            if (DateUtil.yesterday().equals(lp.lastCheckInDate)) {
                newStreak = lp.streakDays + 1;
            } else {
                newStreak = 1;
            }
            lp.lastCheckInDate = date;
            lp.streakDays = newStreak;
        } else {
            newStreak = lp.streakDays;
        }

        BattleManager.BattleReward reward =
                BattleManager.computeComboReward(groups, firstKillToday, newStreak, GameBalance.DEFAULT);

        for (int i = 0; i < groups.size(); i++) {
            MonsterDef m = groups.get(i).monster;
            ExerciseLog log = new ExerciseLog();
            log.date = date;
            log.monsterId = m.id;
            log.monsterName = m.name;
            log.muscleGroup = m.muscleGroup;
            int durationMin = groups.get(i).rounds * 5; // 5 min/round 估算
            log.durationMin = durationMin;
            log.totalRounds = groups.get(i).rounds;
            log.completedRounds = groups.get(i).rounds;
            // 消耗热量：体重 × 时长 × 5.0（MET 力量训练）→ 缺体重时回退 70kg
            double weightKg = 70.0;
            log.caloriesBurned = weightKg * durationMin / 60.0 * 5.0;
            log.expGained = reward.groupExp[i];
            log.coinsGained = reward.groupCoins[i];
            log.isCompleted = true;
            log.createdAt = System.currentTimeMillis();
            exerciseLogDao.insert(log);
        }

        lp.totalExp += reward.totalExp;
        lp.coins += reward.totalCoins;
        lp.killCount += reward.groupsDefeated;
        for (int i = 0; i < groups.size(); i++) {
            String mg = groups.get(i).monster.muscleGroup;
            lp.muscleKills.put(mg, lp.muscleKills.getOrDefault(mg, 0) + 1);
        }
        lp.level = LevelSystem.getLevel(lp.totalExp);
        levelProgressDao.update(lp);
    }
}
