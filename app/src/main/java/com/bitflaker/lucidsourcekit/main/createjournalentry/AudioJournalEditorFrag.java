package com.bitflaker.lucidsourcekit.main.createjournalentry;

import android.app.Fragment;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;

public class AudioJournalEditorFrag extends Fragment {
    private MaterialButton addButton;
    OnAudioRecordingRequested mListener;
    private ImageButton currentPlayingImageButton = null;
    private boolean currentlyPlaying = false;

    public static AudioJournalEditorFrag newInstance() {
        return new AudioJournalEditorFrag();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dream_audio_story, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addButton = getView().findViewById(R.id.btn_add_audio);
        addButton.setOnClickListener(e -> {
            if(currentlyPlaying){
                currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);   // TODO stop playing record
                currentPlayingImageButton = null;
                currentlyPlaying = false;
            }
            mListener.onEvent();
        });
    }

    public void addRecordingToList(String title, String length) {
        LinearLayout llContainer = new LinearLayout(getContext());
        LinearLayout.LayoutParams llparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llparams.setMargins(0, Tools.dpToPx(getContext(), 5), 0, Tools.dpToPx(getContext(), 5));
        llContainer.setLayoutParams(llparams);
        llContainer.setOrientation(LinearLayout.HORIZONTAL);
        llContainer.setBackground(getResources().getDrawable(R.drawable.rounded_border));
        llContainer.setBackgroundTintList(Tools.getAttrColor(R.attr.secondColor, getContext().getTheme()));
        int dp15 = Tools.dpToPx(getContext(), 5);
        llContainer.setPadding(dp15, dp15, dp15, dp15);

        ImageButton playPause = new ImageButton(getContext());
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparams.gravity = Gravity.CENTER_VERTICAL;
        lparams.leftMargin = Tools.dpToPx(getContext(), 5);
        playPause.setLayoutParams(lparams);
        playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        playPause.setBackgroundTintList(Tools.getAttrColor(R.attr.transparent, getContext().getTheme()));
        playPause.setOnClickListener(e -> {
            if(currentlyPlaying && currentPlayingImageButton == playPause) {
                playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);   // TODO stop playing record
                currentPlayingImageButton = null;
                currentlyPlaying = false;
            }
            else if(currentlyPlaying) {
                currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);   // TODO stop playing old record
                playPause.setImageResource(R.drawable.ic_baseline_pause_24);    // TODO start playing record
                currentPlayingImageButton = playPause;
                currentlyPlaying = true;
            }
            else {
                playPause.setImageResource(R.drawable.ic_baseline_pause_24);    // TODO start playing record
                currentPlayingImageButton = playPause;
                currentlyPlaying = true;
            }
        });
        // TODO if recording track ended reset pause button to play button

        TextView titleView = new TextView(getContext());
        LinearLayout.LayoutParams lparamsTxt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparamsTxt.weight = 1;
        lparamsTxt.gravity = Gravity.CENTER_VERTICAL;
        titleView.setLayoutParams(lparamsTxt);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        TextView lengthView = new TextView(getContext());
        LinearLayout.LayoutParams lparamsTxtLength = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparamsTxtLength.gravity = Gravity.CENTER_VERTICAL;
        lengthView.setLayoutParams(lparamsTxtLength);
        lengthView.setText(length);

        ImageButton remove = new ImageButton(getContext());
        LinearLayout.LayoutParams removeLparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        removeLparams.gravity = Gravity.CENTER_VERTICAL;
        removeLparams.rightMargin = Tools.dpToPx(getContext(), 5);
        remove.setLayoutParams(removeLparams);
        remove.setImageResource(R.drawable.ic_baseline_cross_24);
        remove.setBackgroundTintList(Tools.getAttrColor(R.attr.transparent, getContext().getTheme()));
        remove.setOnClickListener(e -> {
            new AlertDialog.Builder(getContext()).setTitle(getResources().getString(R.string.recording_delete_header)).setMessage(getResources().getString(R.string.recording_delete_message))
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                        if(currentPlayingImageButton == playPause) {
                            // TODO stop playing record
                            currentPlayingImageButton = null;
                            currentlyPlaying = false;
                        }
                        ((LinearLayout) getView().findViewById(R.id.ll_records_container)).removeView(llContainer);
                    })
                    .setNegativeButton(getResources().getString(R.string.no), null)
                    .show();
        });

        llContainer.addView(playPause);
        llContainer.addView(titleView);
        llContainer.addView(lengthView);
        llContainer.addView(remove);

        ((LinearLayout) getView().findViewById(R.id.ll_records_container)).addView(llContainer);
    }

    public interface OnAudioRecordingRequested {
        void onEvent();
    }

    public void setOnAudioRecordingRequested(OnAudioRecordingRequested eventListener) {
        mListener = eventListener;
    }
}
