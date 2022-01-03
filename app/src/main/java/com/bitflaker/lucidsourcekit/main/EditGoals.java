package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EditGoals extends AppCompatActivity {
    private RecyclerView editGoals;
    private RelativeLayout editGoalsDialog;
    private ImageButton goalsEditTappedOutside;
    private MaterialButton storeGoal, removeGoal;
    private Chip diffEasy, diffModerate, diffHard;
    private EditText goalText;
    private FloatingActionButton addGoal;
    private ImageView backgroundUnfocus;
    private TextView goalsEditAddTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_goals);

        editGoals = findViewById(R.id.rcv_edit_goals);
        editGoalsDialog = findViewById(R.id.rl_edit_goals);
        goalsEditTappedOutside = findViewById(R.id.btn_goals_edit_tapped_outside);
        goalText = findViewById(R.id.txt_goal_text_enter);
        diffEasy = findViewById(R.id.chp_diff_easy);
        diffModerate = findViewById(R.id.chp_diff_moderate);
        diffHard = findViewById(R.id.chp_diff_hard);
        removeGoal = findViewById(R.id.btn_remove_goal);
        storeGoal = findViewById(R.id.btn_store_goal);
        addGoal = findViewById(R.id.btn_add_goal);
        goalsEditAddTitle = findViewById(R.id.txt_goal_edit_add);
        backgroundUnfocus = findViewById(R.id.img_goals_edit_background_unfocus);

        setupRecycleView();
        goalsEditTappedOutside.setOnClickListener(e -> closeDialogAndSaveGoal());

        addGoal.setOnClickListener(e -> {
            editGoalsDialog.setVisibility(View.VISIBLE);
            goalsEditTappedOutside.setVisibility(View.VISIBLE);
            backgroundUnfocus.setVisibility(View.VISIBLE);
            goalsEditAddTitle.setText("Add goal");  // TODO extract string resource and change margin of store button as well
            removeGoal.setVisibility(View.GONE);
            addGoal.setVisibility(View.GONE);
            storeGoal.setOnClickListener(e2 -> closeDialogAndSaveGoal());
        });
    }

    private void closeDialogAndSaveGoal() {
        if(editGoalsDialog.getVisibility() == View.VISIBLE){
            closeDialog();
            // TODO: store goal and refresh
        }
    }

    private void closeDialogAndSaveEditedGoal(int position) {
        if(editGoalsDialog.getVisibility() == View.VISIBLE){
            closeDialog();
            // TODO: store edited goal and refresh
        }
    }

    private void closeDialog(){
        editGoalsDialog.setVisibility(View.GONE);
        goalsEditTappedOutside.setVisibility(View.GONE);
        backgroundUnfocus.setVisibility(View.GONE);
        addGoal.setVisibility(View.VISIBLE);
    }

    private void setupRecycleView() {
        List<Goal> goals = new ArrayList<>();
        goals.add(new Goal("sdfsdf test", GoalDifficulty.Easy));
        goals.add(new Goal("asdasdfsdsdaffsdfsd test", GoalDifficulty.Moderate));
        goals.add(new Goal("tesasdat sdfsdfsdftsdest", GoalDifficulty.Moderate));
        goals.add(new Goal("tesdfsdffsdfst asdaw", GoalDifficulty.Easy));
        goals.add(new Goal("afsdf sdfsdfsdf", GoalDifficulty.Difficult));
        goals.add(new Goal("testsfsddfs test", GoalDifficulty.Moderate));
        RecyclerViewAdapterEditGoals rvaeg = new RecyclerViewAdapterEditGoals(EditGoals.this, goals);
        editGoals.setAdapter(rvaeg);
        editGoals.setLayoutManager(new LinearLayoutManager(EditGoals.this));
        rvaeg.setOnEntryClickedListener((goal, position) -> {
            editGoalsDialog.setVisibility(View.VISIBLE);
            goalsEditTappedOutside.setVisibility(View.VISIBLE);
            backgroundUnfocus.setVisibility(View.VISIBLE);
            goalsEditAddTitle.setText("Edit goal");  // TODO extract string resource
            removeGoal.setVisibility(View.VISIBLE);
            addGoal.setVisibility(View.GONE);
            goalText.setText(goal.getName());
            switch (goal.getDifficulty()){
                case Easy: diffEasy.setChecked(true); break;
                case Moderate: diffModerate.setChecked(true); break;
                case Difficult: diffHard.setChecked(true); break;
            }
            storeGoal.setOnClickListener(e2 -> closeDialogAndSaveEditedGoal(position));
            removeGoal.setOnClickListener(e -> {
                closeDialog();
                // TODO remove goal
            });
        });
    }
}