package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.DatabaseWrapper;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.bitflaker.lucidsourcekit.main.createjournalentry.AddTextEntry;
import com.google.android.flexbox.FlexboxLayout;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ViewJournalEntry extends AppCompatActivity {

    private TextView title, timestamp, description;
    private ImageView mood, clarity, quality;
    private ProgressBar prgMood, prgClarity, prgQuality;
    private FlexboxLayout tagsContainer;
    private LinearLayout dreamTypesContainer, dreamContent;
    private String selectedDate, selectedTime, titleContent, descriptionContent, qualityContent, clarityContent, moodContent;
    private String[] dreamTypes, tags, recordedAudios, availableTags;
    private ImageButton currentPlayingImageButton, editEntry, deleteEntry;
    private MediaPlayer mPlayer;
    private ActivityResultLauncher<Intent> editEntryActivityResultLauncher;
    private JournalTypes journalType;
    private int position, entryId;
    private DatabaseWrapper dbWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        editEntryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        data.putExtra("position", position);
                        data.putExtra("action", "EDIT");
                        setResult(RESULT_OK, data);
                        finish();
                    }
                });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_journal_entry);

        title = findViewById(R.id.txt_entry_title);
        timestamp = findViewById(R.id.txt_entry_time);
        mood = findViewById(R.id.img_entry_mood);
        clarity = findViewById(R.id.img_entry_clarity);
        quality = findViewById(R.id.img_entry_quality);
        prgMood = findViewById(R.id.prg_entry_mood);
        prgClarity = findViewById(R.id.prg_entry_clarity);
        prgQuality = findViewById(R.id.prg_entry_quality);
        tagsContainer = findViewById(R.id.flx_entry_tags);
        dreamTypesContainer = findViewById(R.id.ll_dream_types);
        dreamContent = findViewById(R.id.ll_dream_content);
        editEntry = findViewById(R.id.btn_edit_entry);
        deleteEntry = findViewById(R.id.btn_delete_entry);
        mPlayer = new MediaPlayer();
        dbWrapper = new DatabaseWrapper(ViewJournalEntry.this);

        getData();
        setData();
        setupActions();
    }

    private void setupActions() {
        editEntry.setOnClickListener(e -> {
            Intent intent = new Intent(ViewJournalEntry.this, AddTextEntry.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("mode", "EDIT");
            intent.putExtra("type", journalType.ordinal());
            intent.putExtra("availableTags", availableTags);
            intent.putExtra("entryId", entryId);
            intent.putExtra("date", selectedDate);
            intent.putExtra("time", selectedTime);
            intent.putExtra("title", titleContent);
            intent.putExtra("description", descriptionContent);
            intent.putExtra("quality", qualityContent);
            intent.putExtra("clarity", clarityContent);
            intent.putExtra("mood", moodContent);
            intent.putExtra("dreamTypes", dreamTypes);
            intent.putExtra("tags", tags);
            intent.putExtra("recordings", recordedAudios);
            editEntryActivityResultLauncher.launch(intent);
        });
        deleteEntry.setOnClickListener(e -> new AlertDialog.Builder(ViewJournalEntry.this, Tools.getThemeDialog()).setTitle(getResources().getString(R.string.entry_delete_header)).setMessage(getResources().getString(R.string.entry_delete_message))
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    dbWrapper.deleteEntry(entryId);
                    Intent data = new Intent();
                    data.putExtra("action", "DELETE");
                    data.putExtra("position", position);
                    setResult(RESULT_OK, data);
                    finish();
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show());
    }

    private void setData() {
        timestamp.setText(MessageFormat.format("{0} {1} {2}", selectedDate, getResources().getString(R.string.journal_time_at), selectedTime));
        title.setText(titleContent);
        if(descriptionContent != null) {
            description = new TextView(ViewJournalEntry.this);
            description.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            description.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            description.setText(descriptionContent);
            description.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getTheme()));
            dreamContent.addView(description);
        }
        else {
            for (String audioRecording : recordedAudios) {
                addRecordingToList("Recording", audioRecording);
            }
        }
        for (String tag : tags) {
            TextView tagView = new TextView(ViewJournalEntry.this);
            tagView.setText(tag);
            tagView.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, getTheme()));
            int dpLarger = Tools.dpToPx(ViewJournalEntry.this, 8);
            int dpSmaller = Tools.dpToPx(ViewJournalEntry.this, 4);
            int dpSmall = Tools.dpToPx(ViewJournalEntry.this, 2);
            tagView.setPadding(dpLarger, dpSmaller, dpLarger, dpSmaller);
            tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tagView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_spinner, getTheme()));
            tagView.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getTheme()));
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(dpSmall, dpSmall, dpSmall, dpSmall);
            tagView.setLayoutParams(llParams);
            tagsContainer.addView(tagView);
        }

        for (String dreamType : dreamTypes) {
            switch (Objects.requireNonNull(DreamTypes.getEnum(dreamType))) {
                case Lucid:
                    dreamTypesContainer.addView(generateTextHighlight(R.drawable.ic_baseline_deblur_24, R.string.dream_lucid));
                    break;
                case Nightmare:
                    dreamTypesContainer.addView(generateText(R.drawable.ic_baseline_priority_high_24, R.string.dream_nightmare));
                    break;
                case FalseAwakening:
                    dreamTypesContainer.addView(generateText(R.drawable.ic_baseline_airline_seat_individual_suite_24, R.string.dream_false_awakening));
                    break;
                case SleepParalysis:
                    dreamTypesContainer.addView(generateText(R.drawable.ic_baseline_accessibility_new_24, R.string.dream_paralysis));
                    break;
            }
        }

        switch (Objects.requireNonNull(DreamMoods.getEnum(moodContent))){
            case Outstanding: setDreamMoodRating(R.drawable.ic_baseline_sentiment_very_satisfied_24, DreamMoods.Outstanding); break;
            case Great: setDreamMoodRating(R.drawable.ic_baseline_sentiment_satisfied_24, DreamMoods.Great); break;
            case Ok: setDreamMoodRating(R.drawable.ic_baseline_sentiment_neutral_24, DreamMoods.Ok); break;
            case Poor: setDreamMoodRating(R.drawable.ic_baseline_sentiment_dissatisfied_24, DreamMoods.Poor); break;
            case Terrible: setDreamMoodRating(R.drawable.ic_baseline_sentiment_very_dissatisfied_24, DreamMoods.Terrible); break;
        }
        switch (Objects.requireNonNull(SleepQuality.getEnum(qualityContent))){
            case Outstanding: setSleepQualityRating(R.drawable.ic_baseline_stars_24, SleepQuality.Outstanding); break;
            case Great: setSleepQualityRating(R.drawable.ic_baseline_star_24, SleepQuality.Great); break;
            case Poor: setSleepQualityRating(R.drawable.ic_baseline_star_half_24, SleepQuality.Poor); break;
            case Terrible: setSleepQualityRating(R.drawable.ic_baseline_star_border_24, SleepQuality.Terrible); break;
        }
        switch (Objects.requireNonNull(DreamClarity.getEnum(clarityContent))){
            case CrystalClear: setDreamClarityRating(R.drawable.ic_baseline_brightness_7_24, DreamClarity.CrystalClear); break;
            case Clear: setDreamClarityRating(R.drawable.ic_baseline_brightness_6_24, DreamClarity.Clear); break;
            case Cloudy: setDreamClarityRating(R.drawable.ic_baseline_brightness_5_24, DreamClarity.Cloudy); break;
            case VeryCloudy: setDreamClarityRating(R.drawable.ic_baseline_brightness_4_24, DreamClarity.VeryCloudy); break;
        }
    }

    private void setDreamMoodRating(int icon, DreamMoods moodEnum) {
        mood.setImageDrawable(ResourcesCompat.getDrawable(getResources(), icon, getTheme()));
        prgMood.setProgress(moodEnum.ordinal());
    }

    private void setDreamClarityRating(int icon, DreamClarity clarityEnum) {
        clarity.setImageDrawable(ResourcesCompat.getDrawable(getResources(), icon, getTheme()));
        prgClarity.setProgress(clarityEnum.ordinal());
    }

    private void setSleepQualityRating(int icon, SleepQuality qualityEnum) {
        quality.setImageDrawable(ResourcesCompat.getDrawable(getResources(), icon, getTheme()));
        prgQuality.setProgress(qualityEnum.ordinal());
    }

    private View generateText(int icon, int text) {
        TextView type = new TextView(ViewJournalEntry.this);
        type.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), icon, getTheme()), null, null, null);
        type.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        type.setText(text);
        type.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getTheme()));
        type.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        type.setCompoundDrawablePadding(Tools.dpToPx(ViewJournalEntry.this, 5));
        type.setGravity(Gravity.CENTER_VERTICAL);
        return type;
    }

    private View generateTextHighlight(int icon, int text) {
        TextView type = new TextView(ViewJournalEntry.this);
        type.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), icon, getTheme()), null, null, null);
        TextViewCompat.setCompoundDrawableTintList(type, Tools.getAttrColorStateList(R.attr.colorPrimary, getTheme()));
        type.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        type.setText(text);
        type.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, getTheme()));
        type.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        type.setCompoundDrawablePadding(Tools.dpToPx(ViewJournalEntry.this, 5));
        type.setGravity(Gravity.CENTER_VERTICAL);
        return type;
    }

    public void addRecordingToList(String title, String audioFile) {
        LinearLayout llContainer = new LinearLayout(ViewJournalEntry.this);
        LinearLayout.LayoutParams llparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llparams.setMargins(0, Tools.dpToPx(ViewJournalEntry.this, 5), 0, Tools.dpToPx(ViewJournalEntry.this, 5));
        llContainer.setLayoutParams(llparams);
        llContainer.setOrientation(LinearLayout.HORIZONTAL);
        llContainer.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_border, getTheme()));
        llContainer.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, getTheme()));
        int dp15 = Tools.dpToPx(ViewJournalEntry.this, 5);
        llContainer.setPadding(dp15, dp15, dp15, dp15);

        ImageButton playPause = new ImageButton(ViewJournalEntry.this);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparams.gravity = Gravity.CENTER_VERTICAL;
        lparams.leftMargin = Tools.dpToPx(ViewJournalEntry.this, 5);
        playPause.setLayoutParams(lparams);
        playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        playPause.setBackgroundTintList(Tools.getAttrColorStateList(android.R.color.transparent, getTheme()));
        playPause.setOnClickListener(e -> {
            if(mPlayer != null && mPlayer.isPlaying() && currentPlayingImageButton == playPause) {
                playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                currentPlayingImageButton = null;
            }
            else if(mPlayer != null && mPlayer.isPlaying()) {
                currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                playPause.setImageResource(R.drawable.ic_baseline_pause_24);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                try {
                    setupAudioPlayer(getFilesDir().getAbsolutePath() + audioFile, playPause);
                    currentPlayingImageButton = playPause;
                } catch (IOException ex) {
                    playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    currentPlayingImageButton = null;
                    ex.printStackTrace();
                }
            }
            else {
                playPause.setImageResource(R.drawable.ic_baseline_pause_24);
                try {
                    setupAudioPlayer(getFilesDir().getAbsolutePath() + audioFile, playPause);
                    currentPlayingImageButton = playPause;
                } catch (IOException ex) {
                    playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    currentPlayingImageButton = null;
                    ex.printStackTrace();
                }
            }
        });

        TextView titleView = new TextView(ViewJournalEntry.this);
        LinearLayout.LayoutParams lparamsTxt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparamsTxt.weight = 1;
        lparamsTxt.gravity = Gravity.CENTER_VERTICAL;
        titleView.setLayoutParams(lparamsTxt);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        TextView lengthView = new TextView(ViewJournalEntry.this);
        LinearLayout.LayoutParams lparamsTxtLength = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lparamsTxtLength.gravity = Gravity.CENTER_VERTICAL;
        lparamsTxtLength.rightMargin = Tools.dpToPx(ViewJournalEntry.this, 20);
        lengthView.setLayoutParams(lparamsTxtLength);
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(getFilesDir().getAbsolutePath() + audioFile);
            mp.prepare();
            lengthView.setText(String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(mp.getDuration()),
                    TimeUnit.MILLISECONDS.toSeconds(mp.getDuration()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mp.getDuration()))
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }

        llContainer.addView(playPause);
        llContainer.addView(titleView);
        llContainer.addView(lengthView);

        ((LinearLayout) findViewById(R.id.ll_dream_content)).addView(llContainer);
    }

    @Override
    protected void onStop() {
        if(mPlayer != null){
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        super.onStop();
    }

    private void setupAudioPlayer(String audioFile, ImageButton playPause) throws IOException {
        mPlayer = new MediaPlayer();
        mPlayer.setDataSource(audioFile);
        mPlayer.setOnCompletionListener(e -> {
            playPause.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        });
        mPlayer.prepare();
        mPlayer.start();
    }

    private void getData() {
        Intent data = getIntent();
        journalType = JournalTypes.values()[data.getIntExtra("type", -1)];
        position = data.getIntExtra("position", -1);
        entryId = data.getIntExtra("entryId", -1);
        selectedDate = data.getStringExtra("date");
        availableTags = data.getStringArrayExtra("availableTags");
        selectedTime = data.getStringExtra("time");
        titleContent = data.getStringExtra("title");
        descriptionContent = data.getStringExtra("description");
        qualityContent = data.getStringExtra("quality");
        clarityContent = data.getStringExtra("clarity");
        moodContent = data.getStringExtra("mood");
        dreamTypes = data.getStringArrayExtra("dreamTypes");
        tags = data.getStringArrayExtra("tags");
        recordedAudios = data.getStringArrayExtra("recordings");
    }
}