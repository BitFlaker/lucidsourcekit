package com.bitflaker.lucidsourcekit.main;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.CircleGraph;
import com.bitflaker.lucidsourcekit.charts.DataValue;
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.database.JournalDatabase;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class Statistics extends Fragment {
    public LinearLayout avgMoodsContainer, avgClaritiesContainer, avgQualitiesContainer, goalsContainer;
    public ChipGroup chartTimeSpan;
    public CircleGraph lucidPercentage;
    private JournalDatabase db;
    private List<Double> avgClarities = new ArrayList<>();
    private List<Double> avgMoods = new ArrayList<>();
    private List<Double> avgQualities = new ArrayList<>();
    private Drawable[] moodIcons;
    private Drawable[] clarityIcons;
    private Drawable[] qualityIcons;
    private MutableLiveData<Boolean> gatheredNewTimeSpanStats = new MutableLiveData<>(false);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_stats_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        avgMoodsContainer = getView().findViewById(R.id.ll_avg_moods);
        avgClaritiesContainer = getView().findViewById(R.id.ll_avg_clarities);
        avgQualitiesContainer = getView().findViewById(R.id.ll_avg_sleep_quality);
        goalsContainer = getView().findViewById(R.id.ll_goals_reached);
        chartTimeSpan = getView().findViewById(R.id.chp_grp_time_span);
        lucidPercentage = getView().findViewById(R.id.ccg_lucid_percentage);
        db = JournalDatabase.getInstance(getContext());

        Drawable iconMood1 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_dissatisfied_24, getContext().getTheme());
        Drawable iconMood2 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_dissatisfied_24, getContext().getTheme());
        Drawable iconMood3 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_neutral_24, getContext().getTheme());
        Drawable iconMood4 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_satisfied_24, getContext().getTheme());
        Drawable iconMood5 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_satisfied_24, getContext().getTheme());
        moodIcons = new Drawable[] { iconMood1, iconMood2, iconMood3, iconMood4, iconMood5 };

        Drawable iconClarity1 = getResources().getDrawable(R.drawable.ic_baseline_brightness_4_24, getContext().getTheme());
        Drawable iconClarity2 = getResources().getDrawable(R.drawable.ic_baseline_brightness_5_24, getContext().getTheme());
        Drawable iconClarity3 = getResources().getDrawable(R.drawable.ic_baseline_brightness_6_24, getContext().getTheme());
        Drawable iconClarity4 = getResources().getDrawable(R.drawable.ic_baseline_brightness_7_24, getContext().getTheme());
        clarityIcons = new Drawable[] { iconClarity1, iconClarity2, iconClarity3, iconClarity4 };

        Drawable iconQuality1 = getResources().getDrawable(R.drawable.ic_baseline_star_border_24, getContext().getTheme());
        Drawable iconQuality2 = getResources().getDrawable(R.drawable.ic_baseline_star_half_24, getContext().getTheme());
        Drawable iconQuality3 = getResources().getDrawable(R.drawable.ic_baseline_star_24, getContext().getTheme());
        Drawable iconQuality4 = getResources().getDrawable(R.drawable.ic_baseline_stars_24, getContext().getTheme());
        qualityIcons = new Drawable[] { iconQuality1, iconQuality2, iconQuality3, iconQuality4 };

        lucidPercentage.setData(20, 80, Tools.dpToPx(getContext(), 15), Tools.dpToPx(getContext(), 1.25));
        getAveragesForLastNDays(7, 0);
        gatheredNewTimeSpanStats.observe(getActivity(), aBoolean -> {
            if(gatheredNewTimeSpanStats.getValue()) {
                generateRodChart(avgMoods.size(), Tools.dpToPx(getContext(), 3f), avgMoodsContainer, moodIcons, avgMoods);
                generateRodChart(avgClarities.size(), Tools.dpToPx(getContext(), 3f), avgClaritiesContainer, clarityIcons, avgClarities);
                generateRodChart(avgQualities.size(), Tools.dpToPx(getContext(), 3f), avgQualitiesContainer, qualityIcons, avgQualities);
            }
        });
        //generateRodChart(7, Tools.dpToPx(getContext(), 3f), goalsContainer, null);

        chartTimeSpan.setOnCheckedChangeListener((chipGroup, i) -> {
            avgMoodsContainer.removeAllViews();
            avgClaritiesContainer.removeAllViews();
            avgQualitiesContainer.removeAllViews();
            goalsContainer.removeAllViews();
            switch (i){
                case R.id.chp_last_7_days:
                    getAveragesForLastNDays(7, 0);
                    break;
                case R.id.chp_last_30_days:
                    getAveragesForLastNDays(30, 0);
                    break;
                case R.id.chp_all_time:
                    getAveragesForLastNDays(5000, 0);
                    break;
            }
        });
    }

    private void generateRodChart(int amount, float lineWidth, ViewGroup container, Drawable[] icons, List<Double> averageValues) {
        // TODO: maybe hide days with no ratings/entries completely?
        RodGraph rg = new RodGraph(getContext());
        LinearLayout.LayoutParams lParamsw = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
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
            else if (amount <= 30) {
                // TODO: guarantee to get label of last day
                if(j % 4 == 0){
                    label = df.format(cldr.getTime());
                }
            }
            else {
                // TODO: use 1/7 of total entry count
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
        if(daysBeforeToday == 0) {
            avgQualities.clear();
            avgMoods.clear();
            avgClarities.clear();
        }
        gatheredNewTimeSpanStats.setValue(false);
        Calendar cldr = new GregorianCalendar(TimeZone.getDefault());
        cldr.setTime(Calendar.getInstance().getTime());
        cldr.add(Calendar.DAY_OF_MONTH, -daysBeforeToday);
        cldr.set(Calendar.HOUR_OF_DAY, 0);
        cldr.set(Calendar.MINUTE, 0);
        cldr.set(Calendar.SECOND, 0);
        cldr.set(Calendar.MILLISECOND, 0);
        long startTime = cldr.getTimeInMillis();
        cldr.set(Calendar.HOUR_OF_DAY, 23);
        cldr.set(Calendar.MINUTE, 59);
        cldr.set(Calendar.SECOND, 59);
        cldr.set(Calendar.MILLISECOND, 999);
        long endTime = cldr.getTimeInMillis();
        db.journalEntryDao().getAverageEntryInTimeSpan(startTime, endTime).subscribe((journalEntry, throwable) -> {
            avgQualities.add(journalEntry.getAvgQualities());
            avgMoods.add(journalEntry.getAvgMoods());
            avgClarities.add(journalEntry.getAvgClarities());
            if(daysBeforeToday == amount-1) {
                gatheredNewTimeSpanStats.setValue(true);
            }
            else {
                getAveragesForLastNDays(amount, daysBeforeToday +1);
            }
        });
    }
}