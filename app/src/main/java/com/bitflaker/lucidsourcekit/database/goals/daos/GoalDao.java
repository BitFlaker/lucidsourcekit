package com.bitflaker.lucidsourcekit.database.goals.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface GoalDao {
    @Query("SELECT * FROM Goal ORDER BY difficulty")
    Flowable<List<Goal>> getAll();

    @Query("SELECT * FROM Goal ORDER BY difficulty")
    Single<List<Goal>> getAllSingle();

    @Query("SELECT COUNT(*) FROM Goal WHERE difficulty <= :difficulty ORDER BY difficulty")
    Single<Integer> getCountUntilDifficulty(float difficulty);

    @Query("SELECT COUNT(*) FROM Goal")
    Single<Integer> getGoalCount();

    @Query("SELECT SUM(difficulty) / COUNT(goalId) FROM Goal")
    Single<Double> getAverageDifficulty();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(List<Goal> goals);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insert(Goal goal);

    @Delete
    Completable delete(Goal goal);

    @Delete
    void deleteAll(List<Goal> selectedGoalIds);

    @Update
    Completable update(Goal goal);
}
