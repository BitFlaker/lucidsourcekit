package com.bitflaker.lucidsourcekit.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingObjectTools {
    private final Context context;

    public RecordingObjectTools(Context context) {
        this.context = context;
    }

    public ImageButton generateDeleteButton() {
        ImageButton deleteButton = new ImageButton(context);
        int dp48 = Tools.dpToPx(context, 48);
        int dp8 = Tools.dpToPx(context, 8);
        LinearLayout.LayoutParams lParamsDeleteButton = new LinearLayout.LayoutParams(dp48, dp48);
        lParamsDeleteButton.setMargins(0, 0, dp8, 0);
        deleteButton.setLayoutParams(lParamsDeleteButton);
        deleteButton.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_baseline_cross_24, context.getTheme()));
        deleteButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_spinner, context.getTheme()));
        deleteButton.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, context.getTheme()));
        return deleteButton;
    }

    public TextView generateDuration(AudioLocation recording, boolean extendedMarginEnd) {
        TextView duration = new TextView(context);
        int dp10 = Tools.dpToPx(context, 10);
        int dp25 = Tools.dpToPx(context, 25);
        LinearLayout.LayoutParams lParamsDuration = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if(!extendedMarginEnd){ lParamsDuration.setMargins(0, 0, dp10, 0); }
        else { lParamsDuration.setMargins(0, 0, dp25, 0); }
        duration.setLayoutParams(lParamsDuration);
        int seconds = (int)(recording.getRecordingLength() / 1000);
        int sec = seconds % 60;
        int min = (seconds / 60)%60;
        int hours = (seconds/60)/60;
        String secS = String.format(Locale.ENGLISH, "%02d" , sec);
        String minS = String.format(Locale.ENGLISH, "%02d" , min);
        String hoursS = String.format(Locale.ENGLISH, "%02d" , hours);
        duration.setText(hours > 0 ? hoursS + ":" : "" + minS + ":" + secS);
        duration.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, context.getTheme()));
        duration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return duration;
    }

    public TextView generateTimestamp(AudioLocation recording) {
        TextView timestamp = new TextView(context);
        timestamp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        Date date = Tools.calendarFromMillis(recording.recordingTimestamp).getTime();
        timestamp.setText(String.format(Locale.getDefault(), "%s • %s", df.format(date), tf.format(date)));
        timestamp.setTextColor(Tools.getAttrColorStateList(R.attr.secondaryTextColor, context.getTheme()));
        timestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return timestamp;
    }

    public TextView generateHeading() {
        TextView heading = new TextView(context);
        heading.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        heading.setText("Recording");
        heading.setTextColor(Tools.getAttrColorStateList(R.attr.primaryTextColor, context.getTheme()));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        heading.setTypeface(null, Typeface.BOLD);
        return heading;
    }

    public LinearLayout generateLabelsContrainer() {
        LinearLayout labelsContainer = new LinearLayout(context);
        int dp10 = Tools.dpToPx(context, 10);
        int dp8 = Tools.dpToPx(context, 8);
        LinearLayout.LayoutParams lParamsLabelsContainer = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsLabelsContainer.setMargins(dp10, dp8, 0, dp8);
        lParamsLabelsContainer.weight = 1;
        labelsContainer.setLayoutParams(lParamsLabelsContainer);
        labelsContainer.setOrientation(LinearLayout.VERTICAL);
        return labelsContainer;
    }

    public LinearLayout generateContainerLayout() {
        LinearLayout entryContainer = new LinearLayout(context);
        int dp20 = Tools.dpToPx(context, 20);
        int dp5 = Tools.dpToPx(context, 5);
        LinearLayout.LayoutParams lParamsEntryContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lParamsEntryContainer.setMargins(dp20, dp5, dp20, dp5);
        entryContainer.setLayoutParams(lParamsEntryContainer);
        entryContainer.setOrientation(LinearLayout.HORIZONTAL);
        entryContainer.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_spinner, context.getTheme()));
        entryContainer.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, context.getTheme()));
        entryContainer.setGravity(Gravity.CENTER_VERTICAL);
        return entryContainer;
    }

    public ImageButton generatePlayButton() {
        ImageButton playButton = new ImageButton(context);
        int dp48 = Tools.dpToPx(context, 48);
        int dp8 = Tools.dpToPx(context, 8);
        LinearLayout.LayoutParams lParamsPlayButton = new LinearLayout.LayoutParams(dp48, dp48);
        lParamsPlayButton.setMargins(dp8, 0, 0, 0);
        playButton.setLayoutParams(lParamsPlayButton);
        playButton.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_baseline_play_arrow_24, context.getTheme()));
        playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.rounded_spinner, context.getTheme()));
        playButton.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerHigh, context.getTheme()));
        return playButton;
    }
}
