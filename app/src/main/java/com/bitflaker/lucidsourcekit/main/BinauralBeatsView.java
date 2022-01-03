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

import java.util.ArrayList;
import java.util.List;

public class BinauralBeatsView extends Fragment {
    private RecyclerView binauralBeatsSelector;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binauralBeatsSelector = getView().findViewById(R.id.rcv_list_binaural_beats);

        List<BinauralBeat> beats = new ArrayList<>();
        List<DataPoint> points = new ArrayList<>();
        points.add(new DataPoint(40));
        points.add(new DataPoint(30));
        points.add(new DataPoint(20));
        points.add(new DataPoint(20));
        points.add(new DataPoint(20));
        points.add(new DataPoint(15));
        points.add(new DataPoint(15));
        points.add(new DataPoint(25));
        points.add(new DataPoint(40));
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
    }
}