package com.bitflaker.lucidsourcekit.database.dreamjournal.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface DreamMoodDao {
    @Query("SELECT * FROM DreamMood")
    Single<List<DreamMood>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(DreamMood... dreamMoods);

    @Delete
    Completable delete(DreamMood dreamMood);
}
