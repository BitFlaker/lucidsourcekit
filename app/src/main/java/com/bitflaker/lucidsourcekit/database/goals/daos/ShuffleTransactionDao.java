package com.bitflaker.lucidsourcekit.database.goals.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.GoalCounts;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ShuffleTransactionDao {
    @Query("SELECT * FROM ShuffleTransaction ORDER BY id")
    Single<List<ShuffleTransaction>> getAll();

    @Query("SELECT goalId, COUNT(*) as count FROM ShuffleTransaction WHERE shuffleId = :shuffleId GROUP BY goalId")
    Single<List<GoalCounts>> getAllCountsFromShuffle(int shuffleId);

    @Query("SELECT * FROM ShuffleTransaction WHERE shuffleId = :shuffleId")
    Single<List<ShuffleTransaction>> getAllFromShuffle(int shuffleId);

    @Query("SELECT * FROM ShuffleTransaction WHERE shuffleId = :shuffleId AND goalId = :goalId ORDER BY achievedAt ASC")
    Single<List<ShuffleTransaction>> getAllFromShuffleGoal(int shuffleId, int goalId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(List<ShuffleTransaction> shuffleTransactions);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(ShuffleTransaction shuffleTransaction);

    @Delete
    Completable delete(ShuffleTransaction shuffleTransaction);

    @Delete
    Completable deleteAll(List<ShuffleTransaction> shuffleTransactions);

    @Update
    Completable update(ShuffleTransaction shuffleTransaction);
}
