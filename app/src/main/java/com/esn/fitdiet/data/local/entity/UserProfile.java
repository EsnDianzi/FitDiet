package com.esn.fitdiet.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Equipment;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;

import java.util.List;

/** Single-user profile. Always stored with id = 1. */
@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey
    public long id = 1;

    public Gender gender;
    public int age;
    public double heightCm;
    public double weightKg;
    public Goal goal;
    public boolean boostTestosterone; // 促睾强化：提升睾酮所需的充足健康脂肪
    public ActivityLevel activityLevel;
    public List<Equipment> equipment;

    // Cached computed targets
    public double bmr;
    public double tdee;
    public double targetCalories;
    public double proteinTarget;
    public double carbsTarget;
    public double fatTarget;

    public long createdAt;
    public long updatedAt;

    public UserProfile() { }
}
