package com.bitflaker.lucidsourcekit.alarms;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.alarms.updated.AlarmHandler;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.notification.NotificationOrderManager;
import com.bitflaker.lucidsourcekit.notification.NotificationScheduleData;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class AlarmReceiverManager extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            if (intent.hasExtra("REPETITION_PATTERN") && intent.hasExtra("REPETITION_PATTERN_INDEX") && intent.hasExtra("REPETITION_INTERVAL") && intent.hasExtra("REQUEST_CODE") && intent.hasExtra("INITIAL_TIME") && intent.hasExtra("STORED_ALARM_ID")) {
                // a repeating alarm was triggered
                openAlarmViewer(context, intent.getLongExtra("STORED_ALARM_ID", -1));
                printAlarmTriggeredStatement(context, true);
                updateAndRescheduleNextAlarm(context, intent);
            } else if (intent.hasExtra("STORED_ALARM_ID")) {
                // a one time alarm was triggered
                openAlarmViewer(context, intent.getLongExtra("STORED_ALARM_ID", -1));
                printAlarmTriggeredStatement(context, false);
                removeOneTimeAlarm(context, intent);
            } else if (intent.hasExtra("SNOOZING_STORED_ALARM_ID")) {
                // a snoozing alarm should go off again
                openAlarmViewer(context, intent.getLongExtra("SNOOZING_STORED_ALARM_ID", -1));
            } else if (intent.hasExtra("NOTIFICATION_CATEGORY_ID")) {
                String notificationCategoryId = intent.getStringExtra("NOTIFICATION_CATEGORY_ID");
                showNotificationIfEnabled(context, notificationCategoryId);
            }
        } else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equalsIgnoreCase(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            // TODO: check if the alarm gets rescheduled after updating the app
//            Toast.makeText(context, "ACTION: " + intent.getAction(), Toast.LENGTH_LONG).show();
            rescheduleAllStoredAlarms(context);
            AlarmHandler.scheduleNextNotification(context).blockingSubscribe();
        }
    }

    private static void showNotificationIfEnabled(Context context, String notificationCategoryId) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getNotificationCategoryDao().getAll().blockingSubscribe(notificationCategories -> {
            NotificationOrderManager notificationOrderManager = NotificationOrderManager.load(notificationCategories);
            if(notificationOrderManager.hasNotifications()) {
                boolean allNotificationsPaused = DataStoreManager.getInstance().getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL).blockingFirst();
                NotificationScheduleData nsd = notificationOrderManager.getNextNotification();
                boolean categoryFound = false, categoryEnabled = false;
                for (NotificationCategory category : notificationCategories) {
                    if(category.getId().equals(notificationCategoryId)) {
                        categoryFound = true;
                        categoryEnabled = category.isEnabled();
                        break;
                    }
                }
                if(categoryFound && categoryEnabled && !allNotificationsPaused) {
                    showNotificationForCategory(context, notificationCategoryId);
                }
                AlarmHandler.scheduleNextNotification(context, nsd).blockingSubscribe();
            }
        });
    }

    public static void showNotificationForCategory(Context context, String notificationCategoryId) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getNotificationCategoryDao().getById(notificationCategoryId).subscribe(notificationCategory -> {
            if(notificationCategory.isEnabled()) {
                db.getNotificationCategoryDao().getById(notificationCategoryId).subscribe(notCat -> {
                    db.getNotificationMessageDao().getAllOfCategoryAndObfuscationType(notCat.getId(), notCat.getObfuscationTypeId()).subscribe(messages -> {
                        if(messages.size() == 0) {
                            Log.e("LSC_NOTIFICATION", "No notification messages available for category " + notCat.getId() + " with obfuscation id " + notCat.getObfuscationTypeId());
                            return;
                        }
                        NotificationMessage msg = getWeightedNotificationMessage(messages);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationCategoryId)
                                .setSmallIcon(R.drawable.icon_no_bg)
                                .setContentTitle("Reminder")
                                .setContentText(msg.getMessage())
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.getMessage()))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        int notifyId = Tools.getUniqueNotificationId(notificationCategoryId);
                        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        nMgr.cancel(notifyId);
                        notificationManager.notify(notifyId, builder.build());
                    }).dispose();
                }).dispose();
            }
        }).dispose();
    }

    private static NotificationMessage getWeightedNotificationMessage(List<NotificationMessage> messages) {
        NavigableMap<Integer, NotificationMessage> map = new TreeMap<>();
        Random rnd = new Random();
        int totalWeight = 0;
        for (NotificationMessage msg : messages) {
            totalWeight += msg.getWeight();
            map.put(totalWeight, msg);
        }
        return map.higherEntry(rnd.nextInt(totalWeight)).getValue();
    }

    private void openAlarmViewer(Context context, long storedAlarmId) {
        // TODO: implement a way of opening the AlarmViewer when the app currently is open (and the reason for the open app is, that it was opened by the AlarmViewer previously) and not to loose progress (not to reset and finish the previous MainActivity)
        Intent alarmViewer = new Intent(context, AlarmViewer.class);
        alarmViewer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmViewer.putExtra("STORED_ALARM_ID", storedAlarmId);
        context.startActivity(alarmViewer);
    }

    private void removeOneTimeAlarm(Context context, Intent intent) {
        long storedAlarmId = intent.getLongExtra("STORED_ALARM_ID", -1);
        AlarmHandler.cancelOneTimeAlarm(context, storedAlarmId).blockingSubscribe();
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
        }).dispose();
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

    private void printAlarmTriggeredStatement(Context context, boolean isRepeating) {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.LONG);
        Toast.makeText(context, (isRepeating ? "REP-" : "SINGLE-") + "ALARM: " + df.format(currentTime), Toast.LENGTH_SHORT).show();
        System.out.println(df.format(currentTime) + " -> " + (isRepeating ? "REP-" : "SINGLE-") + "ALARM TRIGGERED NOW!");
    }

    private void printStoredActiveAlarms(Context context) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getActiveAlarmDao().getAllDetails().subscribe(allActiveAlarms -> {
            for (ActiveAlarmDetails alarm : allActiveAlarms) {
                System.out.println("ALARM: " + alarm.toString());
            }
        }).dispose();
    }
}