package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.QuadraticFunctionCurve;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class Goals extends Fragment {
    private FloatingActionButton floatingEdit;
    private Speedometer difficultySpeedometer;
    private LinearLayout currentGoalsContainer;
    private MainDatabase db;
    private TextView currentSelectionDiff, currentOccurrenceFreq, currentSelectionDiffPart, currentOccurrenceFreqPart, yGoalsAchieved, yGoalsAchievedPart, yGoalsDiff, yGoalsDiffPart, yGoalsOccFreq, yGoalsOccFreqPart, yGoalsSelDiff, yGoalsSelDiffPart;
    private ImageView selectionDiffComparison, occFreqComparison;
    private ImageButton reshuffle;
    private List<Goal> cachedGoals;
    private int cachedUpToDiffCount = -1;
    private ImageButton adjustAlgorithm;
    private int newGoalAmount = -1, newSignificantDifficultyDigits = -1;
    private BottomSheetDialog bsdAdjustAlgorithm;
    private RecyclerView quickScrollAdjustmentsContainer;
    private ViewPager2 mainViewPager;

    public Goals() {

    }

    public Goals(ViewPager2 mainViewPager) {
        this.mainViewPager = mainViewPager;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_goals_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        floatingEdit = getView().findViewById(R.id.btn_add_journal_entry);
        difficultySpeedometer = getView().findViewById(R.id.som_difficulty);
        currentGoalsContainer = getView().findViewById(R.id.ll_current_goals_container);
        currentSelectionDiff = getView().findViewById(R.id.txt_current_selection_diff_full);
        currentOccurrenceFreq = getView().findViewById(R.id.txt_occurrence_freq_full);
        currentSelectionDiffPart = getView().findViewById(R.id.txt_current_selection_diff_part);
        currentOccurrenceFreqPart = getView().findViewById(R.id.txt_occurrence_freq_part);
        selectionDiffComparison = getView().findViewById(R.id.img_selection_diff_comparison);
        occFreqComparison = getView().findViewById(R.id.img_occ_freq_comparison);
        reshuffle = getView().findViewById(R.id.btn_reshuffle_goals);
        adjustAlgorithm = getView().findViewById(R.id.btn_adjust_algorithm);
        quickScrollAdjustmentsContainer = getView().findViewById(R.id.rcv_goal_advices);
        yGoalsAchieved = getView().findViewById(R.id.txt_ygoals_achieved);
        yGoalsAchievedPart = getView().findViewById(R.id.txt_ygoals_achieved_part);
        yGoalsDiff = getView().findViewById(R.id.txt_ygoals_difficulty);
        yGoalsDiffPart = getView().findViewById(R.id.txt_ygoals_difficulty_part);
        yGoalsOccFreq = getView().findViewById(R.id.txt_ygoals_occ_freq);
        yGoalsOccFreqPart = getView().findViewById(R.id.txt_ygoals_occ_freq_part);
        yGoalsSelDiff = getView().findViewById(R.id.txt_ygoals_sel_difficulty);
        yGoalsSelDiffPart = getView().findViewById(R.id.txt_ygoals_sel_difficulty_part);

        @ColorInt int primVar = Tools.getAttrColor(R.attr.colorPrimaryVariant, getContext().getTheme());
        List<GoalAdvice> advices = new ArrayList<>();
        advices.add(new GoalAdvice("GOAL COUNT", "Increase goal count", "Increase the goal count to 4 for more difficulty", R.drawable.ic_round_plus_one_24, Color.TRANSPARENT));
        advices.add(new GoalAdvice("NOTIFICATIONS", "Enable notifications", "Enable notifications to be reminded to look out for the targets", R.drawable.ic_baseline_notifications_active_24, Color.TRANSPARENT));
        advices.add(new GoalAdvice("SHUFFLE", "Shuffle goals", "Shuffle the goals again to get new and possibly better ones", R.drawable.ic_baseline_shuffle_24, Color.TRANSPARENT));
        advices.add(new GoalAdvice("DIFFICULTY", "Increase goals difficulty", "Increase the target goal difficulty to 2.3 for a bigger challenge", R.drawable.ic_baseline_vertical_align_top_24, Color.TRANSPARENT));
        RecyclerViewAdapterGoalAdvice goalAdvice = new RecyclerViewAdapterGoalAdvice(getContext(), advices);


        quickScrollAdjustmentsContainer.setAdapter(goalAdvice);
//        quickScrollAdjustmentsContainer.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        quickScrollAdjustmentsContainer.setLayoutManager(new LinearLayoutManager(getContext()));
//        quickScrollAdjustmentsContainer.addOnItemTouchListener(horizontalScrollViewPger);

        db = MainDatabase.getInstance(getContext());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        adjustAlgorithm.setOnClickListener(e -> {
            if (cachedGoals == null) {
                db.getGoalDao().getAllSingle().subscribe((goals, throwable2) -> {
                    cachedGoals = goals;
                    setupAdjustAlgorithmSheet(preferences);
                });
            }
            else {
                setupAdjustAlgorithmSheet(preferences);
            }
        });

        Pair<Long, Long> todayTimeSpan = Tools.getTimeSpanFrom(0, false);
        updateStats(todayTimeSpan);

        reshuffle.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), Tools.getPopupTheme()), reshuffle);

            popup.getMenuInflater().inflate(R.menu.more_goals_options, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.itm_shuffle) {
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
                }
                else if (item.getItemId() == R.id.itm_edit_goals) {
                    startActivity(new Intent(getContext(), EditGoals.class));
                }
                return true;
            });
            popup.show();
        });
    }

    private void setupAdjustAlgorithmSheet(SharedPreferences preferences) {
        bsdAdjustAlgorithm = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
        bsdAdjustAlgorithm.setContentView(R.layout.algorithm_adjustment_sheet);

        Slider easySld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_easy);
        Slider normalSld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_normal);
        Slider hardSld = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_hard);
        QuadraticFunctionCurve qfc = bsdAdjustAlgorithm.findViewById(R.id.rp_difficulty_level);

        easySld.setValue(preferences.getFloat("goal_difficulty_easy_value", 0));
        normalSld.setValue(preferences.getFloat("goal_difficulty_normal_value", 0));
        hardSld.setValue(preferences.getFloat("goal_difficulty_hard_value", 0));
        qfc.setData(3, 120.5f, preferences.getFloat("goal_function_value_a", 1), preferences.getFloat("goal_function_value_b", 1), preferences.getFloat("goal_function_value_c", 1), cachedGoals.size());
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
        calculateQuadraticCurve(easySlider, normalSlider, hardSlider, qfc);
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

   private void updateStats(Pair<Long, Long> todayTimeSpan) {
       Pair<Long, Long> pastTimeSpan = Tools.getTimeSpanFrom(1, false);
       db.getShuffleHasGoalDao().getShuffleFrom(todayTimeSpan.first, todayTimeSpan.second).subscribe((detailedShuffleHasGoals, throwable) -> {
           List<AppCompatCheckBox> goalChecks = new ArrayList<>();
           float currentDifficulty;
           AtomicReference<Float> strAvgDiff = new AtomicReference<>((float) 0);
           AtomicReference<Integer> yesterdayCountAtmc = new AtomicReference<>(0);
           AtomicReference<Integer> achievedCountAtmc = new AtomicReference<>(0);
           AtomicReference<Float> yesterdaysDifficulty = new AtomicReference<>((float) 0);
           AtomicReference<Float> currentOccFreq = new AtomicReference<>((float) 0);
           AtomicReference<Float> pastOccFreq = new AtomicReference<>((float) 0);
           float totalDifficultySum = 0;
           int count = 0;
           for (DetailedShuffleHasGoal detailedShuffleHasGoal : detailedShuffleHasGoals) {
               AppCompatCheckBox chk = new AppCompatCheckBox(getContext());
               chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
               chk.setButtonDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.checkbox_button, getContext().getTheme()));
               chk.setCompoundDrawableTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), android.R.color.transparent, getContext().getTheme())));
               chk.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
               chk.setHighlightColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
               int dp20 = Tools.dpToPx(getContext(), 20);
               int dp15 = Tools.dpToPx(getContext(), 15);
               chk.setPadding(dp15, dp20,0, dp20);
               chk.setPaintFlags(detailedShuffleHasGoal.achieved ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
               chk.setText(detailedShuffleHasGoal.description);
               chk.setChecked(detailedShuffleHasGoal.achieved);
               chk.setTextColor(Tools.getAttrColor(detailedShuffleHasGoal.achieved ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
               chk.setTypeface(Typeface.create(chk.getTypeface(), detailedShuffleHasGoal.achieved ? Typeface.NORMAL : Typeface.BOLD));
               chk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
               chk.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                   db.getShuffleHasGoalDao().setAchievedState(detailedShuffleHasGoal.shuffleId, detailedShuffleHasGoal.goalId, isChecked);
                   chk.setTextColor(Tools.getAttrColor(isChecked ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
                   chk.setTypeface(Typeface.create(chk.getTypeface(), isChecked ? Typeface.NORMAL : Typeface.BOLD));
                   chk.setPaintFlags(isChecked ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
//                   LinearLayout parent = ((LinearLayout) chk.getParent());
//                   if(isChecked) {
//                       parent.removeView(chk);
//                       parent.addView(chk);
//                   }
//                   else if (parent.indexOfChild(chk) != 0 && ((AppCompatCheckBox) parent.getChildAt(parent.indexOfChild(chk) - 1)).isChecked()) {
//                       parent.removeView(chk);
//                       parent.addView(chk, 0);
//                   }
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
                   int achievedCount = 0;
                   for (DetailedShuffleHasGoal goal : pastShuffleGoals) {
                       yesterdaysTotalDifficultySum += goal.difficulty;
                        if(goal.achieved) {
                            achievedCount++;
                        }
                       yesterdayCount++;
                   }
                   float yesterdayDiff = yesterdaysTotalDifficultySum / yesterdayCount;
                   yesterdayCountAtmc.set(yesterdayCount);
                   achievedCountAtmc.set(achievedCount);
                   yesterdaysDifficulty.set(yesterdayDiff);
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
               currentGoalsContainer.removeAllViews();
               for (AppCompatCheckBox chk : goalChecks) {
                   currentGoalsContainer.addView(chk);
               }
               difficultySpeedometer.setData(25, currentDifficulty, 3);

               String[] numParts = getDecimalNumParts(100 * (float)achievedCountAtmc.get() / yesterdayCountAtmc.get(), 2);
               if(!Float.isNaN(yesterdaysDifficulty.get())){
                   yGoalsDiff.setText(String.format(Locale.ENGLISH, "%.1f", yesterdaysDifficulty.get()));
                   yGoalsDiffPart.setText(String.format(Locale.ENGLISH, "%s%s", "/", yesterdayCountAtmc.get()));
               }
               else {
                   yGoalsDiff.setVisibility(View.GONE);
                   yGoalsDiffPart.setText("- / -");
               }
               yGoalsAchieved.setText(numParts[0]);
               yGoalsAchievedPart.setText(String.format(Locale.ENGLISH, "%s%s", numParts[1], "%"));

               // #### selection difficulty setup
               numParts = getDecimalNumParts(100 * currentDifficulty / strAvgDiff.get(), 1);
               currentSelectionDiff.setText(numParts[0]);
               currentSelectionDiffPart.setText(String.format(Locale.ENGLISH, "%s%s", numParts[1], "%"));
               if(yesterdaysDifficulty.get() < currentDifficulty) {
                   selectionDiffComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_arrow_downward_24, getContext().getTheme()));
               }
               else {
                   selectionDiffComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_arrow_upward_24, getContext().getTheme()));
               }
               selectionDiffComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorSecondaryVariant, getContext().getTheme()));
               numParts = getDecimalNumParts(100 * yesterdaysDifficulty.get() / strAvgDiff.get(), 1);
               yGoalsSelDiff.setText(numParts[0]);
               yGoalsSelDiffPart.setText(String.format(Locale.ENGLISH, "%s%s", numParts[1], "%"));

               // #### occurrence frequency setup
               numParts = getDecimalNumParts(currentOccFreq.get(), 2);
               currentOccurrenceFreq.setText(numParts[0]);
               currentOccurrenceFreqPart.setText(String.format(Locale.ENGLISH, "%s%s", numParts[1], "%"));
               if(pastOccFreq.get() < currentOccFreq.get()) {
                   occFreqComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_arrow_downward_24, getContext().getTheme()));
               }
               else {
                   occFreqComparison.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_round_arrow_upward_24, getContext().getTheme()));
               }
               occFreqComparison.setImageTintList(Tools.getAttrColorStateList(R.attr.colorSecondaryVariant, getContext().getTheme()));
               numParts = getDecimalNumParts(pastOccFreq.get(), 2);
               yGoalsOccFreq.setText(numParts[0]);
               yGoalsOccFreqPart.setText(String.format(Locale.ENGLISH, "%s%s", numParts[1], "%"));
           });
       });
   }

    private String[] getDecimalNumParts(float value, int decimals) {
        String[] numParts = new String[2];
        int fullSelDiff = (int)value;
        numParts[0] = String.format(Locale.ENGLISH, "%d", fullSelDiff);
        numParts[1] = Float.isNaN(value % fullSelDiff) ? "" : String.format(Locale.ENGLISH, "%." + decimals + "f", value % fullSelDiff).substring(1);
        return numParts;
    }

    private void setAdviceData(int pastGoalCount, float pastDifficulty, int achievedCount) {
        // TODO: check values and give appropriate advice in shortcut form of dropdown menu
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

//    private RecyclerView.OnItemTouchListener horizontalScrollViewPger = new RecyclerView.OnItemTouchListener() {
//        int lastX = 0;
//        @Override
//        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
//            switch (e.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    lastX = (int) e.getX();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    boolean isScrollingRight = e.getX() < lastX;
//                    if ((isScrollingRight && ((LinearLayoutManager) quickScrollAdjustmentsContainer.getLayoutManager()).findLastCompletelyVisibleItemPosition() == quickScrollAdjustmentsContainer.getAdapter().getItemCount() - 1) ||
//                            (!isScrollingRight && ((LinearLayoutManager) quickScrollAdjustmentsContainer.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0)) {
//                        mainViewPager.setUserInputEnabled(true);
//                    } else {
//                        mainViewPager.setUserInputEnabled(false);
//                    }
//                    break;
//                case MotionEvent.ACTION_UP:
//                    lastX = 0;
//                    mainViewPager.setUserInputEnabled(true);
//                    break;
//            }
//            return false;
//        }
//
//        @Override
//        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { }
//
//        @Override
//        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
//    };

    private enum NumberPickableFields {
        GOAL_COUNT,
        GOAL_SIGNIFICANT_DIGITS
    }
}