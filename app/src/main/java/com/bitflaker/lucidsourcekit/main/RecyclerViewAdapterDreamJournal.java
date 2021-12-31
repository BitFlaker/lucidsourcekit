package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.bitflaker.lucidsourcekit.general.JournalTypes;
import com.bitflaker.lucidsourcekit.general.SortOrders;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalEntries;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntry;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.flexbox.FlexboxLayout;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class RecyclerViewAdapterDreamJournal extends RecyclerView.Adapter<RecyclerViewAdapterDreamJournal.MainViewHolder> {
    private Context context;
    private DreamJournal journalList;
    private DreamJournalEntriesList entries;
    private DreamJournalEntriesList filteredEntries;
    private int currentSort;
    private AppliedFilter currentFilter;

    public RecyclerViewAdapterDreamJournal(DreamJournal journalList, Context context, DreamJournalEntriesList entries) {
        this.journalList = journalList;
        this.context = context;
        this.entries = entries;
        filteredEntries = null;
        currentFilter = null;
    }

    public void addEntry(StoredJournalEntries entry, String[] tags, String[] types, String[] audioLocations) {
        DreamJournalEntry entryToAdd = new DreamJournalEntry(entry, tags, types, audioLocations);
        entries.addFirst(entryToAdd);
        if (filteredEntries != null && DreamJournalEntriesList.entryCompliesWithFilter(entryToAdd, currentFilter)) {
            filteredEntries.addFirst(entryToAdd);
        }
        sortEntries(currentSort);
    }

    public void changeEntryAt(int position, StoredJournalEntries entry, String[] tags, String[] types, String[] audioLocations) {
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

        holder.dateTime.setText(MessageFormat.format("{0} {1} {2}", current.getDates()[position], context.getResources().getString(R.string.journal_time_at), current.getTimes()[position]));
        holder.title.setText(current.getTitles()[position]);
        if(current.getDescriptions()[position] == null){
            holder.description.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_mic_24, 0, 0, 0);
            int audioCount = current.getAudioLocations().get(position).length;
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
            lparamsDesc.setMargins(0, 0, 0, 0);
            holder.description.setLayoutParams(lparamsDesc);
        }

        int iconCount = holder.titleIcons.getChildCount();
        int skip = 0;
        for (int i = 0; i < iconCount; i++){
            if(!(holder.titleIcons.getChildAt(0+skip) instanceof ImageView)){
                skip++;
                continue;
            }
            holder.titleIcons.removeViewAt(0 + skip);
        }

        int tagCount = holder.tags.getChildCount();
        for (int i = 0; i < tagCount; i++){
            holder.tags.removeViewAt(0);
        }

        for (int i = 0; i < current.getTags().get(position).length; i++){
            TextView tag = new TextView(context);
            tag.setText(current.getTags().get(position)[i]);
            tag.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
            int dpLarger = Tools.dpToPx(context, 8);
            int dpSmaller = Tools.dpToPx(context, 4);
            int dpSmall = Tools.dpToPx(context, 2);
            tag.setPadding(dpLarger,dpSmaller,dpLarger,dpSmaller);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tag.setBackground(context.getResources().getDrawable(R.drawable.rounded_spinner));
            tag.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.slightElevated, context.getTheme()));
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(dpSmall, dpSmall, dpSmall, dpSmall);
            tag.setLayoutParams(llParams);
            holder.tags.addView(tag);
        }

        for (int i = 0; i < current.getTypes().get(position).length; i++) {
            switch (Objects.requireNonNull(DreamTypes.getEnum(current.getTypes().get(position)[i]))) {
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

        holder.entry.setOnClickListener(e -> {
            Intent intent = new Intent(context, ViewJournalEntry.class);
            intent.putExtra("position", position);
            intent.putExtra("type", current.getDescriptions()[position] == null ? JournalTypes.Audio.ordinal() : JournalTypes.Text.ordinal());
            intent.putExtra("availableTags", Tools.getUniqueOnly(getAllTags()));
            intent.putExtra("entryId", current.getEntryIds()[position]);
            intent.putExtra("date", current.getDates()[position]);
            intent.putExtra("time", current.getTimes()[position]);
            intent.putExtra("title", current.getTitles()[position]);
            intent.putExtra("description", current.getDescriptions()[position]);
            intent.putExtra("quality", current.getSleepQualities()[position]);
            intent.putExtra("clarity", current.getDreamClarities()[position]);
            intent.putExtra("mood", current.getDreamMoods()[position]);
            intent.putExtra("dreamTypes", current.getTypes().get(position));
            intent.putExtra("tags", current.getTags().get(position));
            intent.putExtra("recordings", current.getAudioLocations().get(position));
            journalList.viewEntryActivityResultLauncher.launch(intent);
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
            this.entries = entries;
        }
        else {
            this.filteredEntries = entries;
        }
        notifyDataSetChanged();
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

    public List<String[]> getAllTags() {
        return entries.getTags();
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView title, description, dateTime;
        LinearLayout titleIcons;
        FlexboxLayout tags;
        ConstraintLayout entry;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            description = itemView.findViewById(R.id.txt_description);
            dateTime = itemView.findViewById(R.id.txt_date_time);
            titleIcons = itemView.findViewById(R.id.ll_title_icons);
            tags = itemView.findViewById(R.id.ll_tags);
            entry = itemView.findViewById(R.id.csl_entry);
        }
    }
}
