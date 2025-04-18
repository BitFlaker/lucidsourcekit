package com.bitflaker.lucidsourcekit.main.goals;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction;
import com.bitflaker.lucidsourcekit.database.goals.entities.resulttables.DetailedShuffleHasGoal;
import com.bitflaker.lucidsourcekit.databinding.FragmentMainGoalsBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetGoalsAlgorithmEditorBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class GoalsView extends Fragment {
    private MainDatabase db;
    private CompositeDisposable compositeDisposable;
    private FragmentMainGoalsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainGoalsBinding.inflate(inflater, container, false);
        binding.txtGoalsHeading.setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        compositeDisposable = new CompositeDisposable();
        binding.somDifficulty.setDescription("Today's goals average\noccurrence rating");

        DataStoreManager dsManager = DataStoreManager.getInstance();

//        // Start the graph from the time the app was first opened today
//        long firstOpenToday = dsManager.getSetting(DataStoreKeys.FIRST_OPEN_TIME_TODAY_DAY).blockingFirst();
//        binding.gtlAchieved.setShuffleInitTime(firstOpenToday);

        db = MainDatabase.getInstance(getContext());
        binding.btnAdjustAlgorithm.setOnClickListener(e -> setupAdjustAlgorithmSheet());

        updateStats();

        binding.btnMoreOptions.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(getContext(), R.style.Theme_LucidSourceKit_PopupMenu), binding.btnMoreOptions);
            popup.getMenuInflater().inflate(R.menu.more_goals_options, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.itm_shuffle) {
                    new Thread(this::storeNewShuffle).start();
                }
                else if (item.getItemId() == R.id.itm_edit_goals) {
                    startActivity(new Intent(getContext(), GoalsEditorView.class));
                }
                else if (item.getItemId() == R.id.itm_about_goals) {
                    // TODO: show page with details about what goals are and what they are supposed to achieve
                }
                return true;
            });
            popup.show();
        });
    }

    private void setupAdjustAlgorithmSheet() {
        BottomSheetDialog bsdAdjustAlgorithm = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
        SheetGoalsAlgorithmEditorBinding sBinding = SheetGoalsAlgorithmEditorBinding.inflate(getLayoutInflater());
        bsdAdjustAlgorithm.setContentView(sBinding.getRoot());
        final ChangeBounds transition = new ChangeBounds();
        transition.excludeTarget(sBinding.txtTitleGoalAlgorithm, true);
        transition.setDuration(300L);

        sBinding.btnShuffle.setOnClickListener(e -> new Thread(this::storeNewShuffle).start());
        sBinding.crdEditGoals.setOnClickListener(e -> startActivity(new Intent(getContext(), GoalsEditorView.class)));
        sBinding.crdEditWeights.setOnClickListener(e -> {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet), transition);
            sBinding.btnBack.setVisibility(View.VISIBLE);
            sBinding.btnShuffle.setVisibility(View.GONE);
            sBinding.clMainSettings.setVisibility(View.GONE);
            sBinding.clWeightEditor.setVisibility(View.VISIBLE);
            sBinding.txtTitleGoalAlgorithm.setText("Edit weights");
        });
        sBinding.crdEditGoalCount.setOnClickListener(e -> {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet), transition);
            sBinding.btnBack.setVisibility(View.VISIBLE);
            sBinding.clMainSettings.setVisibility(View.GONE);
            sBinding.btnShuffle.setVisibility(View.GONE);
            sBinding.clGoalCountEditor.setVisibility(View.VISIBLE);
            sBinding.txtTitleGoalAlgorithm.setText("Edit goal count");
        });
        sBinding.crdDynamicRating.setOnClickListener(e -> {
            sBinding.swtDynamicRating.setChecked(!sBinding.swtDynamicRating.isChecked());
        });
        sBinding.crdDynamicShuffleCount.setOnClickListener(e -> {
            sBinding.swtDynamicShuffleCount.setChecked(!sBinding.swtDynamicShuffleCount.isChecked());
        });
        sBinding.btnBack.setOnClickListener(e -> {
            TransitionManager.beginDelayedTransition(bsdAdjustAlgorithm.findViewById(com.google.android.material.R.id.design_bottom_sheet), transition);
            sBinding.btnBack.setVisibility(View.GONE);
            sBinding.clMainSettings.setVisibility(View.VISIBLE);
            sBinding.clWeightEditor.setVisibility(View.GONE);
            sBinding.clGoalCountEditor.setVisibility(View.GONE);
            sBinding.btnShuffle.setVisibility(View.VISIBLE);
            sBinding.txtTitleGoalAlgorithm.setText("Personalize goals");
        });

        Slider[] weightSliders = new Slider[] { sBinding.sldAlgoDiffCommon, sBinding.sldAlgoDiffUncommon, sBinding.sldAlgoDiffRare };

        sBinding.sldAlgoDiffCommon.addOnChangeListener((slider, value, fromUser) -> {
            sBinding.txtSldValueCommon.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sBinding.sldAlgoDiffUncommon.addOnChangeListener((slider, value, fromUser) -> {
            sBinding.txtSldValueUncommon.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sBinding.sldAlgoDiffRare.addOnChangeListener((slider, value, fromUser) -> {
            sBinding.txtSldValueRare.setText(String.format(Locale.getDefault(), "%.0f%%", slider.getValue() * 100));
            if(fromUser) { setSliderProportions(weightSliders, slider); }
        });
        sBinding.sldGoalCount.addOnChangeListener((slider, value, fromUser) -> {
            sBinding.txtSldValueGoalCount.setText(String.format(Locale.getDefault(), "%d", (int) slider.getValue()));
        });

        DataStoreManager dsManager = DataStoreManager.getInstance();
        sBinding.sldAlgoDiffCommon.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_COMMON).blockingFirst());
        sBinding.sldAlgoDiffUncommon.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_UNCOMMON).blockingFirst());
        sBinding.sldAlgoDiffRare.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_RARE).blockingFirst());
        sBinding.sldGoalCount.setValue(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT).blockingFirst());
        sBinding.swtDynamicRating.setChecked(dsManager.getSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST).blockingFirst());

        bsdAdjustAlgorithm.setOnDismissListener(e -> {
            saveGoalAlgorithm(sBinding.sldAlgoDiffCommon.getValue(), sBinding.sldAlgoDiffUncommon.getValue(), sBinding.sldAlgoDiffRare.getValue(), (int) sBinding.sldGoalCount.getValue(), sBinding.swtDynamicRating.isChecked());
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
//                    updateGoalStatsYesterdayUI(calculatedStatistics[1]);
                    updateGoalStatsTodayUI(calculatedStatistics[0], calculatedStatistics[1]);
                })));
    }

//    private void updateGoalStatsYesterdayUI(GoalStatisticsCalculator statsCalculator) {
//        if(setPastGoalsVisibility(statsCalculator.hasGoals())) {
//            binding.txtYgoalsDifficulty.setText(String.format(Locale.getDefault(), "%.1f", statsCalculator.getDifficulty()));
//
//            String[] numParts = getDecimalNumParts(100 * statsCalculator.getRatioAchieved(), 2);
//            binding.txtYgoalsAchieved.setText(numParts[0]);
//            binding.txtYgoalsAchievedPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
//
//            numParts = getDecimalNumParts(100 * statsCalculator.getShuffleOccurrenceRating(), 1);
//            binding.txtYgoalsSelDifficulty.setText(numParts[0]);
//            binding.txtYgoalsSelDifficultyPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
//
//            numParts = getDecimalNumParts(statsCalculator.getRecurrenceFrequency(), 2);
//            binding.txtYgoalsOccFreq.setText(numParts[0]);
//            binding.txtYgoalsOccFreqPart.setText(String.format(Locale.getDefault(), "%s%%", numParts[1]));
//        }
//    }
//
//    private boolean setPastGoalsVisibility(boolean hasGoals) {
//        binding.llPastGoalsRatings.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
//        binding.crdPastGoalsAchieved.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
//        binding.crdPastGoalsOccurrenceRating.setVisibility(hasGoals ? View.VISIBLE : View.GONE);
//        binding.crdNoDataPastGoals.setVisibility(hasGoals ? View.GONE : View.VISIBLE);
//        return hasGoals;
//    }

    private void updateGoalStatsTodayUI(GoalStatisticsCalculator current, GoalStatisticsCalculator comparedTo) {
        binding.somDifficulty.setData(25, current.getDifficulty(), 3);

        RecyclerViewAdapterCurrentGoals adapterCurrentGoals = new RecyclerViewAdapterCurrentGoals(getContext(), current.getGoals(), current.getShuffleId());
        binding.rcvCurrentGoals.setAdapter(adapterCurrentGoals);
        binding.rcvCurrentGoals.setLayoutManager(new LinearLayoutManager(getContext()));

        // Highlight all achieved timestamps on graph
        ArrayList<Long> achievedTimes = new ArrayList<>(db.getShuffleTransactionDao().getAchievedTimes(current.getShuffleId()).blockingGet());
        binding.gtlAchieved.setAchieved(achievedTimes);
        boolean showTimeline = achievedTimes.isEmpty();
        binding.gtlAchieved.setVisibility(showTimeline ? View.GONE : View.VISIBLE);
        binding.crdNoGoalTimeline.setVisibility(showTimeline ? View.VISIBLE : View.GONE);

        String numParts = getDecimalNumParts(100 * current.getShuffleOccurrenceRating(), 1);
        binding.txtCurrentSelectionDiffFull.setText(numParts);
        binding.imgSelectionDiffComparison.setImageDrawable(getDiffIndicator(current.getDifficulty(), comparedTo.getDifficulty(), comparedTo.hasGoals()));

        numParts = getDecimalNumParts(current.getRecurrenceFrequency(), 1);
        binding.txtOccurrenceFreqFull.setText(numParts);
        binding.imgOccFreqComparison.setImageDrawable(getDiffIndicator(current.getRecurrenceFrequency(), comparedTo.getRecurrenceFrequency(), comparedTo.hasGoals()));
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
        int dp12 = Tools.dpToPx(getContext(), 12);

        MaterialCheckBox chk = new MaterialCheckBox(getContext());
        chk.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chk.setButtonDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.checkbox_button_round, getContext().getTheme()));
        chk.setButtonTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_icon_check_change, getContext().getTheme()));
        chk.setButtonIconDrawable(Tools.resizeDrawable(getResources(), ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_check_24, getContext().getTheme()), Tools.dpToPx(getContext(), 18), Tools.dpToPx(getContext(), 18)));
        chk.setButtonIconTintList(ResourcesCompat.getColorStateList(getContext().getResources(), R.color.checkbox_button_icon_check, getContext().getTheme()));
        chk.setTextColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        chk.setHighlightColor(Tools.getAttrColor(R.attr.primaryTextColor, getContext().getTheme()));
        chk.setPadding(dp12, 0, 0, 0);
        chk.setPaintFlags(false /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH*/ ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        chk.setText(detailedShuffleHasGoal.description);
        chk.setChecked(false /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH*/);
        chk.setTextColor(Tools.getAttrColor(false /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH*/ ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
        chk.setTypeface(Typeface.create(chk.getTypeface(), false /* TODO: REMOVE OLD GOAL ACHIEVED APPROACH*/ ? Typeface.NORMAL : Typeface.BOLD));
        chk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        chk.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // TODO: MOVE TO NEW GOAL ACHIEVED APPROACH
//            db.getShuffleHasGoalDao().setAchievedState(detailedShuffleHasGoal.shuffleId, detailedShuffleHasGoal.goalId, isChecked);
            chk.setTextColor(Tools.getAttrColor(isChecked ? R.attr.tertiaryTextColor : R.attr.primaryTextColor, getContext().getTheme()));
            chk.setTypeface(Typeface.create(chk.getTypeface(), isChecked ? Typeface.NORMAL : Typeface.BOLD));
            chk.setPaintFlags(isChecked ? chk.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : chk.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        });

        return chk;
    }

    private String getDecimalNumParts(float value, int decimals) {
        String decimalsFormat = "%." + decimals + "f";
        return Float.isNaN(value) ? "0" : String.format(Locale.getDefault(), decimalsFormat, value);
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