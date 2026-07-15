package com.esn.fitdiet.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.esn.fitdiet.data.local.entity.LevelProgress;

@Dao
public interface LevelProgressDao {

    @Query("SELECT * FROM level_progress WHERE id = 1")
    LevelProgress get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LevelProgress progress);

    @Update
    void update(LevelProgress progress);
}
