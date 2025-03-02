package com.bitflaker.lucidsourcekit.main.alarms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.resulttables.AlarmTimestamps;
import com.bitflaker.lucidsourcekit.databinding.ActivityAlarmManagerBinding;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmManagerView extends AppCompatActivity {
    private RecyclerViewAdapterAlarms adapterAlarms;
    private boolean isInSelectionMode = false;
    private long nextAlarmTimeStamp, nextBedtimeTimeStamp;
    private MainDatabase db;
    private ActivityAlarmManagerBinding binding;

    private final ActivityResultCallback<ActivityResult> alarmCreationOrModificationCallback = result -> {
        if(result.getResultCode() == RESULT_OK){
            Intent data = result.getData();
            if (data != null && data.hasExtra("CREATED_NEW_ALARM") && data.hasExtra("ALARM_ID")) {
                if(data.getBooleanExtra("CREATED_NEW_ALARM", false)){
                    adapterAlarms.loadAddedAlarmWithId(data.getLongExtra("ALARM_ID", -1));
                }
                else {
                    adapterAlarms.reloadModifiedAlarmWithId(data.getLongExtra("ALARM_ID", -1));
                }
            }
            fetchNextAlarmAndDisplay(false);
        }
    };
    private final ActivityResultLauncher<Intent> alarmInteractionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), alarmCreationOrModificationCallback);
    private TimerTask nextTimeToCalcTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = MainDatabase.getInstance(AlarmManagerView.this);
        binding = ActivityAlarmManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Tools.makeStatusBarTransparent(AlarmManagerView.this);
        binding.llTopHeading.setLayoutParams(Tools.addRelativeLayoutParamsTopStatusbarSpacing(AlarmManagerView.this, ((RelativeLayout.LayoutParams) binding.llTopHeading.getLayoutParams())));

        binding.slpClock.startClock();
        binding.fabAddAlarm.setOnClickListener(e -> {
            if(!isInSelectionMode){
                alarmInteractionLauncher.launch(new Intent(this, AlarmEditorView.class));
            }
            else {
                new MaterialAlertDialogBuilder(this, R.style.Theme_LucidSourceKit_ThemedDialog)
                        .setTitle("Delete Alarms")
                        .setMessage("Do you really want to delete the selected alarms?")
                        .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                            List<Long> alarmIds = adapterAlarms.getSelectedStoredAlarmIds();
                            db.getStoredAlarmDao().getAllById(alarmIds).blockingSubscribe(allAlarms -> {
                                // TODO: also support one time only alarms
                                // cancel all scheduled alarms
                                for (StoredAlarm alarm : allAlarms) {
                                    AlarmHandler.cancelRepeatingAlarm(getApplicationContext(), alarm.alarmId).blockingSubscribe(() -> {
                                        db.getStoredAlarmDao().delete(alarm).blockingAwait();
                                    });
                                }
                                adapterAlarms.removeSelectedAlarmIds();
                                runOnUiThread(() -> fetchNextAlarmAndDisplay(false));
                            });
                        })
                        .setNegativeButton(getResources().getString(R.string.no), null)
                        .show();
            }
        });

        adapterAlarms = new RecyclerViewAdapterAlarms(this, new ArrayList<>());
        adapterAlarms.setHorizontalPadding(Tools.dpToPx(this, 8));
        adapterAlarms.setOnSelectionModeStateChangedListener(new RecyclerViewAdapterAlarms.OnSelectionModeStateChanged() {
            @Override
            public void onSelectionModeEntered() {
                binding.fabAddAlarm.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorErrorContainer, getTheme()));
                binding.fabAddAlarm.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnErrorContainer, getTheme()));
                binding.fabAddAlarm.setImageResource(R.drawable.ic_baseline_delete_24);
                isInSelectionMode = true;
            }

            @Override
            public void onSelectionModeLeft() {
                binding.fabAddAlarm.setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimaryContainer, getTheme()));
                binding.fabAddAlarm.setImageTintList(Tools.getAttrColorStateList(R.attr.colorOnPrimaryContainer, getTheme()));
                binding.fabAddAlarm.setImageResource(R.drawable.ic_round_add_24);
                isInSelectionMode = false;
            }
        });
        adapterAlarms.setOnEntryClickedListener(storedAlarm -> {
            Intent editor = new Intent(this, AlarmEditorView.class);
            editor.putExtra("ALARM_ID", storedAlarm.alarmId);
            alarmInteractionLauncher.launch(editor);
        });
        adapterAlarms.setOnEntryActiveStateChangedListener((alarmData, checked) -> fetchNextAlarmAndDisplay(false));
        binding.rcvListAlarms.setAdapter(adapterAlarms);
        binding.rcvListAlarms.setLayoutManager(new LinearLayoutManager(this));
        db.getStoredAlarmDao().getAll().subscribe(storedAlarms -> {
            adapterAlarms.setData(storedAlarms);
        }).dispose();
        updateMarkers();
        fetchNextAlarmAndDisplay(false);
    }

    private void updateMarkers() {
        db.getStoredAlarmDao().getAll().subscribe(alarms -> {
            ArrayList<Long> activeAlarmTimes = new ArrayList<>();
            ArrayList<Long> inactiveAlarmTimes = new ArrayList<>();
            for (var alarm : alarms) {
                if (alarm.isAlarmActive) activeAlarmTimes.add(alarm.alarmTimestamp);
                else inactiveAlarmTimes.add(alarm.alarmTimestamp);
            }
            binding.slpClock.setMarkersHigh(activeAlarmTimes);
            binding.slpClock.setMarkersLow(inactiveAlarmTimes);
        }).dispose();
    }

    private void fetchNextAlarmAndDisplay(boolean triggeredByAlarm) {
        updateMarkers();
        db.getActiveAlarmDao().getNextUpcomingAlarmTimestamp().blockingSubscribe(timestamps -> {
            if (timestamps.isEmpty()) {
                nextAlarmTimeStamp = -1;
                nextBedtimeTimeStamp = -1;
                if (nextTimeToCalcTask != null) {
                    nextTimeToCalcTask.cancel();
                    nextTimeToCalcTask = null;
                }
                binding.txtTimeToNextBedtime.setText("--");
                binding.txtTimeToNextAlarm.setText("--");
            }
            else {
                AlarmTimestamps current = timestamps.get(0);
                if (triggeredByAlarm && this.nextAlarmTimeStamp != current.alarmTimestamp()) {
                    adapterAlarms.alarmWentOff(current.alarmTimestamp());
                }
                nextAlarmTimeStamp = current.alarmTimestamp();
                nextBedtimeTimeStamp = current.bedtimeTimestamp();
                if (nextTimeToCalcTask == null) {
                    startTimeToAlarmUpdater();
                }
                setNextTimeTo();
            }
        });
    }

    private void startTimeToAlarmUpdater() {
        // TODO fix that sometimes the alarm still says 00:00:01 even though the alarm already went off (maybe load new data after a slight delay after an exact minute)
            // => for a workaround that might work the alarm should be refreshed 150ms after the alarm should have gone off
        Calendar calDelay = Calendar.getInstance();
        calDelay.set(Calendar.MINUTE, calDelay.get(Calendar.MINUTE) + 1);
        calDelay.set(Calendar.SECOND, 0);
        calDelay.set(Calendar.MILLISECOND, 150);

        nextTimeToCalcTask = new TimerTask() {
            @Override
            public void run() {
                setNextTimeTo();
            }
        };
        new Timer().schedule(nextTimeToCalcTask, calDelay.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 60*1000);
    }

    private void setNextTimeTo() {
        // adding 60000 to always round up to the next minute (as when only e.g. 50 seconds are left for
        // the alarm to go off, it would show 00:00:00, but with the added 60000ms it will show 00:00:01)
        long diffAlarmTime = nextAlarmTimeStamp + 60 * 1000 - Calendar.getInstance().getTimeInMillis();
        long timeOfDayAlarm = Tools.getTimeOfDayMillis(nextAlarmTimeStamp);
        long bedtimeDiff = nextBedtimeTimeStamp <= timeOfDayAlarm ? timeOfDayAlarm - nextBedtimeTimeStamp : timeOfDayAlarm + 1000 * 60 * 60 * 24 - nextBedtimeTimeStamp;
        long diffBedtime = diffAlarmTime - bedtimeDiff;
        long bedtimeDirection = Long.signum(diffBedtime);
        diffBedtime *= bedtimeDirection;
        String bedtimePostfix = bedtimeDirection < 0 ? " ago" : "";

        String timeToAlarm = Tools.getTimeSpanStringZeroed(diffAlarmTime);
        String timeToBedtime = Tools.getTimeSpanStringZeroed(diffBedtime) + bedtimePostfix;

        runOnUiThread(() -> {
            binding.txtTimeToNextAlarm.setText(timeToAlarm);
            binding.txtTimeToNextBedtime.setText(timeToBedtime);
            if (timeToAlarm.isEmpty()) {
                // Fetching time of next alarm with a delay of 20ms as the rescheduling takes some time
                // and the fetched data might still be the same
                new Handler().postDelayed(() -> fetchNextAlarmAndDisplay(true), 15);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(isInSelectionMode){
            adapterAlarms.setIsInSelectionMode(false);
        }
        else {
            super.onBackPressed();
        }
    }
}