package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Speedometer;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.main.goals.GoalStatisticsCalculator;
import com.bitflaker.lucidsourcekit.notification.NotificationManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Goals extends Fragment {
    private Speedometer difficultySpeedometer;
    private LinearLayout currentGoalsContainer, pastGoalRatings;
    private MainDatabase db;
    private TextView currentSelectionDiff, currentOccurrenceFreq, currentSelectionDiffPart, currentOccurrenceFreqPart, yGoalsAchieved, yGoalsAchievedPart, yGoalsDiff, yGoalsDiffPart, yGoalsOccFreq, yGoalsOccFreqPart, yGoalsSelDiff, yGoalsSelDiffPart;
    private ImageView selectionDiffComparison, occFreqComparison;
    private ImageButton reshuffle;
    private MaterialButton adjustAlgorithm;
    private BottomSheetDialog bsdAdjustAlgorithm;
    private RecyclerView quickScrollAdjustmentsContainer;
    private CompositeDisposable compositeDisposable;
    private MaterialCardView pastGoalsAchieved, pastGoalsOccurrenceRating, noDataPastGoals;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goals, container, false);

        view.findViewById(R.id.txt_goals_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));

        pastGoalRatings = view.findViewById(R.id.ll_past_goals_ratings);
        pastGoalsAchieved = view.findViewById(R.id.crd_past_goals_achieved);
        pastGoalsOccurrenceRating = view.findViewById(R.id.crd_past_goals_occurrence_rating);
        noDataPastGoals = view.findViewById(R.id.crd_no_data_past_goals);
        difficultySpeedometer = view.findViewById(R.id.som_difficulty);
        currentGoalsContainer = view.findViewById(R.id.ll_current_goals_container);
        currentSelectionDiff = view.findViewById(R.id.txt_current_selection_diff_full);
        currentOccurrenceFreq = view.findViewById(R.id.txt_occurrence_freq_full);
        currentSelectionDiffPart = view.findViewById(R.id.txt_current_selection_diff_part);
        currentOccurrenceFreqPart = view.findViewById(R.id.txt_occurrence_freq_part);
        selectionDiffComparison = view.findViewById(R.id.img_selection_diff_comparison);
        occFreqComparison = view.findViewById(R.id.img_occ_freq_comparison);
        reshuffle = view.findViewById(R.id.btn_reshuffle_goals);
        adjustAlgorithm = view.findViewById(R.id.btn_adjust_algorithm);
        quickScrollAdjustmentsContainer = view.findViewById(R.id.rcv_goal_advices);
        yGoalsAchieved = view.findViewById(R.id.txt_ygoals_achieved);
        yGoalsAchievedPart = view.findViewById(R.id.txt_ygoals_achieved_part);
        yGoalsDiff = view.findViewById(R.id.txt_ygoals_difficulty);
        yGoalsDiffPart = view.findViewById(R.id.txt_ygoals_difficulty_part);
        yGoalsOccFreq = view.findViewById(R.id.txt_ygoals_occ_freq);
        yGoalsOccFreqPart = view.findViewById(R.id.txt_ygoals_occ_freq_part);
        yGoalsSelDiff = view.findViewById(R.id.txt_ygoals_sel_difficulty);
        yGoalsSelDiffPart = view.findViewById(R.id.txt_ygoals_sel_difficulty_part);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        compositeDisposable = new CompositeDisposable();

        difficultySpeedometer.setDescription("Today's goals average\noccurrence rating");

        List<GoalAdvice> advices = new ArrayList<>();
        advices.add(new GoalAdvice("NOTIFICATIONS", "Configure notifications", "Configure notifications to remind you to look out for the targets", R.drawable.ic_baseline_notifications_active_24, Color.TRANSPARENT, advice -> {
            startActivity(new Intent(getContext(), NotificationManager.class));
        }));
        advices.add(new GoalAdvice("GOAL COUNT", "Increase goal count", "Increase the goal count to 4 for more difficulty", R.drawable.ic_round_plus_one_24, Color.TRANSPARENT, advice -> {}));
        advices.add(new GoalAdvice("SHUFFLE", "Shuffle goals", "Shuffle the goals again to get new and possibly better ones", R.drawable.ic_baseline_shuffle_24, Color.TRANSPARENT, advice -> {}));
        advices.add(new GoalAdvice("DIFFICULTY", "Increase goals difficulty", "Increase the target goal difficulty to 2.3 for a bigger challenge", R.drawable.ic_baseline_vertical_align_top_24, Color.TRANSPARENT, advice -> {}));
        RecyclerViewAdapterGoalAdvice goalAdvice = new RecyclerViewAdapterGoalAdvice(getContext(), advices);

        quickScrollAdjustmentsContainer.setAdapter(goalAdvice);
        quickScrollAdjustmentsContainer.setLayoutManager(new LinearLayoutManager(getContext()));

        db = MainDatabase.getInstance(getContext());
        adjustAlgorithm.setOnClickListener(e -> setupAdjustAlgorithmSheet());

        updateStats();

        reshuffle.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), R.style.Theme_LucidSourceKit_PopupMenu), reshuffle);
//            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), Tools.getPopupTheme()), reshuffle);
            popup.getMenuInflater().inflate(R.menu.more_goals_options, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.itm_shuffle) {
                    new Thread(() -> storeNewShuffle()).start();
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
            saveGoalAlgorithm(sldCommon.getValue(), sldUncommon.getValue(), sldRare.getValue(), (int) sldGoalCount.getValue(), swtAutoAdjustGoalDifficulty.isChecked());
            bsdAdjustAlgorithm.dismiss();
        });

        // TODO only ask whether or not to save changes if actual changes were made
        bsdAdjustAlgorithm.setOnCancelListener(e -> {
            new MaterialAlertDialogBuilder(getContext()).setTitle("Save changes").setMessage("Do you want to save all changes made to the goal algorithm?")
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                        saveGoalAlgorithm(sldCommon.getValue(), sldUncommon.getValue(), sldRare.getValue(), (int) sldGoalCount.getValue(), swtAutoAdjustGoalDifficulty.isChecked());
                    })
                    .setNegativeButton(getResources().getString(R.string.no), null)
                    .show();
        });

        bsdAdjustAlgorithm.show();
    }

    private static void saveGoalAlgorithm(float valueCommon, float valueUncommon, float valueRare, int goalCount, boolean autoAdjustGoalDifficulty) {
        DataStoreManager dsManager = DataStoreManager.getInstance();
        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST, autoAdjustGoalDifficulty).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON, valueCommon).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON, valueUncommon).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE, valueRare).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT, goalCount).blockingSubscribe();
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

    private void updateStats() {
        Pair<Long, Long> tsToday = Tools.getTimeSpanFrom(0, false);
        Pair<Long, Long> tsYesterday = Tools.getTimeSpanFrom(1, false);

        GoalStatisticsCalculator statsCalcToday = new GoalStatisticsCalculator(db, tsToday.first, tsToday.second);
        GoalStatisticsCalculator statsCalcYesterday = new GoalStatisticsCalculator(db, tsYesterday.first, tsYesterday.second);

        compositeDisposable.add(Single.zip(
                statsCalcToday.calculate().subscribeOn(Schedulers.io()),
                statsCalcYesterday.calculate().subscribeOn(Schedulers.io()),
                (statsToday, statsYesterday) -> new GoalStatisticsCalculator[] { statsToday, statsYesterday })
                .subscribeOn(Schedulers.io())
                .subscribe(calculatedStatistics -> getActivity().runOnUiThread(() -> {
                    updateGoalStatsYesterdayUI(calculatedStatistics[1]);
                    updateGoalStatsTodayUI(calculatedStatistics[0], calculatedStatistics[1]);
                })));
    }

    private void updateGoalStatsYesterdayUI(GoalStatisticsCalculator statsCalculator) {
        if(setPastGoalsVisibility(statsCalculator.hasGoals())) {
            yGoalsDiff.setText(String.format(Locale.getDefault(), "%.1f", statsCalculator.getDifficulty()));

            String[] numParts = getDecimalNumParts(100 * statsCalculator.getRatioAchieved(), 2);
            yGoalsAchieved.setText(numParts[0]);
            yGoalsAchievedPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));

            numParts = getDecimalNumParts(100 * statsCalculator.getShuffleOccurrenceRating(), 1);
            yGoalsSelDiff.setText(numParts[0]);
            yGoalsSelDiffPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));

            numParts = getDecimalNumParts(statsCalculator.getRecurrenceFrequency(), 2);
            yGoalsOccFreq.setText(numParts[0]);
            yGoalsOccFreqPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
        }
    }

    private boolean setPastGoalsVisibility(boolean hasGoals) {
        pastGoalRatings.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
        pastGoalsAchieved.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
        pastGoalsOccurrenceRating.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
        noDataPastGoals.setVisibility(hasGoals ? View.GONE : View.VISIBLE);
        return hasGoals;
    }

    private void updateGoalStatsTodayUI(GoalStatisticsCalculator current, GoalStatisticsCalculator comparedTo) {
        difficultySpeedometer.setData(25, current.getDifficulty(), 3);

        currentGoalsContainer.removeAllViews();
        current.getGoals().forEach(goal -> currentGoalsContainer.addView(generateGoalCheckbox(goal)));

        String[] numParts = getDecimalNumParts(100 * current.getShuffleOccurrenceRating(), 1);
        currentSelectionDiff.setText(numParts[0]);
        currentSelectionDiffPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
        selectionDiffComparison.setImageDrawable(getDiffIndicator(current.getDifficulty(), comparedTo.getDifficulty(), comparedTo.hasGoals()));

        numParts = getDecimalNumParts(current.getRecurrenceFrequency(), 2);
        currentOccurrenceFreq.setText(numParts[0]);
        currentOccurrenceFreqPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
        occFreqComparison.setImageDrawable(getDiffIndicator(current.getRecurrenceFrequency(), comparedTo.getRecurrenceFrequency(), comparedTo.hasGoals()));
    }

    @Nullable
    private Drawable getDiffIndicator(float current, float comparedTo, boolean hasPastGoals) {
        @DrawableRes int iconResId = R.drawable.round_mode_standby_24;
        if(hasPastGoals) {
            iconResId = comparedTo < current ? R.drawable.ic_round_arrow_upward_24 : R.drawable.ic_round_arrow_downward_24;
        }
        return ResourcesCompat.getDrawable(getResources(), iconResId, getContext().getTheme());
    }

    @NonNull
    private MaterialCheckBox generateGoalCheckbox(DetailedShuffleHasGoal detailedShuffleHasGoal) {
        int dp24 = Tools.dpToPx(getContext(), 24);
        int dp12 = Tools.dpToPx(getContext(), 12);

        MaterialCheckBox chk = new MaterialCheckBox(getContext());
        chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chk.setButtonDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.checkbox_button_round, getContext().getTheme()));
        chk.setButtonTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_icon_check_change, getContext().getTheme()));
        chk.setButtonIconDrawable(Tools.resizeDrawable(getResources(), ResourcesCompat.getDrawable(getResources(), R.drawable.round_check_24, getContext().getTheme()), Tools.dpToPx(getContext(), 18), Tools.dpToPx(getContext(), 18)));
        chk.setButtonIconTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_button_icon_check, getContext().getTheme()));
        chk.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        chk.setHighlightColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        chk.setPadding(dp12, 0, 0, 0);
        chk.setPaintFlags(detailedShuffleHasGoal.achieved ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        chk.setText(detailedShuffleHasGoal.description);
        chk.setChecked(detailedShuffleHasGoal.achieved);
        chk.setTextColor(Tools.getAttrColor(detailedShuffleHasGoal.achieved ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
        chk.setTypeface(Typeface.create(chk.getTypeface(), detailedShuffleHasGoal.achieved ? Typeface.NORMAL : Typeface.BOLD));
        chk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        chk.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            db.getShuffleHasGoalDao().setAchievedState(detailedShuffleHasGoal.shuffleId, detailedShuffleHasGoal.goalId, isChecked);
            chk.setTextColor(Tools.getAttrColor(isChecked ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
            chk.setTypeface(Typeface.create(chk.getTypeface(), isChecked ? Typeface.NORMAL : Typeface.BOLD));
            chk.setPaintFlags(isChecked ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        });

        return chk;
    }

    private String[] getDecimalNumParts(float value, int decimals) {
        String[] numParts = new String[2];
        int fullSelDiff = (int) value;
        String decimalsFormat = "%." + decimals + "f";
        numParts[0] = String.format(Locale.getDefault(), "%d", fullSelDiff);
        numParts[1] = Float.isNaN(value % fullSelDiff) ? "" : String.format(Locale.getDefault(), decimalsFormat, value % fullSelDiff).substring(1);
        return numParts;
    }

    private void storeNewShuffle() {
        Pair<Long, Long> todayTimeSpan = Tools.getTimeSpanFrom(0, false);
        Maybe<Shuffle> alreadyPresentShuffle = db.getShuffleDao().getLastShuffleInDay(todayTimeSpan.first, todayTimeSpan.second);
        int id = getShuffleIdWithNoGoals(todayTimeSpan, alreadyPresentShuffle);
        List<Goal> goalsResult = Tools.getNewShuffleGoals(db);
        List<ShuffleHasGoal> hasGoals = new ArrayList<>();
        goalsResult.forEach(goal -> hasGoals.add(new ShuffleHasGoal(id, goal.goalId)));
        db.getShuffleHasGoalDao().insertAll(hasGoals).blockingSubscribe();
        updateStats();
    }

    private int getShuffleIdWithNoGoals(Pair<Long, Long> todayTimeSpan, Maybe<Shuffle> alreadyPresentShuffle) {
        int id;
        boolean isShuffleAlreadyPresent = !alreadyPresentShuffle.isEmpty().blockingGet();
        if(isShuffleAlreadyPresent) {
            id = alreadyPresentShuffle.blockingGet().shuffleId;
            db.getShuffleHasGoalDao().deleteAllWithShuffleId(id).blockingSubscribe();
        }
        else {
            id = db.getShuffleDao().insert(new Shuffle(todayTimeSpan.first, todayTimeSpan.second)).blockingGet().intValue();
        }
        return id;
    }

    @Override
    public void onDestroyView() {
        compositeDisposable.clear();
        super.onDestroyView();
    }
}