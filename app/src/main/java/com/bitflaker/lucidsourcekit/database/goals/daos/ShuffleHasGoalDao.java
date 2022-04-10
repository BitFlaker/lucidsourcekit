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

    @Query("SELECT * FROM ShuffleHasGoal a LEFT JOIN Goal ON a.goalId = Goal.goalId LEFT JOIN Shuffle c ON a.shuffleId = c.shuffleId LEFT OUTER JOIN Shuffle b ON c.dayStartTimestamp = b.dayStartTimestamp AND a.shuffleId < b.shuffleId WHERE b.shuffleId is NULL AND c.dayStartTimestamp = :dayStartTimestamp and c.dayEndTimestamp = :dayEndTimestamp")
    Single<List<DetailedShuffleHasGoal>> getCurrentFromLatestShuffle(long dayStartTimestamp, long dayEndTimestamp);

    @Query("SELECT COUNT(*) FROM (SELECT * FROM ShuffleHasGoal a LEFT JOIN Shuffle c ON a.shuffleId = c.shuffleId LEFT OUTER JOIN Shuffle b ON c.dayStartTimestamp = b.dayStartTimestamp AND a.shuffleId < b.shuffleId WHERE b.shuffleId is NULL GROUP BY c.dayStartTimestamp) AS Count")
    Single<Integer> getShuffleCounts();

    @Query("SELECT COUNT(a.goalId) FROM ShuffleHasGoal a LEFT JOIN Shuffle c ON a.shuffleId = c.shuffleId LEFT OUTER JOIN Shuffle b ON c.dayStartTimestamp = b.dayStartTimestamp AND a.shuffleId < b.shuffleId WHERE b.shuffleId is NULL AND a.goalId IN (:goalIds) GROUP BY a.goalId")
    Single<List<Integer>> getCountOfGoalsDrawn(List<Integer> goalIds);

    @Query("SELECT COUNT(*) FROM ShuffleHasGoal a LEFT JOIN Shuffle c ON a.shuffleId = c.shuffleId LEFT OUTER JOIN Shuffle b ON c.dayStartTimestamp = b.dayStartTimestamp AND a.shuffleId < b.shuffleId WHERE b.shuffleId is NULL")
    Single<Integer> getAmountOfTotalDrawnGoals();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(List<ShuffleHasGoal> shuffleHasGoals);

    @Delete
    Completable delete(ShuffleHasGoal shuffleHasGoal);
}
