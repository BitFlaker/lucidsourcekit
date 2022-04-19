package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class Goals extends Fragment {
    private FloatingActionButton floatingEdit;
    private RangeProgress goalsReachedYesterday, averageDifficultyLevelYesterday;
    private QuadraticFunctionCurve difficultyLevel;
    private Speedometer difficultySpeedometer;
    private LinearLayout currentGoalsContainer;
    private MainDatabase db;
    private TextView currentSelectionDiff, currentOccurrenceFreq, algoDetailsDiffTend, algoDetailsDiffSpread, algoDetailsAutoAdjust;
    private ImageView selectionDiffComparison, occFreqComparison;
    private ImageButton reshuffle;
    private List<Goal> cachedGoals;
    private int cachedUpToDiffCount = -1;
    private ExpandableListView expLView;
    private ExpandableListViewAdapter expLViewAdapter;
    private MaterialButton adjustAlgorithm;
    private int newGoalAmount = -1, newSignificantDifficultyDigits = -1;
    private BottomSheetDialog bsdAdjustAlgorithm;

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
        adjustAlgorithm = getView().findViewById(R.id.btn_adjust_algorithm);
        algoDetailsDiffTend = getView().findViewById(R.id.txt_goal_algo_details_diff_tendency);
        algoDetailsDiffSpread = getView().findViewById(R.id.txt_goal_algo_details_diff_spread);
        algoDetailsAutoAdjust = getView().findViewById(R.id.txt_goal_algo_details_auto_adjust);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        algoDetailsDiffTend.setText(String.format(Locale.ENGLISH, "%.1f", preferences.getFloat("goal_difficulty_tendency", 1.8f)));
        algoDetailsDiffSpread.setText(String.format(Locale.ENGLISH, "%.2f", preferences.getFloat("goal_difficulty_variance", 0.15f)));
        algoDetailsAutoAdjust.setText(preferences.getBoolean("goal_difficulty_auto_adjust", true) ? "ENABLED" : "DISABLED"); // TODO: extract string resource

        adjustAlgorithm.setOnClickListener(e -> {
            bsdAdjustAlgorithm = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bsdAdjustAlgorithm.setContentView(R.layout.algorithm_adjustment_sheet);

            Slider easySld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_easy);
            Slider normalSld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_normal);
            Slider hardSld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_hard);
            QuadraticFunctionCurve qfc = bsdAdjustAlgorithm.findViewById(R.id.rp_difficulty_level);

            easySld.setValue(preferences.getFloat("goal_difficulty_easy_value", 0));
            normalSld.setValue(preferences.getFloat("goal_difficulty_normal_value", 0));
            hardSld.setValue(preferences.getFloat("goal_difficulty_hard_value", 0));
            qfc.setData(3, 120.5f, preferences.getFloat("goal_function_value_a", 1), preferences.getFloat("goal_function_value_b", 1), preferences.getFloat("goal_function_value_c", 1), 41);
            qfc.setZeroMinValue(true);
            easySld.setOnTouchListener(preventScrollOnTouch());
            normalSld.setOnTouchListener(preventScrollOnTouch());
            hardSld.setOnTouchListener(preventScrollOnTouch());

            ((Slider) bsdAdjustAlgorithm.findViewById(R.id.sld_algo_avg_goal_difficulty_spread)).setValue(preferences.getFloat("goal_difficulty_variance", 0.15f));
            ((Slider) bsdAdjustAlgorithm.findViewById(R.id.sld_algo_avg_goal_difficulty)).setValue(preferences.getFloat("goal_difficulty_tendency", 1.8f));
            ((SwitchCompat) bsdAdjustAlgorithm.findViewById(R.id.swt_auto_adjust_goal_diff)).setChecked(preferences.getBoolean("goal_difficulty_auto_adjust", true));

            bsdAdjustAlgorithm.findViewById(R.id.btn_cancel_algo_adjust).setOnClickListener(e2 -> {
                bsdAdjustAlgorithm.cancel();
            });

            bsdAdjustAlgorithm.findViewById(R.id.btn_save_algo_adjust).setOnClickListener(e2 -> {
                SharedPreferences.Editor editor = preferences.edit();

                editor.putFloat("goal_difficulty_variance", ((Slider) bsdAdjustAlgorithm.findViewById(R.id.sld_algo_avg_goal_difficulty_spread)).getValue());
                editor.putFloat("goal_difficulty_tendency", ((Slider) bsdAdjustAlgorithm.findViewById(R.id.sld_algo_avg_goal_difficulty)).getValue());
                editor.putBoolean("goal_difficulty_auto_adjust", ((SwitchCompat) bsdAdjustAlgorithm.findViewById(R.id.swt_auto_adjust_goal_diff)).isChecked());

                editor.putFloat("goal_function_value_a", qfc.getA());
                editor.putFloat("goal_function_value_b", qfc.getB());
                editor.putFloat("goal_function_value_c", qfc.getC());
                editor.putFloat("goal_difficulty_easy_value", easySld.getValue());
                editor.putFloat("goal_difficulty_normal_value", normalSld.getValue());
                editor.putFloat("goal_difficulty_hard_value", hardSld.getValue());
                if(newGoalAmount != -1) {
                    editor.putInt("goal_difficulty_count", newGoalAmount);
                }
                if(newSignificantDifficultyDigits != -1) {
                    editor.putInt("goal_difficulty_accuracy", newSignificantDifficultyDigits);
                }

                try {
                    EditText valVariance = (EditText) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_valuation_variance_algo);
                    editor.putFloat("goal_difficulty_value_variance", Float.valueOf(valVariance.getText().toString()));
                }
                catch(Exception ex){
                    // TODO handle invalid input data
                }

                editor.apply();

                algoDetailsDiffTend.setText(String.format(Locale.ENGLISH, "%.1f", preferences.getFloat("goal_difficulty_tendency", 1.8f)));
                algoDetailsDiffSpread.setText(String.format(Locale.ENGLISH, "%.2f", preferences.getFloat("goal_difficulty_variance", 0.15f)));
                algoDetailsAutoAdjust.setText(preferences.getBoolean("goal_difficulty_auto_adjust", true) ? "ENABLED" : "DISABLED"); // TODO: extract string resource

                bsdAdjustAlgorithm.dismiss();
            });

            easySld.addOnChangeListener((slider, value, fromUser) -> updateQuadraticFunction(easySld, normalSld, hardSld, qfc));
            normalSld.addOnChangeListener((slider, value, fromUser) -> updateQuadraticFunction(easySld, normalSld, hardSld, qfc));
            hardSld.addOnChangeListener((slider, value, fromUser) -> updateQuadraticFunction(easySld, normalSld, hardSld, qfc));

            ((MaterialButton) bsdAdjustAlgorithm.findViewById(R.id.btn_expander_advanced)).setOnClickListener(e2 -> {
                LinearLayout expanded = bsdAdjustAlgorithm.findViewById(R.id.ll_advanced_adjustments);
                int visibility = (expanded.getVisibility() + View.GONE) % (View.GONE * 2);
                expanded.setVisibility(visibility);
                switch (visibility){
                    case View.VISIBLE:
                        Drawable contract = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_keyboard_arrow_up_24, getContext().getTheme());
                        ((MaterialButton) e2).setCompoundDrawablesWithIntrinsicBounds(null, null, contract, null);
                        BottomSheetBehavior.from((FrameLayout)bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
                        BottomSheetBehavior.from((FrameLayout)bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet)).setDraggable(false);
                        break;
                    case View.GONE:
                        Drawable expand = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_keyboard_arrow_down_24, getContext().getTheme());
                        ((MaterialButton) e2).setCompoundDrawablesWithIntrinsicBounds(null, null, expand, null);
                        BottomSheetBehavior.from((FrameLayout)bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_COLLAPSED);
                        BottomSheetBehavior.from((FrameLayout)bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet)).setDraggable(true);
                        break;
                    default:
                        break;
                }
            });

            ((MaterialButton) bsdAdjustAlgorithm.findViewById(R.id.btn_change_goal_count)).setOnClickListener(e2 -> createNumberPickerDialog(NumberPickableFields.GOAL_COUNT, 1, 10, newGoalAmount == -1 ? preferences.getInt("goal_difficulty_count", 3) : newGoalAmount));
            ((EditText) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_valuation_variance_algo)).setText(String.format(Locale.ENGLISH, "%.1f", preferences.getFloat("goal_difficulty_value_variance", 10.0f)));
            ((MaterialButton) bsdAdjustAlgorithm.findViewById(R.id.btn_change_difficulty_significant_digits)).setOnClickListener(e2 -> createNumberPickerDialog(NumberPickableFields.GOAL_SIGNIFICANT_DIGITS, 1, 6, newSignificantDifficultyDigits == -1 ? getPotency(preferences.getInt("goal_difficulty_accuracy", 100)) : getPotency(newSignificantDifficultyDigits)));
            ((TextView) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_difficulty_significant_algo)).setText(Integer.toString(getPotency(preferences.getInt("goal_difficulty_accuracy", 100))));
            ((TextView) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_count_algo)).setText(Integer.toString(preferences.getInt("goal_difficulty_count", 3)));

            bsdAdjustAlgorithm.show();
        });

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

    private int getPotency(int number) {
        int i = 0;
        int num = number;
        while(num % 10 == 0 && num != 0) {
            i++;
            num = num / 10;
        }
        return i;
    }

    @NonNull
    private View.OnTouchListener preventScrollOnTouch() {
        return (v, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            v.onTouchEvent(event);
            return true;
        };
    }

    public void createNumberPickerDialog(NumberPickableFields field, int min, int max, int currValue)
    {
        final NumberPicker numberPicker = new NumberPicker(getActivity());
        numberPicker.setMaxValue(max);
        numberPicker.setMinValue(min);
        numberPicker.setValue(currValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(numberPicker);
        builder.setTitle("Change goal count");
        builder.setMessage("Set an amount: ");
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> updateGoalCount(numberPicker.getValue(), field));
        builder.setNegativeButton(getResources().getString(R.string.cancel), null);
        builder.create();
        builder.show();
    }

    public void updateGoalCount(int amount, NumberPickableFields field) {
        switch (field){
            case GOAL_COUNT:
                newGoalAmount = amount;
                ((TextView) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_count_algo)).setText(Integer.toString(newGoalAmount));
                break;
            case GOAL_SIGNIFICANT_DIGITS:
                newSignificantDifficultyDigits = (int) Math.pow(10, amount);
                ((TextView) bsdAdjustAlgorithm.findViewById(R.id.txt_goal_difficulty_significant_algo)).setText(Integer.toString(amount));
                break;
            default:
                break;
        }
    }

    public void updateQuadraticFunction(Slider easySlider, Slider normalSlider, Slider hardSlider, QuadraticFunctionCurve qfc) {
        if (cachedGoals == null) {
            db.getGoalDao().getAllSingle().subscribe((goals, throwable2) -> {
                cachedGoals = goals;
                calculateQuadraticCurve(easySlider, normalSlider, hardSlider, qfc);
            });
        }
        else {
            calculateQuadraticCurve(easySlider, normalSlider, hardSlider, qfc);
        }
    }

    private void calculateQuadraticCurve(Slider easySlider, Slider normalSlider, Slider hardSlider, QuadraticFunctionCurve qfc) {
        if (cachedUpToDiffCount == -1) {
            cachedUpToDiffCount = getCountUpToDifficulty(2.0f);
        }
        PointF weight1 = new PointF(0f, easySlider.getValue());
        PointF weight2 = new PointF(cachedUpToDiffCount - 1, normalSlider.getValue());
        PointF weight3 = new PointF(cachedGoals.size() - 1, hardSlider.getValue());
        double[] points = Tools.calculateQuadraticFunction(weight1, weight2, weight3);
        qfc.setData(3, 120.5f, (float)points[0], (float)points[1], (float)points[2], cachedGoals.size());
    }

    private int getCountUpToDifficulty(float diff) {
        for (int i = 0; i < cachedGoals.size(); i++) {
            if (cachedGoals.get(i).difficulty > diff) {
                return i;
            }
        }
        return cachedGoals.size();
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

    private enum NumberPickableFields {
        GOAL_COUNT,
        GOAL_SIGNIFICANT_DIGITS
    }
}