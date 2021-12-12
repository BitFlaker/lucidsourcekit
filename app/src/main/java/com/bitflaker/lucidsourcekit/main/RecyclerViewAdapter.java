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
import com.bitflaker.lucidsourcekit.general.DreamClarity;
import com.bitflaker.lucidsourcekit.general.DreamMoods;
import com.bitflaker.lucidsourcekit.general.SleepQuality;
import com.bitflaker.lucidsourcekit.general.Tools;

import java.text.MessageFormat;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MainViewHolder> {
    String[] dates, times, titles, descriptions;
    String[][] tags;
    int[] moods, sleepQualities, dreamClarity, sleepTypes;
    boolean[] lucid, nightmare;
    Context context;

    public RecyclerViewAdapter(Context context, String[] dates, String[] times, String[] titles, String[] descriptions, String[][] tags, int[] moods, int[] sleepQualities, int[] dreamClarity, int[] sleepTypes, boolean[] lucid, boolean[] nightmare) {
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
        this.lucid = lucid;
        this.nightmare = nightmare;
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
        for (int i = 0; i < tags[position].length; i++){
            TextView tag = new TextView(context);
            tag.setText(tags[position][i]);
            tag.setTextColor(Color.parseColor("#ffffff"));
            int dp5 = Tools.dpToPx(context, 5);
            int dp2 = Tools.dpToPx(context, 2);
            tag.setPadding(dp5,dp2,dp5,dp2);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tag.setBackground(context.getResources().getDrawable(R.drawable.rounded_spinner));
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(dp2, 0, dp2, 0);
            tag.setLayoutParams(llParams);
            holder.tags.addView(tag);
        }
        if(lucid[position]){
            holder.titleIcons.addView(generateIconHighlight(R.drawable.ic_baseline_deblur_24));
        }
        switch (DreamMoods.values()[moods[position]]){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_satisfied_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_satisfied_24)); break;
            case Ok: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_neutral_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_dissatisfied_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_sentiment_very_dissatisfied_24)); break;
        }
        switch (SleepQuality.values()[sleepQualities[position]]){
            case Outstanding: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_stars_24)); break;
            case Great: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_24)); break;
            case Poor: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_half_24)); break;
            case Terrible: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_star_border_24)); break;
        }
        switch (DreamClarity.values()[dreamClarity[position]]){
            case CrystalClear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_4_24)); break;
            case Clear: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_5_24)); break;
            case Cloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_6_24)); break;
            case VeryCloudy: holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_brightness_7_24)); break;
        }
        if(nightmare[position]){
            holder.titleIcons.addView(generateIcon(R.drawable.ic_baseline_priority_high_24));
        }
    }

    private View generateIcon(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColor(R.attr.inactivePageDot, context.getTheme());
        icon.setImageTintList(stateList);
        icon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return icon;
    }

    private View generateIconHighlight(int iconId) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconId);
        ColorStateList stateList = Tools.getAttrColor(R.attr.activePageDot, context.getTheme());
        icon.setImageTintList(stateList);
        icon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return icon;
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public class MainViewHolder extends RecyclerView.ViewHolder{
        TextView title, description, dateTime;
        LinearLayout titleIcons, tags;

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
