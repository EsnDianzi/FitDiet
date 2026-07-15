package com.esn.fitdiet.data.repository;

import com.esn.fitdiet.data.local.AppDatabase;

/**
 * 全局依赖容器接口（Application 持有）。
 * Activity / ViewModel 通过 getContainer() 获取仓储。
 */
public interface AppContainer {
    AppDatabase getDatabase();
    FoodRepository getFoodRepository();
    BattleRepository getBattleRepository();
    NutritionRepository getNutritionRepository();
    SummaryRepository getSummaryRepository();
    StatsRepository getStatsRepository();
}
