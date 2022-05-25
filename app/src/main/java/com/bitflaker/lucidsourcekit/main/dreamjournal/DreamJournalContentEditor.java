package com.bitflaker.lucidsourcekit.main.dreamjournal;

import static android.Manifest.permission.RECORD_AUDIO;
import static java.util.UUID.randomUUID;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DreamJournalContentEditor extends Fragment {
    private OnContinueButtonClicked mContinueButtonClicked;
    private OnCloseButtonClicked mCloseButtonClicked;
    private ConstraintLayout topHeading;
    private EditText title, description;
    private ScrollView editorScroller;
    private ImageButton continueButton, addRecording, closeEditor, addTags;
    private MaterialButton dateTime;
    private JournalInMemoryManager journalManger;
    private String journalEntryId;
    private boolean isRecordingRunning;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private MediaRecorder mRecorder;
    private String audioFName;
    private RecordingData currentRecording;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_dream_journal_content_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        Tools.makeStatusBarTransparent(DreamJournalEditor.this);

        topHeading = getView().findViewById(R.id.csl_dj_top_bar);
        title = getView().findViewById(R.id.txt_dj_title_dream);
        description = getView().findViewById(R.id.txt_dj_description_dream);
        editorScroller = getView().findViewById(R.id.scrl_editor_scroll);
        continueButton = getView().findViewById(R.id.btn_dj_continue_to_ratings);
        dateTime = getView().findViewById(R.id.btn_dj_date);
        addRecording = getView().findViewById(R.id.btn_dj_add_recording);
        addTags = getView().findViewById(R.id.btn_dj_add_tag);
        closeEditor = getView().findViewById(R.id.btn_dj_close_editor);

//        RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) editorScroller.getLayoutParams();
//        lParams.setMargins(0, Tools.getStatusBarHeight(this), 0, 0);
//        editorScroller.setLayoutParams(lParams);

        closeEditor.setOnClickListener(e -> new AlertDialog.Builder(getContext(), Tools.getThemeDialog()).setTitle("Discard changes").setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    journalManger.discardEntry(journalEntryId);
                    if(mCloseButtonClicked != null){
                        mCloseButtonClicked.onEvent();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show());

        addTags.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bsd.setContentView(R.layout.fragment_tags_editor);

            // TODO add logic

            bsd.show();
        });

        dateTime.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bsd.setContentView(R.layout.fragment_date_change);

            MaterialButton changeDate = bsd.findViewById(R.id.btn_dj_change_date);
            MaterialButton changeTime = bsd.findViewById(R.id.btn_dj_change_time);

            DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
            DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

            JournalInMemory entry = journalManger.getEntry(journalEntryId);
            changeDate.setText(df.format(entry.getTime().getTime()));
            changeTime.setText(tf.format(entry.getTime().getTime()));

            changeDate.setOnClickListener(e1 -> {
                new DatePickerDialog(getContext(), (datePicker, year, monthOfYear, dayOfMonth) -> {
                    Calendar date = entry.getTime();
                    date.set(Calendar.YEAR, year);
                    date.set(Calendar.MONTH, monthOfYear);
                    date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    entry.setTime(date);
                    changeDate.setText(df.format(date.getTime()));
                    // TODO change date on main editor
                }, entry.getTime().get(Calendar.YEAR), entry.getTime().get(Calendar.MONTH), entry.getTime().get(Calendar.DAY_OF_MONTH)).show();
            });

            changeTime.setOnClickListener(e1 -> {
                new TimePickerDialog(getContext(), (timePickerFrom, hourFrom, minuteFrom) -> {
                    Calendar time = entry.getTime();
                    time.set(Calendar.HOUR_OF_DAY, hourFrom);
                    time.set(Calendar.MINUTE, minuteFrom);
                    entry.setTime(time);
                    changeTime.setText(tf.format(time.getTime()));
                    // TODO change time on main editor
                }, entry.getTime().get(Calendar.HOUR_OF_DAY), entry.getTime().get(Calendar.MINUTE), true).show();
            });

            bsd.show();
        });

        addRecording.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialog_Dark);
            bsd.setContentView(R.layout.fragment_recording);

            ImageButton recordAudio = bsd.findViewById(R.id.btn_dj_record_audio);
            LinearLayout recsList = bsd.findViewById(R.id.ll_dj_recs_list);
            LinearLayout recsEntryList = bsd.findViewById(R.id.ll_dj_recs_entry_list);
            RelativeLayout recNow = bsd.findViewById(R.id.rl_dj_recording_audio);
            TextView noRecordingsFound = bsd.findViewById(R.id.txt_dj_no_recs_found);
            TextView recordingText = bsd.findViewById(R.id.txt_dj_recording);
            ImageButton recContinuePause = bsd.findViewById(R.id.btn_dj_pause_continue_recording);
            ImageButton recStop = bsd.findViewById(R.id.btn_dj_stop_recording);

            recordAudio.setOnClickListener(e1 -> {
                recsList.setVisibility(View.GONE);
                recNow.setVisibility(View.VISIBLE);
                recContinuePause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_pause_24, getContext().getTheme()));
                recordingText.setText(getResources().getString(R.string.recording));
                startRecordingAudio(recsList, recNow);
            });

            JournalInMemory entry = journalManger.getEntry(journalEntryId);
            if(entry.getAudioRecordings().size() == 0) {
                noRecordingsFound.setVisibility(View.VISIBLE);
            }
            else {
                for (RecordingData recData : entry.getAudioRecordings()) {
                    recsEntryList.addView(generateAudioEntry(recData));
                }
            }

            recStop.setOnClickListener(e1 -> {
                storeRecording(recsEntryList, recsList, recNow);
                noRecordingsFound.setVisibility(View.GONE);
            });
            recContinuePause.setOnClickListener(e1 -> {
                if(isRecordingRunning) {
                    isRecordingRunning = false;
                    pauseRecordingAudio();
                    recContinuePause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_play_arrow_24, getContext().getTheme()));
                    recordingText.setText(getResources().getString(R.string.recording_paused));
                }
                else {
                    isRecordingRunning = true;
                    continueRecordingAudio();
                    recContinuePause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_pause_24, getContext().getTheme()));
                    recordingText.setText(getResources().getString(R.string.recording));
                }
            });

            bsd.show();
        });

        continueButton.setOnClickListener(e -> {
            if(mContinueButtonClicked != null){
                mContinueButtonClicked.onEvent();
            }
        });

        title.requestFocus();
        // TODO: description has to lose focus and then clicked in again in order to fully get rid of the edittext scrolling => CHANGE!
    }

    private void pauseRecordingAudio() {
        mRecorder.pause();
    }

    private void continueRecordingAudio() {
        mRecorder.resume();
    }

    private void startRecordingAudio(LinearLayout listLayout, RelativeLayout recordLayout) {
        if(ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            isRecordingRunning = true;
            audioFName = getActivity().getFilesDir().getAbsolutePath();
            String filename = "/Recordings/recording_" + randomUUID() + ".aac";
            audioFName += filename;
            currentRecording = new RecordingData(audioFName);
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioEncodingBitRate(128000);
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setOutputFile(audioFName);
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                System.err.println("ERROR preparing audio recording: " + e.getMessage());
            }
            mRecorder.start();
        }
        else {
            listLayout.setVisibility(View.VISIBLE);
            recordLayout.setVisibility(View.GONE);
            ActivityCompat.requestPermissions(getActivity(), new String[]{ RECORD_AUDIO }, REQUEST_AUDIO_PERMISSION_CODE);
        }
    }

    private void stopRecordingAudio() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        audioFName = null;
    }

    private void storeRecording(LinearLayout recList, LinearLayout listLayout, RelativeLayout recordLayout) {
        stopRecordingAudio();
        // TODO add missing data to recording
        currentRecording.setRecordingTime(Calendar.getInstance());
        currentRecording.setRecordingLength(1000*128);
        journalManger.getEntry(journalEntryId).addAudioRecording(currentRecording);
        listLayout.setVisibility(View.VISIBLE);
        recordLayout.setVisibility(View.GONE);
        recList.addView(generateAudioEntry(currentRecording));
        currentRecording = null;
    }

    private LinearLayout generateAudioEntry(RecordingData currentRecording) {
        int dp48 = Tools.dpToPx(getContext(), 48);
        int dp20 = Tools.dpToPx(getContext(), 20);
        int dp10 = Tools.dpToPx(getContext(), 10);
        int dp8 = Tools.dpToPx(getContext(), 8);
        int dp5 = Tools.dpToPx(getContext(), 5);

        LinearLayout entryContainer = new LinearLayout(getContext());
        LinearLayout.LayoutParams lParamsEntryContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsEntryContainer.setMargins(dp20, dp5, dp20, dp5);
        entryContainer.setLayoutParams(lParamsEntryContainer);
        entryContainer.setOrientation(LinearLayout.HORIZONTAL);
        entryContainer.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_spinner, getContext().getTheme()));
        entryContainer.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
        entryContainer.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton playButton = new ImageButton(getContext());
        LinearLayout.LayoutParams lParamsPlayButton = new LinearLayout.LayoutParams(dp48, dp48);
        lParamsPlayButton.setMargins(dp8, 0, 0, 0);
        playButton.setLayoutParams(lParamsPlayButton);
        playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_play_arrow_24, getContext().getTheme()));
        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_spinner, getContext().getTheme()));
        playButton.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
        entryContainer.addView(playButton);

        LinearLayout labelsContainer = new LinearLayout(getContext());
        LinearLayout.LayoutParams lParamsLabelsContainer = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsLabelsContainer.setMargins(dp10, dp8, 0, dp8);
        lParamsLabelsContainer.weight = 1;
        labelsContainer.setLayoutParams(lParamsLabelsContainer);
        labelsContainer.setOrientation(LinearLayout.VERTICAL);
        entryContainer.addView(labelsContainer);

        TextView heading = new TextView(getContext());
        heading.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        heading.setText("Recording");
        heading.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, getContext().getTheme()));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        heading.setTypeface(null, Typeface.BOLD);
        labelsContainer.addView(heading);

        TextView timestamp = new TextView(getContext());
        timestamp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        timestamp.setText(df.format(currentRecording.getRecordingTime().getTime()) + " • " + tf.format(currentRecording.getRecordingTime().getTime()));
        timestamp.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getContext().getTheme()));
        timestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        labelsContainer.addView(timestamp);

        TextView duration = new TextView(getContext());
        LinearLayout.LayoutParams lParamsDuration = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsDuration.setMargins(0, 0, dp10, 0);
        duration.setLayoutParams(lParamsDuration);
        int seconds = (int)(currentRecording.getRecordingLength() / 1000);
        int sec = seconds % 60;
        int min = (seconds / 60)%60;
        int hours = (seconds/60)/60;
        String secS = String.format(Locale.ENGLISH, "%02d" , sec);
        String minS = String.format(Locale.ENGLISH, "%02d" , min);
        String hoursS = String.format(Locale.ENGLISH, "%02d" , hours);
        duration.setText(hours > 0 ? hoursS + ":" : "" + minS + ":" + secS);
        duration.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getContext().getTheme()));
        duration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        entryContainer.addView(duration);

        ImageButton deleteButton = new ImageButton(getContext());
        LinearLayout.LayoutParams lParamsDeleteButton = new LinearLayout.LayoutParams(dp48, dp48);
        lParamsDeleteButton.setMargins(0, 0, dp8, 0);
        deleteButton.setLayoutParams(lParamsDeleteButton);
        deleteButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_cross_24, getContext().getTheme()));
        deleteButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_spinner, getContext().getTheme()));
        deleteButton.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getContext().getTheme()));
        entryContainer.addView(deleteButton);

        return entryContainer;
    }

    public void setJournalEntryId(String id) {
        journalEntryId = id;
        journalManger = JournalInMemoryManager.getInstance();
    }

    public interface OnContinueButtonClicked {
        void onEvent();
    }

    public void setOnContinueButtonClicked(OnContinueButtonClicked eventListener) {
        mContinueButtonClicked = eventListener;
    }

    public interface OnCloseButtonClicked {
        void onEvent();
    }

    public void setOnCloseButtonClicked(OnCloseButtonClicked eventListener) {
        mCloseButtonClicked = eventListener;
    }
}