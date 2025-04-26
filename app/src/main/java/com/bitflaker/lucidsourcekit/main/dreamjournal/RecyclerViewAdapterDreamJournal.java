package com.bitflaker.lucidsourcekit.main.dreamjournal;

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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.SortBy;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamMoods;
import com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamTypes;
import com.bitflaker.lucidsourcekit.data.records.AppliedFilter;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.databinding.EntryJournalBinding;
import com.bitflaker.lucidsourcekit.databinding.SheetJournalEntryBinding;
import com.bitflaker.lucidsourcekit.utils.RecordingObjectTools;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RecyclerViewAdapterDreamJournal extends RecyclerView.Adapter<RecyclerViewAdapterDreamJournal.MainViewHolder> {
    private final Context context;
    private final Activity activity;
    private final DreamJournalView journalFragment;
    private final MainDatabase db;
    private final RecordingObjectTools rot;
    private final AsyncListDiffer<DreamJournalEntry> differ;
    private boolean isSortDescending;
    private SortBy currentSort;
    private List<DreamJournalEntry> itemsBeforeFilter;
    private AppliedFilter currentFilter;
    private MediaPlayer mPlayer;
    private ImageButton currentPlayingImageButton;
    private OnEntryCountChanged entryCountChangedListener;
    private Stack<Runnable> listChangedCallbacks;

    public RecyclerViewAdapterDreamJournal(Activity activity, DreamJournalView journalFragment, List<DreamJournalEntry> entries) {
        this.journalFragment = journalFragment;
        this.context = activity;
        this.activity = activity;

        rot = new RecordingObjectTools(context);
        db = MainDatabase.getInstance(context);
        currentFilter = null;
        itemsBeforeFilter = null;
        currentSort = SortBy.Timestamp;
        isSortDescending = true;
        listChangedCallbacks = new Stack<>();
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
            while (!listChangedCallbacks.isEmpty()) {
                listChangedCallbacks.pop().run();
            }
        });
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new MainViewHolder(EntryJournalBinding.inflate(inflater, parent, false), context);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        List<DreamJournalEntry> currentList = differ.getCurrentList();
        DreamJournalEntry current = currentList.get(position);
        holder.resetEntry();

        if (position == 0) {
            setTopEntryMargin(holder);
        }

        FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams) holder.binding.crdJournalEntryCard.getLayoutParams();
        Calendar currentEntryTime = Tools.calendarFromMillis(current.journalEntry.timeStamp);
        int entryDayIndex = entryIndexOfDay(position, currentList, currentEntryTime);
        int entryCountAfter = countOfDayAfter(position, currentList, currentEntryTime);

        int left = holder.binding.clMainContent.getPaddingLeft();
        int right = holder.binding.clMainContent.getPaddingRight();
        int bottom = holder.binding.clMainContent.getPaddingBottom();
        int dp16 = Tools.dpToPx(context, 16);
        int dp32 = Tools.dpToPx(context, 32);

        // Set first card of day to have corner radius at top
        if (entryDayIndex == 0) {
            holder.setDreamTimeHeading(currentEntryTime);
            lParams.topMargin = 0;
            holder.binding.clMainContent.setPadding(left, dp16, right, bottom);
        }
        else {
            lParams.topMargin = -dp16;
            holder.binding.clMainContent.setPadding(left, dp32, right, bottom);
        }

        // Show questionnaire count only after last journal entry
        boolean isLastEntryOfDay = entryCountAfter == 0;
        holder.binding.flQuestionnaire.setVisibility(isLastEntryOfDay ? View.VISIBLE : View.GONE);
        holder.binding.vDivider.setVisibility(isLastEntryOfDay ? View.INVISIBLE : View.VISIBLE);
        holder.binding.crdQuestionnaire.setOnClickListener(!isLastEntryOfDay ? null : e -> {
            // TODO: Show completed questionnaires for that day / allow to fill out new one
        });

        Calendar cal = Calendar.getInstance();
        boolean isToday = cal.get(Calendar.DAY_OF_YEAR) == currentEntryTime.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == currentEntryTime.get(Calendar.YEAR);
        // TODO: Only show when the sleep quality questionnaire has not yet been filled out today
        holder.binding.btnRateSleep.setVisibility(isToday ? View.VISIBLE : View.GONE);

        List<DreamTypes> dreamTypes = current.getDreamTypes();
        holder.setSpecialDreamIcons(dreamTypes);

        String title = current.journalEntry.title;
        String textContent = current.journalEntry.description;
        holder.setTitleAndTextContent(title, textContent);

        int audioCount = current.audioLocations.size();
        holder.setRecordingsCount(audioCount);

        List<String> tagItems = current.getStringTags();
        RecyclerViewAdapterDreamJournal.MainViewHolder.setTagList(holder.binding.llTagsHolder, holder.calculateAdditionalContainerPadding(), 1, tagItems, activity);

        holder.binding.crdJournalEntryCard.setOnClickListener(e -> viewDreamJournalEntry(current));
    }

    public void setTopEntryMargin(@NonNull MainViewHolder holder) {
        LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) holder.binding.llTopDayHeader.getLayoutParams());
        lParams.topMargin = 0;
        holder.binding.llTopDayHeader.setLayoutParams(lParams);
    }

    private static int entryIndexOfDay(int position, List<DreamJournalEntry> currentList, Calendar current) {
        Calendar cal = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        int currentDayOfYear = current.get(Calendar.DAY_OF_YEAR);
        int count = 0;
        while (position > 0) {
            cal.setTimeInMillis(currentList.get(position - 1).journalEntry.timeStamp);
            boolean sameDay = currentDayOfYear == cal.get(Calendar.DAY_OF_YEAR) && currentYear == cal.get(Calendar.YEAR);
            if (!sameDay) {
                break;
            }
            count++;
            position--;
        }
        return count;
    }

    private static int countOfDayAfter(int position, List<DreamJournalEntry> currentList, Calendar current) {
        Calendar cal = Calendar.getInstance();
        int currentYear = current.get(Calendar.YEAR);
        int currentDayOfYear = current.get(Calendar.DAY_OF_YEAR);
        int count = 0;
        while (position < currentList.size() - 1) {
            cal.setTimeInMillis(currentList.get(position + 1).journalEntry.timeStamp);
            boolean sameDay = currentDayOfYear == cal.get(Calendar.DAY_OF_YEAR) && currentYear == cal.get(Calendar.YEAR);
            if (!sameDay) {
                break;
            }
            count++;
            position++;
        }
        return count;
    }

    public void viewDreamJournalEntry(DreamJournalEntry current) {
        final BottomSheetDialog bsd = new BottomSheetDialog(context, R.style.BottomSheetDialogStyle);
        SheetJournalEntryBinding binding = SheetJournalEntryBinding.inflate(LayoutInflater.from(context));
        bsd.setContentView(binding.getRoot());

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);

        Date entryDate = Tools.calendarFromMillis(current.journalEntry.timeStamp).getTime();
        binding.txtEntryTimestamp.setText(String.format(Locale.getDefault(), "%s • %s", df.format(entryDate), tf.format(entryDate.getTime())));
        binding.txtEntryTitle.setText(current.journalEntry.title);
        binding.txtEntryDreamContent.setText(current.journalEntry.description);
        binding.btnIconRecurring.setVisibility(current.hasSpecialType("REC") ? View.VISIBLE : View.GONE);
        binding.btnIconLucid.setVisibility(current.hasSpecialType("LCD") ? View.VISIBLE : View.GONE);
        binding.btnIconSleepParalysis.setVisibility(current.hasSpecialType("SPL") ? View.VISIBLE : View.GONE);
        binding.btnIconFalseAwakening.setVisibility(current.hasSpecialType("FAW") ? View.VISIBLE : View.GONE);
        binding.btnIconNightmare.setVisibility(current.hasSpecialType("NTM") ? View.VISIBLE : View.GONE);

        if (current.audioLocations.isEmpty()) { binding.llRecordingsContainer.setVisibility(View.GONE); }
        for (AudioLocation recData : current.audioLocations) {
            binding.llRecordingsContainer.addView(generateRecordingsPlayer(recData));
        }

        if (current.journalEntryTags.isEmpty()) { binding.fblTags.setVisibility(View.GONE); }
        for (String tag : current.getStringTags()) {
            binding.fblTags.addView(generateTagView(tag));
        }

        binding.rpDreamMood.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
        binding.rpDreamClarity.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);
        binding.rpSleepQuality.setBackgroundAttrColor(R.attr.colorSurfaceContainerHigh);

        binding.rpDreamMood.setData(4, DreamMood.valueOf(current.journalEntry.moodId), "DREAM MOOD", Tools.resolveIconDreamMood(context, current.journalEntry.moodId), null);
        binding.rpDreamClarity.setData(3, DreamClarity.valueOf(current.journalEntry.clarityId), "DREAM CLARITY", Tools.resolveIconDreamClarity(context, current.journalEntry.clarityId), null);
        binding.rpSleepQuality.setData(3, SleepQuality.valueOf(current.journalEntry.qualityId), "SLEEP QUALITY", Tools.resolveIconSleepQuality(context, current.journalEntry.qualityId), null);

        binding.btnDeleteEntry.setOnClickListener(e1 -> new MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle(context.getResources().getString(R.string.entry_delete_header))
                .setMessage(context.getResources().getString(R.string.entry_delete_message))
                .setPositiveButton(context.getResources().getString(R.string.yes), (dialog, which) -> {
                    deleteJournalEntry(current.journalEntry.entryId);
                    bsd.dismiss();
                })
                .setNegativeButton(context.getResources().getString(R.string.no), null)
                .show()
        );

        binding.btnEditEntry.setOnClickListener(e1 -> {
            Intent intent = new Intent(context, DreamJournalEditorView.class);
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

    public int resetFilters(Runnable callback) {
        listChangedCallbacks.push(callback);
        return resetFilters();
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
            return new AppliedFilter(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    DreamMoods.None,
                    com.bitflaker.lucidsourcekit.data.enums.journalratings.DreamClarity.None,
                    com.bitflaker.lucidsourcekit.data.enums.journalratings.SleepQuality.None
            );
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

    public int indexOfEntry(DreamJournalEntry entry) {
        return differ.getCurrentList().indexOf(entry);
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
        private final DateFormat fullDayInWeekNameFormatter;
        private final DateFormat dateFormat;
        private final Context context;
        public EntryJournalBinding binding;
        float descriptionLineHeight;

        public MainViewHolder(@NonNull EntryJournalBinding binding, Context context) {
            super(binding.getRoot());
            this.context = context;
            this.binding = binding;
            fullDayInWeekNameFormatter = new SimpleDateFormat("EEEE");
            dateFormat = android.text.format.DateFormat.getDateFormat(context);

            descriptionLineHeight = binding.txtDescription.getLineHeight();
            ConstraintSet cs = new ConstraintSet();
            cs.clone(binding.clMainContent);
            cs.constrainMinHeight(binding.txtDescription.getId(), (int) descriptionLineHeight * 2);
            cs.applyTo(binding.clMainContent);

            binding.txtDescription.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int maxLines = (int) (binding.txtDescription.getHeight() / descriptionLineHeight);
                if (binding.txtDescription.getLineCount() != maxLines) {
                    binding.txtDescription.setLines(maxLines);
                    binding.txtDescription.setText(binding.txtDescription.getText());
                }
            });
        }

        public void resetEntry() {
            LinearLayout.LayoutParams lParams = ((LinearLayout.LayoutParams) binding.llTopDayHeader.getLayoutParams());
            lParams.topMargin = Tools.dpToPx(context, 14);
            binding.llTopDayHeader.setLayoutParams(lParams);
            binding.llTopDayHeader.setVisibility(View.GONE);
            binding.llTagsHolder.removeAllViews();
            binding.llTitleIcons.removeAllViews();
        }

        public void setDreamTimeHeading(Calendar currentEntryTime) {
            binding.txtJournalEntryFirstDateIndicatorName.setText(fullDayInWeekNameFormatter.format(currentEntryTime.getTime()));
            binding.txtJournalEntryFirstDateIndicatorDate.setText(dateFormat.format(currentEntryTime.getTime()));
            binding.llTopDayHeader.setVisibility(View.VISIBLE);
        }

        public void setSpecialDreamIcons(List<DreamTypes> dreamTypes) {
            for (DreamTypes type : dreamTypes) {
                binding.llTitleIcons.addView(getSpecialTypeIcon(type));
            }
            binding.llTitleIcons.setVisibility(dreamTypes.isEmpty() ? View.GONE : View.VISIBLE);
        }

        public void setTitleAndTextContent(String title, String textContent) {
            this.binding.txtTitle.setText(title);
            boolean hasTextContent = textContent != null && !textContent.isBlank();
            binding.txtDescription.setText(hasTextContent ? textContent : "This dream journal entry contains no text. How about adding some content now?");
            binding.txtDescription.setTypeface(null, hasTextContent ? Typeface.NORMAL : Typeface.ITALIC);
            binding.txtDescription.setTextColor(Tools.getAttrColor(hasTextContent ? R.attr.secondaryTextColor : R.attr.tertiaryTextColor, context.getTheme()));
        }

        public void setRecordingsCount(int audioCount) {
            binding.txtRecordingsCount.setText(String.format(Locale.getDefault(), "%d", audioCount));
            binding.txtRecordingsCount.setVisibility(audioCount == 0 ? View.GONE : View.VISIBLE);
        }

        public static void setTagList(ViewGroup container, int additionalPadding, int elevationLevel, List<String> tagItems, Activity activity) {
            Context context = container.getContext();
            int layoutWidth = calculateTagsContainerWidth(container, activity, additionalPadding);
            if (tagItems.isEmpty()) {
                container.addView(generateTagInfo(context, "no tags available", elevationLevel));
                return;
            }
            int dividerSpacing = Tools.dpToPx(context, 4);
            int totalTagsWidth = 0;
            for (int i = 0; i < tagItems.size(); i++) {
                @AttrRes int color = elevationLevel == 0 ? R.attr.colorSurfaceContainer : R.attr.colorSurfaceContainerHigh;
                TextView tag = generateTagView(context, tagItems.get(i), color, false, elevationLevel);
                int currentTagWidth = getViewWidth(tag);
                int currentMargin = i == 0 ? 0 : dividerSpacing;
                if (totalTagsWidth + currentTagWidth + currentMargin <= layoutWidth) {
                    container.addView(tag);
                    totalTagsWidth += currentTagWidth + currentMargin;
                }
                else {
                    int removeCount = 0;
                    TextView collapsedTagCount = generateCollapsedTagCountView(context, tagItems.size() - i, elevationLevel);
                    while (totalTagsWidth + getViewWidth(collapsedTagCount) + dividerSpacing > layoutWidth) {
                        removeCount++;
                        int index = container.getChildCount() - 1;
                        View lastView = container.getChildAt(index);
                        container.removeViewAt(index);
                        int marginToRemove = index == 0 ? 0 : dividerSpacing;
                        int viewToRemoveWidth = getViewWidth(lastView);
                        totalTagsWidth -= viewToRemoveWidth + marginToRemove;
                        collapsedTagCount = generateCollapsedTagCountView(context, tagItems.size() - i + removeCount, elevationLevel);
                    }
                    container.addView(collapsedTagCount);
                    break;
                }
            }
        }

        public int calculateAdditionalContainerPadding() {
            int recordingsWidth = MainViewHolder.getViewWidth(binding.txtRecordingsCount);    // Seems to ignore the compound drawable at the start, therefore manually add it below
            if (binding.txtRecordingsCount.getVisibility() != View.GONE) {
                recordingsWidth += Tools.dpToPx(binding.getRoot().getContext(), 20);   // TODO: find a way not to hardcode this: get the sum of all horizontal compound drawable widths (.intrinsicWidth(), .bounds().width() are all 0 at this point)
            }
            int specialIconContainerWidth = MainViewHolder.getViewWidth(binding.llTitleIcons);
            return recordingsWidth + specialIconContainerWidth;
        }

        private static int calculateTagsContainerWidth(ViewGroup container, Activity activity, int additionalPadding) {
            int totalWidth = activity.getWindow().getDecorView().getMeasuredWidth();
            int totalHorizontalMargins = getHorizontalSpacing(container);
            ViewParent viewParent = container;
            while ((viewParent = viewParent.getParent()) instanceof View) {
                View view = (View) viewParent;
                totalHorizontalMargins += getHorizontalSpacing(view);
            }
            return totalWidth - totalHorizontalMargins - additionalPadding;
        }

        private static TextView generateCollapsedTagCountView(Context context, int count, int elevationLevel) {
            return generateTagInfo(context, String.format(Locale.getDefault(), "+ %d", count), elevationLevel);
        }

        @NonNull
        private static TextView generateTagInfo(Context context, String text, int elevationLevel) {
            int dp8 = Tools.dpToPx(context, 8);
            TextView countViewer = new TextView(context);
            @DrawableRes int background = elevationLevel == 0 ? R.drawable.round_border_dashed_surface : R.drawable.round_border_dashed;
            @AttrRes int foreground = elevationLevel == 0 ? R.attr.tertiaryTextColor : R.attr.secondaryTextColor;
            countViewer.setBackgroundResource(background);
            countViewer.setPadding(dp8, dp8 / 2, dp8, dp8 / 2);
            countViewer.setText(text);
            countViewer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            countViewer.setGravity(Gravity.CENTER_VERTICAL);
            countViewer.setTextColor(Tools.getAttrColor(foreground, context.getTheme()));
            countViewer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            return countViewer;
        }

        private static int getViewWidth(View view) {
            if (view.getVisibility() == View.GONE) {
                return 0;
            }
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            return view.getMeasuredWidth();
        }

        @NonNull
        private static TextView generateTagView(Context context, String text, int color, boolean addMargins, int elevationLevel) {
            int dp8 = Tools.dpToPx(context, 8);
            @AttrRes int textColor = elevationLevel == 0 ? R.attr.secondaryTextColor : R.attr.primaryTextColor;
            TextView tag = new TextView(context);
            tag.setSingleLine(true);
            tag.setText(text);
            tag.setTextColor(Tools.getAttrColorStateList(textColor, context.getTheme()));
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

        public void removeQuestionnaire() {
            ((ViewGroup) binding.flQuestionnaire.getParent()).removeView(binding.flQuestionnaire);
            binding.vDivider.setVisibility(View.GONE);
            FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams) binding.crdJournalEntryCard.getLayoutParams();
            lParams.bottomMargin = 0;
            binding.crdJournalEntryCard.setLayoutParams(lParams);
            int left = binding.clMainContent.getPaddingLeft();
            int right = binding.clMainContent.getPaddingRight();
            int top = binding.clMainContent.getPaddingTop();
            binding.clMainContent.setPadding(left, top, right, 0);
        }
    }
}
