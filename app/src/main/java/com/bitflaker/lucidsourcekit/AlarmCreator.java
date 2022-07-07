package com.bitflaker.lucidsourcekit;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.clock.SleepClock;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class AlarmCreator extends AppCompatActivity {

    MaterialCardView tone, volume, volumeIncrease, vibrate, flashlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_creator);
        Tools.makeStatusBarTransparent(this);
        findViewById(R.id.btn_close_alarm_creator).setLayoutParams(Tools.addLinearLayoutParamsTopStatusbarSpacing(this, ((LinearLayout.LayoutParams) findViewById(R.id.btn_close_alarm_creator).getLayoutParams())));

        SleepClock sleepClock = findViewById(R.id.slp_clk_set_time);
        TextView bedtime = findViewById(R.id.txt_time_bedtime);
        TextView alarmTime = findViewById(R.id.txt_time_alarm);
        ImageButton closeCreator = findViewById(R.id.btn_close_alarm_creator);
        tone = findViewById(R.id.crd_alarm_tone);
        volume = findViewById(R.id.crd_alarm_volume);
        volumeIncrease = findViewById(R.id.crd_alarm_volume_increase);
        vibrate = findViewById(R.id.crd_alarm_vibrate);
        flashlight = findViewById(R.id.crd_alarm_use_flashlight);

        bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime()));
        alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm()));
        sleepClock.setOnBedtimeChangedListener((hours, minutes) -> bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes)));
        sleepClock.setOnAlarmTimeChangedListener((hours, minutes) -> alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes)));

        bedtime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setBedTime(hourFrom, minuteFrom), sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime(), true).show());
        alarmTime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setAlarmTime(hourFrom, minuteFrom), sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm(), true).show());
        sleepClock.setDrawHours(true);
        sleepClock.setDrawTimeSetterButtons(true);

        closeCreator.setOnClickListener(e -> finish());

        tone.setOnClickListener(e -> {});
        volume.setOnClickListener(e -> {});
        volumeIncrease.setOnClickListener(e -> {});
        vibrate.setOnClickListener(e -> {});
        flashlight.setOnClickListener(e -> {});
    }
}