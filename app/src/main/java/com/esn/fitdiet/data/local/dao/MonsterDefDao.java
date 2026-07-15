package com.esn.fitdiet.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.esn.fitdiet.data.local.entity.MonsterDef;

import java.util.List;

@Dao
public interface MonsterDefDao {

    @Query("SELECT * FROM monster_def ORDER BY difficulty, id")
    List<MonsterDef> getAll();

    @Query("SELECT * FROM monster_def WHERE id = :id")
    MonsterDef getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MonsterDef> monsters);

    @Query("SELECT COUNT(*) FROM monster_def")
    int count();
}
