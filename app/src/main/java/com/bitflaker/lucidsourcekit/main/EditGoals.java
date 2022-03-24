package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

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
    private MainDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_goals);
        Tools.makeStatusBarTransparent(EditGoals.this);
        findViewById(R.id.txt_edit_goals_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(EditGoals.this));
        db = MainDatabase.getInstance(this);

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
        //goalsEditTappedOutside.setOnClickListener(e -> closeDialogAndSaveGoal());

        addGoal.setOnClickListener(e -> {
            final BottomSheetDialog addGoalSheet = new BottomSheetDialog(EditGoals.this, R.style.BottomSheetDialog_Dark);
            addGoalSheet.setContentView(R.layout.goals_editor_sheet);
            MaterialButton deleteButton = addGoalSheet.findViewById(R.id.btn_delete_goal);
            MaterialButton saveButton = addGoalSheet.findViewById(R.id.btn_save_goal);
            EditText goalDescription = addGoalSheet.findViewById(R.id.txt_goal_description);
            Slider goalDifficultySlider = addGoalSheet.findViewById(R.id.sld_goal_difficulty);
            CheckBox lockDifficulty = addGoalSheet.findViewById(R.id.chk_lock_difficulty);
            TextView goalEditorTitle = addGoalSheet.findViewById(R.id.txt_goal_editor_title);
            TextView successRateInfo = addGoalSheet.findViewById(R.id.txt_success_rate_info);

            successRateInfo.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            goalEditorTitle.setText("Add goal");  // TODO extract string resource

            saveButton.setOnClickListener(view -> {
                if(goalDescription.getText().toString().length() == 0) { return; }
                Goal newGoal = new Goal(goalDescription.getText().toString(), goalDifficultySlider.getValue(), lockDifficulty.isChecked());
                db.getGoalDao().insert(newGoal).subscribe(addGoalSheet::hide);
            });

            addGoalSheet.show();
        });
    }

    private void setupRecycleView() {
        // TODO make flowable so added/changes goals are reloaded
        db.getGoalDao().getAll().subscribe((goals, throwable) -> {
            RecyclerViewAdapterEditGoals rvaeg = new RecyclerViewAdapterEditGoals(EditGoals.this, goals);
            editGoals.setAdapter(rvaeg);
            editGoals.setLayoutManager(new LinearLayoutManager(EditGoals.this));
            rvaeg.setOnEntryClickedListener((goal, position) -> {
                final BottomSheetDialog editGoalSheet = new BottomSheetDialog(EditGoals.this, R.style.BottomSheetDialog_Dark);
                editGoalSheet.setContentView(R.layout.goals_editor_sheet);
                MaterialButton deleteButton = editGoalSheet.findViewById(R.id.btn_delete_goal);
                MaterialButton saveButton = editGoalSheet.findViewById(R.id.btn_save_goal);
                EditText goalDescription = editGoalSheet.findViewById(R.id.txt_goal_description);
                Slider goalDifficultySlider = editGoalSheet.findViewById(R.id.sld_goal_difficulty);
                CheckBox lockDifficulty = editGoalSheet.findViewById(R.id.chk_lock_difficulty);
                TextView goalEditorTitle = editGoalSheet.findViewById(R.id.txt_goal_editor_title);
                TextView successRateInfo = editGoalSheet.findViewById(R.id.txt_success_rate_info);

                // TODO: check if this goal was on schedule at all
                successRateInfo.setText(getResources().getString(R.string.goal_achievement_stats).replace("<COUNT>", "2").replace("<TOTAL>", "10").replace("<PERCENTAGE>", "20"));
                //successRateInfo.setText("This goal has not been on your schedule yet");
                goalEditorTitle.setText("Edit goal");  // TODO extract string resource
                goalDescription.setText(goal.description);
                goalDifficultySlider.setValue(goal.difficulty);
                lockDifficulty.setChecked(goal.difficultyLocked);

                saveButton.setOnClickListener(view -> {
                    if(goalDescription.getText().toString().length() == 0) { return; }
                    goal.description = goalDescription.getText().toString();
                    goal.difficulty = goalDifficultySlider.getValue();
                    goal.difficultyLocked = lockDifficulty.isChecked();
                    db.getGoalDao().update(goal).subscribe(editGoalSheet::hide);
                });

                deleteButton.setOnClickListener(view -> {
                    new AlertDialog.Builder(EditGoals.this, Tools.getThemeDialog()).setTitle("Delete goal").setMessage("Do you really want to delete this goal?") // TODO: extract string resources
                            .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                                db.getGoalDao().delete(goal).subscribe(editGoalSheet::hide);
                            })
                            .setNegativeButton(getResources().getString(R.string.no), null)
                            .show();
                });

                editGoalSheet.show();
            });
        });
    }
}