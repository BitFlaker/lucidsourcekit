package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.DreamMood;

import java.util.List;

@Dao
public interface DreamMoodDao {
    @Query("SELECT * FROM DreamMood")
    List<DreamMood> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(DreamMood... dreamMoods);

    @Delete
    void delete(DreamMood dreamMood);
}
