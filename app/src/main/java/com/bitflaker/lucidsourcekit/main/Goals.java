package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.DataValue;
import com.bitflaker.lucidsourcekit.charts.QuadraticFunctionCurve;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.charts.Speedometer;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Goals extends Fragment {
    private FloatingActionButton floatingEdit;
    private RangeProgress goalsReachedYesterday, averageDifficultyLevelYesterday;
    private QuadraticFunctionCurve difficultyLevel;
    private Speedometer difficultySpeedometer;
    private LinearLayout currentGoalsContainer;
    private MainDatabase db;
    private TextView currentSelectionDiff, currentOccurrenceFreq;
    private ImageView selectionDiffComparison, occFreqComparison;
    private ImageButton reshuffle;
    private List<Goal> cachedGoals;
    private ExpandableListView expLView;
    private ExpandableListViewAdapter expLViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_goals_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        goalsReachedYesterday = getView().findViewById(R.id.rp_goals_reached_yesterday);
        //difficultyLevel = getView().findViewById(R.id.rp_difficulty_level);
        averageDifficultyLevelYesterday = getView().findViewById(R.id.rp_goals_average_difficulty_yesterday);
        floatingEdit = getView().findViewById(R.id.btn_add_journal_entry);
        difficultySpeedometer = getView().findViewById(R.id.som_difficulty);
        currentGoalsContainer = getView().findViewById(R.id.ll_current_goals_container);
        currentSelectionDiff = getView().findViewById(R.id.txt_current_selection_diff);
        currentOccurrenceFreq = getView().findViewById(R.id.txt_occurrence_freq);
        selectionDiffComparison = getView().findViewById(R.id.img_selection_diff_comparison);
        occFreqComparison = getView().findViewById(R.id.img_occ_freq_comparison);
        reshuffle = getView().findViewById(R.id.btn_reshuffle_goals);
        expLView = getView().findViewById(R.id.elv_suggestions);
        db = MainDatabase.getInstance(getContext());
        List<String> headings = new ArrayList<>();
        headings.add("");//Suggestions for improving");
        HashMap<String, List<GoalSuggestion>> entries = new HashMap<>();
        expLViewAdapter = new ExpandableListViewAdapter(getContext(), headings, entries);
        expLView.setAdapter(expLViewAdapter);

        List<GoalSuggestion> suggs = new ArrayList<>();
        suggs.add(new GoalSuggestion(R.drawable.ic_baseline_notifications_active_24, "Activate notifications"));
        suggs.add(new GoalSuggestion(R.drawable.ic_baseline_arrow_drop_up_24, "Increase goal count"));
        suggs.add(new GoalSuggestion(R.drawable.ic_baseline_access_alarm_24, "Set recurring alarm"));

        entries.put(headings.get(0), suggs);
        expLView.deferNotifyDataSetChanged();
        expLView.setDividerHeight(0);
        setGroupIndicatorToRight();

        expLView.setOnGroupExpandListener(groupPosition -> {
            int height = Tools.dpToPx(getContext(), Math.max(50, Tools.pxToDp(getContext(), expLView.getMeasuredHeight()) - 36));
            for (int i = 0; i < expLViewAdapter.getChildrenCount(groupPosition); i++) {
                height += Tools.dpToPx(getContext(), 58)+2; // TODO remove hardcoded height of entries (58dp)
                height += expLView.getDividerHeight();
            }
            expLView.getLayoutParams().height = height;
        });
        expLView.setOnGroupCollapseListener(groupPosition -> expLView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT);

        Pair<Long, Long> todayTimeSpan = Tools.getTimeSpanFrom(0, false);
        updateStats(todayTimeSpan);

        reshuffle.setOnClickListener(view1 -> {
            // TODO show loading indicator
            Thread t = new Thread(() -> {
                // TODO reset cached goals if entering the goal editor
                if (cachedGoals == null) {
                    db.getGoalDao().getAllSingle().subscribe((goals, throwable2) -> {
                        cachedGoals = goals;
                        storeNewShuffle(goals, todayTimeSpan);
                    });
                } else {
                    storeNewShuffle(cachedGoals, todayTimeSpan);
                }
            });
            t.start();
        });

        // TODO: replace with string resources
        goalsReachedYesterday.setData(3, 1, "GOALS REACHED", null, "1/3");
        averageDifficultyLevelYesterday.setData(3, 2.32f, "AVERAGE DIFFICULTY LEVEL", null, "2.32");
        //difficultyLevel.setData(3, 100, -0.047038327526132406f, 2.416376306620209f, 10, 41);

        floatingEdit.setOnClickListener(e -> {
            Intent intent = new Intent(getContext(), EditGoals.class);
            startActivity(intent);
        });

        RodGraph rg = new RodGraph(getContext());
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.leftMargin = Tools.dpToPx(getContext(), 50);
        lParams.rightMargin = Tools.dpToPx(getContext(), 50);
        rg.setLayoutParams(lParams);
        List<DataValue> data = new ArrayList<>();
        data.add(new DataValue(1, "Goal 1"));
        data.add(new DataValue(0, "Goal 2"));
        data.add(new DataValue(2, "Goal 3"));
        rg.setData(data, Tools.dpToPx(getContext(), 3f), Tools.dpToPx(getContext(), 24), null);
    }

    private void setGroupIndicatorToRight() {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        expLView.setIndicatorBounds(dm.widthPixels - getDipsFromPixel(35), dm.widthPixels - getDipsFromPixel(5));
    }

    public int getDipsFromPixel(float pixels) {
        return (int) (pixels * getResources().getDisplayMetrics().density + 250.5f);
    }

   private void updateStats(Pair<Long, Long> todayTimeSpan) {
       Pair<Long, Long> pastTimeSpan = Tools.getTimeSpanFrom(1, false);
       db.getShuffleHasGoalDao().getShuffleFrom(todayTimeSpan.first, todayTimeSpan.second).subscribe((detailedShuffleHasGoals, throwable) -> {
           List<AppCompatCheckBox> goalChecks = new ArrayList<>();
           float currentDifficulty;
           AtomicReference<Float> strAvgDiff = new AtomicReference<>((float) 0);
           AtomicReference<Float> yesterdaysDifficulty = new AtomicReference<>((float) 0);
           AtomicReference<Float> currentOccFreq = new AtomicReference<>((float) 0);
           AtomicReference<Float> pastOccFreq = new AtomicReference<>((float) 0);
           float totalDifficultySum = 0;
           int count = 0;
           for (DetailedShuffleHasGoal detailedShuffleHasGoal : detailedShuffleHasGoals) {
               AppCompatCheckBox chk = new AppCompatCheckBox(getContext());
               chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
               chk.setButtonDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.checkbox_button, getContext().getTheme()));
               Drawable circle = ResourcesCompat.getDrawable(getResources(), R.drawable.small_circle, getContext().getTheme());
               chk.setCompoundDrawablesRelativeWithIntrinsicBounds(circle, circle, null, circle);
               chk.setCompoundDrawableTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), android.R.color.transparent, getContext().getTheme())));
               chk.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
               chk.setCompoundDrawablePadding(Tools.dpToPx(getContext(), 15));
               chk.setText(detailedShuffleHasGoal.description);
               chk.setChecked(detailedShuffleHasGoal.achieved);
               chk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
               chk.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                   // TODO: set to checked in database
               });
               goalChecks.add(chk);
               totalDifficultySum += detailedShuffleHasGoal.difficulty;
               count++;
           }
           currentDifficulty = totalDifficultySum / count;
           db.getGoalDao().getAverageDifficulty().subscribe((avgDiff, throwable1) -> {
               strAvgDiff.set(avgDiff.floatValue());
               db.getShuffleHasGoalDao().getShuffleFrom(pastTimeSpan.first, pastTimeSpan.second).subscribe((pastShuffleGoals, throwable2) -> {
                   float yesterdaysTotalDifficultySum = 0;
                   int yesterdayCount = 0;
                   for (DetailedShuffleHasGoal goal : pastShuffleGoals) {
                       yesterdaysTotalDifficultySum += goal.difficulty;
                       yesterdayCount++;
                   }
                   yesterdaysDifficulty.set(yesterdaysTotalDifficultySum / yesterdayCount);
                   db.getShuffleHasGoalDao().getAmountOfTotalDrawnGoals().subscribe((shuffleCount, throwable3) -> {
                       List<Integer> currentGoalIds = new ArrayList<>();
                       List<Integer> pastGoalIds = new ArrayList<>();
                       int redrawReduction = 0;
                       for (DetailedShuffleHasGoal goal : detailedShuffleHasGoals) {
                           currentGoalIds.add(goal.goalId);
                       }
                       for (DetailedShuffleHasGoal goal : pastShuffleGoals) {
                           if(currentGoalIds.contains(goal.goalId)) {
                               redrawReduction++;
                           }
                           pastGoalIds.add(goal.goalId);
                       }
                       int finalRedrawReduction = redrawReduction;
                       db.getShuffleHasGoalDao().getCountOfGoalsDrawn(currentGoalIds).subscribe((drawCounts, throwable4) -> {
                           int totalDrawCount = 0;
                           for (Integer iVal : drawCounts) {
                               totalDrawCount += iVal;
                           }
                           currentOccFreq.set(100 * totalDrawCount / (float) shuffleCount);

                           db.getShuffleHasGoalDao().getCountOfGoalsDrawn(pastGoalIds).subscribe((pastDrawCounts, throwable5) -> {
                               int totalPastDrawCount = 0;
                               for (Integer iVal : pastDrawCounts) {
                                   totalPastDrawCount += iVal;
                               }
                               pastOccFreq.set(100 * (totalPastDrawCount - finalRedrawReduction) / (float) (shuffleCount - drawCounts.size()));
                           });
                       });
                   });
               });
           });
           getActivity().runOnUiThread(() -> {
               // TODO: hide loading indicators
               currentGoalsContainer.removeViews(1, currentGoalsContainer.getChildCount() - 1);
               for (AppCompatCheckBox chk : goalChecks) {
                   currentGoalsContainer.addView(chk);
               }
               difficultySpeedometer.setData(25, currentDifficulty, 3);
               currentSelectionDiff.setText(String.format("%.1f %%", 100 * currentDifficulty / strAvgDiff.get()));
               if(yesterdaysDifficulty.get() < currentDifficulty) {
                   selectionDiffComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_up_24, getContext().getTheme()));
                   selectionDiffComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorSuccess, getContext().getTheme()));
               }
               else {
                   selectionDiffComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_down_24, getContext().getTheme()));
                   selectionDiffComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorError, getContext().getTheme()));
               }
               currentOccurrenceFreq.setText(String.format("%.2f %%", currentOccFreq.get()));
               if(pastOccFreq.get() < currentOccFreq.get()) {
                   occFreqComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_up_24, getContext().getTheme()));
                   occFreqComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorSuccess, getContext().getTheme()));
               }
               else {
                   occFreqComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_arrow_drop_down_24, getContext().getTheme()));
                   occFreqComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorError, getContext().getTheme()));
               }
           });
       });
   }

    private void storeNewShuffle(List<Goal> goals, Pair<Long, Long> todayTimeSpan) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        List<com.bitflaker.lucidsourcekit.database.goals.entities.Goal> goalsResult = Tools.getSuitableGoals(getContext(), goals, preferences.getFloat("goal_difficulty_tendency", 1.8f), preferences.getFloat("goal_difficulty_variance", 0.15f), preferences.getInt("goal_difficulty_accuracy", 100), preferences.getFloat("goal_difficulty_value_variance", 3.0f), preferences.getInt("goal_difficulty_count", 3));
        db.getShuffleDao().getLastShuffleInDay(todayTimeSpan.first, todayTimeSpan.second).subscribe(alreadyPresentShuffle -> {
            int id = alreadyPresentShuffle.shuffleId;
            List<ShuffleHasGoal> hasGoals = new ArrayList<>();
            for (Goal goal : goalsResult) {
                hasGoals.add(new ShuffleHasGoal(id, goal.goalId));
            }
            db.getShuffleHasGoalDao().deleteAllWithShuffleId(id).subscribe(() -> {
                db.getShuffleHasGoalDao().insertAll(hasGoals).subscribe(() -> {
                    updateStats(todayTimeSpan);
                });
            });
        });
    }
}