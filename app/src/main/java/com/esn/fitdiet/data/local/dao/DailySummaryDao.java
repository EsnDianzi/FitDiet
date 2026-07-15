package com.esn.fitdiet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.esn.fitdiet.data.local.entity.DailySummary;

import java.util.List;

@Dao
public interface DailySummaryDao {

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    LiveData<DailySummary> observeByDate(String date);

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    DailySummary getByDate(String date);

    @Query("SELECT * FROM daily_summary ORDER BY date DESC LIMIT :limit")
    LiveData<List<DailySummary>> observeRecent(int limit);

    /** 同步取近 N 日（F6 趋势聚合用）。 */
    @Query("SELECT * FROM daily_summary ORDER BY date DESC LIMIT :limit")
    List<DailySummary> getRecent(int limit);

    /** 同步取 [startDate, endDate] 闭区间（按日期升序），用于本周 / 自定义区间聚合。 */
    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<DailySummary> getByDateRange(String startDate, String endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailySummary summary);
}
