package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.DataPoint;
import com.bitflaker.lucidsourcekit.charts.LineGraph;

import java.util.ArrayList;
import java.util.List;

public class BinauralBeatsView extends Fragment {
    private RecyclerView binauralBeatsSelector;
    private LineGraph progressLineGraph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binauralBeatsSelector = getView().findViewById(R.id.rcv_list_binaural_beats);
        progressLineGraph = getView().findViewById(R.id.lg_binaural_time_progress);

        List<BinauralBeat> beats = new ArrayList<>();
        List<DataPoint> points = new ArrayList<>();
        points.add(new DataPoint(3));
        points.add(new DataPoint(3));
        points.add(new DataPoint(1));
        points.add(new DataPoint(0));
        points.add(new DataPoint(0));
        points.add(new DataPoint(1));
        points.add(new DataPoint(1));
        points.add(new DataPoint(1));
        points.add(new DataPoint(3));
        beats.add(new BinauralBeat("samplesad ", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sampleasd ", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample asdas d", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sampleasd  sdasd as", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description wiasdk.fjh asildfghasildjfhglaisjdhfl asdghflias ghdlfghasldj bhfl ajshdflj ahsdlfjgh asljkdfghlkjasdh fj ghalsdjfg lasjdgfl jasgdf jkasdgfl iuagsdlf jkgabsldiuf gasljdfghlaui eghfaöihu öuioasehf ölausehf asuilöh fliuawehf ksjahdf liuaseghfkljashefliu ghasth some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("samplea asd asd as", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", points));
        beats.add(new BinauralBeat("sample dd d d  d d dd", "sample description with some length to it", "NULL", points));
        RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), beats);
        rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
            System.out.println(binauralBeat.getTitle());
        });
        binauralBeatsSelector.setAdapter(rvabbs);
        binauralBeatsSelector.setLayoutManager(new LinearLayoutManager(getContext()));


        // TODO auto set values for progress of beats
        List<DataPoint> previewPoints = new ArrayList<>();
        previewPoints.add(new DataPoint(3));
        previewPoints.add(new DataPoint(3));
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(new DataPoint(0));
        previewPoints.add(new DataPoint(0));
        previewPoints.add(new DataPoint(0));
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(new DataPoint(1));
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(new DataPoint(2));
        previewPoints.add(new DataPoint(2));
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(DataPoint.SkipDataPoint());
        previewPoints.add(new DataPoint(3));
        previewPoints.add(new DataPoint(3));
        previewPoints.add(new DataPoint(3));
        BinauralBeat progress = new BinauralBeat("samplesad ", "sample description with some length to it", "NULL", previewPoints);
        progressLineGraph.setData(progress.getDataPoints(), 3, 4f, false);
    }
}