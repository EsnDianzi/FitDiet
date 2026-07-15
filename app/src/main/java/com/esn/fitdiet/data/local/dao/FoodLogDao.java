package com.esn.fitdiet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.NutritionTotals;

import java.util.List;

@Dao
public interface FoodLogDao {

    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY mealType, createdAt")
    LiveData<List<FoodLog>> observeByDate(String date);

    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY mealType, createdAt")
    List<FoodLog> getByDate(String date);

    @Query("SELECT COALESCE(SUM(calories),0) AS calories, COALESCE(SUM(protein),0) AS protein, " +
            "COALESCE(SUM(carbs),0) AS carbs, COALESCE(SUM(fat),0) AS fat " +
            "FROM food_log WHERE date = :date")
    NutritionTotals sumByDate(String date);

    /** 该日最后一条 FoodLog 的写入时间，用于 DailySummary 过期判定（TC-X-11）。 */
    @Query("SELECT COALESCE(MAX(createdAt),0) FROM food_log WHERE date = :date")
    long maxCreatedAt(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodLog foodLog);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FoodLog> foodLogs);

    @Delete
    void delete(FoodLog foodLog);
}
