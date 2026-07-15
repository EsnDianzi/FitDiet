package com.esn.fitdiet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.esn.fitdiet.data.local.entity.ExerciseLog;

import java.util.List;

@Dao
public interface ExerciseLogDao {

    @Query("SELECT * FROM exercise_log WHERE date = :date ORDER BY createdAt")
    LiveData<List<ExerciseLog>> observeByDate(String date);

    @Query("SELECT * FROM exercise_log WHERE date = :date ORDER BY createdAt")
    List<ExerciseLog> getByDate(String date);

    /** 最近 N 条训练记录（按 createdAt 倒序），用于统计页训练历史列表。 */
    @Query("SELECT * FROM exercise_log ORDER BY createdAt DESC LIMIT :limit")
    LiveData<List<ExerciseLog>> observeRecent(int limit);

    /** 同步取最近 N 条训练记录。 */
    @Query("SELECT * FROM exercise_log ORDER BY createdAt DESC LIMIT :limit")
    List<ExerciseLog> getRecent(int limit);

    @Query("SELECT COALESCE(SUM(caloriesBurned),0) FROM exercise_log WHERE date = :date")
    double sumCalories(String date);

    /** 该日最后一条 ExerciseLog 的写入时间，用于 DailySummary 过期判定（TC-X-11）。 */
    @Query("SELECT COALESCE(MAX(createdAt),0) FROM exercise_log WHERE date = :date")
    long maxCreatedAt(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ExerciseLog log);

    @Delete
    void delete(ExerciseLog log);
}
