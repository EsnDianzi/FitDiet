package com.esn.fitdiet.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/** Gamification progress for the single user. Stored with id = 1. */
@Entity(tableName = "level_progress")
public class LevelProgress {
    @PrimaryKey
    public long id = 1;

    public int level;
    public int totalExp;
    public int coins;
    public int streakDays;
    public String lastCheckInDate; // yyyy-MM-dd

    public int killCount;                       // 累计击杀
    public Map<String, Integer> muscleKills;    // 各肌群累计击杀

    public LevelProgress() {
        this.muscleKills = new HashMap<>();
    }
}
