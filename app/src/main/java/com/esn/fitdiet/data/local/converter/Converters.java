package com.esn.fitdiet.data.local.converter;

import androidx.room.TypeConverter;

import com.esn.fitdiet.domain.model.ActivityLevel;
import com.esn.fitdiet.domain.model.Equipment;
import com.esn.fitdiet.domain.model.FoodSource;
import com.esn.fitdiet.domain.model.Gender;
import com.esn.fitdiet.domain.model.Goal;
import com.esn.fitdiet.domain.model.MealType;
import com.esn.fitdiet.domain.model.MuscleGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Room type converters for enums, string lists and muscle kill map. */
public class Converters {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static String genderToString(Gender g) {
        return g == null ? null : g.name();
    }

    @TypeConverter
    public static Gender stringToGender(String s) {
        return s == null ? null : Gender.valueOf(s);
    }

    @TypeConverter
    public static String goalToString(Goal g) {
        return g == null ? null : g.name();
    }

    @TypeConverter
    public static Goal stringToGoal(String s) {
        return s == null ? null : Goal.valueOf(s);
    }

    @TypeConverter
    public static String activityToString(ActivityLevel a) {
        return a == null ? null : a.name();
    }

    @TypeConverter
    public static ActivityLevel stringToActivity(String s) {
        return s == null ? null : ActivityLevel.valueOf(s);
    }

    @TypeConverter
    public static String mealToString(MealType m) {
        return m == null ? null : m.name();
    }

    @TypeConverter
    public static MealType stringToMeal(String s) {
        return s == null ? null : MealType.valueOf(s);
    }

    @TypeConverter
    public static String sourceToString(FoodSource f) {
        return f == null ? null : f.name();
    }

    @TypeConverter
    public static FoodSource stringToSource(String s) {
        return s == null ? null : FoodSource.valueOf(s);
    }

    // ===== 肌群枚举（TEXT name） =====
    @TypeConverter
    public static String muscleGroupToString(MuscleGroup m) {
        return m == null ? null : m.name();
    }

    @TypeConverter
    public static MuscleGroup stringToMuscleGroup(String s) {
        return s == null ? null : MuscleGroup.valueOf(s);
    }

    // ===== 器械枚举列表（JSON） =====
    @TypeConverter
    public static String equipmentToJson(List<Equipment> list) {
        if (list == null || list.isEmpty()) return "[]";
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<Equipment> jsonToEquipment(String s) {
        if (s == null || s.isEmpty() || "[]".equals(s)) return new ArrayList<>();
        return gson.fromJson(s, new TypeToken<List<Equipment>>() {}.getType());
    }

    @TypeConverter
    public static String listToString(List<String> list) {
        if (list == null) return "";
        return android.text.TextUtils.join(",", list);
    }

    @TypeConverter
    public static List<String> stringToList(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(s.split(",")));
    }

    /** Map<String,Integer> 用于 LevelProgress.muscleKills */
    @TypeConverter
    public static String muscleKillsToJson(Map<String, Integer> map) {
        if (map == null) return "{}";
        return gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Integer> jsonToMuscleKills(String s) {
        if (s == null || s.isEmpty()) return new HashMap<>();
        return gson.fromJson(s, new TypeToken<Map<String, Integer>>() {}.getType());
    }
}
