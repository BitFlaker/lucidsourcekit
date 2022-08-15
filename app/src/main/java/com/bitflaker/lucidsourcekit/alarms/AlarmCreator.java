package com.bitflaker.lucidsourcekit.alarms;

import static android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED;
import static com.bitflaker.lucidsourcekit.alarms.AlarmItem.AlarmToneType.RINGTONE;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.clock.SleepClock;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.main.AlarmData;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmCreator extends AppCompatActivity {
    MaterialButton setAlarm;
    MaterialCardView tone, volume, volumeIncrease, vibrate, flashlight, alarmTimeCard, bedtimeTimeCard;
    ChipGroup alarmToneGroup;
    Chip ringtoneChip, customFileChip;
    TextView selectedToneText, incVolumeFor, currAlarmVolume, repeatPatternText;
    Slider alarmVolume;
    SwitchMaterial vibrateAlarm, useFlashlight;
    LinearLayout weekdaysContainer;
    EditText alarmTitle;
    SleepClock sleepClock;
    private final static int PERMISSION_REQUEST_CODE = 776;
    private final static boolean[] everydayRepeatPattern = new boolean[] { true, true, true, true, true, true, true };
    private final static boolean[] allWeekdaysRepeatPattern = new boolean[] { true, true, true, true, true, false, false };
    private final static boolean[] allWeekendsRepeatPattern = new boolean[] { false, false, false, false, false, true, true };
    private final static boolean[] noRepeatPattern = new boolean[] { false, false, false, false, false, false, false };
    private final static String[] supportedAudioFiles = new String[] { "3gp", "m4a", "aac", "ts", "amr", "flac", "mid", "xmf", "mxmf", "rtttl", "rtx", "ota", "imy", "mp3", "mkv", "ogg", "wav" };
    private Chip[] weekdayChips;
    private Integer[] weekdays = new Integer[] { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };
    private final static String[] weekdayShorts = new String[] { "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su" };
    private int currentVolIncMin = 2, currentVolIncSec = 30;
    private Uri ringtoneUri;
    private AlarmItem alarmItem = new AlarmItem();
    ActivityResultLauncher<Intent> ringtoneSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                ringtoneUri = result.getData() != null ? result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                String title = ringtone.getTitle(this);
                selectedToneText.setText(title);
                alarmItem.setAlarmUri(ringtoneUri);
            });
    ActivityResultLauncher<Intent> customFileSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getData() != null){
                    Uri uri = result.getData().getData();
                    File file = new File(uri.getPath());
                    String filename = file.getName();
                    String extension = filename.substring(filename.lastIndexOf(".")+1);
                    if(Arrays.asList(supportedAudioFiles).contains(extension)){
                        ringtoneUri = uri;
                        String title = filename.substring(0, filename.lastIndexOf("."));
                        selectedToneText.setText(title);
                        alarmItem.setAlarmUri(ringtoneUri);
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_ALARM)
                                        .build()
                        );
                        mediaPlayer.setLooping(true);
                        try {
                            mediaPlayer.setDataSource(uri.getPath());
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mediaPlayer.start();
                    }
                    else {
                        Toast.makeText(this, "Unsupported audio file type", Toast.LENGTH_LONG).show();
                    }
                }
            });
    ActivityResultLauncher<Intent> scheduleAlarmSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getData() == null || !result.getData().getAction().equals(ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
                    Toast.makeText(this, "Permission required to schedule alarms", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
    ActivityResultLauncher<Intent> drawOverOtherAppsSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission required to display alarms", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    // TODO add option for auto stop alarm after some time
    // TODO maybe make snooze delay individually changeable in every alarm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_creator);
        Tools.makeStatusBarTransparent(this);
        findViewById(R.id.ll_alarm_creator_heading).setLayoutParams(Tools.addLinearLayoutParamsTopStatusbarSpacing(this, ((LinearLayout.LayoutParams) findViewById(R.id.ll_alarm_creator_heading).getLayoutParams())));

        sleepClock = findViewById(R.id.slp_clk_set_time);
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
        bedtimeTimeCard = findViewById(R.id.crd_bedtime_time);
        alarmTimeCard = findViewById(R.id.crd_alarm_time);
        repeatPatternText = findViewById(R.id.txt_repeat_pattern_text);
        alarmTitle = findViewById(R.id.txt_alarm_name);

        weekdayChips = new Chip[] {
            findViewById(R.id.chp_monday),
            findViewById(R.id.chp_tuesday),
            findViewById(R.id.chp_wednesday),
            findViewById(R.id.chp_thursday),
            findViewById(R.id.chp_friday),
            findViewById(R.id.chp_saturday),
            findViewById(R.id.chp_sunday)
        };

        useFlashlight.setOnTouchListener((v, event) -> {
            View par = ((View) v.getParent().getParent());
            par.onTouchEvent(event);
            return true;
        });
        vibrateAlarm.setOnTouchListener((v, event) -> {
            View par = ((View) v.getParent().getParent());
            par.onTouchEvent(event);
            return true;
        });

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            drawOverOtherAppsSettingsLauncher.launch(intent);
        }

        for (int i = 0; i < weekdayChips.length; i++) {
            Chip currentButton = weekdayChips[i];
            int finalI = i;
            currentButton.setOnClickListener(view -> {
                if(currentButton.isChecked()){
                    alarmItem.addAlarmRepeatWeekdays(weekdays[finalI]);
                }
                else {
                    alarmItem.removeAlarmRepeatWeekdays(weekdays[finalI]);
                }
                updateAlarmRepeatText();
            });
        }

        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        String title = ringtone.getTitle(this);
        selectedToneText.setText(title);

        if(getIntent().hasExtra("ALARM_ID")){
            int alarmId = getIntent().getIntExtra("ALARM_ID", -1);
            alarmItem = AlarmStorage.getInstance(this).getAlarmItemWithId(alarmId);
            AlarmTools.cancelAlarm(getApplicationContext(), alarmItem);
            setEditValuesFromItem();
            sleepClock.setOnFirstDrawFinishedListener(() -> {
                sleepClock.setAlarmTime(alarmItem.getAlarmHour(), alarmItem.getAlarmMinute());
                sleepClock.setBedTime(alarmItem.getBedtimeHour(), alarmItem.getBedtimeMinute());
                setTimeChangedListeners(bedtime, alarmTime);
            });
        }
        else {
            setCurrentAlarmValues(sleepClock);
            setTimeChangedListeners(bedtime, alarmTime);
        }

        bedtimeTimeCard.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setBedTime(hourFrom, minuteFrom), sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime(), true).show());
        alarmTimeCard.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> sleepClock.setAlarmTime(hourFrom, minuteFrom), sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm(), true).show());
        sleepClock.setDrawHours(true);
        sleepClock.setDrawTimeSetterButtons(true);

        closeCreator.setOnClickListener(e -> finish());

        alarmToneGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if(ringtoneChip.isChecked()) {
                alarmItem.setAlarmToneType(RINGTONE);
                selectedToneText.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
                alarmItem.setAlarmUri(ringtoneUri);
            }
            else if (customFileChip.isChecked()) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
                }
                alarmItem.setAlarmToneType(AlarmItem.AlarmToneType.CUSTOM_FILE);
                selectedToneText.setText("- NONE -");
                alarmItem.setAlarmUri(Uri.EMPTY);
            }
        });
        tone.setOnClickListener(e -> {
            if(ringtoneChip.isChecked()){
                final Uri currentTone =  ringtoneUri != null ? ringtoneUri : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);//RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtoneSelectorLauncher.launch(intent);
            }
            else {
                Intent audioPicker = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                customFileSelectorLauncher.launch(audioPicker);
            }
        });
        alarmVolume.addOnChangeListener((slider, value, fromUser) -> {
            currAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", (int)Math.round(value * 100)));
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
            alarmItem.setVibrate(vibrateAlarm.isChecked());
        });
        flashlight.setOnClickListener(e -> {
            useFlashlight.setChecked(!useFlashlight.isChecked());
            alarmItem.setUseFlashlight(useFlashlight.isChecked());
        });

        setAlarm.setOnClickListener(e -> {
            // TODO: make checks (like no tone selected with custom file)
            alarmItem.setTitle(alarmTitle.getText().toString());
            alarmItem.setActive(true);
            if(alarmItem.getAlarmId() == -1) {
                AlarmStorage.getInstance(this).addAlarm(alarmItem);
            }
            else {
                AlarmStorage.getInstance(this).modifyAlarm(alarmItem);
            }
            AlarmTools.scheduleAlarm(getApplicationContext(), alarmItem);
            finish();
        });
        setAlarm.setOnLongClickListener(e -> {
            alarmItem.setActive(true);
            AlarmStorage.getInstance(this).removeAlarm(alarmItem);
            finish();
            return true;
        });

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()){
                scheduleAlarmSettingsLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }
    }

    private void setTimeChangedListeners(TextView bedtime, TextView alarmTime) {
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
    }

    private void setEditValuesFromItem() {
        ringtoneChip.setChecked(alarmItem.getAlarmToneType() == RINGTONE);
        customFileChip.setChecked(alarmItem.getAlarmToneType() == AlarmItem.AlarmToneType.CUSTOM_FILE);
        ringtoneUri = alarmItem.getAlarmUri();
        String title;
        switch(alarmItem.getAlarmToneType()){
            case RINGTONE:
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                title = ringtone.getTitle(this);
                selectedToneText.setText(title);
                break;
            case CUSTOM_FILE:
                File file = new File(ringtoneUri.getPath());
                String filename = file.getName();
                title = filename.substring(0, filename.lastIndexOf("."));
                selectedToneText.setText(title);
                break;
        }
        alarmVolume.setValue(alarmItem.getAlarmVolume() / 100.0f);
        currAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", (int)Math.round(alarmVolume.getValue() * 100)));
        currentVolIncMin = alarmItem.getAlarmVolumeIncreaseMinutes();
        currentVolIncSec = alarmItem.getAlarmVolumeIncreaseSeconds();
        incVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        useFlashlight.setChecked(alarmItem.isUseFlashlight());
        vibrateAlarm.setChecked(alarmItem.isVibrate());
        alarmTitle.setText(alarmItem.getTitle());
        alarmItem.getAlarmRepeatWeekdays().forEach(weekday -> {
            weekdayChips[weekday-2 < 0 ? AlarmData.ActiveDays.SUNDAY.ordinal() : weekday-2].setChecked(true);
            updateAlarmRepeatText();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                ringtoneChip.setChecked(true);
            }
        }
    }

    private void updateAlarmRepeatText() {
        boolean[] activePattern = getCurrentRepeatPattern();
        if (Arrays.equals(activePattern, everydayRepeatPattern)) {
            repeatPatternText.setText("Repeat every day");
        }
        else if (Arrays.equals(activePattern, allWeekdaysRepeatPattern)) {
            repeatPatternText.setText("Repeat every weekday");
        }
        else if (Arrays.equals(activePattern, allWeekendsRepeatPattern)) {
            repeatPatternText.setText("Repeat every weekend");
        }
        else if (Arrays.equals(activePattern, noRepeatPattern)) {
            repeatPatternText.setText("Repeat only once");
        }
        else {
            List<String> activeWeekdays = new ArrayList<>();
            for (int i = 0; i < activePattern.length; i++) {
                if(activePattern[i] == true){
                    activeWeekdays.add(weekdayShorts[i]);
                }
            }
            repeatPatternText.setText(String.format(Locale.ENGLISH, "Repeat every %s", String.join(", ", activeWeekdays)));
        }
    }

    private boolean[] getCurrentRepeatPattern() {
        boolean[] activePattern = new boolean[7];
        for (int i = 0; i < weekdayChips.length; i++) {
            activePattern[i] = weekdayChips[i].isChecked();
        }
        return activePattern;
    }

    private void setCurrentAlarmValues(SleepClock sleepClock) {
        alarmItem.setBedtimeHour(sleepClock.getHoursToBedTime());
        alarmItem.setBedtimeMinute(sleepClock.getMinutesToBedTime());
        alarmItem.setAlarmHour(sleepClock.getHoursToAlarm());
        alarmItem.setAlarmMinute(sleepClock.getMinutesToAlarm());
        if(ringtoneChip.isChecked()) {
            alarmItem.setAlarmToneType(RINGTONE);
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
        alarmItem.setTitle("");
    }

    private void setVolumeIncreaseTime(int minutes, int seconds) {
        currentVolIncMin = minutes;
        currentVolIncSec = seconds;
        incVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        alarmItem.setAlarmVolumeIncreaseMinutes(currentVolIncMin);
        alarmItem.setAlarmVolumeIncreaseSeconds(currentVolIncSec);
    }
}