package com.bitflaker.lucidsourcekit.database.goals.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.GoalStats
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.ShuffleHasGoalStats

@Dao
interface ShuffleHasGoalDao {
    @Query("SELECT * FROM ShuffleHasGoal ORDER BY shuffleId DESC LIMIT 1000")
    suspend fun getAll(): List<ShuffleHasGoal>

    @Query("SELECT * FROM ShuffleHasGoal LEFT JOIN Goal ON ShuffleHasGoal.goalId = Goal.goalId LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE Shuffle.dayStartTimestamp = :dayStartTimestamp and Shuffle.dayEndTimestamp = :dayEndTimestamp")
    suspend fun getShuffleFrom(dayStartTimestamp: Long, dayEndTimestamp: Long): List<DetailedShuffleHasGoal>

    @Query("SELECT COUNT(*) AS goalCount, AVG(Goal.difficulty) AS avgDifficulty, 0 /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH*//*SUM(CASE WHEN ShuffleHasGoal.achieved = 1 then 1 else 0 end)*/ AS achievedCount FROM ShuffleHasGoal LEFT JOIN Goal ON ShuffleHasGoal.goalId = Goal.goalId LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE Shuffle.dayStartTimestamp >= :dayStartTimestamp and Shuffle.dayEndTimestamp <= :dayEndTimestamp")
    suspend fun getShufflesFromBetween(dayStartTimestamp: Long, dayEndTimestamp: Long): ShuffleHasGoalStats

    @Query("SELECT COUNT(goalId) FROM ShuffleHasGoal LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE goalId IN (:goalIds) AND Shuffle.dayStartTimestamp >= :dayStartTimestamp and Shuffle.dayEndTimestamp <= :dayEndTimestamp")
    suspend fun getCountOfGoalsDrawn(goalIds: MutableList<Int>, dayStartTimestamp: Long, dayEndTimestamp: Long): Int

    @Query("SELECT COUNT(goalId) FROM ShuffleHasGoal LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE Shuffle.dayEndTimestamp > :dayEndTimestamp")
    suspend fun getCountOfGoalsInShufflesAfterDay(dayEndTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM ShuffleHasGoal LEFT JOIN Shuffle ON ShuffleHasGoal.shuffleId = Shuffle.shuffleId WHERE Shuffle.dayStartTimestamp >= :dayStartTimestamp and Shuffle.dayEndTimestamp <= :dayEndTimestamp")
    suspend fun getAmountOfTotalDrawnGoals(dayStartTimestamp: Long, dayEndTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insertAll(shuffleHasGoals: List<ShuffleHasGoal>)

    @Query("DELETE FROM ShuffleHasGoal WHERE shuffleId = :id")
    suspend fun deleteAllWithShuffleId(id: Int)

    @Delete
    suspend fun delete(shuffleHasGoal: ShuffleHasGoal)

    @Query("DELETE FROM ShuffleHasGoal")
    suspend fun deleteAll()

    @Update
    suspend fun update(goal: ShuffleHasGoal)

    @Query("SELECT 1 as achievedCount, (SELECT COUNT(*) FROM ShuffleHasGoal WHERE goalId = :goalId) as totalCount")
    suspend fun getAchieveStatsOfGoal(goalId: Int): GoalStats
}