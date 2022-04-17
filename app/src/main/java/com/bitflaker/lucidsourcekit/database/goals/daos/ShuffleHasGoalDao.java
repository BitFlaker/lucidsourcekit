package com.bitflaker.lucidsourcekit.database.goals.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ShuffleHasGoalDao {
    @Query("SELECT * FROM ShuffleHasGoal")
    Single<List<ShuffleHasGoal>> getAll();

    @Query("SELECT * FROM ShuffleHasGoal LEFT JOIN Goal ON ShuffleHasGoal.goalId = Goal.goalId LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE Shuffle.dayStartTimestamp = :dayStartTimestamp and Shuffle.dayEndTimestamp = :dayEndTimestamp")
    Single<List<DetailedShuffleHasGoal>> getShuffleFrom(long dayStartTimestamp, long dayEndTimestamp);

    @Query("SELECT COUNT(goalId) FROM ShuffleHasGoal WHERE goalId IN (:goalIds)")
    Single<List<Integer>> getCountOfGoalsDrawn(List<Integer> goalIds);

    @Query("SELECT COUNT(*) FROM ShuffleHasGoal")
    Single<Integer> getAmountOfTotalDrawnGoals();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(List<ShuffleHasGoal> shuffleHasGoals);

    @Query("DELETE FROM ShuffleHasGoal WHERE shuffleId = :id")
    Completable deleteAllWithShuffleId(int id);

    @Delete
    Completable delete(ShuffleHasGoal shuffleHasGoal);

    @Query("DELETE FROM ShuffleHasGoal")
    Completable deleteAll();
}