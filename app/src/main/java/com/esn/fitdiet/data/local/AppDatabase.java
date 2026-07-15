package com.esn.fitdiet.data.local;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.esn.fitdiet.data.local.converter.Converters;
import com.esn.fitdiet.data.local.dao.DailySummaryDao;
import com.esn.fitdiet.data.local.dao.ExerciseLogDao;
import com.esn.fitdiet.data.local.dao.FoodLogDao;
import com.esn.fitdiet.data.local.dao.LevelProgressDao;
import com.esn.fitdiet.data.local.dao.MonsterDefDao;
import com.esn.fitdiet.data.local.dao.UserProfileDao;
import com.esn.fitdiet.data.local.entity.DailySummary;
import com.esn.fitdiet.data.local.entity.ExerciseLog;
import com.esn.fitdiet.data.local.entity.FoodLog;
import com.esn.fitdiet.data.local.entity.LevelProgress;
import com.esn.fitdiet.data.local.entity.MonsterDef;
import com.esn.fitdiet.data.local.entity.UserProfile;

import java.util.concurrent.Executors;

@Database(entities = {
        UserProfile.class, FoodLog.class, ExerciseLog.class,
        DailySummary.class, MonsterDef.class, LevelProgress.class
}, version = 2, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserProfileDao userProfileDao();
    public abstract FoodLogDao foodLogDao();
    public abstract ExerciseLogDao exerciseLogDao();
    public abstract DailySummaryDao dailySummaryDao();
    public abstract MonsterDefDao monsterDefDao();
    public abstract LevelProgressDao levelProgressDao();

    /** v1 → v2：daily_summary 新增 weightKg 列（F6 趋势快照）。 */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE daily_summary ADD COLUMN weightKg REAL");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "fitdiet.db")
                            .build();
                    seedOnce(INSTANCE);
                }
            }
        }
        return INSTANCE;
    }

    /** Seed preset monsters and default level progress on first launch. */
    private static void seedOnce(AppDatabase db) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (db.monsterDefDao().count() == 0) {
                    db.monsterDefDao().insertAll(MonsterPresets.all());
                }
                if (db.levelProgressDao().get() == null) {
                    LevelProgress p = new LevelProgress();
                    p.id = 1;
                    p.level = 1;
                    p.totalExp = 0;
                    p.coins = 0;
                    p.killCount = 0;
                    p.muscleKills = new java.util.HashMap<>();
                    p.streakDays = 0;
                    p.lastCheckInDate = "";
                    db.levelProgressDao().insert(p);
                }
            } catch (Exception e) {
                Log.e("FitDiet", "seed failed", e);
            }
        });
    }
}
