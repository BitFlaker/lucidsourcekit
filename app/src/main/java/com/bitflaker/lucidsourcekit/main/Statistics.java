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
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Statistics extends Fragment {
    public LinearLayout avgMoodsContainer;
    public ChipGroup chartTimeSpan;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avgMoodsContainer = getView().findViewById(R.id.ll_avg_moods);
        chartTimeSpan = getView().findViewById(R.id.chp_grp_time_span);

        generateRodChart(7, 7f);

        chartTimeSpan.setOnCheckedChangeListener((chipGroup, i) -> {
            int count = avgMoodsContainer.getChildCount();
            for (int j = 0; j < count; j++){
                avgMoodsContainer.removeViewAt(0);
            }
            switch (i){
                case R.id.chp_last_7_days:
                    generateRodChart(7, 7f);
                    break;
                case R.id.chp_last_30_days:
                    generateRodChart(30, 7f);
                    break;
                case R.id.chp_all_time:
                    generateRodChart(50000, 4f);
                    break;
            }
        });
    }

    private void generateRodChart(int amount, float lineWidth) {
        RodGraph rg = new RodGraph(getContext());
        LinearLayout.LayoutParams lParamsw = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParamsw.weight = 1;
        lParamsw.topMargin = Tools.dpToPx(getContext(), 8);
        rg.setLayoutParams(lParamsw);
        avgMoodsContainer.addView(rg);

        List<DataValue> data = new ArrayList<>();
        for (int j = 0; j < amount; j++) {
            data.add(new DataValue(new Random().nextInt(6)-1, "25\nDec"));
        }
        Drawable icon1 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_dissatisfied_24, getContext().getTheme());
        Drawable icon2 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_dissatisfied_24, getContext().getTheme());
        Drawable icon3 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_neutral_24, getContext().getTheme());
        Drawable icon4 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_satisfied_24, getContext().getTheme());
        Drawable icon5 = getResources().getDrawable(R.drawable.ic_baseline_sentiment_very_satisfied_24, getContext().getTheme());
        rg.setData(data, lineWidth, Tools.dpToPx(getContext(), 24), new Drawable[] { icon1, icon2, icon3, icon4, icon5 });
    }
}