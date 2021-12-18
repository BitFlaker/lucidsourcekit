package com.bitflaker.lucidsourcekit.main.createjournalentry;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class AddTextEntry extends AppCompatActivity {

    private MaterialButton dateButton;
    private MaterialButton timeButton;
    private DatePickerDialog dpd;
    private DateFormat dateFormat;
    private TimePickerDialog tpd;
    private LinearLayout dreamEditorBox;

    private ImageButton tapOutsideRecording;
    private ImageButton pauseContinueRecording;
    private ImageButton stopRecording;
    private ImageView backgroundUnfocus;
    private TextView recordingText;
    private boolean isRecordingRunning;
    private AudioJournalEditorFrag audioFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_text_entry);

        dateButton = findViewById(R.id.btn_date);
        timeButton = findViewById(R.id.btn_time);
        dreamEditorBox = findViewById(R.id.ll_dream_editor);
        tapOutsideRecording = findViewById(R.id.btn_recording_tapped_outside);
        pauseContinueRecording = findViewById(R.id.btn_pause_continue_recording);
        stopRecording = findViewById(R.id.btn_stop_recording);
        backgroundUnfocus = findViewById(R.id.img_background_unfocus);
        recordingText = findViewById(R.id.txt_recording);
        isRecordingRunning = false;

        setupTimePicker();
        setupDatePicker();

        switch(JournalTypes.values()[getIntent().getIntExtra("type", -1)]){
            case Text:
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), TextJournalEditorFrag.newInstance(), "").commit();   // TODO check tag argument s
                break;
            case Audio:
                setupRecordingPopup();
                audioFrag = AudioJournalEditorFrag.newInstance();
                audioFrag.setOnAudioRecordingRequested(() -> {
                    showRecording();
                });
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), audioFrag, "").commit();   // TODO check tag argument s
                break;
            case Forms:
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), FormsJournalEditorFrag.newInstance(), "").commit();   // TODO check tag argument s
                break;
        }
    }

    private void setupRecordingPopup() {
        tapOutsideRecording.setOnClickListener(e -> {
            // TODO ask if want to discard and stop recording
            hideRecording();
            String title = "Recording 1 this is a very very very long text and i want to see how it is being handled";
            String length = "02:35";
            audioFrag.addRecordingToList(title, length);
        });
        stopRecording.setOnClickListener(e -> {
            // TODO ask for name and store recording
            hideRecording();
            String title = "Recording 1 this is a very very very long text and i want to see how it is being handled";
            String length = "02:35";
            audioFrag.addRecordingToList(title, length);
        });
        pauseContinueRecording.setOnClickListener(e -> {
            if(isRecordingRunning) {
                isRecordingRunning = false; // TODO STOP RECORDING
                pauseContinueRecording.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                recordingText.setText(getResources().getString(R.string.recording_paused));
            }
            else {
                isRecordingRunning = true;  // TODO START RECORDING
                pauseContinueRecording.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                recordingText.setText(getResources().getString(R.string.recording));
            }
        });
    }

    private void setupTimePicker() {
        Date date = Calendar.getInstance().getTime();
        tpd = new TimePickerDialog(AddTextEntry.this, (timePicker, hourOfDay, minute) -> {
            timeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, date.getHours(), date.getMinutes(), true);
        timeButton.setText(String.format("%02d:%02d", date.getHours(), date.getMinutes()));
        timeButton.setOnClickListener(e -> {
            tpd.show();
        });
    }

    private void setupDatePicker() {
        dpd = new DatePickerDialog(AddTextEntry.this);
        dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        dateButton.setText(dateFormat.format(new Date()));
        dateButton.setOnClickListener(e -> {
            dpd.show();
        });
        dpd.setOnDateSetListener((datePicker, year, month, dayOfMonth) -> {
            dateButton.setText(dateFormat.format(new Date(year, month, dayOfMonth)));
        });
    }

    private void showRecording() {
        findViewById(R.id.rl_recording).setVisibility(View.VISIBLE);
        tapOutsideRecording.setVisibility(View.VISIBLE);
        backgroundUnfocus.setVisibility(View.VISIBLE);
        isRecordingRunning = true;  // TODO START RECORDING
        pauseContinueRecording.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_pause_24));
        recordingText.setText(getResources().getString(R.string.recording));
    }

    private void hideRecording() {
        findViewById(R.id.rl_recording).setVisibility(View.GONE);
        tapOutsideRecording.setVisibility(View.GONE);
        backgroundUnfocus.setVisibility(View.GONE);
        isRecordingRunning = false;  // TODO STOP RECORDING
    }
}

