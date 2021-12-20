package com.bitflaker.lucidsourcekit.main;

import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bitflaker.lucidsourcekit.R;
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
    Context context;

    public RecyclerViewAdapter(Context context, String[] dates, String[] times, String[] titles, String[] descriptions, List<String[]> tags, String[] sleepQualities, String[] dreamClarity, String[] moods, List<String[]> sleepTypes, List<String[]> audioLocations) {
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

    public void addEntry(String date, String time, String title, String description, String[] tags, String sleepQuality, String dreamClarity, String mood, String[] sleepTypes, String[] audioLocations) {
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
        holder.description.setText(descriptions[position]);

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
            case CrystalClear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_4_24)); break;
            case Clear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_5_24)); break;
            case Cloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_6_24)); break;
            case VeryCloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_7_24)); break;
        }
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

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            description = itemView.findViewById(R.id.txt_description);
            dateTime = itemView.findViewById(R.id.txt_date_time);
            titleIcons = itemView.findViewById(R.id.ll_title_icons);
            tags = itemView.findViewById(R.id.ll_tags);
        }
    }
}
