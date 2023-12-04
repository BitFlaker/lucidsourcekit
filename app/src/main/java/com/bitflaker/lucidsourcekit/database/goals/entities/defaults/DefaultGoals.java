package com.bitflaker.lucidsourcekit.database.goals.entities.defaults;

import android.content.Context;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;

import java.util.ArrayList;
import java.util.List;

public class DefaultGoals {
    public static final float[] difficulties = new float[] {
            1.6f,
            3f,
            1.2f,
            1.8f,
            1.3f,
            1f,
            2f,
            1.5f
    };
    private final List<Goal> goalsList;

    public DefaultGoals(Context context){
        String[] goalDescriptions = context.getResources().getStringArray(R.array.goals);
        goalsList = new ArrayList<>();
        int i = 0;
        for (String goal : goalDescriptions) {
            goalsList.add(new Goal(goal, difficulties[i]));
            i++;
        }
    }

    public List<Goal> getGoalsList() {
        return goalsList;
    }
}
