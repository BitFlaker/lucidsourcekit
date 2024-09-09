package com.bitflaker.lucidsourcekit.main.goals;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.databinding.ActivityGoalsEditorBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetGoalsEditorBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class GoalsEditorView extends AppCompatActivity {
    private boolean isInSelectionMode;
    private RecyclerViewAdapterEditGoals editGoalsAdapter;
    private MainDatabase db;
    private ActivityGoalsEditorBinding binding;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalsEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        compositeDisposable = new CompositeDisposable();
        Tools.makeStatusBarTransparent(GoalsEditorView.this);
        binding.txtEditGoalsHeading.setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(GoalsEditorView.this));
        db = MainDatabase.getInstance(this);

        setupRecycleView();

        binding.btnAddGoal.setOnClickListener(e -> {
            if(!isInSelectionMode) {
                final BottomSheetDialog addGoalSheet = new BottomSheetDialog(GoalsEditorView.this, R.style.BottomSheetDialogStyle);
                SheetGoalsEditorBinding sheetBinding = SheetGoalsEditorBinding.inflate(getLayoutInflater());
                addGoalSheet.setContentView(sheetBinding.getRoot());
                AtomicBoolean isLocked = new AtomicBoolean(false);

                changeSliderColor(sheetBinding.sldGoalDifficulty, sheetBinding.sldGoalDifficulty.getValue(), false);
                sheetBinding.sldGoalDifficulty.addOnChangeListener(this::changeSliderColor);
                sheetBinding.sldGoalDifficulty.setValue(1);
                sheetBinding.btnShowGoalDetails.setVisibility(View.GONE);
                sheetBinding.btnDeleteGoal.setVisibility(View.GONE);
                sheetBinding.txtGoalEditorTitle.setText("Add goal");  // TODO extract string resource
                setLockIconAccordingly(sheetBinding.btnToggleLockDifficulty, isLocked);

                sheetBinding.btnToggleLockDifficulty.setOnClickListener(view -> {
                    isLocked.set(!isLocked.get());
                    setLockIconAccordingly(sheetBinding.btnToggleLockDifficulty, isLocked);
                });

                sheetBinding.btnSaveGoal.setOnClickListener(view -> {
                    if(sheetBinding.txtGoalDescription.getText().toString().isEmpty()) { return; }
                    Goal newGoal = new Goal(sheetBinding.txtGoalDescription.getText().toString(), sheetBinding.sldGoalDifficulty.getValue(), isLocked.get());
                    compositeDisposable.add(db.getGoalDao().insert(newGoal).subscribe(() -> {
                        sheetBinding.sldGoalDifficulty.clearOnChangeListeners();
                        addGoalSheet.dismiss();
                    }));
                });

                addGoalSheet.show();
            }
            else {
                List<Integer> selectedGoalIds = editGoalsAdapter.getSelectedGoalIds();
                List<Goal> selectedGoals = editGoalsAdapter.getSelectedGoals();
                new MaterialAlertDialogBuilder(GoalsEditorView.this, R.style.Theme_LucidSourceKit_ThemedDialog).setTitle(selectedGoalIds.size() == 1 ? "Delete goal" : "Delete goals").setMessage("Do you really want to delete " + (selectedGoalIds.size() == 1 ? "this goal" : ("these " + selectedGoalIds.size() + " selected goals")) + "?") // TODO: extract string resources
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
        compositeDisposable.add(db.getGoalDao().getAll().subscribe((goals) -> {
            runOnUiThread(() -> {
                if(editGoalsAdapter == null) {
                    setupEditGoalsAdapter(goals);
                }
                else {
                    editGoalsAdapter.setEntries(goals);
                }
            });
        }));
    }

    private void setupEditGoalsAdapter(java.util.List<Goal> goals) {
        editGoalsAdapter = new RecyclerViewAdapterEditGoals(GoalsEditorView.this, goals);
        binding.rcvEditGoals.setAdapter(editGoalsAdapter);
        binding.rcvEditGoals.setLayoutManager(new LinearLayoutManager(GoalsEditorView.this));
        // TODO: animate change
        editGoalsAdapter.setOnMultiselectEntered(() -> {
            binding.btnAddGoal.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorErrorContainer, getTheme()));
            binding.btnAddGoal.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_delete_24, getTheme()));
            binding.btnAddGoal.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnErrorContainer, getTheme()));
            isInSelectionMode = true;
        });
        editGoalsAdapter.setOnMultiselectExited(() -> {
            binding.btnAddGoal.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimaryContainer, getTheme()));
            binding.btnAddGoal.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_add_24, getTheme()));
            binding.btnAddGoal.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnPrimaryContainer, getTheme()));
            isInSelectionMode = false;
        });
        editGoalsAdapter.setOnEntryClickedListener((goal, position) -> {
            final BottomSheetDialog editGoalSheet = new BottomSheetDialog(GoalsEditorView.this, R.style.BottomSheetDialogStyle);
            SheetGoalsEditorBinding sheetBinding = SheetGoalsEditorBinding.inflate(getLayoutInflater());
            editGoalSheet.setContentView(sheetBinding.getRoot());
            AtomicBoolean isLocked = new AtomicBoolean(goal.difficultyLocked);

            // TODO: check if this goal was on schedule at all
            sheetBinding.btnShowGoalDetails.setVisibility(View.VISIBLE);
            sheetBinding.btnShowGoalDetails.setOnClickListener(e -> {
                db.getShuffleHasGoalDao().getAchieveStatsOfGoal(goal.goalId).subscribe((goalStats, throwable) -> {
                    String message;
                    if(goalStats.totalCount == 0) {
                        message = "This goal has not been on your schedule yet";
                    }
                    else {
                        message = getResources().getString(R.string.goal_achievement_stats)
                                .replace("<COUNT>", Integer.toString(goalStats.achievedCount))
                                .replace("<TOTAL>", Integer.toString(goalStats.totalCount))
                                .replace("<PERCENTAGE>", String.format(Locale.ENGLISH, "%.1f", (100.0f * goalStats.achievedCount / (float)goalStats.totalCount)));
                    }
                    new MaterialAlertDialogBuilder(GoalsEditorView.this, R.style.Theme_LucidSourceKit_ThemedDialog).setTitle("Goal details").setMessage(message)
                            .setPositiveButton(getResources().getString(R.string.ok), null)
                            .show();
                }).dispose();
            });

            sheetBinding.txtGoalEditorTitle.setText("Edit goal");
            sheetBinding.txtGoalDescription.setText(goal.description);
            changeSliderColor(sheetBinding.sldGoalDifficulty, sheetBinding.sldGoalDifficulty.getValue(), false);
            sheetBinding.sldGoalDifficulty.addOnChangeListener(this::changeSliderColor);
            sheetBinding.sldGoalDifficulty.setValue(goal.difficulty);
            setLockIconAccordingly(sheetBinding.btnToggleLockDifficulty, isLocked);

            sheetBinding.btnToggleLockDifficulty.setOnClickListener(view -> {
                isLocked.set(!isLocked.get());
                setLockIconAccordingly(sheetBinding.btnToggleLockDifficulty, isLocked);
            });

            sheetBinding.btnSaveGoal.setOnClickListener(view -> {
                if (sheetBinding.txtGoalDescription.getText().toString().isEmpty()) {
                    return;
                }
                goal.description = sheetBinding.txtGoalDescription.getText().toString();
                goal.difficulty = sheetBinding.sldGoalDifficulty.getValue();
                goal.difficultyLocked = isLocked.get();
                compositeDisposable.add(db.getGoalDao().update(goal).subscribe(() -> {
                    sheetBinding.sldGoalDifficulty.clearOnChangeListeners();
                    editGoalSheet.dismiss();
                }));
            });

            sheetBinding.btnDeleteGoal.setOnClickListener(view -> {
                new MaterialAlertDialogBuilder(GoalsEditorView.this, R.style.Theme_LucidSourceKit_ThemedDialog).setTitle("Delete goal").setMessage("Do you really want to delete this goal?") // TODO: extract string resources
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> compositeDisposable.add(db.getGoalDao().delete(goal).subscribe(editGoalSheet::dismiss)))
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
            toggleLockDifficulty.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerLow, getTheme()));
        } else {
            toggleLockDifficulty.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_lock_open_24, getTheme()));
            toggleLockDifficulty.setImageTintList(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getTheme()));
            toggleLockDifficulty.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerLow, getTheme()));
        }
    }

    public void changeSliderColor(@NonNull Slider slider, float value, boolean fromUser) {
        @ColorInt int color = Tools.getColorAtGradientPosition(
                value,
                slider.getValueFrom(),
                slider.getValueTo(),
                Tools.getAttrColor(R.attr.colorSuccess, getTheme()),
                Tools.getAttrColor(R.attr.colorWarning, getTheme()),
                Tools.getAttrColor(R.attr.colorError, getTheme()
        ));
        ColorStateList activeTrackColor = ColorStateList.valueOf(color);
        ColorStateList inactiveTrackColor = ColorStateList.valueOf(Tools.manipulateAlpha(color, 0.32f));
        slider.setTrackInactiveTintList(inactiveTrackColor);
        slider.setTrackActiveTintList(activeTrackColor);
        slider.setThumbTintList(activeTrackColor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        compositeDisposable.dispose();
    }
}