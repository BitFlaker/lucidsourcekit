package com.bitflaker.lucidsourcekit.database.goals.daos;

import static androidx.room.OnConflictStrategy.REPLACE;

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

    @Query("SELECT * FROM Shuffle WHERE dayStartTimestamp = :dayStartTimestamp and dayEndTimestamp = :dayEndTimestamp ORDER BY shuffleId DESC LIMIT 1")
    Single<Shuffle> getLastShuffleInDay(long dayStartTimestamp, long dayEndTimestamp);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(Shuffle... shuffles);

    @Insert(onConflict = REPLACE)
    Single<Long> insert(Shuffle shuffles);

    @Delete
    Completable delete(Shuffle shuffle);
}
