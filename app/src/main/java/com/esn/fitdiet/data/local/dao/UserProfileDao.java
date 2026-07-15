package com.esn.fitdiet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.esn.fitdiet.data.local.entity.UserProfile;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1")
    LiveData<UserProfile> observe();

    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile get();

    @Query("SELECT COUNT(*) FROM user_profile")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);
}
