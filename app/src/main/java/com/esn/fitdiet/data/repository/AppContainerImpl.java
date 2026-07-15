package com.esn.fitdiet.data.repository;

import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.remote.QwenVisionMock;
import com.esn.fitdiet.data.remote.QwenVisionService;
import com.esn.fitdiet.util.AppExecutors;
import com.esn.fitdiet.util.DateUtil;

/**
 * 全局依赖容器实现。
 */
public class AppContainerImpl implements AppContainer {

    private final AppDatabase db;
    private final AppExecutors executors;
    private final FoodRepository foodRepository;
    private final BattleRepository battleRepository;
    private final NutritionRepository nutritionRepository;
    private final SummaryRepository summaryRepository;
    private final StatsRepository statsRepository;

    public AppContainerImpl(AppDatabase db) {
        this.db = db;
        this.executors = AppExecutors.getInstance();

        // 默认使用 Mock 视觉服务，后续可从设置页切换真实 API
        QwenVisionService visionService = new QwenVisionMock();

        this.foodRepository = new FoodRepository(db.foodLogDao(), visionService, executors);
        this.battleRepository = new BattleRepository(
                db.exerciseLogDao(), db.monsterDefDao(), db.levelProgressDao(), executors);
        this.nutritionRepository = new NutritionRepository(db.userProfileDao(), executors);
        this.summaryRepository = new SummaryRepository(
                db.dailySummaryDao(), db.foodLogDao(), db.exerciseLogDao(),
                db.userProfileDao(), executors);
        this.statsRepository = new StatsRepository(
                db.dailySummaryDao(), db.levelProgressDao(), db.exerciseLogDao(),
                db.userProfileDao());

        // 食物日志写入后立即重算当日 DailySummary，保证首页「摄入」数字实时更新
        this.foodRepository.setOnInsertedHook(() ->
                this.summaryRepository.ensureForDate(DateUtil.today()));

        // 训练完成后立即重算当日 DailySummary，保证首页「消耗」和统计页数据实时更新
        this.battleRepository.setOnSessionCompleteHook(() ->
                this.summaryRepository.ensureForDate(DateUtil.today()));
    }

    @Override
    public AppDatabase getDatabase() {
        return db;
    }

    @Override
    public FoodRepository getFoodRepository() {
        return foodRepository;
    }

    @Override
    public BattleRepository getBattleRepository() {
        return battleRepository;
    }

    @Override
    public NutritionRepository getNutritionRepository() {
        return nutritionRepository;
    }

    @Override
    public SummaryRepository getSummaryRepository() {
        return summaryRepository;
    }

    @Override
    public StatsRepository getStatsRepository() {
        return statsRepository;
    }
}
