package com.esn.fitdiet.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Daily aggregated snapshot, keyed by date. */
@Entity(tableName = "daily_summary")
public class DailySummary {
    @PrimaryKey
    @NonNull
    public String date;        // yyyy-MM-dd

    public double intakeCalories;
    public double intakeProtein;
    public double intakeCarbs;
    public double intakeFat;

    public double bmrCalories;        // auto
    public double exerciseCalories;   // from training
    public double dailyActivityCalories; // default 300, manually editable
    public double totalBurned;
    public double netCalories;        // intake - totalBurned

    /** 当日体重快照（kg），用于 F6 趋势；v2 新增，可空。 */
    public Double weightKg;

    public long updatedAt;             // epoch millis，用于缓存失效/重算判定

    public DailySummary() { }
}
