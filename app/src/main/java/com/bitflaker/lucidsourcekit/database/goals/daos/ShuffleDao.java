package com.bitflaker.lucidsourcekit.database.goals.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ShuffleDao {
    @Query("SELECT * FROM Shuffle")
    Single<List<Shuffle>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(Shuffle... shuffles);

    @Delete
    Completable delete(Shuffle shuffle);
}
