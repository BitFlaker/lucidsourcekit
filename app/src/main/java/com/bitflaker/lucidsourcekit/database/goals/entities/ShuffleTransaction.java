package com.bitflaker.lucidsourcekit.database.goals.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = {
                @ForeignKey(entity = Shuffle.class,
                        parentColumns = "shuffleId",
                        childColumns = "shuffleId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Goal.class,
                        parentColumns = "goalId",
                        childColumns = "goalId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index("shuffleId"),
                @Index("goalId")
        }
)
public class ShuffleTransaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int shuffleId;
    public int goalId;
    public long achievedAt;

    public ShuffleTransaction(int id, int shuffleId, int goalId, long achievedAt) {
        this.id = id;
        this.shuffleId = shuffleId;
        this.goalId = goalId;
        this.achievedAt = achievedAt;
    }

    @Ignore
    public ShuffleTransaction(int shuffleId, int goalId, long achievedAt) {
        this.shuffleId = shuffleId;
        this.goalId = goalId;
        this.achievedAt = achievedAt;
    }
}
