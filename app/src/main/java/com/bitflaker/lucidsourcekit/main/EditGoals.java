package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;

import java.util.ArrayList;
import java.util.List;

public class EditGoals extends AppCompatActivity {
    private RecyclerView editGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_goals);

        editGoals = findViewById(R.id.rcv_edit_goals);

        List<Goal> goals = new ArrayList<>();
        goals.add(new Goal("sdfsdf test", GoalDifficulty.Easy));
        goals.add(new Goal("asdasdfsdsdaffsdfsd test", GoalDifficulty.Moderate));
        goals.add(new Goal("tesasdat sdfsdfsdftsdest", GoalDifficulty.Moderate));
        goals.add(new Goal("tesdfsdffsdfst asdaw", GoalDifficulty.Easy));
        goals.add(new Goal("afsdf sdfsdfsdf", GoalDifficulty.Difficult));
        goals.add(new Goal("testsfsddfs test", GoalDifficulty.Moderate));
        RecyclerViewAdapterEditGoals rvaeg = new RecyclerViewAdapterEditGoals(EditGoals.this, goals);
        editGoals.setAdapter(rvaeg);
    }
}