package com.bitflaker.lucidsourcekit.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.media.MediaPlayer;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.charts.RangeProgress;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.RecordingObjectTools;
import com.bitflaker.lucidsourcekit.general.SortBy;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RecyclerViewAdapterDreamJournal extends RecyclerView.Adapter<RecyclerViewAdapterDreamJournal.MainViewHolder> {
    private final Context context;
    private final Activity activity;
    private final DreamJournal journalFragment;
    private final MainDatabase db;
    private final AsyncListDiffer<DreamJournalEntry> differ;
    private boolean isSortDescending;
    private SortBy currentSort;
    private List<DreamJournalEntry> itemsBeforeFilter;
    private AppliedFilter currentFilter;
    private MediaPlayer mPlayer;
    private ImageButton currentPlayingImageButton;
    private OnEntryCountChanged entryCountChangedListener;

    public RecyclerViewAdapterDreamJournal(Activity activity, DreamJournal journalFragment, List<DreamJournalEntry> entries) {
        this.journalFragment = journalFragment;
        this.context = activity;
        this.activity = activity;
        db = MainDatabase.getInstance(context);
        currentFilter = null;
        itemsBeforeFilter = null;
        currentSort = SortBy.Timestamp;
        isSortDescending = true;
        differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull DreamJournalEntry oldItem, @NonNull DreamJournalEntry newItem) {
                return oldItem.journalEntry.entryId == newItem.journalEntry.entryId;
            }

            @Override
            public boolean areContentsTheSame(@NonNull DreamJournalEntry oldItem, @NonNull DreamJournalEntry newItem) {
                return oldItem.equals(newItem);
            }
        });
        differ.submitList(entries);
        differ.addListListener((previousList, currentList) -> {
            notifyItemRangeChanged(0, currentList.size());
        });
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_journal_entry, parent, false);
        return new MainViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        List<DreamJournalEntry> currentList = differ.getCurrentList();
        DreamJournalEntry current = currentList.get(position);
        holder.resetEntry();

        if (position == 0) {
            setTopEntryMargin(holder);
        }

        Calendar currentEntryTime = Tools.calendarFromMillis(current.journalEntry.timeStamp);
        if (isFirstEntryOfDay(position, currentList, currentEntryTime)) {
            holder.setDreamTimeHeading(currentEntryTime);
        }

        List<DreamTypes> dreamTypes = current.getDreamTypes();
        holder.setSpecialDreamIcons(dreamTypes);

        String title = current.journalEntry.title;
        String textContent = current.journalEntry.description;
        holder.setTitleAndTextContent(title, textContent);

        int audioCount = current.audioLocations.size();
        holder.setRecordingsCount(audioCount);

        List<String> tagItems = current.getStringTags();
        holder.setTagList(tagItems, activity);

        holder.entryCard.setOnClickListener(e -> viewDreamJournalEntry(current));
    }

    public void setTopEntryMargin(@NonNull MainViewHolder holder) {
        LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) holder.firstDateIndicatorDate.getLayoutParams());
        lParams.topMargin = 0;
        holder.firstDateIndicatorDate.setLayoutParams(lParams);
    }

    private static boolean isFirstEntryOfDay(int position, List<DreamJournalEntry> currentList, Calendar cCurrent) {
        boolean showEntryDate = true;
        if(position > 0) {
            Calendar cPast = Calendar.getInstance();
            cPast.setTimeInMillis(currentList.get(position - 1).journalEntry.timeStamp);
            boolean sameDay = cCurrent.get(Calendar.DAY_OF_YEAR) == cPast.get(Calendar.DAY_OF_YEAR) && cCurrent.get(Calendar.YEAR) == cPast.get(Calendar.YEAR);
            showEntryDate = !sameDay;
        }
        return showEntryDate;
    }

    private void viewDreamJournalEntry(DreamJournalEntry current) {
        final BottomSheetDialog bsd = new BottomSheetDialog(context, R.style.BottomSheetDialogStyle);
        bsd.setContentView(R.layout.sheet_journal_entry);

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

        Date entryDate = Tools.calendarFromMillis(current.journalEntry.timeStamp).getTime();
        timestamp.setText(String.format(Locale.getDefault(), "%s • %s", df.format(entryDate), tf.format(entryDate.getTime())));
        title.setText(current.journalEntry.title);
        content.setText(current.journalEntry.description);
        iconRecurring.setVisibility(current.hasSpecialType("REC") ? View.VISIBLE : View.GONE);
        iconLucid.setVisibility(current.hasSpecialType("LCD") ? View.VISIBLE : View.GONE);
        iconSleepParalysis.setVisibility(current.hasSpecialType("SPL") ? View.VISIBLE : View.GONE);
        iconFalseAwakening.setVisibility(current.hasSpecialType("FAW") ? View.VISIBLE : View.GONE);
        iconNightmare.setVisibility(current.hasSpecialType("NTM") ? View.VISIBLE : View.GONE);

        if (current.audioLocations.isEmpty()) { recordingsLayout.setVisibility(View.GONE); }
        for (AudioLocation recData : current.audioLocations) {
            recordingsLayout.addView(generateRecordingsPlayer(recData));
        }

        if (current.journalEntryTags.isEmpty()) { tagsLayout.setVisibility(View.GONE); }
        for (String tag : current.getStringTags()) {
            tagsLayout.addView(generateTagView(tag));
        }

        dreamMood.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
        dreamClarity.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
        sleepQuality.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);

        dreamMood.setData(4, DreamMood.valueOf(current.journalEntry.moodId), "DREAM MOOD", Tools.resolveIconDreamMood(context, current.journalEntry.moodId), null);
        dreamClarity.setData(3, DreamClarity.valueOf(current.journalEntry.clarityId), "DREAM CLARITY", Tools.resolveIconDreamClarity(context, current.journalEntry.clarityId), null);
        sleepQuality.setData(3, SleepQuality.valueOf(current.journalEntry.qualityId), "SLEEP QUALITY", Tools.resolveIconSleepQuality(context, current.journalEntry.qualityId), null);

        deleteEntry.setOnClickListener(e1 -> new MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle(context.getResources().getString(R.string.entry_delete_header))
                .setMessage(context.getResources().getString(R.string.entry_delete_message))
                .setPositiveButton(context.getResources().getString(R.string.yes), (dialog, which) -> {
                    deleteJournalEntry(current.journalEntry.entryId);
                    bsd.dismiss();
                })
                .setNegativeButton(context.getResources().getString(R.string.no), null)
                .show()
        );

        editEntry.setOnClickListener(e1 -> {
            Intent intent = new Intent(context, DreamJournalEntryEditor.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("JOURNAL_ENTRY_ID", current.journalEntry.entryId);
            journalFragment.journalEditorActivityResultLauncher.launch(intent);
            bsd.dismiss();
        });

        bsd.show();
    }

    private void deleteJournalEntry(int entryId) {
        // Get entry and remove it from the database
        JournalEntry entry = db.getJournalEntryDao().getEntryById(entryId).blockingGet();
        db.getJournalEntryDao().delete(entry).blockingSubscribe();

        // Update the current entries and the unfiltered entries
        List<DreamJournalEntry> currentList = new ArrayList<>(differ.getCurrentList());
        DreamJournalEntry entryToRemove =  currentList.stream()
                .filter(x -> x.journalEntry.entryId == entryId)
                .findFirst()
                .orElse(null);
        currentList.remove(entryToRemove);
        differ.submitList(currentList);
        if (itemsBeforeFilter != null) itemsBeforeFilter.remove(entryToRemove);
        if (entryCountChangedListener != null) entryCountChangedListener.onEvent(currentList.size());
    }

    private static int getHorizontalSpacing(View view) {
        int margin = 0;
        if(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params) {
            margin = params.leftMargin + params.rightMargin;
        }
        return margin + view.getPaddingLeft() + view.getPaddingRight();
    }

    private View generateRecordingsPlayer(AudioLocation audioLocation) {
        RecordingObjectTools rot = RecordingObjectTools.getInstance(context);
        LinearLayout entryContainer = rot.generateContainerLayout();

        ImageButton playButton = rot.generatePlayButton();
        playButton.setOnClickListener(e -> handlePlayPauseMediaPlayer(audioLocation, playButton));
        entryContainer.addView(playButton);

        LinearLayout labelsContainer = rot.generateLabelsContrainer();
        entryContainer.addView(labelsContainer);

        labelsContainer.addView(rot.generateHeading());
        labelsContainer.addView(rot.generateTimestamp(audioLocation));
        entryContainer.addView(rot.generateDuration(audioLocation, true));

        return entryContainer;
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

    private void stopCurrentPlayback() {
        if(currentPlayingImageButton != null){
            currentPlayingImageButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        currentPlayingImageButton = null;
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public int filter(AppliedFilter filter) {
        currentFilter = filter;
        if (itemsBeforeFilter == null) {
            itemsBeforeFilter = new ArrayList<>(differ.getCurrentList());
        }
        List<DreamJournalEntry> filteredEntries = itemsBeforeFilter.stream()
                .filter(e -> e.compliesWithFilter(filter))
                .collect(Collectors.toList());
        differ.submitList(filteredEntries);
        return filteredEntries.size();
    }

    public int resetFilters() {
        currentFilter = null;
        differ.submitList(itemsBeforeFilter);
        int itemsCount = itemsBeforeFilter.size();
        itemsBeforeFilter = null;
        return itemsCount;
    }

    public void submitSortedEntries(SortBy sortBy, boolean descending) {
        submitSortedEntries(sortBy, descending, (Runnable) null);
    }

    public void submitSortedEntries(SortBy sortBy, boolean descending, Runnable commitCallback) {
        List<DreamJournalEntry> entries = new ArrayList<>(differ.getCurrentList());
        submitSortedEntries(sortBy, descending, entries, commitCallback);
    }

    public void submitSortedEntries(SortBy sortBy, boolean descending, List<DreamJournalEntry> entries) {
        submitSortedEntries(sortBy, descending, entries, null);
    }

    public void submitSortedEntries(SortBy sortBy, boolean descending, List<DreamJournalEntry> entries, Runnable commitCallback) {
        currentSort = sortBy;
        isSortDescending = descending;

        switch (sortBy) {
            case Timestamp -> entries.sort(Comparator.comparing(x -> x.journalEntry.timeStamp));
            case Title -> entries.sort(Comparator.comparing(x -> x.journalEntry.title));
            case Description -> entries.sort(Comparator.comparing(x -> x.journalEntry.description));
        }
        if (descending) {
            Collections.reverse(entries);
        }

        // Commit the list with or without the optional commit callback
        if (commitCallback != null) { differ.submitList(entries, commitCallback); }
        else { differ.submitList(entries); }
    }

    public AppliedFilter getCurrentFilter() {
        if (currentFilter == null) {
            return AppliedFilter.DEFAULT;
        }
        return currentFilter;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public void updateDataForEntry(DreamJournalEntry newData, Consumer<Integer> scrollToPositionCallback) {
        List<DreamJournalEntry> currentEntries = new ArrayList<>(differ.getCurrentList());
        if (itemsBeforeFilter != null) {
            updateOrAdd(newData, itemsBeforeFilter);
        }
        int index = updateOrAdd(newData, currentEntries);
        if (index == -1 && entryCountChangedListener != null) {
            entryCountChangedListener.onEvent(currentEntries.size());
        }
        submitSortedEntries(currentSort, isSortDescending, currentEntries, () -> {
            if (index != -1) return;
            int sortedIndex = currentEntries.indexOf(newData);
            scrollToPositionCallback.accept(sortedIndex);
        });
    }

    private static int updateOrAdd(DreamJournalEntry newData, List<DreamJournalEntry> currentEntries) {
        int index = indexOf(newData, currentEntries);
        if (index == -1) {
            currentEntries.add(newData);
        }
        else {
            currentEntries.set(index, newData);
        }
        return index;
    }

    private static int indexOf(DreamJournalEntry newData, List<DreamJournalEntry> currentEntries) {
        return currentEntries.indexOf(currentEntries.stream()
                .filter(x -> x.journalEntry.entryId == newData.journalEntry.entryId)
                .findFirst()
                .orElse(null)
        );
    }

    @NonNull
    private TextView generateTagView(String text) {
        int dp8 = Tools.dpToPx(context, 8);
        TextView tag = new TextView(context);
        tag.setSingleLine(true);
        tag.setText(text);
        tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
        tag.setPadding(dp8, dp8 / 2, dp8, dp8 / 2);
        tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tag.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.small_rounded_rectangle, context.getTheme()));
        tag.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, context.getTheme()));
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lParams.setMargins(0, dp8 / 4, dp8 / 2, dp8 / 4);
        tag.setLayoutParams(lParams);
        return tag;
    }

    public enum Operation {
        ADDED,
        DELETED,
        CHANGED,
        NONE
    }

    public interface OnEntryCountChanged {
        void onEvent(int entryCount);
    }

    public void setOnEntryCountChangedListener(OnEntryCountChanged listener) {
        entryCountChangedListener = listener;
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, firstDateIndicatorName, firstDateIndicatorDate, recordingsCount;
        LinearLayout titleIcons, tagsHolder;
        MaterialCardView entryCard;
        ConstraintLayout mainContent;
        float descriptionLineHeight;
        private final DateFormat fullDayInWeekNameFormatter;
        private final DateFormat dateFormat;
        private final Context context;

        public MainViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            fullDayInWeekNameFormatter = new SimpleDateFormat("EEEE");
            dateFormat = android.text.format.DateFormat.getDateFormat(context);
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
                if (description.getLineCount() != maxLines) {
                    description.setLines(maxLines);
                    description.setText(description.getText());
                }
            });
        }

        public void resetEntry() {
            LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) firstDateIndicatorDate.getLayoutParams());
            lParams.topMargin = Tools.dpToPx(context, 14);
            firstDateIndicatorDate.setLayoutParams(lParams);
            firstDateIndicatorName.setVisibility(View.GONE);
            firstDateIndicatorDate.setVisibility(View.GONE);
            tagsHolder.removeAllViews();
            titleIcons.removeAllViews();
        }

        public void setDreamTimeHeading(Calendar currentEntryTime) {
            firstDateIndicatorName.setVisibility(View.VISIBLE);
            firstDateIndicatorName.setText(fullDayInWeekNameFormatter.format(currentEntryTime.getTime()));
            firstDateIndicatorDate.setVisibility(View.VISIBLE);
            firstDateIndicatorDate.setText(dateFormat.format(currentEntryTime.getTime()));
        }

        public void setSpecialDreamIcons(List<DreamTypes> dreamTypes) {
            for (DreamTypes type : dreamTypes) {
                titleIcons.addView(getSpecialTypeIcon(type));
            }
            titleIcons.setVisibility(dreamTypes.isEmpty() ? View.GONE : View.VISIBLE);
        }

        public void setTitleAndTextContent(String title, String textContent) {
            this.title.setText(title);
            boolean hasTextContent = !textContent.isEmpty();
            description.setText(hasTextContent ? textContent : "This dream journal entry contains no text. How about adding some content now?");
            description.setTypeface(null, hasTextContent ? Typeface.NORMAL : Typeface.ITALIC);
            description.setTextColor(Tools.getAttrColor(hasTextContent ? R.attr.secondaryTextColor : R.attr.tertiaryTextColor, context.getTheme()));
        }

        public void setRecordingsCount(int audioCount) {
            recordingsCount.setText(String.format(Locale.getDefault(), "%d", audioCount));
            recordingsCount.setVisibility(audioCount == 0 ? View.GONE : View.VISIBLE);
        }

        public void setTagList(List<String> tagItems, Activity activity) {
            int layoutWidth = calculateTagsContainerWidth(activity);
            if (tagItems.isEmpty()) {
                tagsHolder.addView(generateTagInfo("no tags available"));
                return;
            }
            int dividerSpacing = Tools.dpToPx(context, 4);
            int totalTagsWidth = 0;
            for (int i = 0; i < tagItems.size(); i++) {
                TextView tag = generateTagView(tagItems.get(i), R.attr.colorSurfaceContainerHigh, false);
                int currentTagWidth = getViewWidth(tag);
                int currentMargin = i == 0 ? 0 : dividerSpacing;
                if (totalTagsWidth + currentTagWidth + currentMargin <= layoutWidth) {
                    tagsHolder.addView(tag);
                    totalTagsWidth += currentTagWidth + currentMargin;
                }
                else {
                    int removeCount = 0;
                    TextView collapsedTagCount = generateCollapsedTagCountView(tagItems.size() - i);
                    while (totalTagsWidth + getViewWidth(collapsedTagCount) + dividerSpacing > layoutWidth) {
                        removeCount++;
                        int index = tagsHolder.getChildCount() - 1;
                        View lastView = tagsHolder.getChildAt(index);
                        tagsHolder.removeViewAt(index);
                        int marginToRemove = index == 0 ? 0 : dividerSpacing;
                        int viewToRemoveWidth = getViewWidth(lastView);
                        totalTagsWidth -= viewToRemoveWidth + marginToRemove;
                        collapsedTagCount = generateCollapsedTagCountView(tagItems.size() - i + removeCount);
                    }
                    tagsHolder.addView(collapsedTagCount);
                    break;
                }
            }
        }

        private int calculateTagsContainerWidth(Activity activity) {
            int totalWidth = activity.getWindow().getDecorView().getMeasuredWidth();
            int totalHorizontalMargins = getHorizontalSpacing(tagsHolder);
            ViewParent viewParent = tagsHolder;
            while ((viewParent = viewParent.getParent()) instanceof View) {
                View view = (View) viewParent;
                totalHorizontalMargins += getHorizontalSpacing(view);
            }
            int recordingsWidth = getViewWidth(recordingsCount);    // Seems to ignore the compound drawable at the start, therefore manually add it below
            if (recordingsCount.getVisibility() != View.GONE) {
                recordingsWidth += Tools.dpToPx(context, 20);   // TODO: find a way not to hardcode this: get the sum of all horizontal compound drawable widths (.intrinsicWidth(), .bounds().width() are all 0 at this point)
            }
            int specialIconContainerWidth = getViewWidth(titleIcons);
            return totalWidth - totalHorizontalMargins - recordingsWidth - specialIconContainerWidth;
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
            if (view.getVisibility() == View.GONE) {
                return 0;
            }
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            return view.getMeasuredWidth();
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

        private ImageView getSpecialTypeIcon(DreamTypes dreamType) {
            return switch (dreamType) {
                case Lucid -> generateIconHighlight(R.drawable.rounded_award_star_24);
                case Nightmare -> generateIconHighlight(R.drawable.rounded_sentiment_stressed_24);
                case FalseAwakening -> generateIconHighlight(R.drawable.rounded_cinematic_blur_24);
                case SleepParalysis -> generateIconHighlight(R.drawable.ic_baseline_accessibility_new_24);
                case Recurring -> generateIconHighlight(R.drawable.ic_round_loop_24);
                default -> null;
            };
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
    }
}
