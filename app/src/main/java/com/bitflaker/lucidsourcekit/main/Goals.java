package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Speedometer;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.notification.NotificationManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Arrays;
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
        advices.add(new GoalAdvice("NOTIFICATIONS", "Configure notifications", "Configure notifications to remind you to look out for the targets", R.drawable.ic_baseline_notifications_active_24, Color.TRANSPARENT, advice -> {
            startActivity(new Intent(getContext(), NotificationManager.class));
        }));
        advices.add(new GoalAdvice("GOAL COUNT", "Increase goal count", "Increase the goal count to 4 for more difficulty", R.drawable.ic_round_plus_one_24, Color.TRANSPARENT, advice -> {}));
        advices.add(new GoalAdvice("SHUFFLE", "Shuffle goals", "Shuffle the goals again to get new and possibly better ones", R.drawable.ic_baseline_shuffle_24, Color.TRANSPARENT, advice -> {}));
        advices.add(new GoalAdvice("DIFFICULTY", "Increase goals difficulty", "Increase the target goal difficulty to 2.3 for a bigger challenge", R.drawable.ic_baseline_vertical_align_top_24, Color.TRANSPARENT, advice -> {}));
        RecyclerViewAdapterGoalAdvice goalAdvice = new RecyclerViewAdapterGoalAdvice(getContext(), advices);

        quickScrollAdjustmentsContainer.setAdapter(goalAdvice);
//        quickScrollAdjustmentsContainer.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        quickScrollAdjustmentsContainer.setLayoutManager(new LinearLayoutManager(getContext()));
//        quickScrollAdjustmentsContainer.addOnItemTouchListener(horizontalScrollViewPger);

        db = MainDatabase.getInstance(getContext());
        adjustAlgorithm.setOnClickListener(e -> {
            if (cachedGoals == null) {
                db.getGoalDao().getAllSingle().subscribe((goals, throwable2) -> {
                    cachedGoals = goals;
                    setupAdjustAlgorithmSheet();
                });
            }
            else {
                setupAdjustAlgorithmSheet();
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

    private void setupAdjustAlgorithmSheet() {
        bsdAdjustAlgorithm = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
        bsdAdjustAlgorithm.setContentView(R.layout.sheet_algorithm_adjustment);

        Slider sldCommon = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_common);
        Slider sldUncommon = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_uncommon);
        Slider sldRare = bsdAdjustAlgorithm.findViewById(R.id.sld_algo_diff_rare);
        Slider sldGoalCount = bsdAdjustAlgorithm.findViewById(R.id.sld_goal_count);
        MaterialSwitch swtAutoAdjustGoalDifficulty = bsdAdjustAlgorithm.findViewById(R.id.swt_auto_adjust_goal_difficulty);
        TextView txtValueCommon = bsdAdjustAlgorithm.findViewById(R.id.txt_sld_value_common);
        TextView txtValueUncommon = bsdAdjustAlgorithm.findViewById(R.id.txt_sld_value_uncommon);
        TextView txtValueRare = bsdAdjustAlgorithm.findViewById(R.id.txt_sld_value_rare);
        TextView txtValueGoalCount = bsdAdjustAlgorithm.findViewById(R.id.txt_sld_value_goal_count);
        MaterialButton btnSave = bsdAdjustAlgorithm.findViewById(R.id.btn_save_algo_adjust);
        Slider[] weightSliders = new Slider[] { sldCommon, sldUncommon, sldRare };

        sldCommon.addOnChangeListener((slider, value, fromUser) -> {
            txtValueCommon.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sldUncommon.addOnChangeListener((slider, value, fromUser) -> {
            txtValueUncommon.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sldRare.addOnChangeListener((slider, value, fromUser) -> {
            txtValueRare.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sldGoalCount.addOnChangeListener((slider, value, fromUser) -> {
            txtValueGoalCount.setText(String.format(Locale.getDefault(), "%d", (int) sldGoalCount.getValue()));
        });

        DataStoreManager dsManager = DataStoreManager.getInstance();
        sldCommon.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON).blockingFirst());
        sldUncommon.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON).blockingFirst());
        sldRare.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE).blockingFirst());
        sldGoalCount.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT).blockingFirst());
        swtAutoAdjustGoalDifficulty.setChecked(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST).blockingFirst());

        btnSave.setOnClickListener(e2 -> {
            dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST, swtAutoAdjustGoalDifficulty.isChecked()).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON, sldCommon.getValue()).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON, sldUncommon.getValue()).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE, sldRare.getValue()).blockingSubscribe();
            dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT, (int) sldGoalCount.getValue()).blockingSubscribe();
            bsdAdjustAlgorithm.dismiss();
        });

        bsdAdjustAlgorithm.show();
    }

    private void setSliderProportions(Slider[] sliders, Slider slider) {
        if(sliders == null || slider == null || sliders.length == 0) {
            return;
        }

        float total = 0.0f;
        int otherSliderCounter = 0;
        Slider[] otherSliders = new Slider[sliders.length - 1];
        for (Slider otherSlider : sliders) {
            if (otherSlider != slider) {
                otherSliders[otherSliderCounter++] = otherSlider;
                total += otherSlider.getValue();
            }
        }

        float[] parts = new float[otherSliders.length];
        if(total == 0) {
            Arrays.fill(parts, 1.0f / otherSliders.length);
        }
        else {
            for (int i = 0; i < otherSliders.length; i++) {
                parts[i] = otherSliders[i].getValue() / total;
            }
        }

        float diff = 1 - (total + slider.getValue());

        for (int i = 0; i < otherSliders.length; i++) {
            Slider current = otherSliders[i];
            current.setValue(Math.min(Math.max(current.getValue() + parts[i] * diff, 0), 1));
        }
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
               MaterialCheckBox chk = new MaterialCheckBox(getContext());
               chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
               chk.setButtonDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.checkbox_button_round, getContext().getTheme()));
               chk.setButtonTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_icon_check_change, getContext().getTheme()));
               chk.setButtonIconDrawable(Tools.resizeDrawable(getResources(), ResourcesCompat.getDrawable(getResources(), R.drawable.round_check_24, getContext().getTheme()), Tools.dpToPx(getContext(), 18), Tools.dpToPx(getContext(), 18)));
               chk.setButtonIconTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_button_icon_check, getContext().getTheme()));
//               chk.setCompoundDrawableTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(), android.R.color.transparent, getContext().getTheme())));
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
                           }).dispose();
                       }).dispose();
                   }).dispose();
               }).dispose();
           }).dispose();
           getActivity().runOnUiThread(() -> {
               // TODO: hide loading indicators
               currentGoalsContainer.removeAllViews();
               for (AppCompatCheckBox chk : goalChecks) {
                   currentGoalsContainer.addView(chk);
               }
               difficultySpeedometer.setDescription("Today's goals combined\ndifficulty rating");
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
       }).dispose();
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
        List<Goal> goalsResult = Tools.getSuitableGoals(goals);
        db.getShuffleDao().getLastShuffleInDay(todayTimeSpan.first, todayTimeSpan.second).subscribe(alreadyPresentShuffle -> {
            int id = alreadyPresentShuffle.shuffleId;
            List<ShuffleHasGoal> hasGoals = new ArrayList<>();
            for (Goal goal : goalsResult) {
                hasGoals.add(new ShuffleHasGoal(id, goal.goalId));
            }
            db.getShuffleHasGoalDao().deleteAllWithShuffleId(id).subscribe(() -> {
                db.getShuffleHasGoalDao().insertAll(hasGoals).subscribe(() -> {
                    updateStats(todayTimeSpan);
                }).dispose();
            }).dispose();
        }).dispose();
    }
}