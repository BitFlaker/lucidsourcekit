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

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.CircleGraph;
import com.bitflaker.lucidsourcekit.charts.DataValue;
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Statistics extends Fragment {
    public LinearLayout avgMoodsContainer, avgClaritiesContainer, avgQualitiesContainer, goalsContainer;
    public ChipGroup chartTimeSpan;
    public CircleGraph lucidPercentage;

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

        Drawable iconMood1 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_dissatisfied_24, getContext().getTheme());
        Drawable iconMood2 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_dissatisfied_24, getContext().getTheme());
        Drawable iconMood3 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_neutral_24, getContext().getTheme());
        Drawable iconMood4 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_satisfied_24, getContext().getTheme());
        Drawable iconMood5 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_satisfied_24, getContext().getTheme());
        Drawable[] moodIcons = new Drawable[] { iconMood1, iconMood2, iconMood3, iconMood4, iconMood5 };

        Drawable iconClarity1 = getResources().getDrawable(R.drawable.ic_baseline_brightness_4_24, getContext().getTheme());
        Drawable iconClarity2 = getResources().getDrawable(R.drawable.ic_baseline_brightness_5_24, getContext().getTheme());
        Drawable iconClarity3 = getResources().getDrawable(R.drawable.ic_baseline_brightness_6_24, getContext().getTheme());
        Drawable iconClarity4 = getResources().getDrawable(R.drawable.ic_baseline_brightness_7_24, getContext().getTheme());
        Drawable[] clarityIcons = new Drawable[] { iconClarity1, iconClarity2, iconClarity3, iconClarity4 };

        Drawable iconQuality1 = getResources().getDrawable(R.drawable.ic_baseline_star_border_24, getContext().getTheme());
        Drawable iconQuality2 = getResources().getDrawable(R.drawable.ic_baseline_star_half_24, getContext().getTheme());
        Drawable iconQuality3 = getResources().getDrawable(R.drawable.ic_baseline_star_24, getContext().getTheme());
        Drawable iconQuality4 = getResources().getDrawable(R.drawable.ic_baseline_stars_24, getContext().getTheme());
        Drawable[] qualityIcons = new Drawable[] { iconQuality1, iconQuality2, iconQuality3, iconQuality4 };

        lucidPercentage.setData(20, 80, Tools.dpToPx(getContext(), 15), Tools.dpToPx(getContext(), 1.25));
        generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgMoodsContainer, moodIcons, "25\nDec");
        generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgClaritiesContainer, clarityIcons, "25\nDec");
        generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgQualitiesContainer, qualityIcons, "25\nDec");
        generateRodChart(7, Tools.dpToPx(getContext(), 3f), goalsContainer, null, "25\nDec");

        chartTimeSpan.setOnCheckedChangeListener((chipGroup, i) -> {
            avgMoodsContainer.removeAllViews();
            avgClaritiesContainer.removeAllViews();
            avgQualitiesContainer.removeAllViews();
            goalsContainer.removeAllViews();
            switch (i){
                case R.id.chp_last_7_days:
                    generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgMoodsContainer, moodIcons, "25\nDec");
                    generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgClaritiesContainer, clarityIcons, "25\nDec");
                    generateRodChart(7, Tools.dpToPx(getContext(), 3f), avgQualitiesContainer, qualityIcons, "25\nDec");
                    generateRodChart(7, Tools.dpToPx(getContext(), 3f), goalsContainer, null, "25\nDec");
                    break;
                case R.id.chp_last_30_days:
                    generateRodChart(30, Tools.dpToPx(getContext(), 3f), avgMoodsContainer, moodIcons, null);
                    generateRodChart(30, Tools.dpToPx(getContext(), 3f), avgClaritiesContainer, clarityIcons, null);
                    generateRodChart(30, Tools.dpToPx(getContext(), 3f), avgQualitiesContainer, qualityIcons, null);
                    generateRodChart(30, Tools.dpToPx(getContext(), 3f), goalsContainer, null, null);
                    break;
                case R.id.chp_all_time:
                    generateRodChart(5000, Tools.dpToPx(getContext(), 1.5f), avgMoodsContainer, moodIcons, null);
                    generateRodChart(5000, Tools.dpToPx(getContext(), 1.5f), avgClaritiesContainer, clarityIcons, null);
                    generateRodChart(5000, Tools.dpToPx(getContext(), 1.5f), avgQualitiesContainer, qualityIcons, null);
                    generateRodChart(5000, Tools.dpToPx(getContext(), 1.5f), goalsContainer, null, null);
                    break;
            }
        });
    }

    private void generateRodChart(int amount, float lineWidth, ViewGroup container, Drawable[] icons, String label) {
        RodGraph rg = new RodGraph(getContext());
        LinearLayout.LayoutParams lParamsw = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParamsw.weight = 1;
        lParamsw.topMargin = Tools.dpToPx(getContext(), 8);
        rg.setLayoutParams(lParamsw);
        container.addView(rg);

        List<DataValue> data = new ArrayList<>();
        for (int j = 0; j < amount; j++) {
            if(icons != null){
                data.add(new DataValue(new Random().nextInt(icons.length + 1)-1, label));
            }
            else {
                data.add(new DataValue(new Random().nextInt(5)-1, label));
            }
        }
        rg.setData(data, lineWidth, Tools.dpToPx(getContext(), 24), icons);
    }
}