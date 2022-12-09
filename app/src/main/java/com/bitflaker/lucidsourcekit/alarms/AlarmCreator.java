package com.bitflaker.lucidsourcekit.alarms;

import static android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED;
import static com.bitflaker.lucidsourcekit.alarms.AlarmItem.AlarmToneType.CUSTOM_FILE;
import static com.bitflaker.lucidsourcekit.alarms.AlarmItem.AlarmToneType.RINGTONE;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.clock.SleepClock;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
    private final static boolean[] noRepeatPattern = new boolean[] { false, false, false, false, false, false, false };
    private final static boolean[] everydayRepeatPattern = new boolean[] { true, true, true, true, true, true, true };
    private final static boolean[] allWeekdaysRepeatPattern = new boolean[] { false, true, true, true, true, true, false };
    private final static boolean[] allWeekendsRepeatPattern = new boolean[] { true, false, false, false, false, false, true };
    private final static String[] supportedAudioFiles = new String[] { "3gp", "m4a", "aac", "ts", "amr", "flac", "mid", "xmf", "mxmf", "rtttl", "rtx", "ota", "imy", "mp3", "mkv", "ogg", "wav" };
    private final static String[] weekdayShorts = new String[] { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
    private Chip[] weekdayChips;
    private int currentVolIncMin = 2, currentVolIncSec = 30;
    private Uri ringtoneUri;
    private StoredAlarm storedAlarm;
    private MainDatabase db;
    ActivityResultLauncher<Intent> ringtoneSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                ringtoneUri = result.getData() != null ? result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                String title = ringtone.getTitle(this);
                selectedToneText.setText(title);
                storedAlarm.alarmUri = ringtoneUri.toString();
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
                        storedAlarm.alarmUri = ringtoneUri.toString();
//                        MediaPlayer mediaPlayer = new MediaPlayer();
//                        mediaPlayer.setAudioAttributes(
//                                new AudioAttributes.Builder()
//                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                                        .setUsage(AudioAttributes.USAGE_ALARM)
//                                        .build()
//                        );
//                        mediaPlayer.setLooping(true);
//                        try {
//                            mediaPlayer.setDataSource(this, uri);
//                            mediaPlayer.prepare();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        mediaPlayer.start();
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
        db = MainDatabase.getInstance(this);

        weekdayChips = new Chip[] {
                findViewById(R.id.chp_sunday),
                findViewById(R.id.chp_monday),
                findViewById(R.id.chp_tuesday),
                findViewById(R.id.chp_wednesday),
                findViewById(R.id.chp_thursday),
                findViewById(R.id.chp_friday),
                findViewById(R.id.chp_saturday)
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
                storedAlarm.pattern[finalI] = currentButton.isChecked();
                updateAlarmRepeatText();
            });
        }

        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        String title = ringtone.getTitle(this);
        selectedToneText.setText(title);

        if(getIntent().hasExtra("ALARM_ID")){
            int alarmId = getIntent().getIntExtra("ALARM_ID", -1);
            db.getStoredAlarmDao().getById(alarmId).subscribe(loadedStoredAlarm -> {
                storedAlarm = loadedStoredAlarm;
                setEditValuesFromItem();
                sleepClock.setOnFirstDrawFinishedListener(() -> {
                    long alarmHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.alarmTimestamp);
                    long alarmMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmTimestamp) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(storedAlarm.alarmTimestamp));
                    long bedtimeHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.bedtimeTimestamp);
                    long bedtimeMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.bedtimeTimestamp) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(storedAlarm.bedtimeTimestamp));
                    sleepClock.setAlarmTime((int)alarmHours, (int)alarmMinutes);
                    sleepClock.setBedTime((int)bedtimeHours, (int)bedtimeMinutes);
                    setTimeChangedListeners(bedtime, alarmTime);
                });
            }).dispose();
        }
        else {
            storedAlarm = new StoredAlarm();
            storedAlarm.alarmId = 0;
            storedAlarm.requestCodeActiveAlarm = -1;
            storedAlarm.pattern = Arrays.copyOf(noRepeatPattern, noRepeatPattern.length);
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
                storedAlarm.alarmToneTypeId = RINGTONE.ordinal();
                selectedToneText.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
                storedAlarm.alarmUri = ringtoneUri.toString();
            }
            else if (customFileChip.isChecked()) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
                }
                storedAlarm.alarmToneTypeId = CUSTOM_FILE.ordinal();
                selectedToneText.setText("- NONE -");
                storedAlarm.alarmUri = Uri.EMPTY.toString();
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
                Intent audioPicker = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
                customFileSelectorLauncher.launch(audioPicker);
            }
        });
        alarmVolume.addOnChangeListener((slider, value, fromUser) -> {
            currAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", (int)Math.round(value * 100)));
            storedAlarm.alarmVolume = value;
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
            storedAlarm.isVibrationActive = vibrateAlarm.isChecked();
        });
        flashlight.setOnClickListener(e -> {
            useFlashlight.setChecked(!useFlashlight.isChecked());
            storedAlarm.isFlashlightActive = useFlashlight.isChecked();
        });

        setAlarm.setOnClickListener(e -> {
            // TODO: make checks (like no tone selected with custom file)
            storedAlarm.title = alarmTitle.getText().toString();
            storedAlarm.isAlarmActive = true;
            // TODO: make this work with one time only alarms as well
            if(storedAlarm.alarmId == 0) {
                // create the alarm and schedule it
                db.getStoredAlarmDao().insert(storedAlarm).subscribe(alarmId -> {
                    storedAlarm.alarmId = alarmId.intValue();
                    scheduleAlarmAndExit(true);
                }).dispose();
            }
            else {
                // cancel the alarm if it currently is running, then update the stored alarm in the
                // database and finally schedule the alarm
                AlarmHandler.cancelRepeatingAlarm(getApplicationContext(), storedAlarm.alarmId).subscribe(() -> {
                    storedAlarm.requestCodeActiveAlarm = -1;
                    db.getStoredAlarmDao().update(storedAlarm).subscribe(() -> scheduleAlarmAndExit(false)).dispose();
                }).dispose();
            }
        });
//        setAlarm.setOnLongClickListener(e -> {
//            alarmItem.setActive(true);
//            AlarmStorage.getInstance(this).removeAlarm(alarmItem);
//            finish();
//            return true;
//        });

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()){
                scheduleAlarmSettingsLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }
    }

    private void scheduleAlarmAndExit(boolean createdNewAlarm) {
        int index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        AlarmHandler.scheduleAlarmRepeatedlyAt(getApplicationContext(), storedAlarm.alarmId, getMillisUntilMidnight() + storedAlarm.alarmTimestamp, storedAlarm.pattern, index, 1000*60*60*24).blockingSubscribe(() -> {
            Intent intent = new Intent();
            intent.putExtra("CREATED_NEW_ALARM", createdNewAlarm);
            intent.putExtra("ALARM_ID", storedAlarm.alarmId);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private long getMillisSinceMidnight() {
        return Calendar.getInstance().getTimeInMillis() - getMillisUntilMidnight();
    }

    private long getMillisUntilMidnight() {
        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);
        midnightCalendar.set(Calendar.MILLISECOND, 0);
        return midnightCalendar.getTimeInMillis();
    }

    private void setTimeChangedListeners(TextView bedtime, TextView alarmTime) {
        bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToBedTime(), sleepClock.getMinutesToBedTime()));
        alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", sleepClock.getHoursToAlarm(), sleepClock.getMinutesToAlarm()));
        sleepClock.setOnBedtimeChangedListener((hours, minutes) -> {
            storedAlarm.bedtimeTimestamp = (long) hours * 60L * 60L * 1000L + (long) minutes * 60L * 1000L;
            bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });
        sleepClock.setOnAlarmTimeChangedListener((hours, minutes) -> {
            storedAlarm.alarmTimestamp = (long) hours * 60L * 60L * 1000L + (long) minutes * 60L * 1000L;
            alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });
    }

    private void setEditValuesFromItem() {
        ringtoneChip.setChecked(storedAlarm.alarmToneTypeId == RINGTONE.ordinal());
        customFileChip.setChecked(storedAlarm.alarmToneTypeId == CUSTOM_FILE.ordinal());
        ringtoneUri = Uri.parse(storedAlarm.alarmUri);
        String title;
        switch(AlarmItem.AlarmToneType.values()[storedAlarm.alarmToneTypeId]){
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
        alarmVolume.setValue(storedAlarm.alarmVolume);
        currAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", (int)Math.round(alarmVolume.getValue() * 100)));
        long volIncMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmVolumeIncreaseTimestamp);
        long volIncSeconds = TimeUnit.MILLISECONDS.toSeconds(storedAlarm.alarmVolumeIncreaseTimestamp) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmVolumeIncreaseTimestamp));
        currentVolIncMin = (int)volIncMinutes;
        currentVolIncSec = (int)volIncSeconds;
        incVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        useFlashlight.setChecked(storedAlarm.isFlashlightActive);
        vibrateAlarm.setChecked(storedAlarm.isVibrationActive);
        alarmTitle.setText(storedAlarm.title);
        for (int i = 0; i < storedAlarm.pattern.length; i++) {
            weekdayChips[i].setChecked(storedAlarm.pattern[i]);
        }
        updateAlarmRepeatText();
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
        storedAlarm.bedtimeTimestamp = (long) sleepClock.getHoursToBedTime() * 60L * 60L * 1000L + (long) sleepClock.getMinutesToBedTime() * 60L * 1000L;
        storedAlarm.alarmTimestamp = (long) sleepClock.getHoursToAlarm() * 60L * 60L * 1000L + (long) sleepClock.getMinutesToAlarm() * 60L * 1000L;
        storedAlarm.alarmToneTypeId = ringtoneChip.isChecked() ? RINGTONE.ordinal() : (customFileChip.isChecked() ? CUSTOM_FILE.ordinal() : -1);
        storedAlarm.alarmUri = ringtoneUri.toString();
        storedAlarm.alarmVolume = alarmVolume.getValue();
        storedAlarm.alarmVolumeIncreaseTimestamp = (long) currentVolIncMin * 60L * 1000L + (long) currentVolIncSec * 1000L;
        storedAlarm.isFlashlightActive = useFlashlight.isChecked();
        storedAlarm.isVibrationActive = vibrateAlarm.isChecked();
        storedAlarm.title = "";
    }

    private void setVolumeIncreaseTime(int minutes, int seconds) {
        currentVolIncMin = minutes;
        currentVolIncSec = seconds;
        incVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        storedAlarm.alarmVolumeIncreaseTimestamp = (long) currentVolIncMin * 60L * 1000L + (long) currentVolIncSec * 1000L;
    }
}