package com.esn.fitdiet.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A completed training session = a defeated monster. */
@Entity(tableName = "exercise_log",
        indices = {
                @Index(value = {"date", "muscleGroup"}, name = "idx_exerciselog_date_muscle"),
                @Index(value = {"monsterId"}, name = "idx_exerciselog_monster")
        })
public class ExerciseLog {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;

    public String date;        // yyyy-MM-dd
    public long monsterId;
    public String monsterName;
    public String muscleGroup;
    public int durationMin;

    public int totalRounds;      // 本肌群总轮次（全动作合计）
    public int completedRounds;  // 已完成轮次（支持断点续练）

    public double caloriesBurned;
    public int expGained;
    public int coinsGained;
    public boolean isCompleted;

    public long createdAt;

    public ExerciseLog() { }
}
