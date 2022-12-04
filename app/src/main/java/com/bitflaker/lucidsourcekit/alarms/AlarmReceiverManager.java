package com.bitflaker.lucidsourcekit.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmReceiverManager extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == null){
//                printStoredActiveAlarms(context);
            printAlarmTriggeredStatement(context);
            if(intent.hasExtra("REPETITION_PATTERN") && intent.hasExtra("REPETITION_PATTERN_INDEX") && intent.hasExtra("REPETITION_INTERVAL") && intent.hasExtra("REQUEST_CODE") && intent.hasExtra("INITIAL_TIME")){
                updateAndRescheduleNextAlarm(context, intent);
            }
        }
        else if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equalsIgnoreCase(Intent.ACTION_MY_PACKAGE_REPLACED)){
            // TODO: check if the alarm gets rescheduled after updating the app
            Toast.makeText(context, "ACTION: " + intent.getAction(), Toast.LENGTH_LONG).show();
            rescheduleAllStoredAlarms(context);
        }
//            Intent alarmDisplayer = new Intent(context, AlarmDisplayer.class);
//            alarmDisplayer.setFlags(FLAG_ACTIVITY_NEW_TASK);
//            alarmDisplayer.putExtra("ALARM_ID", intent.getIntExtra("ALARM_ID", -1));
//            context.startActivity(alarmDisplayer);
    }

    private void rescheduleAllStoredAlarms(Context context) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getActiveAlarmDao().getAllDetails().subscribe(all -> {
            for (ActiveAlarmDetails alarm : all) {
                // skip unreferenced alarm entry
                if(alarm.requestCode == -1){
                    continue;
                }
                AlarmHandler.updateScheduledRepeatingAlarmTo(context, alarm.storedAlarmId, alarm.initialTime, alarm.pattern, alarm.patternIndex, alarm.interval, alarm.requestCode).blockingSubscribe();
            }
        });
    }

    private void updateAndRescheduleNextAlarm(Context context, Intent intent) {
        boolean[] repetitionPattern = intent.getBooleanArrayExtra("REPETITION_PATTERN");
        int repetitionPatternCurrentIndex = intent.getIntExtra("REPETITION_PATTERN_INDEX", 0);
        int interval = intent.getIntExtra("REPETITION_INTERVAL", 0);
        int requestCode = intent.getIntExtra("REQUEST_CODE", 0);
        long initialTime = intent.getLongExtra("INITIAL_TIME", 0);
        long storedAlarmId = intent.getLongExtra("STORED_ALARM_ID", 0);
        AlarmHandler.updateScheduledRepeatingAlarmTo(context, storedAlarmId, initialTime + interval, repetitionPattern, repetitionPatternCurrentIndex + 1, interval, requestCode).blockingSubscribe();
    }

    private void printAlarmTriggeredStatement(Context context) {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.LONG);
        Toast.makeText(context, "ALARM: " + df.format(currentTime), Toast.LENGTH_SHORT).show();
        System.out.println(df.format(currentTime) + " -> ALARM TRIGGERED NOW!");
    }

    private void printStoredActiveAlarms(Context context) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getActiveAlarmDao().getAllDetails().subscribe(allActiveAlarms -> {
            for (ActiveAlarmDetails alarm : allActiveAlarms) {
                System.out.println("ALARM: " + alarm.toString());
            }
        });
    }
}