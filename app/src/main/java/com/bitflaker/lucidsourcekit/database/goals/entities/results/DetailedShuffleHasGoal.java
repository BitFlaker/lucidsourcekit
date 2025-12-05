package com.bitflaker.lucidsourcekit.database.goals.entities.results;

public class DetailedShuffleHasGoal {
    public int goalId;
    public int shuffleId;
    public long dayStartTimestamp;
    public long dayEndTimestamp;
    public String description;
    public float difficulty;
    public boolean difficultyLocked;
    public boolean achieved;
}
