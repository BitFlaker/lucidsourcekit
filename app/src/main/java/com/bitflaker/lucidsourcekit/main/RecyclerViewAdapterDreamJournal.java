package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.general.RecordingObjectTools;
import com.bitflaker.lucidsourcekit.general.SortOrders;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemory;
import com.bitflaker.lucidsourcekit.main.dreamjournal.JournalInMemoryManager;
import com.bitflaker.lucidsourcekit.main.dreamjournal.RecordingData;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RecyclerViewAdapterDreamJournal extends RecyclerView.Adapter<RecyclerViewAdapterDreamJournal.MainViewHolder> {
    private final DateFormat dateFormat;
    private Context context;
    private Activity activity;
    private DreamJournal journalList;
    private DreamJournalEntriesList entries;
    private DreamJournalEntriesList filteredEntries;
    private int currentSort = 0;
    private AppliedFilter currentFilter;
    private MainDatabase db;
    private RecyclerView mRecyclerView;
    private DateFormat fullDayInWeekNameFormatter;
    private MediaPlayer mPlayer;
    private ImageButton currentPlayingImageButton;

    public RecyclerViewAdapterDreamJournal(DreamJournal journalList, Activity activity, Context context, DreamJournalEntriesList entries) {
        this.journalList = journalList;
        this.context = context;
        this.activity = activity;
        this.entries = entries.clone();
        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        db = MainDatabase.getInstance(context);
        filteredEntries = null;
        currentFilter = null;
        fullDayInWeekNameFormatter = new SimpleDateFormat("EEEE");
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_journal_entry, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        DreamJournalEntriesList current;
        if(filteredEntries != null) { current = filteredEntries; }
        else { current = entries; }

        LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) holder.firstDateIndicatorDate.getLayoutParams());
        lParams.topMargin = position == 0 ? 0 : Tools.dpToPx(context, 14);
        holder.firstDateIndicatorDate.setLayoutParams(lParams);

        Calendar cCurrent = current.getTimestamps()[position];
        boolean showEntryDate = true;
        if(position > 0) {
            Calendar cPast = current.getTimestamps()[position - 1];
            boolean sameDay = cCurrent.get(Calendar.DAY_OF_YEAR) == cPast.get(Calendar.DAY_OF_YEAR) && cCurrent.get(Calendar.YEAR) == cPast.get(Calendar.YEAR);
            showEntryDate = !sameDay;
        }
        holder.firstDateIndicatorName.setVisibility(showEntryDate ? View.VISIBLE : View.GONE);
        holder.firstDateIndicatorDate.setVisibility(showEntryDate ? View.VISIBLE : View.GONE);
        if (showEntryDate) {
            holder.firstDateIndicatorName.setText(fullDayInWeekNameFormatter.format(cCurrent.getTime()));
            holder.firstDateIndicatorDate.setText(dateFormat.format(cCurrent.getTime()));
        }

        holder.title.setText(current.getTitles()[position]);

        String currentDescription = current.getDescriptions()[position];
        if(currentDescription.isEmpty()) {
            holder.description.setText("This dream journal entry contains no text. How about adding some content now?");
            holder.description.setTypeface(null, Typeface.ITALIC);
            holder.description.setTextColor(Tools.getAttrColor(R.attr.tertiaryTextColor, context.getTheme()));
        }
        else {
            holder.description.setText(currentDescription);
            holder.description.setTypeface(null, Typeface.NORMAL);
            holder.description.setTextColor(Tools.getAttrColor(R.attr.secondaryTextColor, context.getTheme()));
            holder.setDescription(currentDescription);
        }

        // Settings the icon for recordings count
        int audioCount = current.getAudioLocations().get(position).size();
        holder.recordingsCount.setText(String.format(Locale.getDefault(), "%d", audioCount));
//        holder.recordingsCount.setVisibility(audioCount == 0 ? View.GONE : View.VISIBLE);     // TODO: make auto visibility available again when tag list is calculated properly
        holder.recordingsCount.setVisibility(View.GONE);

        // Setting the tag list -> TODO: reduce the calculated width by the (width + margin) of the recordingsCount in case it is visible
        List<String> tagItems = current.getTags().get(position).stream().map(assignedTags -> assignedTags.description).collect(Collectors.toList());
        int layoutWidth = calculateTagsContainerWidth(holder.tagsHolder);
        holder.tagsHolder.removeAllViews();
        addTags(holder, tagItems, layoutWidth);

        holder.titleIcons.removeAllViews();
        List<JournalEntryHasType> types = current.getTypes().get(position);
        for (int i = 0; i < types.size(); i++) {
            DreamTypes dreamType = DreamTypes.getEnum(types.get(i).typeId);
            ImageView specialTypeIcon = getSpecialTypeIcon(dreamType);
            if (specialTypeIcon != null) {
                holder.titleIcons.addView(specialTypeIcon);
            }
        }

        holder.entryCard.setOnClickListener(e -> {
            final BottomSheetDialog bsd = new BottomSheetDialog(context, R.style.BottomSheetDialogStyle);
            bsd.setContentView(R.layout.sheet_journal_entry);

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
                tagsLayout.addView(generateTagView(tag, R.attr.colorSurfaceContainerHigh, true));
            }

            Drawable[] dreamMoodIcons = Tools.getIconsDreamMood(context);
            Drawable[] dreamClarityIcons = Tools.getIconsDreamClarity(context);
            Drawable[] sleepQualityIcons = Tools.getIconsSleepQuality(context);

            int dmIndex = getIndexOfDreamMood(jim.getDreamMood());
            int dcIndex = getIndexOfDreamClarity(jim.getDreamClarity());
            int sqIndex = getIndexOfSleepQuality(jim.getSleepQuality());

            dreamMood.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
            dreamClarity.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
            sleepQuality.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);

            dreamMood.setData(4, dmIndex, "DREAM MOOD", dreamMoodIcons[dmIndex], null);
            dreamClarity.setData(3, dcIndex, "DREAM CLARITY", dreamClarityIcons[dcIndex], null);
            sleepQuality.setData(3, sqIndex, "SLEEP QUALITY", sleepQualityIcons[sqIndex], null);

            deleteEntry.setOnClickListener(e1 ->
                    new MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog).setTitle(context.getResources().getString(R.string.entry_delete_header)).setMessage(context.getResources().getString(R.string.entry_delete_message))
                        .setPositiveButton(context.getResources().getString(R.string.yes), (dialog, which) -> {
                            JournalEntry entry = db.getJournalEntryDao().getEntryById(jim.getEntryId()).blockingGet();
                            db.getJournalEntryDao().delete(entry).blockingSubscribe();
                            if(filteredEntries != null) {
                                DreamJournalEntry entryToRemove = filteredEntries.get(position);
                                filteredEntries.removeAt(position);
                                entries.remove(entryToRemove);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, filteredEntries.size());
                            }
                            else {
                                entries.removeAt(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, entries.size());
                            }
                            bsd.dismiss();
                        })
                        .setNegativeButton(context.getResources().getString(R.string.no), null)
                        .show());

            editEntry.setOnClickListener(e1 -> {
                jim.setEditMode(JournalInMemory.EditMode.EDIT);
                Intent intent = new Intent(context, DreamJournalEntryEditor.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("journal_in_memory_id", id);
                journalList.journalEditorActivityResultLauncher.launch(intent);
                bsd.dismiss();
            });

            bsd.show();
        });
    }

    private @Nullable ImageView getSpecialTypeIcon(DreamTypes dreamType) {
        return switch (dreamType) {
            case Lucid -> generateIconHighlight(R.drawable.rounded_award_star_24);
            case Nightmare -> generateIconHighlight(R.drawable.rounded_sentiment_stressed_24);
            case FalseAwakening -> generateIconHighlight(R.drawable.rounded_cinematic_blur_24);
            case SleepParalysis -> generateIconHighlight(R.drawable.ic_baseline_accessibility_new_24);
            case Recurring -> generateIconHighlight(R.drawable.ic_round_loop_24);
            default -> null;
        };
    }

    private int calculateTagsContainerWidth(@NonNull ViewGroup tagsContainer) {
        // Remove all horizontal margins and paddings from all parents, so we get the resulting width of the tag container
        int totalWidth = activity.getWindow().getDecorView().getMeasuredWidth();
        int totalHorizontalMargins = getHorizontalSpacing(tagsContainer);
        ViewParent viewParent = tagsContainer;
        while ((viewParent = viewParent.getParent()) instanceof View) {
            View view = (View) viewParent;
            totalHorizontalMargins += getHorizontalSpacing(view);
        }
        totalHorizontalMargins += Tools.dpToPx(context, 20);    // add the known fixed width of the icons sidebar // TODO: only add this in case any icons are actually being displayed
        return totalWidth - totalHorizontalMargins;
    }

    private static int getHorizontalSpacing(View view) {
        int margin = 0;
        if(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            margin = params.leftMargin + params.rightMargin;
        }
        return margin + view.getPaddingLeft() + view.getPaddingRight();
    }

    private void addTags(@NonNull MainViewHolder holder, List<String> tagItems, int layoutWidth) {
        if (tagItems.size() == 0) {
            holder.tagsHolder.addView(generateTagInfo("no tags available"));
            return;
        }
        int dividerSpacing = Tools.dpToPx(context, 4);
        int totalTagsWidth = 0;
        for (int i = 0; i < tagItems.size(); i++) {
            TextView tag = generateTagView(tagItems.get(i), R.attr.colorSurfaceContainerHigh, false);
            int currentTagWidth = getViewWidth(tag);
            int currentMargin = i == 0 ? 0 : dividerSpacing;
            if (totalTagsWidth + currentTagWidth + currentMargin <= layoutWidth) {
                holder.tagsHolder.addView(tag);
                totalTagsWidth += currentTagWidth + currentMargin;
            }
            else {
                TextView collapsedTagCount = generateCollapsedTagCountView(tagItems.size() - i);
                int collapsedTagCountWidth = getViewWidth(collapsedTagCount);
                while (totalTagsWidth + collapsedTagCountWidth + dividerSpacing > layoutWidth) {
                    int index = holder.tagsHolder.getChildCount() - 1;
                    View lastView = holder.tagsHolder.getChildAt(index);
                    int marginToRemove = index == 0 ? 0 : dividerSpacing;
                    int viewToRemoveWidth = getViewWidth(lastView);
                    holder.tagsHolder.removeView(lastView);
                    totalTagsWidth -= viewToRemoveWidth + marginToRemove;
                }
                holder.tagsHolder.addView(collapsedTagCount);
                break;
            }
        }
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
    private TextView generateTagView(String text, int color, boolean addMargins) {
        int dp8 = Tools.dpToPx(context, 8);
        TextView tag = new TextView(context);
        tag.setSingleLine(true);
        tag.setText(text);
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
        tag.setPadding(dp8, dp8 / 2, dp8, dp8 / 2);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tag.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.small_rounded_rectangle, context.getTheme()));
        tag.setBackgroundTintList(Tools.getAttrColorStateList(color, context.getTheme()));
        if (addMargins) {
            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lParams.setMargins(0, dp8 / 4, dp8 / 2, dp8 / 4);
            tag.setLayoutParams(lParams);
        }
        return tag;
    }

    private TextView generateCollapsedTagCountView(int count) {
        return generateTagInfo(String.format(Locale.getDefault(), "+ %d", count));
    }

    @NonNull
    private TextView generateTagInfo(String text) {
        int dp8 = Tools.dpToPx(context, 8);
        TextView countViewer = new TextView(context);
        countViewer.setBackgroundResource(R.drawable.round_border_dashed);
        countViewer.setPadding(dp8, dp8 / 2, dp8, dp8 / 2);
        countViewer.setText(text);
        countViewer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        countViewer.setGravity(Gravity.CENTER_VERTICAL);
        countViewer.setTextColor(Tools.getAttrColor(R.attr.secondaryTextColor, context.getTheme()));
        countViewer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        return countViewer;
    }

    private int getViewWidth(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return view.getMeasuredWidth();
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

    private ImageView generateIcon(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColorStateList(R.attr.secondaryTextColor, context.getTheme());
        icon.setImageTintList(stateList);
        int size = Tools.dpToPx(context, 20);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private ImageView generateIconHighlight(int iconId) {
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
            this.entries = entries.clone();
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
        if(this.entries.size() == entries.size()) {
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
        if(filteredEntries != null) { current = filteredEntries; }
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

    public void updateDataForEntry(JournalEntry entry, List<AssignedTags> assignedTags, List<JournalEntryHasType> journalEntryHasTypes, List<AudioLocation> audioLocations) {
        for (int i = 0; i < entries.size(); i++) {
            if(entries.get(i).getEntry().entryId == entry.entryId) {
                entries.get(i).setEntryData(entry);
                entries.get(i).setAudioLocations(audioLocations);
                entries.get(i).setTags(assignedTags);
                entries.get(i).setTypes(journalEntryHasTypes);
                notifyItemChanged(i);
                return;
            }
        }

        int indexToInsert = getIndexToInsert(SortOrders.values()[currentSort], entries, entry);
        DreamJournalEntry newEntry = entries.insert(indexToInsert, entry, assignedTags, audioLocations, journalEntryHasTypes);

        int indexToInsertCurrent = indexToInsert;
        boolean isFilteredAndComplies = filteredEntries != null && DreamJournalEntriesList.entryCompliesWithFilter(newEntry, currentFilter);
        DreamJournalEntriesList current = entries;

        if(isFilteredAndComplies) {
            indexToInsertCurrent = getIndexToInsert(SortOrders.values()[currentSort], filteredEntries, entry);
            filteredEntries.insert(indexToInsertCurrent, entry, assignedTags, audioLocations, journalEntryHasTypes);
            current = filteredEntries;
        }

        if(isFilteredAndComplies || filteredEntries == null) {
            notifyItemInserted(indexToInsertCurrent);
            notifyItemRangeChanged(indexToInsertCurrent, current.size());
            mRecyclerView.scrollToPosition(indexToInsertCurrent);
        }
    }

    private static int getIndexToInsert(SortOrders order, DreamJournalEntriesList entries, JournalEntry entry) {
        switch (order) {
            case Title_AZ:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByTitle(true, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
            case Title_ZA:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByTitle(false, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
            case Description_AZ:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByDescription(true, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
            case Description_ZA:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByDescription(false, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
            case Timestamp_Newest_first:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByTimestamp(true, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
            case Timestamp_Oldest_first:
                for (int i = 0; i < entries.size(); i++) {
                    if(DreamJournalEntriesList.compareByTimestamp(false, entries.get(i).getEntry(), entry) == 1) {
                        return i;
                    }
                }
                break;
        }
        return entries.size();
    }

    public enum Operation {
        ADDED,
        DELETED,
        CHANGED,
        NONE
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, firstDateIndicatorName, firstDateIndicatorDate, recordingsCount;
        LinearLayout titleIcons, tagsHolder;
        MaterialCardView entryCard;
        ConstraintLayout mainContent;
        String descriptionText;
        float descriptionLineHeight;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            description = itemView.findViewById(R.id.txt_description);
            titleIcons = itemView.findViewById(R.id.ll_title_icons);
            tagsHolder = itemView.findViewById(R.id.ll_tags_holder);
            entryCard = itemView.findViewById(R.id.crd_journal_entry_card);
            mainContent = itemView.findViewById(R.id.cl_main_content);
            firstDateIndicatorName = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_name);
            firstDateIndicatorDate = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_date);
            recordingsCount = itemView.findViewById(R.id.txt_recordings_count);

            descriptionLineHeight = description.getLineHeight();
            ConstraintSet cs = new ConstraintSet();
            cs.clone(mainContent);
            cs.constrainMinHeight(description.getId(), (int) descriptionLineHeight * 2);
            cs.applyTo(mainContent);

            this.description.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int maxLines = (int) (description.getHeight() / descriptionLineHeight);

                // Optimisation idea to just set the text of the TextView to the amount
                // of characters fitting into the TextView plus a few more to get the
                // ellipsize effect and preventing scroll lag. It did not seem provide significant
                // performance improvements and had some issues (e.g. setting the text to some text
                // from a different entry, because the event was fired delayed)

//                    if(descriptionText == null) {
//                        return;
//                    }
//
//                    Paint paint = description.getPaint();
//                    StringBuilder sb = new StringBuilder();
//                    String textToCheck = descriptionText.trim();
//                    for (int i = 0; i < maxLines; i++) {
//                        boolean isLastLine = i == maxLines - 1;
//                        int charCountFitting = getCharCountFitting(paint, textToCheck, description);
//                        String currentLine = getCurrentLine(charCountFitting, textToCheck, isLastLine, 6);
//                        sb.append(currentLine);
//                        if(!isLastLine) { sb.append("\n"); }
//                        textToCheck = textToCheck.replaceFirst(Pattern.quote(currentLine), "").trim();
//                    }

                if (description.getLineCount() != maxLines) {
                    description.setLines(maxLines);
                    description.setText(description.getText());
                }
            });
        }

        public void setDescription(String descriptionText) {
            this.descriptionText = descriptionText;
        }
    }
}
