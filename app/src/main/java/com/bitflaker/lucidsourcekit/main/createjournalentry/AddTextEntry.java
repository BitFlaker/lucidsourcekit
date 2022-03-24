package com.bitflaker.lucidsourcekit.main.createjournalentry;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static java.util.UUID.randomUUID;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class AddTextEntry extends AppCompatActivity {
    private MaterialButton dateButton, timeButton;
    private ImageButton addTag;
    private DatePickerDialog dpd;
    private DateFormat dateFormat;
    private TimePickerDialog tpd;
    private LinearLayout dreamEditorBox;
    private MaterialButton addEntry;
    private ToggleButton nightmare, paralysis, falseAwakening, lucid;
    private Slider qualitySlider;
    private Slider claritySlider;
    private Slider moodSlider;

    private MediaRecorder mRecorder;
    private String audioFName;
    private List<String> recordedAudios;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private ImageButton tapOutsideRecording, pauseContinueRecording, stopRecording;
    private ImageView backgroundUnfocus;
    private TextView recordingText;
    private boolean isRecordingRunning;
    private AudioJournalEditorFrag audioFrag;
    private FormsJournalEditorFrag formsFrag;
    private TextJournalEditorFrag textFrag;

    private MainDatabase db;
    private Calendar timestampCalendar;
    private JournalTypes currentType;

    private boolean storedByUser = false;
    private FlexboxLayout tagContainer;

    private EditText entryTitle;
    private boolean isInEditingMode = false;
    private int entryId = -1;
    private AutoCompleteTextView tagsEnter;
    private FlexboxLayout tagsToAddContainer;

    private String[] availableTags;

    // TODO wrong date on default date selection => 3/2/22 instead of 3/15/22

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_text_entry);
        Tools.colorStatusBar(this, R.attr.colorPrimary, getTheme());    // a bit sketchy but translucent statusbar breaks autocompleteTextView focus zoom

        dateButton = findViewById(R.id.btn_date);
        timeButton = findViewById(R.id.btn_time);
        dreamEditorBox = findViewById(R.id.ll_dream_editor);
        tapOutsideRecording = findViewById(R.id.btn_recording_tapped_outside);
        pauseContinueRecording = findViewById(R.id.btn_pause_continue_recording);
        stopRecording = findViewById(R.id.btn_stop_recording);
        backgroundUnfocus = findViewById(R.id.img_background_unfocus);
        recordingText = findViewById(R.id.txt_recording);
        addTag = findViewById(R.id.btn_add_tag);
        addEntry = findViewById(R.id.btn_create_journal_entry);
        nightmare = findViewById(R.id.tgl_nightmare);
        paralysis = findViewById(R.id.tgl_paralysis);
        falseAwakening = findViewById(R.id.tgl_false_awakening);
        lucid = findViewById(R.id.tgl_lucid);
        qualitySlider = findViewById(R.id.sld_sleep_quality);
        claritySlider = findViewById(R.id.sld_clarity);
        moodSlider = findViewById(R.id.sld_mood);
        tagContainer = findViewById(R.id.flx_tags);
        entryTitle = findViewById(R.id.txt_title_dream);
        isRecordingRunning = false;
        recordedAudios = new ArrayList<>();
        tagsEnter = findViewById(R.id.txt_tags_enter);
        tagsToAddContainer = findViewById(R.id.flx_tags_to_add);
        Intent data = getIntent();
        availableTags = data.getStringArrayExtra("availableTags");
        timestampCalendar = new GregorianCalendar(TimeZone.getDefault());

        setupTimePicker();
        setupDatePicker();
        setupTagAddButton();
        setupTagsPopup();
        setupSliders();
        setupAddEntryButton();

        currentType = JournalTypes.values()[data.getIntExtra("type", -1)];
        data.getIntExtra("type", -1);
        switch(currentType){
            case Text:
                textFrag = new TextJournalEditorFrag();
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), textFrag, null).commit();
                break;
            case Audio:
                setupRecordingPopup();
                audioFrag = AudioJournalEditorFrag.newInstance();
                audioFrag.setOnAudioRecordingRequested(this::showRecording);
                audioFrag.setOnAudioRecordingRemoved(e -> {
                    File audio = new File(getFilesDir().getAbsolutePath() + e);
                    audio.delete();
                    recordedAudios.remove(e);
                });
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), audioFrag, null).commit();
                break;
            case Forms:
                formsFrag = FormsJournalEditorFrag.newInstance();
                getFragmentManager().beginTransaction().add(dreamEditorBox.getId(), formsFrag, null).commit();
                break;
        }

        if(data.hasExtra("mode") && data.getStringExtra("mode").equals("EDIT")) {
            isInEditingMode = true;

            entryId = data.getIntExtra("entryId", -1);

            long timestampMillis = data.getLongExtra("timestamp", 0);
            timestampCalendar.setTimeInMillis(timestampMillis);
            dateButton.setText(dateFormat.format(timestampCalendar.getTime()));
            DateFormat time = android.text.format.DateFormat.getTimeFormat(this);
            timeButton.setText(time.format(timestampCalendar.getTime()));

            entryTitle.setText(data.getStringExtra("title"));
            if(currentType == JournalTypes.Text) {
                textFrag.setTextOnReady(data.getStringExtra("description"));
            }

            qualitySlider.setValue(SleepQuality.getEnum(data.getStringExtra("quality")).ordinal());
            claritySlider.setValue(DreamClarity.getEnum(data.getStringExtra("clarity")).ordinal());
            moodSlider.setValue(DreamMoods.getEnum(data.getStringExtra("mood")).ordinal());

            String[] editDreamTypes = data.getStringArrayExtra("dreamTypes");
            for (String editDreamType : editDreamTypes) {
                switch (DreamTypes.getEnum(editDreamType)) {
                    case Lucid: lucid.setChecked(true); break;
                    case FalseAwakening: falseAwakening.setChecked(true); break;
                    case SleepParalysis: paralysis.setChecked(true); break;
                    case Nightmare: nightmare.setChecked(true); break;
                }
            }

            String[] editTags = data.getStringArrayExtra("tags");
            for (String editTag : editTags) {
                tagContainer.addView(generateTagView(editTag), tagContainer.getChildCount() - 1);
            }

            if(currentType == JournalTypes.Audio) {
                String[] editRecordings = data.getStringArrayExtra("recordings");
                for (String editRecording : editRecordings) {
                    recordedAudios.add(editRecording);
                    audioFrag.addRecordingToListOnReady(editRecording);
                }
            }

            addEntry.setText(getResources().getString(R.string.save_journal_entry_changes));
        }
    }

    private void setupAddEntryButton() {
        addEntry.setOnClickListener(e -> {
            db = MainDatabase.getInstance(this);
            String title = entryTitle.getText().toString();
            String description = null;
            switch (currentType) {
                case Text:
                    description = ((EditText) findViewById(R.id.txt_description_dream)).getText().toString();
                    break;
                case Forms:
                    description = formsFrag.getFormResult();
                    break;
            }

            if(isVitalDataFilledIn(title, description)) {
                if(((FlexboxLayout) findViewById(R.id.flx_tags)).getChildCount() == 0){
                    String finalDescription = description;
                    new AlertDialog.Builder(AddTextEntry.this, Tools.getThemeDialog()).setTitle(getResources().getString(R.string.no_tags_heading)).setMessage(getResources().getString(R.string.no_tags_message))
                            .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                                storeEntry(title, finalDescription);
                            })
                            .setNegativeButton(getResources().getString(R.string.no), null)
                            .show();
                }
                else {
                    storeEntry(title, description);
                }
            }
            else{
                Toast.makeText(AddTextEntry.this, getResources().getString(R.string.form_not_filled), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void storeEntry(String title, String description) {
        // TODO start loading animation
        String quality = SleepQuality.values()[((int) qualitySlider.getValue())].getId();
        String clarity = DreamClarity.values()[((int) claritySlider.getValue())].getId();
        String mood = DreamMoods.values()[((int) moodSlider.getValue())].getId();
        JournalEntry entry = new JournalEntry(timestampCalendar.getTimeInMillis(), title, description, quality, clarity, mood);

        if(entryId != -1) {
            entry.setEntryId(entryId);
            db.getJournalEntryHasTagDao().deleteAll(entryId);
            db.getJournalEntryIsTypeDao().deleteAll(entryId);
        }

        db.getJournalEntryDao().insert(entry).subscribe((insertedIds, throwable) -> {
            int currentEntryId = insertedIds.intValue();
            List<String> dreamTypes = new ArrayList<>();
            if(nightmare.isChecked()) { dreamTypes.add(DreamTypes.Nightmare.getId()); }
            if(paralysis.isChecked()) { dreamTypes.add(DreamTypes.SleepParalysis.getId()); }
            if(falseAwakening.isChecked()) { dreamTypes.add(DreamTypes.FalseAwakening.getId()); }
            if(lucid.isChecked()) { dreamTypes.add(DreamTypes.Lucid.getId()); }
            List<JournalEntryHasType> journalEntryHasTypes = new ArrayList<>();
            for (String type : dreamTypes) {
                journalEntryHasTypes.add(new JournalEntryHasType(currentEntryId, type));
            }
            db.getJournalEntryIsTypeDao().insertAll(journalEntryHasTypes).subscribe(() -> {
                List<String> tags = new ArrayList<>();
                int childCount = tagContainer.getChildCount();
                for (int i = 0; i < childCount; i++){
                    if(tagContainer.getChildAt(i) instanceof TextView){
                        TextView storedTag = (TextView) tagContainer.getChildAt(i);
                        String tag = storedTag.getText().toString();
                        tags.add(tag);
                    }
                }
                List<JournalEntryTag> journalEntryTagsToInsert = new ArrayList<>();
                for (String tag : tags) {
                    journalEntryTagsToInsert.add(new JournalEntryTag(tag));
                }
                db.getJournalEntryTagDao().insertAll(journalEntryTagsToInsert).subscribe((insertedTagIds, throwable1) -> {
                    db.getJournalEntryTagDao().getIdsByDescription(tags).subscribe((journalEntryTags, throwable2) -> {
                        List<JournalEntryHasTag> journalEntryHasTags = new ArrayList<>();
                        for (int i = 0; i < journalEntryTags.size(); i++) {
                            journalEntryHasTags.add(new JournalEntryHasTag(currentEntryId, journalEntryTags.get(i).tagId));
                        }
                        db.getJournalEntryHasTagDao().insertAll(journalEntryHasTags).subscribe((integers, throwable3) -> {
                            List<AudioLocation> audioLocations = new ArrayList<>();
                            for (String location : recordedAudios){
                                audioLocations.add(new AudioLocation(currentEntryId, location));
                            }
                            db.getAudioLocationDao().insertAll(audioLocations).subscribe((integers1, throwable4) -> {
                                // TODO hide loading animation
                                Intent data = new Intent();
                                data.putExtra("entryId", currentEntryId);
                                setResult(RESULT_OK, data);
                                storedByUser = true;
                                finish();
                            });
                        });
                    });
                });
            });
        });
    }

    private boolean isVitalDataFilledIn(String title, String description) {
        boolean titleOk = title.length() > 0;
        boolean descriptionOk = currentType.compareTo(JournalTypes.Audio) == 0 || description.length() > 0;
        boolean audioOk = currentType.compareTo(JournalTypes.Audio) != 0 || recordedAudios.size() > 0;
        return titleOk && descriptionOk && audioOk;
    }

    @Override
    protected void onStop() {
        if(!storedByUser && !isInEditingMode){
            for (String recordedAudio : recordedAudios) {
                File audio = new File(getFilesDir().getAbsolutePath() + recordedAudio);
                audio.delete();
            }
        }
        super.onStop();
    }

    // TODO change to AutocompleteEditText and set Data
    private void setupTagsPopup() {
        tagsEnter.setAdapter(new ArrayAdapter<>(AddTextEntry.this, android.R.layout.simple_list_item_1, availableTags));
        tagsEnter.setOnEditorActionListener((textView, i, keyEvent) -> {
            String enteredTag = tagsEnter.getText().toString();
            if(i == IME_ACTION_DONE && !getCurrentTagsToAdd().contains(enteredTag) && enteredTag.length() > 0){
                addTagFilterEntry(enteredTag);
            }
            return true;
        });
        tagsEnter.setOnItemClickListener((adapterView, view, i, l) -> {
            String enteredTag = tagsEnter.getText().toString();
            if(!getCurrentTagsToAdd().contains(enteredTag)){
                addTagFilterEntry(enteredTag);
            }
        });
    }

    private void addTagFilterEntry(String enteredTag) {
        Chip tag = generateTagChip(enteredTag);
        tag.setOnClickListener(e -> tagsToAddContainer.removeView(tag));
        tagsToAddContainer.addView(tag);
        tagsEnter.setText("");
    }

    private List<String> getCurrentTagsToAdd() {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagsToAddContainer.getChildCount(); i++){
            tags.add(((Chip) tagsToAddContainer.getChildAt(i)).getText().toString());
        }
        return tags;
    }

    private Chip generateTagChip(String text) {
        Chip tag = new Chip(new ContextThemeWrapper(AddTextEntry.this, R.style.Widget_MaterialComponents_Chip_Entry));
        tag.setText(text);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparams.leftMargin = Tools.dpToPx(AddTextEntry.this, 3);
        lparams.rightMargin = Tools.dpToPx(AddTextEntry.this, 3);
        tag.setLayoutParams(lparams);
        tag.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated2x, getTheme()));
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, getTheme()));
        tag.setCloseIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white, getTheme())));
        tag.setCheckedIconVisible(false);
        tag.setCloseIconVisible(true);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        return tag;
    }

    private void setupTagAddButton() {
        addTag.setOnClickListener(e -> showTagEditor());
    }

    private void setupSliders() {
        Slider mood = findViewById(R.id.sld_mood);
        Slider clarity = findViewById(R.id.sld_clarity);
        Slider quality = findViewById(R.id.sld_sleep_quality);

        ImageView imgVeryDissatisfied = findViewById(R.id.img_very_dissatisfied);
        ImageView imgDissatisfied = findViewById(R.id.img_dissatisfied);
        ImageView imgNeutral = findViewById(R.id.img_neutral_satisfied);
        ImageView imgSatisfied = findViewById(R.id.img_satisfied);
        ImageView imgVerySatisfied = findViewById(R.id.img_very_satisfied);

        ImageView imgVeryUnclear = findViewById(R.id.img_very_unclear);
        ImageView imgUnclear = findViewById(R.id.img_unclear);
        ImageView imgClear = findViewById(R.id.img_clear);
        ImageView imgVeryClear = findViewById(R.id.img_very_clear);

        ImageView imgVeryBadQuality = findViewById(R.id.img_very_bad_quality);
        ImageView imgBadQuality = findViewById(R.id.img_bad_quality);
        ImageView imgGoodQuality = findViewById(R.id.img_good_quality);
        ImageView imgVeryGoodQuality = findViewById(R.id.img_very_good_quality);

        mood.addOnChangeListener((slider, value, fromUser) -> {
            disableAllHighlighted(new ImageView[] { imgVeryDissatisfied, imgDissatisfied, imgNeutral, imgSatisfied, imgVerySatisfied });
            switch ((int)value) {
                case 0: setIconActive(imgVeryDissatisfied); break;
                case 1: setIconActive(imgDissatisfied); break;
                case 2: setIconActive(imgNeutral); break;
                case 3: setIconActive(imgSatisfied); break;
                case 4: setIconActive(imgVerySatisfied); break;
            }
        });

        clarity.addOnChangeListener((slider, value, fromUser) -> {
            disableAllHighlighted(new ImageView[] { imgVeryUnclear, imgUnclear, imgClear, imgVeryClear });
            switch ((int)value) {
                case 0: setIconActive(imgVeryUnclear); break;
                case 1: setIconActive(imgUnclear); break;
                case 2: setIconActive(imgClear); break;
                case 3: setIconActive(imgVeryClear); break;
            }
        });

        quality.addOnChangeListener((slider, value, fromUser) -> {
            disableAllHighlighted(new ImageView[] { imgVeryBadQuality, imgBadQuality, imgGoodQuality, imgVeryGoodQuality });
            switch ((int)value) {
                case 0: setIconActive(imgVeryBadQuality); break;
                case 1: setIconActive(imgBadQuality); break;
                case 2: setIconActive(imgGoodQuality); break;
                case 3: setIconActive(imgVeryGoodQuality); break;
            }
        });
    }

    private void setIconActive(ImageView img) {
        img.setImageTintList(Tools.getAttrColorStateList(R.attr.primaryTextColor, AddTextEntry.this.getTheme()));
        android.view.ViewGroup.LayoutParams lparam = img.getLayoutParams();
        lparam.height = Tools.dpToPx(AddTextEntry.this, 30);
        img.setLayoutParams(lparam);
    }

    private void disableAllHighlighted(ImageView[] imageViews) {
        for (ImageView img : imageViews) {
            img.setImageTintList(Tools.getAttrColorStateList(R.attr.secondaryTextColor, AddTextEntry.this.getTheme()));
            android.view.ViewGroup.LayoutParams lparam = img.getLayoutParams();
            lparam.height = Tools.dpToPx(AddTextEntry.this, 20);
            img.setLayoutParams(lparam);
        }
    }

    private void setupRecordingPopup() {
        stopRecording.setOnClickListener(e -> {
            storeRecording();
        });
        pauseContinueRecording.setOnClickListener(e -> {
            if(isRecordingRunning) {
                isRecordingRunning = false;
                pauseRecordingAudio();
                pauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_play_arrow_24, getTheme()));
                recordingText.setText(getResources().getString(R.string.recording_paused));
            }
            else {
                isRecordingRunning = true;
                continueRecordingAudio();
                pauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_pause_24, getTheme()));
                recordingText.setText(getResources().getString(R.string.recording));
            }
        });
    }

    private void pauseRecordingAudio() {
        mRecorder.pause();
    }

    private void continueRecordingAudio() {
        mRecorder.resume();
    }

    private void startRecordingAudio() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            audioFName = getFilesDir().getAbsolutePath();
            String filename = "/Recordings/recording_" + randomUUID() + ".aac";
            audioFName += filename;
            recordedAudios.add(filename);
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
            hideRecording();
            ActivityCompat.requestPermissions(AddTextEntry.this, new String[]{ RECORD_AUDIO }, REQUEST_AUDIO_PERMISSION_CODE);
        }
    }

    private void stopRecordingAudio() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        audioFName = null;
    }

    private void storeRecording() {
        stopRecordingAudio();
        hideRecording();
        // TODO replace text with icon?
        audioFrag.addRecordingToList(recordedAudios.get(recordedAudios.size() - 1));
    }

    private void setupTimePicker() {
        Date date = Calendar.getInstance().getTime();
        tpd = new TimePickerDialog(AddTextEntry.this, (timePicker, hourOfDay, minute) -> {
            timestampCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            timestampCalendar.set(Calendar.MINUTE, minute);
            timeButton.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, date.getHours(), date.getMinutes(), true);
        //timestampCalendar.set(Calendar.HOUR_OF_DAY, date.getHours());
        //timestampCalendar.set(Calendar.MINUTE, date.getMinutes());
        timestampCalendar.setTime(Calendar.getInstance().getTime());
        timeButton.setText(String.format("%02d:%02d", date.getHours(), date.getMinutes()));
        timeButton.setOnClickListener(e -> tpd.show());
    }

    private void setupDatePicker() {
        dpd = new DatePickerDialog(AddTextEntry.this);
        dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        Date currDate = java.util.Calendar.getInstance().getTime();
        dateButton.setText(dateFormat.format(currDate));
        //timestampCalendar.set(currDate.getYear(), currDate.getMonth(), currDate.getDay());
        dateButton.setOnClickListener(e -> dpd.show());
        dpd.setOnDateSetListener((datePicker, year, monthOfYear, dayOfMonth) -> {
            timestampCalendar.set(year, monthOfYear, dayOfMonth);
            dateButton.setText(dateFormat.format(new Date(year, monthOfYear, dayOfMonth)));
        });
    }

    private void showRecording() {
        tapOutsideRecording.setOnClickListener(e -> storeRecording());
        findViewById(R.id.rl_recording).setVisibility(View.VISIBLE);
        tapOutsideRecording.setVisibility(View.VISIBLE);
        backgroundUnfocus.setVisibility(View.VISIBLE);
        isRecordingRunning = true;
        startRecordingAudio();
        pauseContinueRecording.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_pause_24, getTheme()));
        recordingText.setText(getResources().getString(R.string.recording));
    }

    private void hideRecording() {
        findViewById(R.id.rl_recording).setVisibility(View.GONE);
        tapOutsideRecording.setVisibility(View.GONE);
        backgroundUnfocus.setVisibility(View.GONE);
        isRecordingRunning = false;
    }

    private void showTagEditor() {
        tagsToAddContainer.removeAllViews();
        int childCount = tagContainer.getChildCount();
        for (int i = 0; i < childCount; i++){
            if(tagContainer.getChildAt(i) instanceof TextView){
                TextView storedTag = (TextView) tagContainer.getChildAt(i);
                Chip tag = generateTagChip(storedTag.getText().toString());
                tag.setOnClickListener(e -> tagsToAddContainer.removeView(tag));
                tagsToAddContainer.addView(tag);
            }
        }
        tapOutsideRecording.setOnClickListener(e -> hideTagEditor());
        findViewById(R.id.rl_tagging).setVisibility(View.VISIBLE);
        tapOutsideRecording.setVisibility(View.VISIBLE);
        backgroundUnfocus.setVisibility(View.VISIBLE);
    }

    private void hideTagEditor() {
        tagContainer.removeViews(0, tagContainer.getChildCount() - 1);
        int newChildCount = tagsToAddContainer.getChildCount();
        for (int i = 0; i < newChildCount; i++){
            if(tagsToAddContainer.getChildAt(i) instanceof Chip){
                Chip chip = (Chip)tagsToAddContainer.getChildAt(i);
                tagContainer.addView(generateTagView(chip.getText().toString()), tagContainer.getChildCount() - 1);
            }
        }

        EditText tagEnter = ((EditText) findViewById(R.id.txt_tags_enter));
        String enteredTag = tagEnter.getText().toString();
        if(tagEnter.getText().length() > 0 && !getCurrentTagsToAdd().contains(enteredTag)){
            tagContainer.addView(generateTagView(enteredTag), tagContainer.getChildCount() - 1);
        }
        tagEnter.setText("");

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(AddTextEntry.INPUT_METHOD_SERVICE);
        if(getCurrentFocus() != null){
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        findViewById(R.id.rl_tagging).setVisibility(View.GONE);
        tapOutsideRecording.setVisibility(View.GONE);
        backgroundUnfocus.setVisibility(View.GONE);
    }

    private View generateTagView(String text) {
        TextView tag = new TextView(this);
        tag.setText(text);
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, getTheme()));
        int horizontal = Tools.dpToPx(this, 12);
        int vertical = Tools.dpToPx(this, 8);
        int smallMargin = Tools.dpToPx(this, 3);
        tag.setPadding(horizontal,vertical,horizontal,vertical);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_spinner, getTheme()));
        tag.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getTheme()));
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llParams.setMargins(smallMargin, smallMargin, smallMargin, smallMargin);
        tag.setLayoutParams(llParams);
        return tag;
    }
}

