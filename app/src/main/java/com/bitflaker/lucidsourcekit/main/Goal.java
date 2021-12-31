package com.bitflaker.lucidsourcekit.main;

public class Goal {
    private final String name;
    private final GoalDifficulty difficulty;

    public Goal(String name, GoalDifficulty difficulty) {
        this.name = name;
        this.difficulty = difficulty;
    }

    public String getName() {
        return name;
    }

    public GoalDifficulty getDifficulty() {
        return difficulty;
    }
}
