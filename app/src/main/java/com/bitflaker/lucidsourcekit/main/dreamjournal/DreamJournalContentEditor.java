package com.bitflaker.lucidsourcekit.main.dreamjournal;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static java.util.UUID.randomUUID;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.RecordingObjectTools;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class DreamJournalContentEditor extends Fragment {
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private OnContinueButtonClicked mContinueButtonClicked;
    private OnCloseButtonClicked mCloseButtonClicked;
    private EditText title, description;
    private FlexboxLayout formsContainer;
    private MaterialButton continueButton, addRecording, closeEditor, addTags;
    private MaterialButton dateTime;
    private ImageButton currentPlayingImageButton;
    private boolean isRecordingRunning;
    private MediaRecorder mRecorder;
    private String audioFName;
    private AudioLocation currentRecording;
    private MainDatabase db;
    private List<String> allAvailableTags;
    private MediaPlayer mPlayer;
    private DreamJournalEntry.EntryType entryType;
    private DreamJournalEntry entry;
    private CompositeDisposable compositeDisposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dream_journal_content_editor, container, false);

        db = MainDatabase.getInstance(getContext());
        compositeDisposable = new CompositeDisposable();
        title = view.findViewById(R.id.txt_dj_title_dream);
        description = view.findViewById(R.id.txt_dj_description_dream);
        continueButton = view.findViewById(R.id.btn_dj_continue_to_ratings);
        dateTime = view.findViewById(R.id.btn_dj_date);
        addRecording = view.findViewById(R.id.btn_dj_add_recording);
        addTags = view.findViewById(R.id.btn_dj_add_tag);
        formsContainer = view.findViewById(R.id.flx_dj_form_dream);
        closeEditor = view.findViewById(R.id.btn_dj_close_editor);

        DateFormat mediumDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        DateFormat shortDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        DateFormat shortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        dateTime.setText(shortDateTimeFormat.format(Tools.calendarFromMillis(entry.journalEntry.timeStamp).getTime()));

        if(entryType == DreamJournalEntry.EntryType.FORMS_TEXT) {
            formsContainer.setVisibility(View.VISIBLE);
            description.setVisibility(View.GONE);
            setupForms();
        }

        setupCloseButton();
        setupTagsEditor();
        setupDateTimePicker(mediumDateFormat, shortDateTimeFormat, shortTimeFormat);
        setupRecordingsEditor();
        setupContinueButton();

        title.setText(entry.journalEntry.title);
        title.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                entry.journalEntry.title = s.toString();
            }
        });
        description.setText(entry.journalEntry.description);
        description.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                entry.journalEntry.description = s.toString();
            }
        });
        if(TextUtils.isEmpty(entry.journalEntry.title)) {
            new Handler().postDelayed(() -> {
                title.requestFocus();
                InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        }
        return view;
    }

    private void setupForms() {
        String formsTemplate = "I was in <<EDIT>> and I saw <<EDIT>>. The daytime was <<EDIT>>. Characters in my dream were <<EDIT>>. I was <<EDIT>>. The characters in my dream behaved <<EDIT>>."; // TODO make this text editable for users
        String[] formsTemplateSplit = Arrays.stream(formsTemplate.split("<<EDIT>>")).filter(x -> x != null && !x.isEmpty() && !x.isBlank()).toArray(String[]::new);
        for (int i = 0; i < formsTemplateSplit.length; i++) {
            String[] sentences = new String[] { formsTemplateSplit[i] };
            if (startsWithSentenceEnd(formsTemplateSplit[i])) {
                sentences = separateAtSentenceEnd(formsTemplateSplit[i]);
            }
            for (String sentence : sentences) {
                formsContainer.addView(generateTextView(sentence));
            }
            if(i < formsTemplateSplit.length - 1) {
                formsContainer.addView(generateEditText());
            }
        }
    }

    private EditText generateEditText() {
        int dpm5 = Tools.dpToPx(getContext(), -5);
        EditText et = new EditText(getContext());
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins(0, dpm5, 0, dpm5);
        et.setLayoutParams(lParams);
        et.setMinWidth(Tools.dpToPx(getContext(), 70));
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        // This will lead to a lot of events while typing the description
        // (after every character written), but it works very smoothly even for long
        // texts and therefore this implementation seems to be fine for now
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                entry.journalEntry.description = getFormResult();
            }
        });
        return et;
    }

    private TextView generateTextView(String sentence) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setText(sentence);
        return tv;
    }

    private String[] separateAtSentenceEnd(String sentence) {
        String[] separated = new String[2];
        int splitPos = getLastSentenceEnd(sentence);
        if(splitPos < sentence.length()){
            separated[0] = sentence.substring(0, splitPos + 1);
            separated[1] = sentence.substring(splitPos + 1);
        }
        else {
            separated[0] = sentence.substring(0, splitPos);
            separated[1] = "";
        }
        return separated;
    }

    private int getLastSentenceEnd(String sentence) {
        for (int i = 0; i < sentence.length(); i++){
            if(!isSentenceEndSymbol(sentence.charAt(i))) {
                return i;
            }
        }
        return sentence.length();
    }

    private boolean isSentenceEndSymbol(char c) {
        char[] sentenceEnds = new char[]{'.', '!', '?'};
        for (char sentenceEnd : sentenceEnds) {
            if (sentenceEnd == c) { return true; }
        }
        return false;
    }

    private boolean startsWithSentenceEnd(String sentence) {
        if(sentence.isEmpty()){
            return false;
        }
        return isSentenceEndSymbol(sentence.charAt(0));
    }

    public String getFormResult() {
        StringBuilder sb = new StringBuilder();
        int count = formsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = formsContainer.getChildAt(i);
            if (view instanceof EditText editText) {
                sb.append(editText.getText());
            }
            else if (view instanceof TextView textView) {
                sb.append(textView.getText());
            }
        }
        return sb.toString();
    }

    private void setupContinueButton() {
        continueButton.setOnClickListener(e -> {
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            if(mContinueButtonClicked != null) {
                mContinueButtonClicked.onEvent();
            }
        });
    }

    private void setupCloseButton() {
        closeEditor.setOnClickListener(e -> new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Discard changes")
                .setMessage("Do you really want to discard all changes")
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    entry.removeAllAddedRecordings();
                    if(mCloseButtonClicked != null) {
                        mCloseButtonClicked.onEvent();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show());
    }

    private void setupRecordingsEditor() {
        addRecording.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            bsd.setContentView(R.layout.sheet_journal_recording);

            ImageButton recordAudio = bsd.findViewById(R.id.btn_dj_record_audio);
            LinearLayout recsList = bsd.findViewById(R.id.ll_dj_recs_list);
            LinearLayout recsEntryList = bsd.findViewById(R.id.ll_dj_recs_entry_list);
            RelativeLayout recNow = bsd.findViewById(R.id.rl_dj_recording_audio);
            TextView noRecordingsFound = bsd.findViewById(R.id.txt_dj_no_recs_found);
            TextView recordingText = bsd.findViewById(R.id.txt_dj_recording);
            ImageButton recContinuePause = bsd.findViewById(R.id.btn_dj_pause_continue_recording);
            ImageButton recStop = bsd.findViewById(R.id.btn_dj_stop_recording);

            showStoredRecordings(recsEntryList, noRecordingsFound);
            setupStartRecordingButton(recordAudio, recsList, recNow, recordingText, recContinuePause);
            setupStopRecordingButton(recsList, recsEntryList, recNow, noRecordingsFound, recStop);
            setupPauseContinueRecordingButton(recordingText, recContinuePause);

            bsd.setOnDismissListener(e1 -> {
                if(isRecordingRunning) {
                    storeRecording(recsEntryList, recsList, recNow, noRecordingsFound);
                    isRecordingRunning = false;
                }
                if(mPlayer != null && mPlayer.isPlaying()) {
                    stopCurrentPlayback();
                }
            });

            bsd.show();
        });
    }

    private void showStoredRecordings(LinearLayout recsEntryList, TextView noRecordingsFound) {
        if(entry.audioLocations.isEmpty()) {
            noRecordingsFound.setVisibility(View.VISIBLE);
        }
        else {
            for (AudioLocation recData : entry.audioLocations) {
                recsEntryList.addView(generateAudioEntry(recData, noRecordingsFound));
            }
        }
    }

    private void setupPauseContinueRecordingButton(TextView recordingText, ImageButton recContinuePause) {
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
    }

    private void setupStopRecordingButton(LinearLayout recsList, LinearLayout recsEntryList, RelativeLayout recNow, TextView noRecordingsFound, ImageButton recStop) {
        recStop.setOnClickListener(e1 -> {
            storeRecording(recsEntryList, recsList, recNow, noRecordingsFound);
            noRecordingsFound.setVisibility(View.GONE);
            isRecordingRunning = false;
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
    }

    private void setupStartRecordingButton(ImageButton recordAudio, LinearLayout recsList, RelativeLayout recNow, TextView recordingText, ImageButton recContinuePause) {
        recordAudio.setOnClickListener(e1 -> {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            stopCurrentPlaybackIfPlaying();
            recsList.setVisibility(View.GONE);
            recNow.setVisibility(View.VISIBLE);
            recContinuePause.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_pause_24, getContext().getTheme()));
            recordingText.setText(getResources().getString(R.string.recording));
            startRecordingAudio(recsList, recNow);
        });
    }

    private void setupDateTimePicker(DateFormat df, DateFormat dtf, DateFormat tf) {
        dateTime.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            bsd.setContentView(R.layout.sheet_dream_timestamp);

            MaterialButton changeDate = bsd.findViewById(R.id.btn_dj_change_date);
            MaterialButton changeTime = bsd.findViewById(R.id.btn_dj_change_time);

            Date date = Tools.calendarFromMillis(entry.journalEntry.timeStamp).getTime();
            changeDate.setText(df.format(date));
            changeTime.setText(tf.format(date));

            setupChangeDateButton(df, dtf, changeDate);
            setupChangeTimeButton(dtf, tf, changeTime);

            bsd.show();
        });
    }

    private void setupChangeTimeButton(DateFormat dtf, DateFormat tf, MaterialButton changeTime) {
        changeTime.setOnClickListener(e1 -> {
            Calendar time = Tools.calendarFromMillis(entry.journalEntry.timeStamp);
            new TimePickerDialog(getContext(), (timePickerFrom, hourFrom, minuteFrom) -> {
                time.set(Calendar.HOUR_OF_DAY, hourFrom);
                time.set(Calendar.MINUTE, minuteFrom);
                entry.journalEntry.timeStamp = time.getTimeInMillis();
                changeTime.setText(tf.format(time.getTime()));
                dateTime.setText(dtf.format(time.getTime()));
            }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true).show();
        });
    }

    private void setupChangeDateButton(DateFormat df, DateFormat dtf, MaterialButton changeDate) {
        changeDate.setOnClickListener(e1 -> {
            Calendar date = Tools.calendarFromMillis(entry.journalEntry.timeStamp);
            new DatePickerDialog(getContext(), (datePicker, year, monthOfYear, dayOfMonth) -> {
                date.set(Calendar.YEAR, year);
                date.set(Calendar.MONTH, monthOfYear);
                date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                entry.journalEntry.timeStamp = date.getTimeInMillis();
                changeDate.setText(df.format(date.getTime()));
                dateTime.setText(dtf.format(date.getTime()));
            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTagsEditor() {
        compositeDisposable.add(db.getJournalEntryTagDao().getAllTagTexts().subscribe((tagsTexts, throwable) -> allAvailableTags = tagsTexts));
        addTags.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogStyle);
            bsd.setContentView(R.layout.sheet_journal_tags_editor);

            FlexboxLayout tagsContainer = bsd.findViewById(R.id.flx_dj_tags_to_add);
            AutoCompleteTextView tagAddBox = bsd.findViewById(R.id.txt_dj_tags_enter);

            showAllSetTags(tagsContainer);
            tagAddBox.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, allAvailableTags));
            setupTagEditorEditText(tagsContainer, tagAddBox);
            setupTagEditorSelection(tagsContainer, tagAddBox);

            bsd.show();
        });
    }

    private void setupTagEditorSelection(FlexboxLayout tagsContainer, AutoCompleteTextView tagAddBox) {
        tagAddBox.setOnItemClickListener((adapterView, view1, i, l) -> {
            String enteredTag = tagAddBox.getText().toString();
            tryAddTag(tagsContainer, tagAddBox, enteredTag);
        });
    }

    private void setupTagEditorEditText(FlexboxLayout tagsContainer, AutoCompleteTextView tagAddBox) {
        tagAddBox.setOnEditorActionListener((textView, i, keyEvent) -> {
            String enteredTag = tagAddBox.getText().toString();
            if (i == IME_ACTION_DONE  && !enteredTag.isEmpty()) {
                tryAddTag(tagsContainer, tagAddBox, enteredTag);
            }
            return true;
        });
    }

    private void tryAddTag(FlexboxLayout tagsContainer, AutoCompleteTextView tagAddBox, String enteredTag) {
        if(entry.journalEntryTags.stream().noneMatch(x -> x.description.equalsIgnoreCase(enteredTag))){
            entry.journalEntryTags.add(new JournalEntryTag(enteredTag));
            Chip tagChip = generateTagChip(enteredTag);
            tagChip.setOnCloseIconClickListener(e1 -> {
                removeTag(enteredTag);
                tagsContainer.removeView(tagChip);
            });
            tagsContainer.addView(tagChip);
            tagAddBox.setText("");
        }
    }

    private void showAllSetTags(FlexboxLayout tagsContainer) {
        List<String> tags = entry.getStringTags();
        for (String tag : tags) {
            Chip tagChip = generateTagChip(tag);
            tagChip.setOnCloseIconClickListener(e1 -> {
                removeTag(tag);
                tagsContainer.removeView(tagChip);
            });
            tagsContainer.addView(tagChip);
        }
    }

    private void removeTag(String tag) {
        entry.journalEntryTags.remove(entry.journalEntryTags.stream()
                .filter(x -> x.description.equalsIgnoreCase(tag))
                .findFirst()
                .orElse(null));
    }

    private void stopCurrentPlaybackIfPlaying() {
        if(mPlayer != null && mPlayer.isPlaying()){
            stopCurrentPlayback();
        }
    }

    private void stopCurrentPlayback() {
        if(currentPlayingImageButton != null){
            currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        currentPlayingImageButton = null;
    }

    private Chip generateTagChip(String text) {
        Chip tag = new Chip(getContext());
        tag.setText(text);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparams.leftMargin = Tools.dpToPx(getContext(), 3);
        lparams.rightMargin = Tools.dpToPx(getContext(), 3);
        tag.setLayoutParams(lparams);
        tag.setChipStrokeWidth(0);
        tag.setChipBackgroundColor(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, getContext().getTheme()));
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, getContext().getTheme()));
        tag.setCloseIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white, getContext().getTheme())));
        tag.setCheckedIconVisible(false);
        tag.setCloseIconVisible(true);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        return tag;
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
            audioFName = getActivity().getFilesDir().getAbsolutePath() + "/Recordings/recording_" + randomUUID() + ".aac";
            currentRecording = new AudioLocation(audioFName, Calendar.getInstance().getTimeInMillis());
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioEncodingBitRate(128000);
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setOutputFile(audioFName);
            try {
                mRecorder.prepare();
                mRecorder.start();
            } catch (IOException e) {
                System.err.println("ERROR preparing audio recording: " + e.getMessage());
            }
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

    private void storeRecording(LinearLayout recList, LinearLayout listLayout, RelativeLayout recordLayout, TextView noRecordingsFound) {
        stopRecordingAudio();
        entry.addAudioLocation(currentRecording);
        listLayout.setVisibility(View.VISIBLE);
        recordLayout.setVisibility(View.GONE);
        recList.addView(generateAudioEntry(currentRecording, noRecordingsFound));
        currentRecording = null;
    }

    private LinearLayout generateAudioEntry(AudioLocation recording, TextView noRecordingsFound) {
        RecordingObjectTools rot = RecordingObjectTools.getInstance(getContext());
        LinearLayout entryContainer = rot.generateContainerLayout();

        ImageButton playButton = rot.generatePlayButton();
        playButton.setOnClickListener(e -> handlePlayPauseMediaPlayer(recording, playButton));
        entryContainer.addView(playButton);

        LinearLayout labelsContainer = rot.generateLabelsContrainer();
        entryContainer.addView(labelsContainer);

        labelsContainer.addView(rot.generateHeading());
        labelsContainer.addView(rot.generateTimestamp(recording));
        entryContainer.addView(rot.generateDuration(recording, false));

        ImageButton deleteButton = rot.generateDeleteButton();
        deleteButton.setOnClickListener(e -> setupRecordingsDeleteDialog(recording, entryContainer, playButton, noRecordingsFound));
        entryContainer.addView(deleteButton);

        return entryContainer;
    }

    private void setupRecordingsDeleteDialog(AudioLocation recording, LinearLayout entryContainer, ImageButton playButton, TextView noRecordingsFound) {
        new MaterialAlertDialogBuilder(getContext(), R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle(getResources().getString(R.string.recording_delete_header))
                .setMessage(getResources().getString(R.string.recording_delete_message))
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    if (currentPlayingImageButton == playButton) { stopCurrentPlayback(); }
                    ((LinearLayout) entryContainer.getParent()).removeView(entryContainer);
                    entry.deleteAudioLocation(recording.audioPath);
                    if(entry.audioLocations.isEmpty()) {
                        noRecordingsFound.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show();
    }

    private void handlePlayPauseMediaPlayer(AudioLocation currentRecording, ImageButton playButton) {
        if(mPlayer != null && mPlayer.isPlaying() && currentPlayingImageButton == playButton) {
            mPlayer.pause();
            currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
        else if(mPlayer != null && !mPlayer.isPlaying() && currentPlayingImageButton == playButton) {
            mPlayer.start();
            currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_pause_24);
        }
        else if(mPlayer != null && mPlayer.isPlaying()) {
            stopCurrentPlayback();
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
            setupAudioPlayer(currentRecording.audioPath);
            currentPlayingImageButton = playButton;
        }
        else {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
            setupAudioPlayer(currentRecording.audioPath);
            currentPlayingImageButton = playButton;
        }
    }

    private void setupAudioPlayer(String audioFile) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFile);
            mPlayer.setOnCompletionListener(e -> stopCurrentPlayback());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            mPlayer = null;
        }
    }

    public void setJournalEntryId(DreamJournalEntry entry) {
        this.entry = entry;
    }

    public void setJournalEntryType(DreamJournalEntry.EntryType type) {
        this.entryType = type;
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

    @Override
    public void onStop() {
        compositeDisposable.dispose();
        super.onStop();
    }
}