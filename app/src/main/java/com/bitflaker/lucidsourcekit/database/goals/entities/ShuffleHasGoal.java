package com.bitflaker.lucidsourcekit.database.goals.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(primaryKeys = { "shuffleId", "goalId" },
        foreignKeys = {
        @ForeignKey(entity = Shuffle.class,
                parentColumns = "shuffleId",
                childColumns = "shuffleId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Goal.class,
                parentColumns = "goalId",
                childColumns = "goalId",
                onDelete = ForeignKey.CASCADE)
        }
)
public class ShuffleHasGoal {
    public int shuffleId;
    public int goalId;
    @ColumnInfo(defaultValue = "0")
    public boolean achieved;

    public ShuffleHasGoal(int shuffleId, int goalId, boolean achieved) {
        this.shuffleId = shuffleId;
        this.goalId = goalId;
        this.achieved = achieved;
    }

    @Ignore
    public ShuffleHasGoal(int shuffleId, int goalId) {
        this.shuffleId = shuffleId;
        this.goalId = goalId;
    }
}
