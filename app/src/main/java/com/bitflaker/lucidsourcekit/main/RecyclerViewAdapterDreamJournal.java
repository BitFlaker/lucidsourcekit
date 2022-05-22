package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.resulttables.AssignedTags;
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.SortOrders;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
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

    public RecyclerViewAdapterDreamJournal(DreamJournal journalList, Context context, DreamJournalEntriesList entries) {
        this.journalList = journalList;
        this.context = context;
        this.entries = entries;
        db = MainDatabase.getInstance(context);
        filteredEntries = null;
        currentFilter = null;
        fullDayInWeekNameFormatter = new SimpleDateFormat("EEEE");
    }

    public void addEntry(JournalEntry entry, List<AssignedTags> tags, List<JournalEntryHasType> types, List<AudioLocation> audioLocations) {
        DreamJournalEntry entryToAdd = new DreamJournalEntry(entry, tags, types, audioLocations);
        entries.addFirst(entryToAdd);
        if (filteredEntries != null && DreamJournalEntriesList.entryCompliesWithFilter(entryToAdd, currentFilter)) {
            filteredEntries.addFirst(entryToAdd);
        }
        sortEntries(currentSort);
    }

    public void changeEntryAt(int position, JournalEntry entry, List<AssignedTags> tags, List<JournalEntryHasType> types, List<AudioLocation> audioLocations) {
        if(filteredEntries == null){
            entries.changeAt(position, entry, tags, types, audioLocations);
        }
        else{
            DreamJournalEntry oldEntry = filteredEntries.get(position);
            filteredEntries.changeAt(position, entry, tags, types, audioLocations);
            entries.change(oldEntry, filteredEntries.get(position));
        }
    }

    public void removeEntryAt(int position) {
        if(filteredEntries == null){
            entries.removeAt(position);
        }
        else {
            DreamJournalEntry entryToBeRemoved = filteredEntries.get(position);
            filteredEntries.removeAt(position);
            entries.remove(entryToBeRemoved);
        }
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

        Calendar cldrCurrent = current.getTimestamps()[position];
        Calendar cldrPast = current.getTimestamps()[Math.max(position - 1, 0)];
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        holder.dateTime.setText(MessageFormat.format("~ {0} {1} {2}", dateFormat.format(cldrCurrent.getTime()), context.getResources().getString(R.string.journal_time_at), timeFormat.format(cldrCurrent.getTime())));
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

        if(current.getTags().get(position).size() == 0){
            holder.tags.setVisibility(View.GONE);
        }
        else {
            holder.tags.setVisibility(View.VISIBLE);
        }
        holder.tags.removeAllViews();
        for (int i = 0; i < current.getTags().get(position).size(); i++){
            TextView tag = new TextView(context);
            tag.setText(current.getTags().get(position).get(i).description);
            tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
            int dpLarger = Tools.dpToPx(context, 8);
            int dpSmaller = Tools.dpToPx(context, 4);
            int dpSmall = Tools.dpToPx(context, 2);
            tag.setPadding(dpLarger,dpSmaller,dpLarger,dpSmaller);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tag.setBackground(context.getResources().getDrawable(R.drawable.rounded_spinner));
            tag.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated2x, context.getTheme()));
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(dpSmall, dpSmall, dpSmall, dpSmall);
            tag.setLayoutParams(llParams);
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
            Intent intent = new Intent(context, ViewJournalEntry.class);

            List<List<JournalEntryHasType>> typesList = current.getTypes();
            String[] types = new String[typesList.get(position).size()];
            for (int i = 0; i < typesList.get(position).size(); i++) {
                types[i] = typesList.get(position).get(i).typeId;
            }
            List<List<AssignedTags>> tagsList = current.getTags();
            String[] tags = new String[tagsList.get(position).size()];
            for (int i = 0; i < tagsList.get(position).size(); i++) {
                tags[i] = tagsList.get(position).get(i).description;
            }
            List<List<AudioLocation>> audioLocationsList = current.getAudioLocations();
            String[] recs = new String[audioLocationsList.get(position).size()];
            for (int i = 0; i < audioLocationsList.get(position).size(); i++) {
                recs[i] = audioLocationsList.get(position).get(i).audioPath;
            }

            db.getJournalEntryTagDao().getAll().subscribe((journalEntryTags, throwable) -> {
                String[] availableTags = new String[journalEntryTags.size()];
                for (int i = 0; i < journalEntryTags.size(); i++) {
                    availableTags[i] = journalEntryTags.get(i).description;
                }

                intent.putExtra("position", position);
                intent.putExtra("type", current.getDescriptions()[position] == null ? JournalTypes.Audio.ordinal() : JournalTypes.Text.ordinal());
                intent.putExtra("availableTags", availableTags);
                intent.putExtra("entryId", current.getEntryIds()[position]);
                intent.putExtra("timestamp", current.getTimestamps()[position].getTimeInMillis());
                intent.putExtra("title", current.getTitles()[position]);
                intent.putExtra("description", current.getDescriptions()[position]);
                intent.putExtra("quality", current.getSleepQualities()[position]);
                intent.putExtra("clarity", current.getDreamClarities()[position]);
                intent.putExtra("mood", current.getDreamMoods()[position]);
                intent.putExtra("dreamTypes", types);
                intent.putExtra("tags", tags);
                intent.putExtra("recordings", recs);
                journalList.viewEntryActivityResultLauncher.launch(intent);
            });
        });
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

    public int getLucidDreamsCount() {
        return entries.getLucidDreamsCount();
    }

    public int getTotalDreamsCount() {
        return entries.getTotalDreamsCount();
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
            switch (changes.first){
                case ADDED:
                    index = changes.second;
                    mRecyclerView.scrollToPosition(index);
                    notifyItemInserted(index);
                    notifyItemRangeChanged(index, entries.size());
                    break;
                case DELETED:
                    index = changes.second;
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, entries.size());
                    break;
                case CHANGED:
                    // TODO: when timestamp changed and therefore entry moved in list => old one still there => duplicate entries
                    // TODO: get index of changed item
                    notifyDataSetChanged();
                    break;
            }
        }
        else {
            this.filteredEntries = entries;
        }
        //notifyDataSetChanged();
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

    public List<List<AssignedTags>> getAllTags() {
        return entries.getTags();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public enum Operation {
        ADDED,
        DELETED,
        CHANGED,
        NONE
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView title, description, dateTime, firstDateIndicatorName, firstDateIndicatorDate;
        LinearLayout titleIcons;
        FlexboxLayout tags;
        ConstraintLayout entry;
        MaterialCardView entryCard;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            description = itemView.findViewById(R.id.txt_description);
            dateTime = itemView.findViewById(R.id.txt_date_time);
            titleIcons = itemView.findViewById(R.id.ll_title_icons);
            tags = itemView.findViewById(R.id.ll_tags);
            entry = itemView.findViewById(R.id.csl_entry);
            entryCard = itemView.findViewById(R.id.crd_journal_entry_card);
            firstDateIndicatorName = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_name);
            firstDateIndicatorDate = itemView.findViewById(R.id.txt_journal_entry_first_date_indicator_date);
        }
    }
}
