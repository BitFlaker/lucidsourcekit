package com.bitflaker.lucidsourcekit.main.alarms;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.main.notification.visual.VisualNotificationActivity;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.main.notification.NotificationOrderManager;
import com.bitflaker.lucidsourcekit.main.notification.NotificationScheduleData;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class AlarmReceiver extends BroadcastReceiver {
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
            if(DataStoreManager.getInstance() == null) {
                DataStoreManager.initialize(context);
            }
            rescheduleAllStoredAlarms(context);
            AlarmHandler.scheduleNextNotification(context).blockingSubscribe();
        }
    }

    private static void showNotificationIfEnabled(Context context, String notificationCategoryId) {
        MainDatabase db = MainDatabase.getInstance(context);
        List<NotificationCategory> categories = db.getNotificationCategoryDao().getAll().blockingGet();
        NotificationOrderManager manager = NotificationOrderManager.load(categories);
        if (manager.hasNotifications()) {
            if (!DataStoreManager.isInitialized()) {
                DataStoreManager.initialize(context);
            }

            // Check if the desired notification category id is still enabled
            boolean categoryEnabled = false;
            for (NotificationCategory category : categories) {
                if (category.getId().equals(notificationCategoryId)) {
                    categoryEnabled = category.isEnabled();
                    break;
                }
            }

            // Only show the notification if notifications are not paused and the
            // current notification's category is enabled
            boolean allNotificationsPaused = DataStoreManager.getInstance().getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL).blockingFirst();
            if(categoryEnabled && !allNotificationsPaused) {
                showNotificationForCategory(context, notificationCategoryId);
            }

            // Schedule next notification
            NotificationScheduleData nsd = manager.getNextNotification();
            AlarmHandler.scheduleNextNotification(context, nsd).blockingSubscribe();
        }
    }

    public static void showNotificationForCategory(Context context, String notificationCategoryId) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getNotificationCategoryDao().getById(notificationCategoryId).subscribe(notificationCategory -> {
            if (notificationCategory.isEnabled()) {
                db.getNotificationCategoryDao().getById(notificationCategoryId).subscribe(category ->
                    db.getNotificationMessageDao().getAllOfCategoryAndObfuscationType(category.getId(), category.getObfuscationTypeId()).subscribe(messages -> {
                        if (messages.isEmpty()) {
                            Log.e("LSC_NOTIFICATION", "No notification messages available for category " + category.getId() + " with obfuscation id " + category.getObfuscationTypeId());
                            return;
                        }

                        // In case the notification is a reality check reminder, show the full screen
                        // notification if it is enabled and the screen is currently locked
                        if (category.getId().equals("RCR")) {
                            boolean fullScreenEnabled = DataStoreManager.getInstance().getSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN).blockingFirst();
                            boolean screenOff = isScreenOff(context);
                            if (fullScreenEnabled && screenOff) {
                                Intent fullScreenNotification = new Intent(context, VisualNotificationActivity.class);
                                fullScreenNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                context.startActivity(fullScreenNotification);
                            }
                        }

                        // Get message for category based on the notification weights and build it
                        NotificationMessage msg = getWeightedNotificationMessage(messages);
                        Notification builder = new NotificationCompat.Builder(context, notificationCategoryId)
                                .setSmallIcon(R.drawable.icon_no_bg)
                                .setContentTitle("Reminder")
                                .setContentText(msg.getMessage())
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.getMessage()))
                                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                                .build();

                        // Show notification only when the permission for posting notifications is granted
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            int notifyId = Tools.getUniqueNotificationId(notificationCategoryId);
                            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            manager.cancel(notifyId);
                            manager.notify(notifyId, builder);
                        }
                    }).dispose()
                ).dispose();
            }
        }).dispose();
    }

    private static boolean isScreenOff(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return false;
            }
        }
        return true;
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