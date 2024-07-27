package com.bitflaker.lucidsourcekit.main.goals;

import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;
import com.bitflaker.lucidsourcekit.utils.Tools;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public class GoalStatisticsCalculator {
    private int count;
    private int achieved;
    private float difficulty;
    private float recurrenceFrequency;
    private float shuffleOccurrenceRating;
    private List<DetailedShuffleHasGoal> goals;
    private final long timestampDayStart;
    private final long timestampDayEnd;
    private final MainDatabase db;
    private final boolean isDayDone;

    public GoalStatisticsCalculator(MainDatabase db, long timestampDayStart, long timestampDayEnd) {
        this.db = db;
        this.timestampDayStart = timestampDayStart;
        this.timestampDayEnd = timestampDayEnd;
        this.isDayDone = Tools.isTimeInPast(timestampDayEnd);
        this.count = 0;
        this.achieved = 0;
        this.difficulty = 0.0f;
        this.recurrenceFrequency = 0.0f;
        this.shuffleOccurrenceRating = 0.0f;
    }

    public Single<GoalStatisticsCalculator> calculate() {
        return Single.fromCallable(() -> {
            float totalAverageDifficulty = db.getGoalDao().getAverageDifficulty().blockingGet().floatValue();

            List<Integer> goalIds = new ArrayList<>();
            Maybe<List<DetailedShuffleHasGoal>> maybeGoalsCurrent = db.getShuffleHasGoalDao().getShuffleFrom(timestampDayStart, timestampDayEnd);
            if(!maybeGoalsCurrent.isEmpty().blockingGet()) {
                goals = maybeGoalsCurrent.blockingGet();
                if(goals.isEmpty()) {
                    return this;
                }
                int countAchieved = 0;
                float difficultySum = 0;
                for (DetailedShuffleHasGoal goal : goals) {
                    if(isDayDone && goal.achieved) {
                        countAchieved++;
                    }
                    difficultySum += goal.difficulty;
                    goalIds.add(goal.goalId);
                }
                achieved = countAchieved;
                count = goalIds.size();
                difficulty = difficultySum / count;
                shuffleOccurrenceRating = difficulty / totalAverageDifficulty;
                int totalDrawCount = db.getShuffleHasGoalDao().getCountOfGoalsDrawn(goalIds, timestampDayStart, timestampDayEnd).blockingGet();
                int totalCountOfGoalsShuffled = db.getShuffleHasGoalDao().getAmountOfTotalDrawnGoals(timestampDayStart, timestampDayEnd).blockingGet();
                recurrenceFrequency = 100 * totalDrawCount / (float) totalCountOfGoalsShuffled;
            }

            return this;
        });
    }

    public int getCount() {
        return count;
    }

    public int getAchieved() {
        return achieved;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public float getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    public float getShuffleOccurrenceRating() {
        return shuffleOccurrenceRating;
    }

    public List<DetailedShuffleHasGoal> getGoals() {
        return goals;
    }

    public float getRatioAchieved() {
        return count == 0 ? 0 : achieved / (float) count;
    }

    public boolean isDayDone() {
        return isDayDone;
    }

    public boolean hasGoals() {
        return count > 0;
    }
}
