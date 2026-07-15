package com.esn.fitdiet.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.repository.BattleRepository;
import com.esn.fitdiet.data.repository.FoodRepository;
import com.esn.fitdiet.data.repository.SummaryRepository;
import com.esn.fitdiet.util.DateUtil;

import java.util.List;

/**
 * 主页 ViewModel（MVVM）：聚合当日汇总 / 当日饮食 / 当前等级。
 * 仅暴露 LiveData 与同步查询，所有 IO 由注入的 Repository 处理（方案 §1.6）。
 */
public class HomeViewModel extends ViewModel {

    private final SummaryRepository summaryRepo;
    private final FoodRepository foodRepo;
    private final BattleRepository battleRepo;

    public HomeViewModel(SummaryRepository summaryRepo,
                       FoodRepository foodRepo,
                       BattleRepository battleRepo) {
        this.summaryRepo = summaryRepo;
        this.foodRepo = foodRepo;
        this.battleRepo = battleRepo;
    }

    /** 当日汇总（净热量/摄入/消耗）。 */
    public LiveData<DailySummary> todaySummary() {
        return summaryRepo.observeByDate(DateUtil.today());
    }

    /** 当日饮食记录。 */
    public LiveData<List<FoodLog>> todayFood() {
        return foodRepo.observeByDate(DateUtil.today());
    }

    /** 当前等级（无进度默认 1）。 */
    public int getCurrentLevel() {
        LevelProgress lp = battleRepo.getProgress();
        return (lp != null) ? lp.level : 1;
    }

    /** 确保当日汇总已生成（兜底回填）。 */
    public void ensureTodaySummary() {
        summaryRepo.ensureForDate(DateUtil.today());
    }
}
