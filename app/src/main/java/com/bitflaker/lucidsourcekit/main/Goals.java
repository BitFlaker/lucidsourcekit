package com.bitflaker.lucidsourcekit.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.DataValue;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class Goals extends Fragment {
    private LinearLayout difficultyChartContainer;
    private FloatingActionButton floatingEdit;
    private RangeProgress goalsReachedYesterday, difficultyLevel, averageDifficultyLevel, averageDifficultyLevelYesterday;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_goals_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        difficultyChartContainer = getView().findViewById(R.id.ll_difficulty);
        goalsReachedYesterday = getView().findViewById(R.id.rp_goals_reached_yesterday);
        difficultyLevel = getView().findViewById(R.id.rp_difficulty_level);
        averageDifficultyLevel = getView().findViewById(R.id.rp_average_difficulty_level);
        averageDifficultyLevelYesterday = getView().findViewById(R.id.rp_goals_average_difficulty_yesterday);
        floatingEdit = getView().findViewById(R.id.btn_add_journal_entry);

        // TODO: replace with string resources
        goalsReachedYesterday.setData(3, 1, "GOALS REACHED", null, "1/3");
        difficultyLevel.setData(3, 1.35f, "DIFFICULTY LEVEL", null, "1.35");
        averageDifficultyLevel.setData(3, 1.57f, "AVERAGE DIFFICULTY LEVEL", null, "1.57");
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
        //difficultyChartContainer.addView(rg, 1);
    }
}