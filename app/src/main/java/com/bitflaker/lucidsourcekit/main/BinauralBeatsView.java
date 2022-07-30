package com.bitflaker.lucidsourcekit.main;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.Brainwaves;
import com.bitflaker.lucidsourcekit.charts.LineGraph;
import com.bitflaker.lucidsourcekit.charts.TextLegend;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeatsCollection;
import com.bitflaker.lucidsourcekit.main.binauralbeats.BinauralBeatsPlayer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BinauralBeatsView extends Fragment {
    private LineGraph progressLineGraph;
    private ImageButton displayAllBeats, backgroundNoises, repeatButton, autoStopButton, playTrack;
    private List<BackgroundNoise> noises;
    private boolean repeatBeat, playingFinished, isAutoStopTimerRunning;
    private TextView currentTrackName, currentTrackDescription, binauralTimeline, binauralTimeTotal, binauralFrequency, freqGreekLetterName, freqGreekLetter, freqCarrierFreq, carrierFreqHeading;
    private TextLegend binauralLegend;
    private MaterialCardView legendCard;
    private BinauralBeatsPlayer binBeatPlayer;
    private LinearLayout timeContainer, carrierFreqContainer;
    private Handler autoStopHandler = new Handler();
    private int autoStopInterval = -1;

    private Runnable stopCurrentlyPlayingTrack = () -> {
        playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
        binBeatPlayer.pause();
        autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
        autoStopInterval = -1;
    };

    // TODO get real noises

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_binaural_beats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getView().findViewById(R.id.txt_binaural_beats_heading).setLayoutParams(Tools.getRelativeLayoutParamsTopStatusbar(getContext()));
        progressLineGraph = getView().findViewById(R.id.lg_binaural_time_progress);
        legendCard = getView().findViewById(R.id.crd_legend);
        displayAllBeats = getView().findViewById(R.id.btn_display_all_beats);
        backgroundNoises = getView().findViewById(R.id.btn_add_background_noise);
        repeatButton = getView().findViewById(R.id.btn_loop_track);
        autoStopButton = getView().findViewById(R.id.btn_auto_stop);
        currentTrackName = getView().findViewById(R.id.txt_curr_track_name);
        currentTrackDescription = getView().findViewById(R.id.txt_curr_track_description);
        playTrack = getView().findViewById(R.id.btn_play_track);
        binauralLegend = getView().findViewById(R.id.tl_binaural_legend);
        binauralTimeline = getView().findViewById(R.id.txt_binaural_beats_timeline);
        binauralTimeTotal = getView().findViewById(R.id.txt_binaural_beats_total_time);
        binauralFrequency = getView().findViewById(R.id.txt_current_binaural_frequency);
        freqGreekLetterName = getView().findViewById(R.id.txt_current_frequency_name);
        freqGreekLetter = getView().findViewById(R.id.txt_current_frequency_greek_letter);
        freqCarrierFreq = getView().findViewById(R.id.txt_carrier_frequency);
        timeContainer = getView().findViewById(R.id.ll_bbp_time_container);
        carrierFreqContainer = getView().findViewById(R.id.ll_bbp_carrier_freq_heading);
        carrierFreqHeading = getView().findViewById(R.id.txt_bbp_carrier_freq_heading);

        repeatBeat = false;
        playingFinished = false;

        generateNoises();

        displayAllBeats.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            bottomSheetDialog.setContentView(R.layout.binaural_beats_selector_sheet);
            RecyclerView rcv = bottomSheetDialog.findViewById(R.id.rcv_list_binaural_beats);

            RecyclerViewAdapterBinauralBeatsSelector rvabbs = new RecyclerViewAdapterBinauralBeatsSelector(getContext(), BinauralBeatsCollection.getInstance().getBinauralBeats());
            rvabbs.setOnEntryClickedListener((binauralBeat, position) -> {
                playingFinished = false;
                bottomSheetDialog.dismiss();
                timeContainer.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams) carrierFreqContainer.getLayoutParams();
                lParams.topMargin = Tools.dpToPx(getContext(), 15);
                carrierFreqContainer.setLayoutParams(lParams);
                progressLineGraph.setVisibility(View.VISIBLE);
                carrierFreqHeading.setVisibility(View.VISIBLE);
                currentTrackName.setVisibility(View.VISIBLE);
                currentTrackName.setText(binauralBeat.getTitle());
                currentTrackDescription.setText(binauralBeat.getDescription());
                binauralTimeTotal.setText(String.format(" / %s", getTimeStringFromSeconds((int) binauralBeat.getFrequencyList().getDuration())));
                binauralTimeline.setText(getTimeStringFromSeconds(0));
                binauralFrequency.setText("0.00");
                freqCarrierFreq.setText(String.format(Locale.ENGLISH, "%.0f Hz", binauralBeat.getBaseFrequency()));
                playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                setDataForProgress(binauralBeat, 0);
                if(binBeatPlayer != null){
                    binBeatPlayer.stop();
                    progressLineGraph.resetProgress();
                }
                binBeatPlayer = new BinauralBeatsPlayer(binauralBeat);
                progressLineGraph.setData(binauralBeat.getFrequencyList(), 32, 4f, 0f, false, Brainwaves.getStageColors(), Brainwaves.getStageFrequencyCenters());
                binBeatPlayer.setOnTrackProgressListener(((currentBinauralBeat, progress) -> {
                    setDataForProgress(currentBinauralBeat, progress);
                    FragmentActivity fragAct = getActivity();
                    if (fragAct != null) {
                        fragAct.runOnUiThread(() -> progressLineGraph.updateProgress(progress));
                        fragAct.runOnUiThread(() -> progressLineGraph.updateProgress(progress));
                    }
                    else {
                        binBeatPlayer.stop();
                    }
                }));
                binBeatPlayer.setOnTrackFinishedListener(currentBinauralBeat -> {
                    playingFinished = true;
                    setEndValues(currentBinauralBeat);
                    if(repeatBeat) {
                        playingFinished = false;
                        progressLineGraph.resetProgress();
                        binBeatPlayer.play();
                    }
                    else {
                        playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    }
                });
            });
            rcv.setAdapter(rvabbs);
            rcv.setLayoutManager(new LinearLayoutManager(getContext()));

            bottomSheetDialog.show();
        });

        backgroundNoises.setOnClickListener(e -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
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
            if(autoStopInterval != -1){
                new AlertDialog.Builder(getContext(), Tools.getThemeDialog()).setTitle("Disable Auto-Stop").setMessage("Do you really want to disable Auto-Stop?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            autoStopHandler.removeCallbacks(stopCurrentlyPlayingTrack);
                            autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_24));
                            autoStopInterval = -1;
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
            else {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
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
                    isAutoStopTimerRunning = false;
                    autoStopInterval = (autoStopHours.getValue() * 3600 + autoStopMinutes.getValue() * 60 + autoStopSeconds.getValue()) * 1000;
                    autoStopHandler = new Handler();
                    if(binBeatPlayer.isPlaying()) {
                        startAutoStopTimeNow();
                    }
                    autoStopButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_outline_timer_off_24));
                    bottomSheetDialog.dismiss();
                });
                bottomSheetDialog.show();
            }
        });

        playTrack.setOnClickListener(e -> {
            if(binBeatPlayer != null){
                if(playingFinished) {
                    binauralTimeline.setText(getTimeStringFromSeconds(0));
                    progressLineGraph.resetProgress();
                    playingFinished = false;
                }
                if(!binBeatPlayer.isPlaying()){
                    playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_pause_24));
                    binBeatPlayer.play();
                    if(autoStopInterval != -1 && !isAutoStopTimerRunning) {
                        startAutoStopTimeNow();
                    }
                }
                else {
                    playTrack.setImageDrawable(getContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    binBeatPlayer.pause();
                }
            }
            else {
                Toast.makeText(getContext(), "no binaural beat selected", Toast.LENGTH_SHORT).show();
            }
        });

        progressLineGraph.setBottomLineSpacing(10);
        progressLineGraph.setDrawGradient(true);
        progressLineGraph.setGradientOpacity(0.25f);
        progressLineGraph.setDrawProgressIndicator(false);

        String[] labels = new String[] { "β", "α", "θ", "δ" };
        binauralLegend.setData(labels, Brainwaves.getStageColors(), Tools.getAttrColor(R.attr.secondaryTextColor, getContext().getTheme()), Tools.getAttrColor(R.attr.tertiaryTextColor, getContext().getTheme()), 18);
    }

    private void startAutoStopTimeNow() {
        autoStopHandler.postAtTime(stopCurrentlyPlayingTrack, System.currentTimeMillis() + autoStopInterval);
        autoStopHandler.postDelayed(stopCurrentlyPlayingTrack, autoStopInterval);
        isAutoStopTimerRunning = true;
    }

    private void setEndValues(BinauralBeat currentBinauralBeat) {
        getActivity().runOnUiThread(() -> {
            int finishProgress = (int) currentBinauralBeat.getFrequencyList().getDuration();
            binauralTimeline.setText(getTimeStringFromSeconds(finishProgress));
            binauralFrequency.setText(String.format(Locale.ENGLISH, "%.2f", currentBinauralBeat.getFrequencyList().getFrequencyAtDuration(finishProgress)));
        });
    }

    private void setDataForProgress(BinauralBeat binauralBeat, int progress) {
        double currFreq = binauralBeat.getFrequencyList().getFrequencyAtDuration(progress);
        int stageIndex = Brainwaves.getStageIndex(currFreq);
        String greekLetter = Brainwaves.getStageFrequencyGreekLetters()[stageIndex];
        String greekLetterName = Brainwaves.getStageFrequencyGreekLetterNames()[stageIndex];
        FragmentActivity fragAct = getActivity();
        if (fragAct != null) {
            fragAct.runOnUiThread(() -> {
                freqGreekLetter.setText(greekLetter);
                freqGreekLetterName.setText(greekLetterName);
                binauralLegend.setCurrentSelectedIndex(stageIndex);
                binauralFrequency.setText(String.format(Locale.ENGLISH, "%.2f", currFreq));
                binauralTimeline.setText(getTimeStringFromSeconds(progress));
            });
        }
        else {
            binBeatPlayer.stop();
        }
    }

    private String getTimeStringFromSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return (hours > 0 ? String.format("%02d:", hours) : "") + String.format("%02d:%02d", minutes, seconds);
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