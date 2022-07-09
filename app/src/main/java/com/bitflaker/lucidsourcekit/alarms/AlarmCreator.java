package com.bitflaker.lucidsourcekit.alarms;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.clock.SleepClock;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Locale;

public class AlarmCreator extends AppCompatActivity {
    MaterialButton setAlarm;
    MaterialCardView tone, volume, volumeIncrease, vibrate, flashlight;
    ChipGroup alarmToneGroup;
    Chip ringtoneChip, customFileChip;
    TextView selectedToneText, incVolumeFor, currAlarmVolume;
    Slider alarmVolume;
    SwitchMaterial vibrateAlarm, useFlashlight;
    LinearLayout weekdaysContainer;
    private Integer[] weekdays = new Integer[] { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };
    private int currentVolIncMin = 2, currentVolIncSec = 30;
    private Uri ringtoneUri;
    private AlarmItem alarmItem = new AlarmItem();
    ActivityResultLauncher<Intent> ringtoneSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                ringtoneUri = result.getData() != null ? result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                String title = ringtone.getTitle(this);
                selectedToneText.setText(title);
                alarmItem.setAlarmUri(ringtoneUri);
        System.out.println(ringtoneUri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_creator);
        Tools.makeStatusBarTransparent(this);
        findViewById(R.id.ll_alarm_creator_heading).setLayoutParams(Tools.addLinearLayoutParamsTopStatusbarSpacing(this, ((LinearLayout.LayoutParams) findViewById(R.id.ll_alarm_creator_heading).getLayoutParams())));

        SleepClock sleepClock = findViewById(R.id.slp_clk_set_time);
        TextView bedtime = findViewById(R.id.txt_time_bedtime);
        TextView alarmTime = findViewById(R.id.txt_time_alarm);
        ImageButton closeCreator = findViewById(R.id.btn_close_alarm_creator);
        tone = findViewById(R.id.crd_alarm_tone);
        volume = findViewById(R.id.crd_alarm_volume);
        volumeIncrease = findViewById(R.id.crd_alarm_volume_increase);
        vibrate = findViewById(R.id.crd_alarm_vibrate);
        flashlight = findViewById(R.id.crd_alarm_use_flashlight);
        alarmToneGroup = findViewById(R.id.chp_grp_alarm_tone);
        customFileChip = findViewById(R.id.chp_custom_file);
        ringtoneChip = findViewById(R.id.chp_ringtone);
        selectedToneText = findViewById(R.id.txt_tone_selected);
        incVolumeFor = findViewById(R.id.txt_inc_volume_for);
        alarmVolume = findViewById(R.id.sld_alarm_volume);
        currAlarmVolume = findViewById(R.id.txt_curr_alarm_volume);
        vibrateAlarm = findViewById(R.id.swt_vibrate_alarm);
        useFlashlight = findViewById(R.id.swt_alarm_use_flashlight);
        setAlarm = findViewById(R.id.btn_create_alarm);
        weekdaysContainer = findViewById(R.id.ll_weekdays_container);

        for (int i = 0; i < weekdaysContainer.getChildCount(); i++) {
            Chip currentButton = (Chip) weekdaysContainer.getChildAt(i);
            int finalI = i;
            currentButton.setOnClickListener(view -> {
                if(currentButton.isChecked()){
                    alarmItem.addAlarmRepeatWeekdays(weekdays[finalI]);
                }
                else {
                    alarmItem.removeAlarmRepeatWeekdays(weekdays[finalI]);
                }
            });
        }

        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        String title = ringtone.getTitle(this);
        selectedToneText.setText(title);

        setCurrentAlarmValues(sleepClock);

        bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime()));
        alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm()));
        sleepClock.setOnBedtimeChangedListener((hours, minutes) -> {
            alarmItem.setBedtimeHour(hours);
            alarmItem.setBedtimeMinute(minutes);
            bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });
        sleepClock.setOnAlarmTimeChangedListener((hours, minutes) -> {
            alarmItem.setAlarmHour(hours);
            alarmItem.setAlarmMinute(minutes);
            alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });

        bedtime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setBedTime(hourFrom, minuteFrom), sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime(), true).show());
        alarmTime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setAlarmTime(hourFrom, minuteFrom), sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm(), true).show());
        sleepClock.setDrawHours(true);
        sleepClock.setDrawTimeSetterButtons(true);

        closeCreator.setOnClickListener(e -> finish());

        alarmToneGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if(ringtoneChip.isChecked()) {
                alarmItem.setAlarmToneType(AlarmItem.AlarmToneType.RINGTONE);
            }
            else if (customFileChip.isChecked()){
                alarmItem.setAlarmToneType(AlarmItem.AlarmToneType.CUSTOM_FILE);
            }
        });
        tone.setOnClickListener(e -> {
            if(ringtoneChip.isChecked()){
                final Uri currentTone =  ringtoneUri != null ? ringtoneUri : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);//RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtoneSelectorLauncher.launch(intent);
            }
        });
        alarmVolume.addOnChangeListener((slider, value, fromUser) -> {
            currAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", (int)(value * 100)));
            alarmItem.setAlarmVolume((int)(value * 100));
        });
        volumeIncrease.setOnClickListener(e -> {
            final LinearLayout container = new LinearLayout(this);
            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(lParams);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER);
            final NumberPicker minuteNumberPicker = new NumberPicker(this);
            minuteNumberPicker.setMaxValue(59);
            minuteNumberPicker.setMinValue(0);
            minuteNumberPicker.setValue(currentVolIncMin);
            final TextView doublePoint = new TextView(this);
            doublePoint.setText(":");
            LinearLayout.LayoutParams lParamsMin = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lParamsMin.leftMargin = Tools.dpToPx(this, 5);
            lParamsMin.rightMargin = Tools.dpToPx(this, 5);
            doublePoint.setLayoutParams(lParamsMin);
            final NumberPicker secondsNumberPicker = new NumberPicker(this);
            secondsNumberPicker.setMaxValue(59);
            secondsNumberPicker.setMinValue(0);
            secondsNumberPicker.setValue(currentVolIncSec);
            container.addView(minuteNumberPicker);
            container.addView(doublePoint);
            container.addView(secondsNumberPicker);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(container);
            builder.setTitle("Increase volume for");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> setVolumeIncreaseTime(minuteNumberPicker.getValue(), secondsNumberPicker.getValue()));
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        vibrate.setOnClickListener(e -> {
            vibrateAlarm.setChecked(!vibrateAlarm.isChecked());
            alarmItem.setVibrate(vibrate.isChecked());
        });
        flashlight.setOnClickListener(e -> {
            useFlashlight.setChecked(!useFlashlight.isChecked());
            alarmItem.setUseFlashlight(useFlashlight.isChecked());
        });

        setAlarm.setOnClickListener(e -> {
            // TODO: make checks (like no tone selected with custom file)
            alarmItem.setActive(true);
            AlarmStorage.getInstance(this).addAlarm(alarmItem);
            finish();
        });
        setAlarm.setOnLongClickListener(e -> {
            alarmItem.setActive(true);
            AlarmStorage.getInstance(this).removeAlarm(alarmItem);
            finish();
            return true;
        });
    }

    private void setCurrentAlarmValues(SleepClock sleepClock) {
        alarmItem.setBedtimeHour(sleepClock.getHoursToBedTime());
        alarmItem.setBedtimeMinute(sleepClock.getMinutesToBedTime());
        alarmItem.setAlarmHour(sleepClock.getHoursToAlarm());
        alarmItem.setAlarmMinute(sleepClock.getMinutesToAlarm());
        if(ringtoneChip.isChecked()) {
            alarmItem.setAlarmToneType(AlarmItem.AlarmToneType.RINGTONE);
        }
        else if (customFileChip.isChecked()){
            alarmItem.setAlarmToneType(AlarmItem.AlarmToneType.CUSTOM_FILE);
        }
        alarmItem.setAlarmUri(ringtoneUri);
        alarmItem.setAlarmVolume((int)(alarmVolume.getValue() * 100));
        alarmItem.setAlarmVolumeIncreaseMinutes(currentVolIncMin);
        alarmItem.setAlarmVolumeIncreaseSeconds(currentVolIncSec);
        alarmItem.setUseFlashlight(useFlashlight.isChecked());
        alarmItem.setVibrate(vibrate.isChecked());
    }

    private void setVolumeIncreaseTime(int minutes, int seconds) {
        currentVolIncMin = minutes;
        currentVolIncSec = seconds;
        incVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        alarmItem.setAlarmVolumeIncreaseMinutes(currentVolIncMin);
        alarmItem.setAlarmVolumeIncreaseSeconds(currentVolIncSec);
    }
}