package com.esn.fitdiet.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.MealType;

/** A single food entry logged for a meal. */
@Entity(tableName = "food_log",
        indices = {@Index(value = {"date", "mealType"}, name = "idx_foodlog_date_meal")})
public class FoodLog {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;

    public String date;        // yyyy-MM-dd
    public MealType mealType;
    public String foodName;
    public double calories;
    public double protein;     // g
    public double carbs;       // g
    public double fat;         // g
    public double fiber;       // g
    public String imagePath;   // nullable
    public FoodSource source;

    public long createdAt;

    public FoodLog() { }
}
