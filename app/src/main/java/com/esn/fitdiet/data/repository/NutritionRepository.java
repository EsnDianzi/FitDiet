package com.esn.fitdiet.data.repository;

import androidx.lifecycle.LiveData;

import com.esn.fitdiet.data.local.dao.UserProfileDao;
import com.esn.fitdiet.data.local.entity.UserProfile;
import com.esn.fitdiet.domain.calculator.NutritionCalculator;
import com.esn.fitdiet.domain.model.NutritionTarget;
import com.esn.fitdiet.util.AppExecutors;

/**
 * 营养 / 个人档案仓储（开发方案 §1.3）。
 *
 * <p>封装 {@link UserProfileDao} 与 {@link NutritionCalculator}：异步落库、同步读取、
 * 以及由档案直接计算营养目标（纯函数，便于单测）。
 */
public class NutritionRepository {

    /** 无建档时的 BMR 回退值。 */
    public static final double DEFAULT_BMR = 1600.0;

    private final UserProfileDao userProfileDao;
    private final AppExecutors executors;

    public NutritionRepository(UserProfileDao userProfileDao, AppExecutors executors) {
        this.userProfileDao = userProfileDao;
        this.executors = executors;
    }

    public LiveData<UserProfile> observeProfile() {
        return userProfileDao.observe();
    }

    public UserProfile getProfile() {
        return userProfileDao.get();
    }

    /** 异步保存档案（写入在 diskIO 执行，方案 §1.6）。 */
    public void saveProfile(UserProfile profile) {
        executors.diskIO().execute(() -> userProfileDao.insert(profile));
    }

    /** 异步更新档案（保留 createdAt，刷新 updatedAt）。 */
    public void updateProfile(UserProfile profile) {
        executors.diskIO().execute(() -> {
            profile.updatedAt = System.currentTimeMillis();
            userProfileDao.update(profile);
        });
    }

    /** 由档案直接计算营养目标；档案为空返回 null。 */
    public NutritionTarget computeTarget(UserProfile profile) {
        if (profile == null) return null;
        return NutritionCalculator.compute(
                profile.gender, profile.age, profile.heightCm, profile.weightKg,
                profile.activityLevel, profile.goal, profile.boostTestosterone);
    }

    /** 取 BMR：优先档案缓存值，无建档回退默认值。 */
    public double getBmr() {
        UserProfile p = userProfileDao.get();
        if (p != null && p.bmr > 0) return p.bmr;
        return DEFAULT_BMR;
    }
}
