package com.bitflaker.lucidsourcekit.main;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.charts.FrequencyData;
import com.bitflaker.lucidsourcekit.charts.FrequencyList;
import com.bitflaker.lucidsourcekit.charts.LineGraph;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class BinauralBeatsView extends Fragment {
    private RecyclerView binauralBeatsSelector;
    private LineGraph progressLineGraph;
    private ImageButton displayAllBeats, backgroundNoises, repeatButton, autoStopButton, playTrack;
    private List<BackgroundNoise> noises;
    private boolean repeatBeat;
    private Date autoStopTime;
    private RelativeLayout noBeatSelected, beatSelected;
    private TextView currentTrackName, currentTrackDescription;
    private boolean playing = false;

    // TODO really repeat beats
    // TODO really start and pause beats
    // TODO really stop with Auto-Stop
    // TODO get real noises
    // TODO get real binaural beats

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressLineGraph = getView().findViewById(R.id.lg_binaural_time_progress);
        displayAllBeats = getView().findViewById(R.id.btn_display_all_beats);
        backgroundNoises = getView().findViewById(R.id.btn_add_background_noise);
        repeatButton = getView().findViewById(R.id.btn_loop_track);
        autoStopButton = getView().findViewById(R.id.btn_auto_stop);
        noBeatSelected = getView().findViewById(R.id.rl_no_binaural_beat_selected);
        beatSelected = getView().findViewById(R.id.rl_playing_binaural_beat);
        currentTrackName = getView().findViewById(R.id.txt_curr_track_name);
        currentTrackDescription = getView().findViewById(R.id.txt_curr_track_description);
        playTrack = getView().findViewById(R.id.btn_play_track);

        repeatBeat = false;
        autoStopTime = null;

        generateNoises();

        FrequencyList freqs = new FrequencyList();
        freqs.add(new FrequencyData(32, 3));
        freqs.add(new FrequencyData(32, 13, 20));
        freqs.add(new FrequencyData(13, 10));
        freqs.add(new FrequencyData(13, 8, 20));
        freqs.add(new FrequencyData(8, 10));
        freqs.add(new FrequencyData(8, 4, 35));
        freqs.add(new FrequencyData(4, 25));
        freqs.add(new FrequencyData(4, 0.5f, 5));
        freqs.add(new FrequencyData(0.5f, 5));
        freqs.add(new FrequencyData(0.5f, 8, 5));
        freqs.add(new FrequencyData(8, 5));
        freqs.add(new FrequencyData(8, 32, 35));

        displayAllBeats.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bottomSheetDialog.setContentView(R.layout.binaural_beats_selector_sheet);
            RecyclerView rcv = bottomSheetDialog.findViewById(R.id.rcv_list_binaural_beats);

            List<BinauralBeat> beats = new ArrayList<>();
            beats.add(new BinauralBeat("Quick Nap Lucidity ", "Great for supporting the induction of lucid dreams during a quick nap.", "NULL", freqs));
            beats.add(new BinauralBeat("sampleasd ", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample asdas d", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sampleasd  sdasd as", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description wiasdk.fjh asildfghasildjfhglaisjdhfl asdghflias ghdlfghasldj bhfl ajshdflj ahsdlfjgh asljkdfghlkjasdh fj ghalsdjfg lasjdgfl jasgdf jkasdgfl iuagsdlf jkgabsldiuf gasljdfghlaui eghfaöihu öuioasehf ölausehf asuilöh fliuawehf ksjahdf liuaseghfkljashefliu ghasth some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("samplea asd asd as", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample", "sample description with some length to it", "NULL", freqs));
            beats.add(new BinauralBeat("sample dd d d  d d dd", "sample description with some length to it", "NULL", freqs));
            RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), beats);
            rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
                bottomSheetDialog.hide();
                System.out.println(binauralBeat.getTitle());
                noBeatSelected.setVisibility(GONE);
                beatSelected.setVisibility(VISIBLE);
                currentTrackName.setText(binauralBeat.getTitle());
                currentTrackDescription.setText(binauralBeat.getDescription());
                // TODO start pause beats and remove playing variable
                playing = true;
                playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_pause_24));
            });
            rcv.setAdapter(rvabbs);
            rcv.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        backgroundNoises.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bottomSheetDialog.setContentView(R.layout.background_noises_manager);
            RecyclerView rcv = bottomSheetDialog.findViewById(R.id.rcv_list_background_noises);

            RecyclerViewAdapterBackgroundNoisesManager manager = new RecyclerViewAdapterBackgroundNoisesManager(getContext(), noises);
            manager.setOnEntryClickedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName());
            });
            manager.setOnVolumeChangedListener((backgroundNoise, position) -> {
                System.out.println(backgroundNoise.getName() + " | " + backgroundNoise.getVolume());
            });
            rcv.setAdapter(manager);
            rcv.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        repeatButton.setOnClickListener(e -> {
            repeatBeat = !repeatBeat;
            if (repeatBeat){
                repeatButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_on_24));
                Toast.makeText(getContext(), "repeat is now on", Toast.LENGTH_SHORT).show();
            }
            else {
                repeatButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_repeat_24));
                Toast.makeText(getContext(), "repeat is now off", Toast.LENGTH_SHORT).show();
            }
        });

        autoStopButton.setOnClickListener(e -> {
            if(autoStopTime != null){
                new AlertDialog.Builder(getContext(), Tools.getThemeDialog()).setTitle("Disable Auto-Stop").setMessage("Do you really want to disable Auto-Stop?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            autoStopTime = null;
                            autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
            else {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
                bottomSheetDialog.setContentView(R.layout.auto_stop_duration_sheet);
                MaterialButton applyAutoStop = bottomSheetDialog.findViewById(R.id.btn_apply_auto_stop);
                NumberPicker autoStopHours = bottomSheetDialog.findViewById(R.id.np_hours_auto_stop);
                NumberPicker autoStopMinutes = bottomSheetDialog.findViewById(R.id.np_minutes_auto_stop);
                NumberPicker autoStopSeconds = bottomSheetDialog.findViewById(R.id.np_seconds_auto_stop);

                autoStopHours.setMinValue(0);
                autoStopHours.setMaxValue(23);
                autoStopHours.setValue(0);
                autoStopMinutes.setMinValue(0);
                autoStopMinutes.setMaxValue(59);
                autoStopMinutes.setValue(0);
                autoStopSeconds.setMinValue(0);
                autoStopSeconds.setMaxValue(59);
                autoStopSeconds.setValue(0);

                applyAutoStop.setOnClickListener(g -> {
                    Date dt = new Date();
                    Calendar cal = new GregorianCalendar();
                    cal.setTimeInMillis(dt.getTime());
                    cal.add(Calendar.SECOND, autoStopHours.getValue()*3600+autoStopMinutes.getValue()*60+autoStopSeconds.getValue());
                    autoStopTime = cal.getTime();
                    System.out.println(autoStopTime);
                    autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_off_24));
                    bottomSheetDialog.hide();
                });

                bottomSheetDialog.show();
            }
        });

        playing = false;
        playTrack.setOnClickListener(e -> {
            // TODO start pause beats and remove playing variable
            playing = !playing;
            if(!playing){
                playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
            }
            else {
                playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_pause_24));
            }
        });

        progressLineGraph.setData(freqs, 32, 4f, false, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());

        Thread newThread = new Thread(() -> {
            for (int i = 0; i < freqs.getDuration(); i++){
                progressLineGraph.updateProgress(i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        newThread.start();
    }

    private void generateNoises() {
        noises = new ArrayList<>();
        noises.add(new BackgroundNoise("Flowing water", R.drawable.ic_baseline_water_24, 25));
        noises.add(new BackgroundNoise("Explosions", R.drawable.ic_baseline_brightness_7_24, 0));
        noises.add(new BackgroundNoise("Wind", R.drawable.ic_baseline_air_24, 75));
        noises.add(new BackgroundNoise("Alarm", R.drawable.ic_baseline_access_alarm_24, 10));
        noises.add(new BackgroundNoise("White noise", R.drawable.ic_baseline_waves_24, 100));
    }
}