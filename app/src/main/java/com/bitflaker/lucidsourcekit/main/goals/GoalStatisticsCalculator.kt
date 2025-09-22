package com.bitflaker.lucidsourcekit.main.goals

import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal
import com.bitflaker.lucidsourcekit.utils.Tools

class GoalStatisticsCalculator(
    private val db: MainDatabase,
    private val dayStartTimestamp: Long,
    private val dayEndTimestamp: Long
) {
    private var count: Int = 0
    private var achieved: Int = 0
    private var goals: List<DetailedShuffleHasGoal>? = null
    private val isDayDone: Boolean = Tools.isTimeInPast(dayEndTimestamp)
    var difficulty: Float = 0.0f
        private set
    var recurrenceFrequency: Float = 0.0f
        private set
    var shuffleOccurrenceRating: Float = 0.0f
        private set

    suspend fun calculate(): GoalStatisticsCalculator {
        val totalAverageDifficulty = db.goalDao.getAverageDifficulty()
        val goalIds = ArrayList<Int>()
        val currentGoals = db.shuffleHasGoalDao.getShuffleFrom(dayStartTimestamp, dayEndTimestamp)

        if (currentGoals.isEmpty()) {
            return this
        }

        goals = currentGoals
        var countAchieved = 0
        var difficultySum = 0f
        for (goal in currentGoals) {
            if (isDayDone && false /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH */) {
                countAchieved++
            }
            difficultySum += goal.difficulty
            goalIds.add(goal.goalId)
        }
        achieved = countAchieved
        count = goalIds.size
        difficulty = difficultySum / count
        shuffleOccurrenceRating = (difficulty / totalAverageDifficulty).toFloat()
        val totalDrawCount: Int = db.shuffleHasGoalDao.getCountOfGoalsDrawn(goalIds, dayStartTimestamp, dayEndTimestamp)
        val totalCountOfGoalsShuffled: Int = db.shuffleHasGoalDao.getAmountOfTotalDrawnGoals(dayStartTimestamp, dayEndTimestamp)
        recurrenceFrequency = 100 * totalDrawCount / totalCountOfGoalsShuffled.toFloat()

        return this
    }

    fun getGoals(): List<Goal>? {
        return goals?.map {
            Goal(
                it.goalId,
                it.description,
                it.difficulty,
                it.difficultyLocked
            )
        }
    }

    val shuffleId: Int
        get() {
            val currentGoals = goals
            if (currentGoals.isNullOrEmpty()) {
                return 0
            }
            return currentGoals[0].shuffleId
        }

    val ratioAchieved: Float
        get() = if (count == 0) 0f else achieved / count.toFloat()

    fun hasGoals(): Boolean {
        return count > 0
    }
}
