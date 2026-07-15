package com.esn.fitdiet.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.esn.fitdiet.domain.model.Equipment;

import java.util.List;

/** A training "monster" the user can challenge. Pre-seeded. */
@Entity(tableName = "monster_def")
public class MonsterDef {
    @PrimaryKey
    public long id;

    public String name;
    public String muscleGroup;
    public String desc;
    public int baseExp;
    public int baseCoins;
    public int difficulty;     // 1-5
    public List<Equipment> equipmentNeeded; // JSON 列表，空 = 徒手
    public int iconRes;
    public boolean defaultUnlocked;

    public MonsterDef() { }
}
