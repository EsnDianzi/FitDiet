package com.esn.fitdiet.data.repository;

import android.content.Context;
import android.os.Build;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.NutritionTarget;
import com.esn.fitdiet.util.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * NutritionRepository 测试（方案 §1.3）：由档案计算营养目标、取 BMR、异步存档。
 * Room 内存库，主线程直查。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class NutritionRepositoryTest {

    private AppDatabase db;
    private NutritionRepository repo;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repo = new NutritionRepository(db.userProfileDao(), AppExecutors.getInstance());
    }

    @After
    public void tearDown() {
        db.close();
    }

    private UserProfile profile(Gender g, int age, double h, double w,
                                ActivityLevel lvl, Goal goal, boolean boost) {
        UserProfile p = new UserProfile();
        p.id = 1;
        p.gender = g;
        p.age = age;
        p.heightCm = h;
        p.weightKg = w;
        p.activityLevel = lvl;
        p.goal = goal;
        p.boostTestosterone = boost;
        return p;
    }

    // ===== 由档案计算目标（对齐 LayerVerification / TC-F1-01） =====
    @Test
    public void computeTarget_fromProfile() {
        UserProfile p = profile(Gender.MALE, 35, 179, 72.5,
                ActivityLevel.MODERATE, Goal.RECOMP, true);
        db.userProfileDao().insert(p);

        NutritionTarget t = repo.computeTarget(p);
        assertNotNull(t);
        assertEquals("BMR ≈ 1673.75", 1673.75, t.getBmr(), 1.0);
        assertEquals("蛋白 = 2.0×72.5 = 145", 145.0, t.getProteinG(), 1.0);
    }

    @Test
    public void computeTarget_nullProfile_returnsNull() {
        assertNull(repo.computeTarget(null));
    }

    // ===== 取 BMR（优先档案缓存，否则回退默认值） =====
    @Test
    public void getBmr_fromProfile() {
        UserProfile p = profile(Gender.FEMALE, 28, 165, 55,
                ActivityLevel.LIGHT, Goal.CUT, false);
        p.bmr = 1350.5;
        db.userProfileDao().insert(p);
        assertEquals(1350.5, repo.getBmr(), 0.001);
    }

    @Test
    public void getBmr_defaultWhenEmpty() {
        // 无建档
        assertEquals(NutritionRepository.DEFAULT_BMR, repo.getBmr(), 0.001);
    }

    // ===== 异步存档（diskIO）→ 可同步读出 =====
    @Test
    public void saveAndGetProfile_roundtrip() throws InterruptedException {
        UserProfile p = profile(Gender.MALE, 30, 175, 80,
                ActivityLevel.ACTIVE, Goal.BULK, true);
        p.bmr = 1700;
        repo.saveProfile(p);

        // diskIO 异步写入，轮询等待落库
        UserProfile read = null;
        for (int i = 0; i < 50 && read == null; i++) {
            read = db.userProfileDao().get();
            if (read == null) Thread.sleep(20);
        }
        assertNotNull("存档已落库", read);
        assertEquals(Gender.MALE, read.gender);
        assertEquals(30, read.age);
        assertEquals(1700, read.bmr, 0.001);
    }
}
