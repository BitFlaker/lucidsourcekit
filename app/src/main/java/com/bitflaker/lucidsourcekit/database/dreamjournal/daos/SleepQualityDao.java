package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface SleepQualityDao {
    @Query("SELECT * FROM SleepQuality")
    Single<List<SleepQuality>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(SleepQuality... sleepQualities);

    @Delete
    Completable delete(SleepQuality sleepQuality);
}
