package com.bitflaker.lucidsourcekit.database.goals.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Goal {
    @PrimaryKey(autoGenerate = true)
    public int goalId;
    @NonNull
    public String description;
    public float difficulty;
    @ColumnInfo(defaultValue = "0")
    public boolean difficultyLocked;

    @Ignore
    public boolean isSelected;

    public Goal(int goalId, @NonNull String description, float difficulty, boolean difficultyLocked) {
        this.goalId = goalId;
        this.description = description;
        this.difficulty = difficulty;
        this.difficultyLocked = difficultyLocked;
    }

    @Ignore
    public Goal(@NonNull String description, float difficulty, boolean difficultyLocked) {
        this.description = description;
        this.difficulty = difficulty;
        this.difficultyLocked = difficultyLocked;
    }

    @Ignore
    public Goal(@NonNull String description, float difficulty) {
        this.description = description;
        this.difficulty = difficulty;
    }
}
