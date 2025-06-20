package com.bitflaker.lucidsourcekit.main.alarms;

import static android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.AlarmToneType;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.databinding.ActivityAlarmEditorBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.views.SleepClock;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AlarmEditorView extends AppCompatActivity {
    private ActivityAlarmEditorBinding binding;

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
                binding.txtToneSelected.setText(title);
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
                        binding.txtToneSelected.setText(title);
                        storedAlarm.alarmUri = ringtoneUri.toString();
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
        binding = ActivityAlarmEditorBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = MainDatabase.getInstance(this);

        weekdayChips = new Chip[] {
                binding.chpSunday,
                binding.chpMonday,
                binding.chpTuesday,
                binding.chpWednesday,
                binding.chpThursday,
                binding.chpFriday,
                binding.chpSaturday
        };

        Tools.setEditTextSingleLine(binding.txtAlarmName);
        if (!Settings.canDrawOverlays(this)) {
            new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                    .setTitle("Permission")
                    .setMessage("Displaying an alarm requires the app to open the alarm viewer on top of other applications. Grant the permission to proceed.")
                    .setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        drawOverOtherAppsSettingsLauncher.launch(intent);
                    })
                    .setOnCancelListener(dialog -> {
                        Toast.makeText(this, "Permission required to display alarms", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .show();
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
        binding.txtToneSelected.setText(title);

        if(getIntent().hasExtra("ALARM_ID")){
            long alarmId = getIntent().getLongExtra("ALARM_ID", -1);
            db.getStoredAlarmDao().getById(alarmId).subscribe(loadedStoredAlarm -> {
                storedAlarm = loadedStoredAlarm;
                setEditValuesFromItem();
                binding.slpClkSetTime.setOnFirstDrawFinishedListener(() -> {
                    long alarmHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.alarmTimestamp);
                    long alarmMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmTimestamp) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(storedAlarm.alarmTimestamp));
                    long bedtimeHours = TimeUnit.MILLISECONDS.toHours(storedAlarm.bedtimeTimestamp);
                    long bedtimeMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.bedtimeTimestamp) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(storedAlarm.bedtimeTimestamp));
                    binding.slpClkSetTime.setAlarmTime((int)alarmHours, (int)alarmMinutes);
                    binding.slpClkSetTime.setBedTime((int)bedtimeHours, (int)bedtimeMinutes);
                    setTimeChangedListeners(binding.txtTimeBedtime, binding.txtTimeAlarm);
                });
            }).dispose();
        }
        else {
            storedAlarm = new StoredAlarm();
            storedAlarm.alarmId = 0;
            storedAlarm.requestCodeActiveAlarm = -1;
            storedAlarm.pattern = Arrays.copyOf(noRepeatPattern, noRepeatPattern.length);
            setCurrentAlarmValues(binding.slpClkSetTime);
            setTimeChangedListeners(binding.txtTimeBedtime, binding.txtTimeAlarm);
        }

        binding.crdBedtimeTime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> binding.slpClkSetTime.setBedTime(hourFrom, minuteFrom), binding.slpClkSetTime.getHoursToBedTime(), binding.slpClkSetTime.getMinutesToBedTime(), true).show());
        binding.crdAlarmTime.setOnClickListener((view) -> new TimePickerDialog(this, (timePickerFrom, hourFrom, minuteFrom) -> binding.slpClkSetTime.setAlarmTime(hourFrom, minuteFrom), binding.slpClkSetTime.getHoursToAlarm(), binding.slpClkSetTime.getMinutesToAlarm(), true).show());
        binding.slpClkSetTime.setDrawHours(true);
        binding.slpClkSetTime.setDrawTimeSetterButtons(true);

        binding.btnCloseAlarmCreator.setOnClickListener(e -> finish());

        binding.chpGrpAlarmTone.setOnCheckedChangeListener((group, checkedId) -> {
            if(binding.chpRingtone.isChecked()) {
                storedAlarm.alarmToneTypeId = AlarmToneType.RINGTONE.ordinal();
                binding.txtToneSelected.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
                storedAlarm.alarmUri = ringtoneUri.toString();
            }
            else if (binding.chpBinauralBeats.isChecked()){
                // TODO: binaural beats selection visible
                storedAlarm.alarmToneTypeId = AlarmToneType.BINAURAL_BEAT.ordinal();
                binding.txtToneSelected.setText("- NONE -");
                storedAlarm.alarmUri = Uri.EMPTY.toString();
            }
            else if (binding.chpCustomFile.isChecked()) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
                }
                storedAlarm.alarmToneTypeId = AlarmToneType.CUSTOM_FILE.ordinal();
                binding.txtToneSelected.setText("- NONE -");
                storedAlarm.alarmUri = Uri.EMPTY.toString();
            }
        });
        binding.crdAlarmTone.setOnClickListener(e -> {
            if(binding.chpRingtone.isChecked()){
                final Uri currentTone =  ringtoneUri != null ? ringtoneUri : RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);//RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                ringtoneSelectorLauncher.launch(intent);
            }
            else if(binding.chpBinauralBeats.isChecked()){
                // TODO: open binaural beats selector
            }
            else if(binding.chpCustomFile.isChecked()) {
                Intent audioPicker = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
                customFileSelectorLauncher.launch(audioPicker);
            }
        });
        binding.sldAlarmVolume.addOnChangeListener((slider, value, fromUser) -> {
            binding.txtCurrAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", Math.round(value * 100)));
            storedAlarm.alarmVolume = value;
        });
        binding.crdAlarmVolumeIncrease.setOnClickListener(e -> {
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

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setView(container);
            builder.setTitle("Increase volume for");
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> setVolumeIncreaseTime(minuteNumberPicker.getValue(), secondsNumberPicker.getValue()));
            builder.setNegativeButton(getResources().getString(R.string.cancel), null);
            builder.create();
            builder.show();
        });
        binding.swtVibrateAlarm.setOnCheckedChangeListener((view, checked) -> storedAlarm.isVibrationActive = checked);
        binding.swtAlarmUseFlashlight.setOnCheckedChangeListener((view, checked) -> storedAlarm.isFlashlightActive = checked);

        binding.btnCreateAlarm.setOnClickListener(e -> {
            // TODO: make checks (like no tone selected with custom file)
            storedAlarm.title = binding.txtAlarmName.getText().toString();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                    .setTitle("Permission")
                    .setMessage("To ensure alarms go off on time, the permission to schedule exact alarms is required. Grant the permission to proceed.")
                    .setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                        scheduleAlarmSettingsLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                    })
                    .setOnCancelListener(dialog -> {
                        Toast.makeText(this, "Permission required to schedule alarms", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .show();
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
        bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", binding.slpClkSetTime.getHoursToBedTime(), binding.slpClkSetTime.getMinutesToBedTime()));
        alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", binding.slpClkSetTime.getHoursToAlarm(), binding.slpClkSetTime.getMinutesToAlarm()));
        binding.slpClkSetTime.setOnBedtimeChangedListener((hours, minutes) -> {
            storedAlarm.bedtimeTimestamp = (long) hours * 60L * 60L * 1000L + (long) minutes * 60L * 1000L;
            bedtime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });
        binding.slpClkSetTime.setOnAlarmTimeChangedListener((hours, minutes) -> {
            storedAlarm.alarmTimestamp = (long) hours * 60L * 60L * 1000L + (long) minutes * 60L * 1000L;
            alarmTime.setText(String.format(Locale.ENGLISH, "%02d:%02d", hours, minutes));
        });
    }

    private void setEditValuesFromItem() {
        binding.chpRingtone.setChecked(storedAlarm.alarmToneTypeId == AlarmToneType.RINGTONE.ordinal());
        binding.chpBinauralBeats.setChecked(storedAlarm.alarmToneTypeId == AlarmToneType.BINAURAL_BEAT.ordinal());
        binding.chpCustomFile.setChecked(storedAlarm.alarmToneTypeId == AlarmToneType.CUSTOM_FILE.ordinal());
        ringtoneUri = Uri.parse(storedAlarm.alarmUri);
        String title;
        switch(AlarmToneType.values()[storedAlarm.alarmToneTypeId]){
            case RINGTONE:
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                title = ringtone.getTitle(this);
                binding.txtToneSelected.setText(title);
                break;
            case CUSTOM_FILE:
                File file = new File(ringtoneUri.getPath());
                String filename = file.getName();
                title = filename.substring(0, filename.lastIndexOf("."));
                binding.txtToneSelected.setText(title);
                break;
            case BINAURAL_BEAT:
                // TODO: Get selected binaural beat and display it appropriately
                break;
        }
        binding.sldAlarmVolume.setValue(storedAlarm.alarmVolume);
        binding.txtCurrAlarmVolume.setText(String.format(Locale.ENGLISH, "%d%%", Math.round(binding.sldAlarmVolume.getValue() * 100)));
        long volIncMinutes = TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmVolumeIncreaseTimestamp);
        long volIncSeconds = TimeUnit.MILLISECONDS.toSeconds(storedAlarm.alarmVolumeIncreaseTimestamp) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(storedAlarm.alarmVolumeIncreaseTimestamp));
        currentVolIncMin = (int)volIncMinutes;
        currentVolIncSec = (int)volIncSeconds;
        binding.txtIncVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        binding.swtAlarmUseFlashlight.setChecked(storedAlarm.isFlashlightActive);
        binding.swtVibrateAlarm.setChecked(storedAlarm.isVibrationActive);
        binding.txtAlarmName.setText(storedAlarm.title);
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
                binding.chpRingtone.setChecked(true);
            }
        }
    }

    private void updateAlarmRepeatText() {
        boolean[] activePattern = getCurrentRepeatPattern();
        if (Arrays.equals(activePattern, everydayRepeatPattern)) {
            binding.txtRepeatPatternText.setText("Repeat every day");
        }
        else if (Arrays.equals(activePattern, allWeekdaysRepeatPattern)) {
            binding.txtRepeatPatternText.setText("Repeat every weekday");
        }
        else if (Arrays.equals(activePattern, allWeekendsRepeatPattern)) {
            binding.txtRepeatPatternText.setText("Repeat every weekend");
        }
        else if (Arrays.equals(activePattern, noRepeatPattern)) {
            binding.txtRepeatPatternText.setText("Repeat only once");
        }
        else {
            List<String> activeWeekdays = new ArrayList<>();
            for (int i = 0; i < activePattern.length; i++) {
                if(activePattern[i]) {
                    activeWeekdays.add(weekdayShorts[i]);
                }
            }
            binding.txtRepeatPatternText.setText(String.format(Locale.ENGLISH, "Repeat every %s", String.join(", ", activeWeekdays)));
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
        storedAlarm.alarmToneTypeId = binding.chpRingtone.isChecked() ? AlarmToneType.RINGTONE.ordinal() : (binding.chpCustomFile.isChecked() ? AlarmToneType.CUSTOM_FILE.ordinal() : (binding.chpBinauralBeats.isChecked() ? AlarmToneType.BINAURAL_BEAT.ordinal() : -1));
        storedAlarm.alarmUri = ringtoneUri.toString();
        storedAlarm.alarmVolume = binding.sldAlarmVolume.getValue();
        storedAlarm.alarmVolumeIncreaseTimestamp = (long) currentVolIncMin * 60L * 1000L + (long) currentVolIncSec * 1000L;
        storedAlarm.isFlashlightActive = binding.swtAlarmUseFlashlight.isChecked();
        storedAlarm.isVibrationActive = binding.swtVibrateAlarm.isChecked();
        storedAlarm.title = "";
    }

    private void setVolumeIncreaseTime(int minutes, int seconds) {
        currentVolIncMin = minutes;
        currentVolIncSec = seconds;
        binding.txtIncVolumeFor.setText(String.format(Locale.ENGLISH, "%dm %ds", currentVolIncMin, currentVolIncSec));
        storedAlarm.alarmVolumeIncreaseTimestamp = (long) currentVolIncMin * 60L * 1000L + (long) currentVolIncSec * 1000L;
    }
}