package com.bitflaker.lucidsourcekit.database.goals.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ShuffleHasGoalDao {
    @Query("SELECT * FROM ShuffleHasGoal")
    Single<List<ShuffleHasGoal>> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Completable insertAll(ShuffleHasGoal... shuffleHasGoals);

    @Delete
    Completable delete(ShuffleHasGoal shuffleHasGoal);
}
