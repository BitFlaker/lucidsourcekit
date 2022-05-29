package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.general.RecordingObjectTools;
import com.bitflaker.lucidsourcekit.general.SortOrders;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemory;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemoryManager;
import com.bitflaker.lucidsourcekit.main.dreamjournal.RecordingData;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class RecyclerViewAdapterDreamJournal extends RecyclerView.Adapter<RecyclerViewAdapterDreamJournal.MainViewHolder> {
    private Context context;
    private DreamJournal journalList;
    private DreamJournalEntriesList entries;
    private DreamJournalEntriesList filteredEntries;
    private int currentSort;
    private AppliedFilter currentFilter;
    private MainDatabase db;
    private RecyclerView mRecyclerView;
    private DateFormat fullDayInWeekNameFormatter;
    private MediaPlayer mPlayer;
    private ImageButton currentPlayingImageButton;

    public RecyclerViewAdapterDreamJournal(DreamJournal journalList, Context context, DreamJournalEntriesList entries) {
        this.journalList = journalList;
        this.context = context;
        this.entries = entries;
        db = MainDatabase.getInstance(context);
        filteredEntries = null;
        currentFilter = null;
        fullDayInWeekNameFormatter = new SimpleDateFormat("EEEE");
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.journal_entry, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        DreamJournalEntriesList current;
        if(filteredEntries != null){ current = filteredEntries; }
        else { current = entries; }

        if(position == 0){
            LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) holder.firstDateIndicatorName.getLayoutParams());
            lParams.topMargin = 0;
            holder.firstDateIndicatorName.setLayoutParams(lParams);
        }
        else {
            LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) holder.firstDateIndicatorName.getLayoutParams());
            lParams.topMargin = Tools.dpToPx(context, 14);
            holder.firstDateIndicatorName.setLayoutParams(lParams);
        }
        Calendar cldrCurrent = current.getTimestamps()[position];
        Calendar cldrPast = current.getTimestamps()[Math.max(position - 1, 0)];
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        holder.dateTime.setText(MessageFormat.format("{0} • {1}", dateFormat.format(cldrCurrent.getTime()), timeFormat.format(cldrCurrent.getTime())));
        cldrCurrent.set(Calendar.HOUR_OF_DAY, 0);
        cldrCurrent.set(Calendar.MINUTE, 0);
        cldrCurrent.set(Calendar.SECOND, 0);
        cldrCurrent.set(Calendar.MILLISECOND, 0);
        cldrPast.set(Calendar.HOUR_OF_DAY, 0);
        cldrPast.set(Calendar.MINUTE, 0);
        cldrPast.set(Calendar.SECOND, 0);
        cldrPast.set(Calendar.MILLISECOND, 0);
        if(cldrCurrent.getTime().equals(cldrPast.getTime()) && position - 1 >= 0) {
            holder.firstDateIndicatorName.setVisibility(View.GONE);
            holder.firstDateIndicatorDate.setVisibility(View.GONE);
        }
        else {
            holder.firstDateIndicatorName.setText(fullDayInWeekNameFormatter.format(cldrCurrent.getTime()));
            holder.firstDateIndicatorDate.setText(dateFormat.format(cldrCurrent.getTime()));
            holder.firstDateIndicatorName.setVisibility(View.VISIBLE);
            holder.firstDateIndicatorDate.setVisibility(View.VISIBLE);
        }

        holder.title.setText(current.getTitles()[position]);
        if(current.getDescriptions()[position] == null){
            holder.description.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_mic_24, 0, 0, 0);
            int audioCount = current.getAudioLocations().get(position).size();
            holder.description.setText(audioCount == 1 ? "1 " + context.getResources().getString(R.string.recording_single) : audioCount + " " + context.getResources().getString(R.string.recording_multiple));
            ConstraintLayout.LayoutParams lparamsDesc = (ConstraintLayout.LayoutParams) holder.description.getLayoutParams();
            int slightMargin = Tools.dpToPx(context, 10);
            int verySlightMargin = Tools.dpToPx(context, 5);
            lparamsDesc.setMargins(-verySlightMargin, slightMargin, 0, slightMargin);
            holder.description.setLayoutParams(lparamsDesc);
        }
        else {
            holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            holder.description.setText(current.getDescriptions()[position]);
            ConstraintLayout.LayoutParams lparamsDesc = (ConstraintLayout.LayoutParams) holder.description.getLayoutParams();
            lparamsDesc.setMargins(0, Tools.dpToPx(context, 10), 0, 0);
            holder.description.setLayoutParams(lparamsDesc);
            holder.description.setMaxHeight(Tools.spToPx(context, holder.description.getTextSize())-5);
        }

        int iconCount = holder.titleIcons.getChildCount();
        int skip = 0;
        for (int i = 0; i < iconCount; i++){
            if(!(holder.titleIcons.getChildAt(skip) instanceof ImageView)){
                skip++;
                continue;
            }
            holder.titleIcons.removeViewAt(skip);
        }

        if(current.getTags().get(position).size() == 0) {
            holder.tagsHolder.setVisibility(View.GONE);
        }
        else {
            holder.tagsHolder.setVisibility(View.VISIBLE);
        }
        holder.tags.removeAllViews();
        for (int i = 0; i < current.getTags().get(position).size(); i++){
            TextView tag = generateTagView(current.getTags().get(position).get(i).description, R.attr.slightElevated2x);
            holder.tags.addView(tag);
        }

        for (int i = 0; i < current.getTypes().get(position).size(); i++) {
            switch (Objects.requireNonNull(DreamTypes.getEnum(current.getTypes().get(position).get(i).typeId))) {
                case Lucid:
                    holder.titleIcons.addView(generateIconHighlight(R.drawable.ic_baseline_deblur_24));
                    break;
                case Nightmare:
                    holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_priority_high_24));
                    break;
                case FalseAwakening:
                    holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_airline_seat_individual_suite_24));
                    break;
                case SleepParalysis:
                    holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_accessibility_new_24));
                    break;
                case Recurring:
                    holder.titleIcons.addView(generateIcon(R.drawable.ic_round_loop_24));
                    break;
            }
        }

        switch (Objects.requireNonNull(DreamMoods.getEnum(current.getDreamMoods()[position]))){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_satisfied_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_satisfied_24)); break;
            case Ok: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_neutral_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_dissatisfied_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_dissatisfied_24)); break;
        }
        switch (Objects.requireNonNull(SleepQuality.getEnum(current.getSleepQualities()[position]))){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_stars_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_half_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_border_24)); break;
        }
        switch (Objects.requireNonNull(DreamClarity.getEnum(current.getDreamClarities()[position]))){
            case CrystalClear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_7_24)); break;
            case Clear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_6_24)); break;
            case Cloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_5_24)); break;
            case VeryCloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_4_24)); break;
        }

        holder.entryCard.setOnClickListener(e -> {
            // TODO start loading animation

            final BottomSheetDialog bsd = new BottomSheetDialog(context, R.style.BottomSheetDialogStyle);
            bsd.setContentView(R.layout.journal_entry_viewer_sheet);

            boolean[] isOfType = setIsOfTypeValues(position, current);
            List<String> tags = setTagsValues(position, current);
            List<RecordingData> recs = setRecordingDataValues(position, current);

            JournalInMemoryManager jimm = JournalInMemoryManager.getInstance();
            String id = jimm.newEntry();
            JournalInMemory jim = jimm.getEntry(id);
            jim.setTitle(current.getTitles()[position]);
            jim.setDescription(current.getDescriptions()[position]);
            jim.setEntryId(current.getEntryIds()[position]);
            jim.setTime(current.getTimestamps()[position]);
            jim.setTags(tags);
            jim.setAudioRecordings(recs);
            jim.setPosition(position);
            jim.setEntryType(JournalInMemory.EntryType.PLAIN_TEXT);
            jim.setDreamClarity(current.getDreamClarities()[position]);
            jim.setSleepQuality(current.getSleepQualities()[position]);
            jim.setDreamMood(current.getDreamMoods()[position]);
            jim.setDreamMood(current.getDreamMoods()[position]);
            jim.setNightmare(isOfType[0]);
            jim.setParalysis(isOfType[1]);
            jim.setFalseAwakening(isOfType[2]);
            jim.setLucid(isOfType[3]);
            jim.setRecurring(isOfType[4]);

            TextView timestamp = bsd.findViewById(R.id.txt_entry_timestamp);
            ImageButton deleteEntry = bsd.findViewById(R.id.btn_delete_entry);
            ImageButton editEntry = bsd.findViewById(R.id.btn_edit_entry);
            TextView title = bsd.findViewById(R.id.txt_entry_title);
            TextView content = bsd.findViewById(R.id.txt_entry_dream_content);
            FlexboxLayout tagsLayout = bsd.findViewById(R.id.fbl_tags);
            LinearLayout recordingsLayout = bsd.findViewById(R.id.ll_recordings_container);
            ImageButton iconRecurring = bsd.findViewById(R.id.btn_icon_recurring);
            ImageButton iconLucid = bsd.findViewById(R.id.btn_icon_lucid);
            ImageButton iconSleepParalysis = bsd.findViewById(R.id.btn_icon_sleep_paralysis);
            ImageButton iconFalseAwakening = bsd.findViewById(R.id.btn_icon_false_awakening);
            ImageButton iconNightmare = bsd.findViewById(R.id.btn_icon_nightmare);
            RangeProgress dreamMood = bsd.findViewById(R.id.rp_dream_mood);
            RangeProgress dreamClarity = bsd.findViewById(R.id.rp_dream_clarity);
            RangeProgress sleepQuality = bsd.findViewById(R.id.rp_sleep_quality);
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
            DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

            timestamp.setText(df.format(jim.getTime().getTime()) + " • " + tf.format(jim.getTime().getTime()));
            title.setText(jim.getTitle());
            content.setText(jim.getDescription());
            iconRecurring.setVisibility(jim.isRecurring() ? View.VISIBLE : View.GONE);
            iconLucid.setVisibility(jim.isLucid() ? View.VISIBLE : View.GONE);
            iconSleepParalysis.setVisibility(jim.isParalysis() ? View.VISIBLE : View.GONE);
            iconFalseAwakening.setVisibility(jim.isFalseAwakening() ? View.VISIBLE : View.GONE);
            iconNightmare.setVisibility(jim.isNightmare() ? View.VISIBLE : View.GONE);

            if(jim.getAudioRecordings().size() == 0) { recordingsLayout.setVisibility(View.GONE); }
            for (RecordingData recData : jim.getAudioRecordings()) {
                recordingsLayout.addView(generateRecordingsPlayer(recData));
            }
            if(jim.getTags().size() == 0) { tagsLayout.setVisibility(View.GONE); }
            for (String tag : jim.getTags()) {
                tagsLayout.addView(generateTagView(tag, R.attr.slightElevated));
            }

            Drawable[] dreamMoodIcons = Tools.getIconsDreamMood(context);
            Drawable[] dreamClarityIcons = Tools.getIconsDreamClarity(context);
            Drawable[] sleepQualityIcons = Tools.getIconsSleepQuality(context);

            int dmIndex = getIndexOfDreamMood(jim.getDreamMood());
            int dcIndex = getIndexOfDreamClarity(jim.getDreamClarity());
            int sqIndex = getIndexOfSleepQuality(jim.getSleepQuality());

            dreamMood.setBackgroundAttrColor(R.attr.slightElevated);
            dreamClarity.setBackgroundAttrColor(R.attr.slightElevated);
            sleepQuality.setBackgroundAttrColor(R.attr.slightElevated);

            dreamMood.setData(4, dmIndex, "DREAM MOOD", dreamMoodIcons[dmIndex], null);
            dreamClarity.setData(3, dcIndex, "DREAM CLARITY", dreamClarityIcons[dcIndex], null);
            sleepQuality.setData(3, sqIndex, "SLEEP QUALITY", sleepQualityIcons[sqIndex], null);

            deleteEntry.setOnClickListener(e1 ->
                    new AlertDialog.Builder(context, Tools.getThemeDialog()).setTitle(context.getResources().getString(R.string.entry_delete_header)).setMessage(context.getResources().getString(R.string.entry_delete_message))
                        .setPositiveButton(context.getResources().getString(R.string.yes), (dialog, which) -> {
                            db.getJournalEntryDao().getEntryById(jim.getEntryId()).subscribe(journalEntry -> {
                                db.getJournalEntryDao().delete(journalEntry).subscribe(bsd::dismiss);
                            });
                        })
                        .setNegativeButton(context.getResources().getString(R.string.no), null)
                        .show());

            editEntry.setOnClickListener(e1 -> {
                jim.setEditMode(JournalInMemory.EditMode.EDIT);
                Intent intent = new Intent(context, DreamJournalEntryEditor.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("journal_in_memory_id", id);
                journalList.viewEntryActivityResultLauncher.launch(intent);
                bsd.dismiss();
            });

            bsd.show();
        });
    }

    private View generateRecordingsPlayer(RecordingData recData) {
        RecordingObjectTools rot = RecordingObjectTools.getInstance(context);
        LinearLayout entryContainer = rot.generateContainerLayout();

        ImageButton playButton = rot.generatePlayButton();
        playButton.setOnClickListener(e -> handlePlayPauseMediaPlayer(recData, playButton));
        entryContainer.addView(playButton);

        LinearLayout labelsContainer = rot.generateLabelsContrainer();
        entryContainer.addView(labelsContainer);

        labelsContainer.addView(rot.generateHeading());
        labelsContainer.addView(rot.generateTimestamp(recData));
        entryContainer.addView(rot.generateDuration(recData, true));

        return entryContainer;
    }

    private void handlePlayPauseMediaPlayer(RecordingData currentRecording, ImageButton playButton) {
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
            setupAudioPlayer(currentRecording.getFilepath());
            currentPlayingImageButton = playButton;
        }
        else {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
            setupAudioPlayer(currentRecording.getFilepath());
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

    private void stopCurrentPlayback() {
        if(currentPlayingImageButton != null){
            currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        currentPlayingImageButton = null;
    }

    @NonNull
    private TextView generateTagView(String text, int color) {
        TextView tag = new TextView(context);
        tag.setSingleLine(true);
        tag.setText(text);
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
        int dpLarger = Tools.dpToPx(context, 8);
        int dpSmaller = Tools.dpToPx(context, 4);
        int dpSmall = Tools.dpToPx(context, 2);
        tag.setPadding(dpLarger,dpSmaller,dpLarger,dpSmaller);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tag.setBackground(context.getResources().getDrawable(R.drawable.small_rounded_rectangle));
        tag.setBackgroundTintList(Tools.getAttrColorStateList(color, context.getTheme()));
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llParams.setMargins(0, dpSmall, dpSmall*2, dpSmall);
        tag.setLayoutParams(llParams);
        return tag;
    }

    private int getIndexOfDreamMood(String value) {
        DreamMood[] data = DreamMood.populateData();
        for (int i = 0; i < data.length; i++) {
            if(data[i].moodId.equals(value)){
                return i;
            }
        }
        return -1;
    }

    private int getIndexOfDreamClarity(String value) {
        com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity[] data = com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity.populateData();
        for (int i = 0; i < data.length; i++) {
            if(data[i].clarityId.equals(value)){
                return i;
            }
        }
        return -1;
    }

    private int getIndexOfSleepQuality(String value) {
        com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality[] data = com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality.populateData();
        for (int i = 0; i < data.length; i++) {
            if(data[i].qualityId.equals(value)){
                return i;
            }
        }
        return -1;
    }

    private List<RecordingData> setRecordingDataValues(int position, DreamJournalEntriesList current) {
        List<RecordingData> recs = new ArrayList<>();
        List<List<AudioLocation>> audioLocationsList = current.getAudioLocations();
        for (int i = 0; i < audioLocationsList.get(position).size(); i++) {
            RecordingData recData = new RecordingData(audioLocationsList.get(position).get(i).audioPath);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(audioLocationsList.get(position).get(i).recordingTimestamp);
            recData.setRecordingTime(cal);
            MediaPlayer dataReader = new MediaPlayer();
            try {
                dataReader.setDataSource(recData.getFilepath());
                dataReader.prepare();
                recData.setRecordingLength(dataReader.getDuration());
            } catch (IOException e) {
                e.printStackTrace();
                recData.setRecordingLength(0);
            }
            recs.add(recData);
        }
        return recs;
    }

    private List<String> setTagsValues(int position, DreamJournalEntriesList current) {
        List<String> tags = new ArrayList<>();
        List<List<AssignedTags>> tagsList = current.getTags();
        for (int i = 0; i < tagsList.get(position).size(); i++) {
            tags.add(tagsList.get(position).get(i).description);
        }
        return tags;
    }

    private boolean[] setIsOfTypeValues(int position, DreamJournalEntriesList current) {
        DreamType[] dts = DreamType.populateData();
        boolean[] isOfType = new boolean[dts.length];
        List<List<JournalEntryHasType>> typesList = current.getTypes();
        for (int i = 0; i < typesList.get(position).size(); i++) {
            isOfType[getIndexOfType(dts, typesList.get(position).get(i).typeId)] = true;
        }
        return isOfType;
    }

    private int getIndexOfType(DreamType[] dts, String typeId) {
        int index = -1;
        for (DreamType dt : dts) {
            index++;
            if (dt.typeId.equals(typeId)) {
                return index;
            }
        }
        return index;
    }

    private View generateIcon(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColorStateList(R.attr.secondaryTextColor, context.getTheme());
        icon.setImageTintList(stateList);
        int size = Tools.dpToPx(context, 20);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private View generateIconHighlight(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColorStateList(R.attr.secondaryTextColor, context.getTheme());
        icon.setImageTintList(stateList);
        int size = Tools.dpToPx(context, 20);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    public DreamJournalEntriesList getEntries() {
        if(filteredEntries == null){
            return entries;
        }
        return filteredEntries;
    }

    public void setEntries(DreamJournalEntriesList entries) {
        if(filteredEntries == null) {
            Pair<Operation, Integer> changes = getChanges(entries);
            this.entries = entries;
            int index;
            switch (changes.first) {
                case ADDED:
                    index = changes.second;
                    mRecyclerView.scrollToPosition(index);
                    int entryId = entries.get(changes.second).getEntry().entryId;
                    db.getJournalEntryHasTagDao().getAllFromEntryId(entryId).subscribe((assignedTags, throwable1) -> {
                        db.getAudioLocationDao().getAllFromEntryId(entryId).subscribe((audioLocations, throwable3) -> {
                            entries.get(index).setTags(assignedTags);
                            entries.get(index).setAudioLocations(audioLocations);
                            notifyItemInserted(index);
                            notifyItemRangeChanged(index, entries.size());
                        });
                    });
                    break;
                case DELETED:
                    index = changes.second;
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, entries.size());
                    break;
                case CHANGED:
                    // TODO: when timestamp changed and therefore entry moved in list => old one still there => duplicate entries
                    // TODO: get index of changed item
//                    index = changes.second;
//                    db.getJournalEntryHasTagDao().getAllFromEntryId(entryId).subscribe((assignedTags, throwable1) -> {
//                        db.getAudioLocationDao().getAllFromEntryId(entryId).subscribe((audioLocations, throwable3) -> {
//                            entries.get(index).setTags(assignedTags);
//                            entries.get(index).setAudioLocations(audioLocations);
//                            notifyDataSetChanged();
//                        });
//                    });
                    notifyDataSetChanged();
                    break;
            }
        }
        else {
            this.filteredEntries = entries;
        }
    }

    private Pair<Operation, Integer> getChanges(DreamJournalEntriesList entries) {
        if(this.entries.size() == entries.size()){
            // Operation has to either be a changed event or nothing
            return new Pair<>(Operation.CHANGED, -1);
        }
        else if(this.entries.size() < entries.size()) {
            for (int i = 0; i < entries.size(); i++) {
                if(this.entries.size() > i && this.entries.get(i).getEntry().entryId != entries.get(i).getEntry().entryId || this.entries.size() <= i) {
                    return new Pair<>(Operation.ADDED, i);
                }
            }
        }
        else {
            for (int i = 0; i < this.entries.size(); i++) {
                if(entries.size() > i && this.entries.get(i).getEntry().entryId != entries.get(i).getEntry().entryId || entries.size() <= i) {
                    return new Pair<>(Operation.DELETED, i);
                }
            }
        }

        return new Pair<>(Operation.NONE, -1);
    }

    public void notifyEntryChanged(int entryId) {
        Pair<Integer, DreamJournalEntry> dje = entries.getEntryById(entryId);
        if(dje != null) {
            int index = dje.first;
            notifyItemChanged(index);
        }
    }

    @Override
    public int getItemCount() {
        if (filteredEntries == null) {
            return entries.size();
        }
        return filteredEntries.size();
    }

    public void filter(AppliedFilter filter) {
        currentFilter = filter;
        filteredEntries = entries.filter(filter);
        sortEntries(currentSort);
        notifyDataSetChanged();
    }

    public void resetFilters() {
        filteredEntries = null;
        currentFilter = null;
        sortEntries(currentSort);
        notifyDataSetChanged();
    }

    public void sortEntries(int sortBy) {
        currentSort = sortBy;
        DreamJournalEntriesList current;
        if(filteredEntries != null){ current = filteredEntries; }
        else { current = entries; }

        switch (SortOrders.values()[sortBy]){
            case Title_AZ:
                current.sortByTitle(true);
                break;
            case Title_ZA:
                current.sortByTitle(false);
                break;
            case Description_AZ:
                current.sortByDescription(true);
                break;
            case Description_ZA:
                current.sortByDescription(false);
                break;
            case Timestamp_Newest_first:
                current.sortByTimestamp(true);
                break;
            case Timestamp_Oldest_first:
                current.sortByTimestamp(false);
                break;
        }
        setEntries(current);
    }

    public AppliedFilter getCurrentFilter() {
        if (currentFilter == null){
            return AppliedFilter.getEmptyFilter();
        }
        return currentFilter;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public void updateDataForEntry(int entryId, List<AssignedTags> assignedTags, List<JournalEntryHasType> journalEntryHasTypes, List<AudioLocation> audioLocations) {
        for (int i = 0; i < entries.size(); i++) {
            if(entries.get(i).getEntry().entryId == entryId) {
                entries.get(i).setAudioLocations(audioLocations);
                entries.get(i).setTags(assignedTags);
                entries.get(i).setTypes(journalEntryHasTypes);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public enum Operation {
        ADDED,
        DELETED,
        CHANGED,
        NONE
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView title, description, dateTime, firstDateIndicatorName, firstDateIndicatorDate, wrappedTags;
        LinearLayout titleIcons, tags, tagsHolder;
        ConstraintLayout entry;
        MaterialCardView entryCard;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            wrappedTags = itemView.findViewById(R.id.txt_wrapped_tags);
            description = itemView.findViewById(R.id.txt_description);
            dateTime = itemView.findViewById(R.id.txt_date_time);
            titleIcons = itemView.findViewById(R.id.ll_title_icons);
            tags = itemView.findViewById(R.id.ll_tags);
            tagsHolder = itemView.findViewById(R.id.ll_tags_holder);
            entry = itemView.findViewById(R.id.csl_entry);
            entryCard = itemView.findViewById(R.id.crd_journal_entry_card);
            firstDateIndicatorName = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_name);
            firstDateIndicatorDate = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_date);

            View.OnLayoutChangeListener listener = (view, i, i1, i2, i3, i4, i5, i6, i7) -> {
                int wd = tags.getMeasuredWidth();
                int totalChildCount = tags.getChildCount();
                int childWidth = 0;
                int margin = Tools.dpToPx(context, 2)*2;
                for (int j = 0; j < totalChildCount; j++) {
                    childWidth += tags.getChildAt(j).getMeasuredWidth() + margin;
                }
                int diff = wd - childWidth;
                int removeCounter = 0;
                while(diff < 0) {
                    View child = tags.getChildAt(totalChildCount - 1 - removeCounter);
                    int removeWidth = child.getMeasuredWidth() == 0 ? 0 : -diff; //child.getMeasuredWidth() + margin;
                    tags.removeView(child);
                    removeCounter++;
                    diff += removeWidth;
                }

                if(removeCounter > 0) {
                    wrappedTags.setText("+" + removeCounter);
                }
                else {
                    wrappedTags.setText("");
                }
                tags.invalidate();
                wrappedTags.invalidate();
            };

            // TODO check if removing is actually necessary and if there is a case where multiple listeners are on there at the same time
            tags.removeOnLayoutChangeListener(listener);
            tags.addOnLayoutChangeListener(listener);
        }
    }
}
