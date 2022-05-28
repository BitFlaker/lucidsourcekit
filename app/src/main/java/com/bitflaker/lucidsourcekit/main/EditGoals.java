package com.bitflaker.lucidsourcekit.main;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditGoals extends AppCompatActivity {
    private RecyclerView editGoals;
    private RelativeLayout editGoalsDialog;
    private ImageButton goalsEditTappedOutside;
    private MaterialButton storeGoal, removeGoal;
    private Chip diffEasy, diffModerate, diffHard;
    private EditText goalText;
    private FloatingActionButton addGoal;
    private RecyclerViewAdapterEditGoals editGoalsAdapter;
    private ImageView backgroundUnfocus;
    private TextView goalsEditAddTitle;
    private MainDatabase db;
    private boolean isInSelectionMode;

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

        addGoal.setOnClickListener(e -> {
            if(!isInSelectionMode) {
                final BottomSheetDialog addGoalSheet = new BottomSheetDialog(EditGoals.this, R.style.BottomSheetDialogStyle);
                addGoalSheet.setContentView(R.layout.goals_editor_sheet);
                MaterialButton deleteButton = addGoalSheet.findViewById(R.id.btn_delete_goal);
                MaterialButton saveButton = addGoalSheet.findViewById(R.id.btn_save_goal);
                EditText goalDescription = addGoalSheet.findViewById(R.id.txt_goal_description);
                Slider goalDifficultySlider = addGoalSheet.findViewById(R.id.sld_goal_difficulty);
                ImageButton toggleLockDifficulty = addGoalSheet.findViewById(R.id.btn_toggle_lock_difficulty);
                TextView goalEditorTitle = addGoalSheet.findViewById(R.id.txt_goal_editor_title);
                TextView successRateInfo = addGoalSheet.findViewById(R.id.txt_success_rate_info);
                AtomicBoolean isLocked = new AtomicBoolean(false);

                goalDifficultySlider.addOnChangeListener(this::changeSliderColor);
                goalDifficultySlider.setValue(1);
                successRateInfo.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
                goalEditorTitle.setText("Add goal");  // TODO extract string resource
                setLockIconAccordingly(toggleLockDifficulty, isLocked);

                toggleLockDifficulty.setOnClickListener(view -> {
                    isLocked.set(!isLocked.get());
                    setLockIconAccordingly(toggleLockDifficulty, isLocked);
                });

                saveButton.setOnClickListener(view -> {
                    if(goalDescription.getText().toString().length() == 0) { return; }
                    Goal newGoal = new Goal(goalDescription.getText().toString(), goalDifficultySlider.getValue(), isLocked.get());
                    db.getGoalDao().insert(newGoal).subscribe(() -> {
                        goalDifficultySlider.clearOnChangeListeners();
                        addGoalSheet.dismiss();
                    });
                });

                addGoalSheet.show();
            }
            else {
                List<Integer> selectedGoalIds = editGoalsAdapter.getSelectedGoalIds();
                List<Goal> selectedGoals = editGoalsAdapter.getSelectedGoals();
                new AlertDialog.Builder(EditGoals.this, Tools.getThemeDialog()).setTitle(selectedGoalIds.size() == 1 ? "Delete goal" : "Delete goals").setMessage("Do you really want to delete " + (selectedGoalIds.size() == 1 ? "this goal" : ("these " + selectedGoalIds.size() + " selected goals")) + "?") // TODO: extract string resources
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            db.getGoalDao().deleteAll(selectedGoals);
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
        });
    }

    private void setupRecycleView() {
        // TODO make flowable so added/changes goals are reloaded
        db.getGoalDao().getAll().subscribe((goals) -> {
            runOnUiThread(() -> {
                if(editGoalsAdapter == null) {
                    setupEditGoalsAdapter(goals);
                }
                else {
                    editGoalsAdapter.setEntries(goals);
                }
            });
        });
    }

    private void setupEditGoalsAdapter(java.util.List<Goal> goals) {
        editGoalsAdapter = new RecyclerViewAdapterEditGoals(EditGoals.this, goals);
        editGoals.setAdapter(editGoalsAdapter);
        editGoals.setLayoutManager(new LinearLayoutManager(EditGoals.this));
        // TODO: animate change
        editGoalsAdapter.setOnMultiselectEntered(() -> {
            addGoal.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorError, getTheme()));
            addGoal.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_delete_24, getTheme()));
            addGoal.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnError, getTheme()));
            isInSelectionMode = true;
        });
        editGoalsAdapter.setOnMultiselectExited(() -> {
            addGoal.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getTheme()));
            addGoal.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_add_24, getTheme()));
            addGoal.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnPrimary, getTheme()));
            isInSelectionMode = false;
        });
        editGoalsAdapter.setOnEntryClickedListener((goal, position) -> {
            final BottomSheetDialog editGoalSheet = new BottomSheetDialog(EditGoals.this, R.style.BottomSheetDialogStyle);
            editGoalSheet.setContentView(R.layout.goals_editor_sheet);
            MaterialButton deleteButton = editGoalSheet.findViewById(R.id.btn_delete_goal);
            MaterialButton saveButton = editGoalSheet.findViewById(R.id.btn_save_goal);
            EditText goalDescription = editGoalSheet.findViewById(R.id.txt_goal_description);
            Slider goalDifficultySlider = editGoalSheet.findViewById(R.id.sld_goal_difficulty);
            ImageButton toggleLockDifficulty = editGoalSheet.findViewById(R.id.btn_toggle_lock_difficulty);
            TextView goalEditorTitle = editGoalSheet.findViewById(R.id.txt_goal_editor_title);
            TextView successRateInfo = editGoalSheet.findViewById(R.id.txt_success_rate_info);
            AtomicBoolean isLocked = new AtomicBoolean(goal.difficultyLocked);

            // TODO: check if this goal was on schedule at all
            successRateInfo.setVisibility(View.GONE);
            db.getShuffleHasGoalDao().getAchieveStatsOfGoal(goal.goalId).subscribe((goalStats, throwable) -> {
                if(goalStats.totalCount == 0){
                    successRateInfo.setText("This goal has not been on your schedule yet");
                }
                else {
                    successRateInfo.setText(getResources().getString(R.string.goal_achievement_stats)
                            .replace("<COUNT>", Integer.toString(goalStats.achievedCount))
                            .replace("<TOTAL>", Integer.toString(goalStats.totalCount))
                            .replace("<PERCENTAGE>", String.format(Locale.ENGLISH, "%.1f", (100.0f * goalStats.achievedCount / (float)goalStats.totalCount))));
                }
                successRateInfo.setVisibility(View.VISIBLE);
            });
            goalEditorTitle.setText("Edit goal");  // TODO extract string resource
            goalDescription.setText(goal.description);
            goalDifficultySlider.addOnChangeListener(this::changeSliderColor);
            goalDifficultySlider.setValue(goal.difficulty);
            setLockIconAccordingly(toggleLockDifficulty, isLocked);

            toggleLockDifficulty.setOnClickListener(view -> {
                isLocked.set(!isLocked.get());
                setLockIconAccordingly(toggleLockDifficulty, isLocked);
            });

            saveButton.setOnClickListener(view -> {
                if (goalDescription.getText().toString().length() == 0) {
                    return;
                }
                goal.description = goalDescription.getText().toString();
                goal.difficulty = goalDifficultySlider.getValue();
                goal.difficultyLocked = isLocked.get();
                db.getGoalDao().update(goal).subscribe(() -> {
                    goalDifficultySlider.clearOnChangeListeners();
                    editGoalSheet.dismiss();
                });
            });

            deleteButton.setOnClickListener(view -> {
                new AlertDialog.Builder(EditGoals.this, Tools.getThemeDialog()).setTitle("Delete goal").setMessage("Do you really want to delete this goal?") // TODO: extract string resources
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            db.getGoalDao().delete(goal).subscribe(editGoalSheet::dismiss);
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            });

            editGoalSheet.show();
        });
    }

    private void setLockIconAccordingly(ImageButton toggleLockDifficulty, AtomicBoolean isLocked) {
        if (isLocked.get()) {
            toggleLockDifficulty.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_lock_24, getTheme()));
            toggleLockDifficulty.setImageTintList(Tools.getAttrColorStateList(R.attr.primaryTextColor, getTheme()));
        } else {
            toggleLockDifficulty.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_lock_open_24, getTheme()));
            toggleLockDifficulty.setImageTintList(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getTheme()));
        }
    }

    public void changeSliderColor(@NonNull Slider slider, float value, boolean fromUser) {
        @ColorInt int color = Tools.getColorAtGradientPosition(value,
                slider.getValueFrom(), slider.getValueTo(), Tools.getAttrColor(R.attr.colorSuccess, getTheme()),
                Tools.getAttrColor(R.attr.colorWarning, getTheme()), Tools.getAttrColor(R.attr.colorError, getTheme()));
        ColorStateList csl = ColorStateList.valueOf(color);
        slider.setTrackTintList(csl);
        slider.setThumbTintList(csl);
    }
}