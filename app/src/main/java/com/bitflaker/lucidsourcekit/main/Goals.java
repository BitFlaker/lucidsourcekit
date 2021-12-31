package com.bitflaker.lucidsourcekit.main;

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
import com.bitflaker.lucidsourcekit.charts.RodGraph;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.util.ArrayList;
import java.util.List;

public class Goals extends Fragment {
    private LinearLayout difficultyChartContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        difficultyChartContainer = getView().findViewById(R.id.ll_difficulty);
        getView().findViewById(R.id.btn_add_journal_entry).setOnClickListener(e -> {

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