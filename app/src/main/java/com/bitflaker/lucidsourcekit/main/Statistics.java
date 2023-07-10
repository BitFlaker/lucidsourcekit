package com.bitflaker.lucidsourcekit.main;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.CircleGraph;
import com.bitflaker.lucidsourcekit.charts.DataValue;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.TagCount;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Statistics extends Fragment {
    public LinearLayout avgMoodsContainer, avgClaritiesContainer, avgQualitiesContainer, goalsContainer, mostUsedTagsContainer, ldreamCountContainer, avgContainer, goalsReachedContainer;
    public ChipGroup chartTimeSpan;
    public CircleGraph lucidPercentage;
    public TextView totalEntriesNotice, currentStreak, longestStreak, neLucidDreamCount, neAvgData, neAvgClarities, neAvgMoods, neAvgSQualities, neGoalsReached, neMostUsedTags;
    public RangeProgress rpDreamMood, rpDreamClarity, rpSleepQuality, rpDreamsPerNight, rpGoalsReached, rpAvgDiff;
    private MainDatabase db;
    private List<Double> avgClarities = new ArrayList<>();
    private List<Double> avgMoods = new ArrayList<>();
    private List<Double> avgQualities = new ArrayList<>();
    private List<Double> dreamCounts = new ArrayList<>();
    private long startTimeSpan = 0;
    private long endTimeSpan = 0;
    private Drawable[] moodIcons;
    private Drawable[] clarityIcons;
    private Drawable[] qualityIcons;
    private MutableLiveData<Boolean> gatheredNewTimeSpanStats = new MutableLiveData<>(false);
    private int selectedDaysCount = 7;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_stats_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        mostUsedTagsContainer = getView().findViewById(R.id.ll_most_used_tags);
        avgMoodsContainer = getView().findViewById(R.id.ll_avg_moods);
        avgClaritiesContainer = getView().findViewById(R.id.ll_avg_clarities);
        avgQualitiesContainer = getView().findViewById(R.id.ll_avg_sleep_quality);
        goalsContainer = getView().findViewById(R.id.ll_goals_reached);
        chartTimeSpan = getView().findViewById(R.id.chp_grp_time_span);
        lucidPercentage = getView().findViewById(R.id.ccg_lucid_percentage);
        totalEntriesNotice = getView().findViewById(R.id.txt_total_journal_entries_stat);
        currentStreak = getView().findViewById(R.id.txt_current_streak);
        longestStreak = getView().findViewById(R.id.txt_longest_streak);
        rpDreamMood = getView().findViewById(R.id.rp_dream_mood);
        rpDreamClarity = getView().findViewById(R.id.rp_dream_clarity);
        rpSleepQuality = getView().findViewById(R.id.rp_sleep_quality);
        rpDreamsPerNight = getView().findViewById(R.id.rp_dreams_per_night);
        neLucidDreamCount = getView().findViewById(R.id.txt_not_enough_data_ldream_count);
        ldreamCountContainer = getView().findViewById(R.id.ll_stat_ldream_count_container);
        avgContainer = getView().findViewById(R.id.ll_stat_avg_container);
        neAvgData = getView().findViewById(R.id.txt_not_enough_data_avg_data);
        neAvgClarities = getView().findViewById(R.id.txt_not_enough_data_avg_clarities);
        neAvgMoods = getView().findViewById(R.id.txt_not_enough_data_avg_moods);
        neAvgSQualities = getView().findViewById(R.id.txt_not_enough_data_avg_squality);
        neGoalsReached = getView().findViewById(R.id.txt_not_enough_data_goals_reached);
        neMostUsedTags = getView().findViewById(R.id.txt_not_enough_data_most_used_tags);
        goalsReachedContainer = getView().findViewById(R.id.ll_goals_reached);
        rpGoalsReached = getView().findViewById(R.id.rp_goals_reached);
        rpAvgDiff = getView().findViewById(R.id.rp_avg_goal_diff);
        db = MainDatabase.getInstance(getContext());

        moodIcons = Tools.getIconsDreamMood(getContext());
        clarityIcons = Tools.getIconsDreamClarity(getContext());
        qualityIcons = Tools.getIconsSleepQuality(getContext());

        currentStreak.setText(Long.toString(PreferenceManager.getDefaultSharedPreferences(getContext()).getLong("app_open_streak", 0)));
        longestStreak.setText(Long.toString(PreferenceManager.getDefaultSharedPreferences(getContext()).getLong("longest_app_open_streak", 0)));

        // TODO: add loading indicators while gathering data
        // TODO: refresh stats after entry modified/added/deleted

        getAveragesForLastNDays(selectedDaysCount, 0);
        gatheredNewTimeSpanStats.observe(getActivity(), aBoolean -> {
            if(gatheredNewTimeSpanStats.getValue()) {
                // TODO: optimize code for less redundancy
                if(!Tools.hasNoData(avgMoods)) {
                    neAvgMoods.setVisibility(View.GONE);
                    avgMoodsContainer.setVisibility(View.VISIBLE);
                    generateRodChart(avgMoods.size(), Tools.dpToPx(getContext(), 3f), avgMoodsContainer, moodIcons, avgMoods);
                }
                else {
                    neAvgMoods.setVisibility(View.VISIBLE);
                    avgMoodsContainer.setVisibility(View.GONE);
                }

                if(!Tools.hasNoData(avgClarities)){
                    neAvgClarities.setVisibility(View.GONE);
                    avgClaritiesContainer.setVisibility(View.VISIBLE);
                    generateRodChart(avgClarities.size(), Tools.dpToPx(getContext(), 3f), avgClaritiesContainer, clarityIcons, avgClarities);
                }
                else {
                    neAvgClarities.setVisibility(View.VISIBLE);
                    avgClaritiesContainer.setVisibility(View.GONE);
                }

                if(!Tools.hasNoData(avgQualities)){
                    neAvgSQualities.setVisibility(View.GONE);
                    avgQualitiesContainer.setVisibility(View.VISIBLE);
                    generateRodChart(avgQualities.size(), Tools.dpToPx(getContext(), 3f), avgQualitiesContainer, qualityIcons, avgQualities);
                }
                else {
                    neAvgSQualities.setVisibility(View.VISIBLE);
                    avgQualitiesContainer.setVisibility(View.GONE);
                }

                if(Tools.hasNoData(avgMoods) && Tools.hasNoData(avgClarities) && Tools.hasNoData(avgQualities) && Tools.hasNoData(dreamCounts)){
                    neAvgData.setVisibility(View.VISIBLE);
                    avgContainer.setVisibility(View.GONE);
                }
                else {
                    neAvgData.setVisibility(View.GONE);
                    avgContainer.setVisibility(View.VISIBLE);

                    float averageMood = calcAverage(avgMoods, true);
                    float averageClarity = calcAverage(avgClarities, true);
                    float averageQuality = calcAverage(avgQualities, true);
                    float averageDreamCount = calcAverage(dreamCounts, false);
                    // TODO: extract string resources
                    rpDreamMood.setData(4, averageMood, "DREAM MOOD", moodIcons[Math.round(averageMood)], null);
                    rpDreamClarity.setData(3, averageClarity, "DREAM CLARITY", clarityIcons[Math.round(averageClarity)], null);
                    rpSleepQuality.setData(3, averageQuality, "SLEEP QUALITY", qualityIcons[Math.round(averageQuality)], null);
                    rpDreamsPerNight.setData(Collections.max(dreamCounts).floatValue(), averageDreamCount, "DREAMS PER NIGHT", null, String.format(Locale.ENGLISH, "%.2f", averageDreamCount));
                }

                Pair<Long, Long> timeSpan = Tools.getTimeSpanFrom(selectedDaysCount - 1, true);
                db.getJournalEntryDao().getLucidEntriesCount(timeSpan.first, timeSpan.second).subscribe((lucidEntriesCount, throwable) -> {
                    db.getJournalEntryDao().getEntriesCount(timeSpan.first, timeSpan.second).subscribe((totalEntriesCount, throwable2) -> {
                        // TODO: maybe display numbers as well?
                        if(totalEntriesCount != 0) {
                            neLucidDreamCount.setVisibility(View.GONE);
                            ldreamCountContainer.setVisibility(View.VISIBLE);
                            lucidPercentage.setData(lucidEntriesCount, totalEntriesCount-lucidEntriesCount, Tools.dpToPx(getContext(), 15), Tools.dpToPx(getContext(), 1.25));
                        }
                        else {
                            ldreamCountContainer.setVisibility(View.GONE);
                            neLucidDreamCount.setVisibility(View.VISIBLE);
                        }
                        totalEntriesNotice.setText(getResources().getString(R.string.total_entries_count).replace("<TOTAL_COUNT>", totalEntriesCount.toString()));
                    });
                });

                db.getJournalEntryHasTagDao().getMostUsedTagsList(timeSpan.first, timeSpan.second, 10).subscribe((tagCounts, throwable) -> {
                    // TODO: only make 1 object that draws all of the graphs
                    mostUsedTagsContainer.removeAllViews();
                    if(tagCounts.size() > 0){
                        mostUsedTagsContainer.setVisibility(View.VISIBLE);
                        neMostUsedTags.setVisibility(View.GONE);
                        int maxCount = tagCounts.get(0).getCount();
                        for (TagCount p : tagCounts) {
                            RangeProgress rngProg = new RangeProgress(getContext());
                            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dpToPx(getContext(), 25));
                            int margin = Tools.dpToPx(getContext(), 5);
                            llParams.setMargins(0, margin, 0, margin);
                            rngProg.setLayoutParams(llParams);
                            mostUsedTagsContainer.addView(rngProg);
                            rngProg.setData(maxCount, p.getCount(), p.getTag(), null, Integer.toString(p.getCount()));
                        }
                    }
                    else {
                        mostUsedTagsContainer.setVisibility(View.GONE);
                        neMostUsedTags.setVisibility(View.VISIBLE);
                    }
                });

                db.getShuffleHasGoalDao().getShufflesFromBetween(timeSpan.first, timeSpan.second).subscribe((shuffleHasGoalStats, throwable) -> {
                    if(shuffleHasGoalStats.goalCount == 0) {
                        neGoalsReached.setVisibility(View.VISIBLE);
                        goalsReachedContainer.setVisibility(View.GONE);
                    }
                    else {
                        neGoalsReached.setVisibility(View.GONE);
                        goalsReachedContainer.setVisibility(View.VISIBLE);
                        rpGoalsReached.setData(shuffleHasGoalStats.goalCount, shuffleHasGoalStats.achievedCount, "ACHIEVED", null, String.format(Locale.ENGLISH, "%d/%d", shuffleHasGoalStats.achievedCount, shuffleHasGoalStats.goalCount));
                        rpAvgDiff.setData(3, (float)shuffleHasGoalStats.avgDifficulty, "AVERAGE DIFFICULTY LEVEL", null, String.format(Locale.ENGLISH, "%.2f", shuffleHasGoalStats.avgDifficulty));
                    }
                });
            }
        });

        chartTimeSpan.setOnCheckedChangeListener((chipGroup, i) -> {
            avgMoodsContainer.removeAllViews();
            avgClaritiesContainer.removeAllViews();
            avgQualitiesContainer.removeAllViews();
            switch (i) {
                case R.id.chp_last_7_days:
                    selectedDaysCount = 7;
                    getAveragesForLastNDays(selectedDaysCount, 0);
                    break;
                case R.id.chp_last_31_days:
                    selectedDaysCount = 31;
                    getAveragesForLastNDays(selectedDaysCount, 0);
                    break;
                case R.id.chp_all_time:
                    selectedDaysCount = 250;
                    getAveragesForLastNDays(selectedDaysCount, 0);
                    break;
            }
        });
    }

    private float calcAverage(List<Double> vals, boolean ignoreMissedDays) {
        if(vals.size() == 0){ return 0; }
        double sum = 0;
        int i = 0;
        for (Double d : vals) {
            if(ignoreMissedDays && d == -1.0) {
                continue;
            }
            sum += d == -1 ? 0 : d;
            i++;
        }
        return (float)(sum / (double)i);
    }

    private void generateRodChart(int amount, float lineWidth, ViewGroup container, Drawable[] icons, List<Double> averageValues) {
        RodGraph rg = new RodGraph(getContext());
        LinearLayout.LayoutParams lParamsw = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsw.weight = 1;
        lParamsw.topMargin = Tools.dpToPx(getContext(), 8);
        rg.setLayoutParams(lParamsw);
        container.addView(rg);
        List<DataValue> data = new ArrayList<>();
        Calendar cldr = new GregorianCalendar(TimeZone.getDefault());
        cldr.setTime(Calendar.getInstance().getTime());
        SimpleDateFormat df = new SimpleDateFormat("d\nMMM");
        for (int j = 0; j < amount; j++) {
            String label = null;
            if(amount <= 7){
                label = df.format(cldr.getTime());
            }
            else if (amount <= 31) {
                if(j % 6 == 0) {
                    label = df.format(cldr.getTime());
                }
            }
            else {
                // TODO: use 1/7 of total entry count and guarantee that last day is written and probably do not use rod graph or use averages of days (=> loss of accuracy)
                if(j % 10 == 0){
                    label = df.format(cldr.getTime());
                }
            }
            data.add(new DataValue(averageValues.get(j), label));
            cldr.add(Calendar.DAY_OF_MONTH, -1);
        }
        rg.setData(data, lineWidth, Tools.dpToPx(getContext(), 24), icons);
    }

    private void getAveragesForLastNDays(int amount, int daysBeforeToday) {
        gatheredNewTimeSpanStats.setValue(false);
        Pair<Long, Long> timeSpan = Tools.getTimeSpanFrom(daysBeforeToday, false);
        if(daysBeforeToday == 0) {
            avgQualities.clear();
            avgMoods.clear();
            avgClarities.clear();
            dreamCounts.clear();
            endTimeSpan = timeSpan.second;
        }
        db.getJournalEntryDao().getAverageEntryInTimeSpan(timeSpan.first, timeSpan.second).subscribe((averageEntryValues, throwable) -> {
            if(averageEntryValues.getDreamCount() > 0){
                avgQualities.add(averageEntryValues.getAvgQualities());
                avgMoods.add(averageEntryValues.getAvgMoods());
                avgClarities.add(averageEntryValues.getAvgClarities());
                dreamCounts.add(averageEntryValues.getDreamCount());
            }
            else {
                avgQualities.add(-1.0);
                avgMoods.add(-1.0);
                avgClarities.add(-1.0);
                dreamCounts.add(-1.0);
            }
            if(daysBeforeToday == amount-1) {
                startTimeSpan = timeSpan.first;
                gatheredNewTimeSpanStats.setValue(true);
            }
            else {
                getAveragesForLastNDays(amount, daysBeforeToday + 1);
            }
        });
    }
}