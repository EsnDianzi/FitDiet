package com.esn.fitdiet.data.repository;

import android.content.Context;
import android.os.Build;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * SummaryRepository 集成测试（方案 §1.3 / §3.4 / §6.4，TC-X-11 补算）。
 * Room 内存库，主线程直查。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class SummaryRepositoryTest {

    private AppDatabase db;
    private SummaryRepository repo;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repo = new SummaryRepository(db.dailySummaryDao(), db.foodLogDao(),
                db.exerciseLogDao(), db.userProfileDao(), AppExecutors.getInstance());
    }

    @After
    public void tearDown() {
        db.close();
    }

    private FoodLog food(String date, double cal, long createdAt) {
        FoodLog f = new FoodLog();
        f.date = date;
        f.mealType = MealType.LUNCH;
        f.foodName = "测试食物";
        f.calories = cal;
        f.protein = 10;
        f.carbs = 20;
        f.fat = 5;
        f.source = FoodSource.AI;
        f.createdAt = createdAt;
        return f;
    }

    private ExerciseLog exercise(String date, double cal, long createdAt) {
        ExerciseLog e = new ExerciseLog();
        e.date = date;
        e.monsterId = 1;
        e.monsterName = "胸甲虫";
        e.muscleGroup = "胸";
        e.durationMin = 30;
        e.totalRounds = 3;
        e.completedRounds = 3;
        e.caloriesBurned = cal;
        e.expGained = 210;
        e.coinsGained = 21;
        e.isCompleted = true;
        e.createdAt = createdAt;
        return e;
    }

    // ===== 聚合逻辑 =====
    @Test
    public void generateForDate_aggregatesIntakeAndExercise() {
        String date = DateUtil.today();
        db.foodLogDao().insert(food(date, 500, 1000));
        db.foodLogDao().insert(food(date, 300, 1001));
        db.exerciseLogDao().insert(exercise(date, 200, 1002));
        // 无建档 → BMR 回退 1600，dailyActivity 默认 300

        DailySummary s = repo.generateForDate(date);

        assertEquals(800, s.intakeCalories, 0.001);   // 500+300
        assertEquals(200, s.exerciseCalories, 0.001);
        assertEquals(SummaryRepository.DEFAULT_BMR, s.bmrCalories, 0.001); // 1600
        assertEquals(SummaryRepository.DEFAULT_ACTIVITY_CALORIES, s.dailyActivityCalories, 0.001); // 300
        assertEquals(2100, s.totalBurned, 0.001);     // 1600+200+300
        assertEquals(-1300, s.netCalories, 0.001);    // 800-2100
        assertNotNull(db.dailySummaryDao().getByDate(date));
    }

    @Test
    public void generateForDate_bmrFromProfile() {
        String date = DateUtil.today();
        UserProfile p = new UserProfile();
        p.id = 1;
        p.bmr = 1673.75;
        db.userProfileDao().insert(p);

        db.foodLogDao().insert(food(date, 500, 1000));

        DailySummary s = repo.generateForDate(date);
        assertEquals(1673.75, s.bmrCalories, 0.001); // 取档案 BMR
        assertEquals(1673.75 + 0 + 300, s.totalBurned, 0.001); // bmr+exercise(0)+dailyActivity(300)
    }

    @Test
    public void computeForDate_doesNotInsert() {
        String date = DateUtil.today();
        db.foodLogDao().insert(food(date, 500, 1000));
        DailySummary s = repo.computeForDate(date);
        assertNotNull(s);
        assertNull("compute 仅计算不写库", db.dailySummaryDao().getByDate(date));
    }

    @Test
    public void ensureForDate_idempotent() {
        String date = DateUtil.today();
        db.foodLogDao().insert(food(date, 500, 1000));
        assertTrue(repo.ensureForDate(date));      // 首次生成
        assertNotNull(db.dailySummaryDao().getByDate(date));
        // REPLACE 幂等：再 ensure 不应产生第二行（仍只有 1 行）
        assertEquals(false, repo.ensureForDate(date));
        assertNotNull("仍为同一行", db.dailySummaryDao().getByDate(date));
    }

    // ===== TC-X-11 补算（缺失 / 过期） =====
    @Test
    public void backfillMissing_createsSummary() {
        String date = DateUtil.yesterday();
        db.foodLogDao().insert(food(date, 650, 1000));
        // 该日尚无 DailySummary
        assertNull(db.dailySummaryDao().getByDate(date));

        repo.backfillIfNeeded(new String[]{date});

        DailySummary s = db.dailySummaryDao().getByDate(date);
        assertNotNull("缺失摘要已补算", s);
        assertEquals(650, s.intakeCalories, 0.001);
    }

    @Test
    public void staleDetection_triggersRecompute() {
        String date = DateUtil.today();
        db.foodLogDao().insert(food(date, 500, 1000));
        DailySummary s0 = repo.generateForDate(date);
        long updatedAt = s0.updatedAt;
        assertEquals(500, s0.intakeCalories, 0.001);

        // 摘要生成后又有新食物写入（createdAt 晚于 updatedAt）→ 摘要过期
        db.foodLogDao().insert(food(date, 400, updatedAt + 1_000_000));

        List<String> stale = repo.findMissingOrStale(new String[]{date});
        assertTrue("检测到过期，需重算", stale.contains(date));

        repo.backfillIfNeeded(new String[]{date});
        DailySummary s1 = db.dailySummaryDao().getByDate(date);
        assertEquals(900, s1.intakeCalories, 0.001);   // 500+400 重算
        assertTrue(s1.updatedAt >= updatedAt);
    }
}
