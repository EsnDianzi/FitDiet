package com.esn.fitdiet.data.repository;

import android.content.Context;
import android.os.Build;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.local.entity.MonsterDef;
import com.esn.fitdiet.game.BattleManager;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * F2 集成测试（测试端 BC-IT-01~03）：使用 Room 内存库验证一次完整打卡会话
 * 对 ExerciseLog + LevelProgress + MonsterDef 的多实体联动写入。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class BattleIntegrationTest {

    private AppDatabase db;
    private BattleRepository repo;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repo = new BattleRepository(db.exerciseLogDao(), db.monsterDefDao(),
                db.levelProgressDao(), AppExecutors.getInstance());
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static MonsterDef monster(long id, String name, String mg, int difficulty) {
        MonsterDef m = new MonsterDef();
        m.id = id;
        m.name = name;
        m.muscleGroup = mg;
        m.difficulty = difficulty;
        return m;
    }

    private void seedProgress(String lastCheckInDate, int streakDays) {
        LevelProgress lp = new LevelProgress();
        lp.id = 1;
        lp.level = 1;
        lp.totalExp = 0;
        lp.coins = 0;
        lp.killCount = 0;
        lp.muscleKills = new HashMap<>();
        lp.streakDays = streakDays;
        lp.lastCheckInDate = lastCheckInDate;
        db.levelProgressDao().insert(lp);
    }

    private ExerciseLog findByMuscle(List<ExerciseLog> logs, String mg) {
        for (ExerciseLog l : logs) if (mg.equals(l.muscleGroup)) return l;
        return null;
    }

    // ===== BC-IT-01：完整会话（胸 d3×3rounds + 二头 d2×2rounds） =====
    @Test
    public void completeComboWritesAllEntities() {
        seedProgress(DateUtil.yesterday(), 0); // 昨日打卡 → 今日首杀连续天数 +1

        MonsterDef chest = monster(1, "胸甲虫", "胸", 3);
        MonsterDef arm = monster(2, "二头怪", "二头", 2);
        db.monsterDefDao().insertAll(java.util.Arrays.asList(chest, arm)); // 预置 MonsterDef
        String date = DateUtil.today();
        List<BattleManager.GroupPlan> groups = Arrays.asList(
                new BattleManager.GroupPlan(chest, 3),
                new BattleManager.GroupPlan(arm, 2));

        repo.completeComboSync(groups, date);

        // ExerciseLog：胸、二头各 1 行
        List<ExerciseLog> logs = db.exerciseLogDao().getByDate(date);
        assertEquals(2, logs.size());
        ExerciseLog chestLog = findByMuscle(logs, "胸");
        ExerciseLog armLog = findByMuscle(logs, "二头");
        assertEquals(210, chestLog.expGained);   // 3×10×3 + 40×3
        assertEquals(21, chestLog.coinsGained);  // round(210×0.1)
        assertEquals(120, armLog.expGained);     // 2×10×2 + 40×2
        assertEquals(12, armLog.coinsGained);
        assertTrue(chestLog.isCompleted);
        assertEquals(3, chestLog.totalRounds);
        assertEquals(3, chestLog.completedRounds);
        assertEquals(2, armLog.totalRounds);
        assertEquals(2, armLog.completedRounds);

        // LevelProgress：totalExp=385, coins=39, level=3, killCount=2
        LevelProgress after = db.levelProgressDao().get();
        assertEquals(385, after.totalExp);
        assertEquals(39, after.coins);
        assertEquals(3, after.level);            // getLevel(385)=3
        assertEquals(2, after.killCount);
        assertEquals(1, (int) after.muscleKills.get("胸"));
        assertEquals(1, (int) after.muscleKills.get("二头"));
        assertEquals(1, after.streakDays);        // 昨日→+1
        assertEquals(date, after.lastCheckInDate);

        // MonsterDef 未被改动
        MonsterDef read = db.monsterDefDao().getById(1);
        assertEquals("胸甲虫", read.name);
        assertEquals(3, read.difficulty);
    }

    // ===== BC-IT-02：跨级（一次性大 EXP） =====
    @Test
    public void crossLevelPersists() {
        seedProgress("", 0); // 无历史 → 首杀 streakDays=1

        MonsterDef big = monster(9, "硬拉暴君", "背", 5);
        String date = DateUtil.today();
        List<BattleManager.GroupPlan> groups = Arrays.asList(new BattleManager.GroupPlan(big, 20));

        repo.completeComboSync(groups, date);

        LevelProgress after = db.levelProgressDao().get();
        assertEquals(1205, after.totalExp); // 20×10×5 + 40×5 + 首杀5
        assertEquals(5, after.level);       // 跨 4 级，余量正确
        assertEquals(1, after.killCount);
        assertEquals(1, (int) after.muscleKills.get("背"));
    }

    // ===== BC-IT-03：断点续练（暂停后以 completedRounds 重建，总 EXP/金币一致） =====
    @Test
    public void resumeFromCompletedRounds() {
        seedProgress(DateUtil.yesterday(), 0);

        MonsterDef chest = monster(1, "胸甲虫", "胸", 3);
        MonsterDef arm = monster(2, "二头怪", "二头", 2);
        List<BattleManager.GroupPlan> groups = Arrays.asList(
                new BattleManager.GroupPlan(chest, 3),
                new BattleManager.GroupPlan(arm, 2));

        // 一次性奖励基准（首杀、streak=1）
        BattleManager.BattleReward oneShot =
                BattleManager.computeComboReward(groups, true, 1, com.esn.fitdiet.game.GameBalance.DEFAULT);

        // 阶段一：推进胸的第 1 轮后"退出"
        BattleManager.Session s1 = new BattleManager.Session(groups,
                com.esn.fitdiet.game.GameBalance.DEFAULT);
        s1.selectCombo().startAction().startRound().completeRound();
        int[] done = s1.getCompletedRounds();
        assertEquals(1, done[0]);
        assertEquals(0, done[1]);

        // 阶段二：以已完成轮次重建并续推至完成
        BattleManager.Session s2 = BattleManager.Session.restore(groups, done,
                com.esn.fitdiet.game.GameBalance.DEFAULT);
        s2.startAction()                // 胸，剩余血条 2 → ACTION_ACTIVE
                .startRound().completeRound()   // 胸 round2 → RESTING
                .restComplete().completeRound() // 胸 round3 → BOSS_DOWN（进入二头）
                .startAction()           // 二头，血条 2 → ACTION_ACTIVE
                .startRound().completeRound()   // 二头 round1 → RESTING
                .restComplete().completeRound(); // 二头 round2 → SESSION_DONE（restComplete 已到 ROUND_ACTIVE）

        assertEquals(BattleManager.State.SESSION_DONE, s2.getState());
        assertEquals(2, s2.getGroupsDefeated());
        assertEquals(0, s2.getBloodRemaining());
        assertArrayEquals(new int[]{3, 2}, s2.getCompletedRounds());

        // 重建后总轮次与一次性完成一致
        BattleManager.BattleReward resumed =
                BattleManager.computeComboReward(groups, true, 1, com.esn.fitdiet.game.GameBalance.DEFAULT);
        assertEquals(oneShot.totalExp, resumed.totalExp);
        assertEquals(oneShot.totalCoins, resumed.totalCoins);

        // 落库（用最终 groups 一次性写入）与断点续练结果一致
        String date = DateUtil.today();
        repo.completeComboSync(groups, date);
        LevelProgress after = db.levelProgressDao().get();
        assertEquals(oneShot.totalExp, after.totalExp);
        assertEquals(oneShot.totalCoins, after.coins);
    }
}
