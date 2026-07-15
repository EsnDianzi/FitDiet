package com.esn.fitdiet.ui.main;

import android.content.Context;
import android.os.Build;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.repository.BattleRepository;
import com.esn.fitdiet.data.repository.FoodRepository;
import com.esn.fitdiet.data.repository.SummaryRepository;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * HomeViewModel 测试（MVVM 接线，方案 §1.3）。
 * InstantTaskExecutorRule 让 LiveData 同步派发；observeForever 读取当日汇总 / 饮食。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private HomeViewModel vm;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        SummaryRepository summaryRepo = new SummaryRepository(
                db.dailySummaryDao(), db.foodLogDao(),
                db.exerciseLogDao(), db.userProfileDao(), AppExecutors.getInstance());
        FoodRepository foodRepo = new FoodRepository(
                db.foodLogDao(), null, AppExecutors.getInstance());
        BattleRepository battleRepo = new BattleRepository(
                db.exerciseLogDao(), db.monsterDefDao(),
                db.levelProgressDao(), AppExecutors.getInstance());
        vm = new HomeViewModel(summaryRepo, foodRepo, battleRepo);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void todaySummary_observed() {
        DailySummary s = new DailySummary();
        s.date = DateUtil.today();
        s.intakeCalories = 800;
        s.netCalories = -1300;
        s.updatedAt = 1000;
        db.dailySummaryDao().insert(s);

        AtomicReference<DailySummary> got = new AtomicReference<>();
        vm.todaySummary().observeForever(got::set);
        assertNotNull("当日汇总可见", got.get());
        assertEquals(800, got.get().intakeCalories, 0.001);
    }

    @Test
    public void todayFood_observed() {
        FoodLog f = new FoodLog();
        f.date = DateUtil.today();
        f.mealType = MealType.LUNCH;
        f.foodName = "米饭";
        f.calories = 174;
        f.source = FoodSource.AI;
        db.foodLogDao().insert(f);

        AtomicReference<java.util.List<FoodLog>> got = new AtomicReference<>();
        vm.todayFood().observeForever(got::set);
        assertNotNull(got.get());
        assertEquals(1, got.get().size());
        assertEquals("米饭", got.get().get(0).foodName);
    }

    @Test
    public void getCurrentLevel_readsBattleProgress() {
        LevelProgress lp = new LevelProgress();
        lp.id = 1;
        lp.level = 5;
        db.levelProgressDao().insert(lp);
        assertEquals(5, vm.getCurrentLevel());
    }

    @Test
    public void ensureTodaySummary_fillsMissing() {
        // 当日有饮食但无汇总 → ensure 生成
        FoodLog f = new FoodLog();
        f.date = DateUtil.today();
        f.mealType = MealType.LUNCH;
        f.foodName = "鸡胸肉";
        f.calories = 165;
        f.source = FoodSource.AI;
        db.foodLogDao().insert(f);

        assertNull(db.dailySummaryDao().getByDate(DateUtil.today()));
        vm.ensureTodaySummary();
        assertNotNull("当日汇总已兜底生成", db.dailySummaryDao().getByDate(DateUtil.today()));
    }
}
