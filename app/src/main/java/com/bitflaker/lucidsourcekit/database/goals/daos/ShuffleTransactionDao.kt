package com.bitflaker.lucidsourcekit.database.goals.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction
import com.bitflaker.lucidsourcekit.database.goals.entities.results.GoalCounts

@Dao
interface ShuffleTransactionDao {
    @Query("SELECT * FROM ShuffleTransaction ORDER BY id")
    suspend fun getAll(): List<ShuffleTransaction>

    @Query("SELECT goalId, COUNT(*) as count FROM ShuffleTransaction WHERE shuffleId = :shuffleId GROUP BY goalId")
    suspend fun getAllCountsFromShuffle(shuffleId: Int): List<GoalCounts>

    @Query("SELECT * FROM ShuffleTransaction WHERE shuffleId = :shuffleId")
    suspend fun getAllFromShuffle(shuffleId: Int): List<ShuffleTransaction>

    @Query("SELECT * FROM ShuffleTransaction WHERE shuffleId = :shuffleId AND goalId = :goalId ORDER BY achievedAt ASC")
    suspend fun getAllFromShuffleGoal(shuffleId: Int, goalId: Int): MutableList<ShuffleTransaction>

    @Query("SELECT achievedAt FROM ShuffleTransaction WHERE shuffleId = :shuffleId ORDER BY achievedAt ASC")
    suspend fun getAchievedTimes(shuffleId: Int): MutableList<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(shuffleTransactions: List<ShuffleTransaction>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shuffleTransaction: ShuffleTransaction)

    @Delete
    suspend fun delete(shuffleTransaction: ShuffleTransaction)
    @Delete
    suspend fun deleteAll(shuffleTransactions: List<ShuffleTransaction>)

    @Update
    suspend fun update(shuffleTransaction: ShuffleTransaction)
}
