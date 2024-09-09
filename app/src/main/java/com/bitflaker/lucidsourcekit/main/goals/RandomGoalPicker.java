package com.bitflaker.lucidsourcekit.main.goals;

import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomGoalPicker {
    private final NavigableMap<Float, Goal> map;
    private final Random random;
    private float total;

    public RandomGoalPicker() {
        map = new TreeMap<>();
        random = new Random();
        total = 0;
    }

    public void add(float weight, Goal goal) {
        if(weight <= 0.0f) {
            throw new IllegalArgumentException("weight must be greater than zero. Provided value: " + weight);
        }
        total += weight;
        map.put(total, goal);
    }

    public Goal getRandomGoal() {
        Goal goal = null;
        float value = random.nextFloat() * total;
        Map.Entry<Float, Goal> entry = map.higherEntry(value);
        if(entry != null) {
            goal = entry.getValue();
            if (total == entry.getKey()) {
                Map.Entry<Float, Goal> lower = map.lowerEntry(entry.getKey());
                if (lower != null) {
                    total = lower.getKey();
                }
            }
            map.remove(entry.getKey(), entry.getValue());
        }
        return goal;
    }
}
