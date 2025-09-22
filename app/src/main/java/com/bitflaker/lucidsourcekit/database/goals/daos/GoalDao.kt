package com.bitflaker.lucidsourcekit.database.goals.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM Goal ORDER BY difficulty")
    fun getAll(): Flow<List<Goal>>

    @Query("SELECT * FROM Goal ORDER BY difficulty")
    suspend fun getAllSingle(): List<Goal>

    @Query("SELECT * FROM Goal ORDER BY difficulty")
    suspend fun getAllMaybe(): List<Goal>?

    @Query("SELECT COUNT(*) FROM Goal WHERE difficulty <= :difficulty ORDER BY difficulty")
    suspend fun getCountUntilDifficulty(difficulty: Float): Int

    @Query("SELECT COUNT(*) FROM Goal")
    suspend fun getGoalCount(): Int

    @Query("SELECT IFNULL(SUM(difficulty) / COUNT(goalId), 0) FROM Goal")
    suspend fun getAverageDifficulty(): Double

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(goals: List<Goal>)

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Delete
    suspend fun deleteAll(selectedGoalIds: List<Goal>)

    @Update
    suspend fun update(goal: Goal)
}
