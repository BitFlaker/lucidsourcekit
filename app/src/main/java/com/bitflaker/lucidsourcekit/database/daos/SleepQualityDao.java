package com.bitflaker.lucidsourcekit.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.entities.SleepQuality;

import java.util.List;

@Dao
public interface SleepQualityDao {
    @Query("SELECT * FROM SleepQuality")
    List<SleepQuality> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(SleepQuality... sleepQualities);

    @Delete
    void delete(SleepQuality sleepQuality);
}
