package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.database.values.DreamClarity;
import com.bitflaker.lucidsourcekit.general.database.values.DreamMoods;
import com.bitflaker.lucidsourcekit.general.database.values.DreamTypes;
import com.bitflaker.lucidsourcekit.general.database.values.SleepQuality;
import com.google.android.flexbox.FlexboxLayout;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MainViewHolder> {
    String[] dates, times, titles, descriptions, moods, sleepQualities, dreamClarity;
    List<String[]> tags, audioLocations, sleepTypes;
    Integer[] entryIds;
    Context context;
    DreamJournal journalList;

    public RecyclerViewAdapter(DreamJournal journalList, Context context, Integer[] entryIds, String[] dates, String[] times, String[] titles, String[] descriptions, List<String[]> tags, String[] sleepQualities, String[] dreamClarity, String[] moods, List<String[]> sleepTypes, List<String[]> audioLocations) {
        this.entryIds = entryIds;
        this.journalList = journalList;
        this.dates = dates;
        this.times = times;
        this.titles = titles;
        this.descriptions = descriptions;
        this.tags = tags;
        this.moods = moods;
        this.sleepQualities = sleepQualities;
        this.dreamClarity = dreamClarity;
        this.sleepTypes = sleepTypes;
        this.context = context;
        this.audioLocations = audioLocations;
    }

    public void addEntry(Integer entryId, String date, String time, String title, String description, String[] tags, String sleepQuality, String dreamClarity, String mood, String[] sleepTypes, String[] audioLocations) {
        this.entryIds = Tools.addFirst(this.entryIds, entryId);
        this.dates = Tools.addFirst(this.dates, date);
        this.times = Tools.addFirst(this.times, time);
        this.titles = Tools.addFirst(this.titles, title);
        this.descriptions = Tools.addFirst(this.descriptions, description);
        this.tags.add(0, tags);
        this.moods = Tools.addFirst(this.moods, mood);
        this.sleepQualities = Tools.addFirst(this.sleepQualities, sleepQuality);
        this.dreamClarity = Tools.addFirst(this.dreamClarity, dreamClarity);
        this.sleepTypes.add(0, sleepTypes);
        this.audioLocations.add(0, audioLocations);
    }

    public void changeEntryAt(int position, String date, String time, String title, String description, String[] tags, String sleepQuality, String dreamClarity, String mood, String[] sleepTypes, String[] audioLocations) {
        this.dates[position] = date;
        this.times[position] = time;
        this.titles[position] = title;
        this.descriptions[position] = description;
        this.moods[position] = mood;
        this.sleepQualities[position] = sleepQuality;
        this.dreamClarity[position] = dreamClarity;
        this.tags.set(position, tags);
        this.sleepTypes.set(position, sleepTypes);
        this.audioLocations.set(position, audioLocations);
        // TODO after removing item: still works correctly?
    }

    public void removeEntryAt(int position) {
        this.entryIds = Tools.removeAt(this.entryIds, position);
        this.dates = Tools.removeAt(this.dates, position);
        this.times = Tools.removeAt(this.times, position);
        this.titles = Tools.removeAt(this.titles, position);
        this.descriptions = Tools.removeAt(this.descriptions, position);
        this.moods = Tools.removeAt(this.moods, position);
        this.sleepQualities = Tools.removeAt(this.sleepQualities, position);
        this.dreamClarity = Tools.removeAt(this.dreamClarity, position);
        this.tags.remove(position);
        this.sleepTypes.remove(position);
        this.audioLocations.remove(position);
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
        holder.dateTime.setText(MessageFormat.format("{0} {1} {2}", dates[position], context.getResources().getString(R.string.journal_time_at), times[position]));
        holder.title.setText(titles[position]);
        if(descriptions[position] == null){
            holder.description.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_mic_24, 0, 0, 0);
            int audioCount = audioLocations.get(position).length;
            holder.description.setText(audioCount == 1 ? "1 " + context.getResources().getString(R.string.recording_single) : audioCount + " " + context.getResources().getString(R.string.recording_multiple));
            ConstraintLayout.LayoutParams lparamsDesc = (ConstraintLayout.LayoutParams) holder.description.getLayoutParams();
            int slightMargin = Tools.dpToPx(context, 10);
            int verySlightMargin = Tools.dpToPx(context, 5);
            lparamsDesc.setMargins(-verySlightMargin, slightMargin, 0, slightMargin);
            holder.description.setLayoutParams(lparamsDesc);
        }
        else {
            holder.description.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            holder.description.setText(descriptions[position]);
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

        for (int i = 0; i < tags.get(position).length; i++){
            TextView tag = new TextView(context);
            tag.setText(tags.get(position)[i]);
            tag.setTextColor(Color.parseColor("#ffffff"));
            int dpLarger = Tools.dpToPx(context, 8);
            int dpSmaller = Tools.dpToPx(context, 4);
            int dpSmall = Tools.dpToPx(context, 2);
            tag.setPadding(dpLarger,dpSmaller,dpLarger,dpSmaller);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tag.setBackground(context.getResources().getDrawable(R.drawable.rounded_spinner));
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(dpSmall, dpSmall, dpSmall, dpSmall);
            tag.setLayoutParams(llParams);
            holder.tags.addView(tag);
        }

        for (int i = 0; i < sleepTypes.get(position).length; i++) {
            switch (Objects.requireNonNull(DreamTypes.getEnum(sleepTypes.get(position)[i]))) {
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

        switch (Objects.requireNonNull(DreamMoods.getEnum(moods[position]))){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_satisfied_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_satisfied_24)); break;
            case Ok: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_neutral_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_dissatisfied_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_dissatisfied_24)); break;
        }
        switch (Objects.requireNonNull(SleepQuality.getEnum(sleepQualities[position]))){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_stars_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_half_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_border_24)); break;
        }
        switch (Objects.requireNonNull(DreamClarity.getEnum(dreamClarity[position]))){
            case CrystalClear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_7_24)); break;
            case Clear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_6_24)); break;
            case Cloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_5_24)); break;
            case VeryCloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_4_24)); break;
        }

        holder.entry.setOnClickListener(e -> {
            Intent intent = new Intent(context, ViewJournalEntry.class);
            intent.putExtra("position", position);
            intent.putExtra("type", descriptions[position] == null ? JournalTypes.Audio.ordinal() : JournalTypes.Text.ordinal());
            intent.putExtra("entryId", entryIds[position]);
            intent.putExtra("date", dates[position]);
            intent.putExtra("time", times[position]);
            intent.putExtra("title", titles[position]);
            intent.putExtra("description", descriptions[position]);
            intent.putExtra("quality", sleepQualities[position]);
            intent.putExtra("clarity", dreamClarity[position]);
            intent.putExtra("mood", moods[position]);
            intent.putExtra("dreamTypes", sleepTypes.get(position));
            intent.putExtra("tags", tags.get(position));
            intent.putExtra("recordings", audioLocations.get(position));
            //context.startActivity(intent);
            journalList.viewEntryActivityResultLauncher.launch(intent);
        });
    }

    private View generateIcon(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColor(R.attr.inactivePageDot, context.getTheme());
        icon.setImageTintList(stateList);
        int size = Tools.dpToPx(context, 20);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private View generateIconHighlight(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColor(R.attr.activePageDot, context.getTheme());
        icon.setImageTintList(stateList);
        int size = Tools.dpToPx(context, 20);
        icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return icon;
    }

    @Override
    public int getItemCount() {
        return titles.length;
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
