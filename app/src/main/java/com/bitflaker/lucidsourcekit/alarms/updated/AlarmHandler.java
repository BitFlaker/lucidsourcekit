package com.bitflaker.lucidsourcekit.alarms.updated;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import com.bitflaker.lucidsourcekit.alarms.AlarmCreator;
import com.bitflaker.lucidsourcekit.alarms.AlarmReceiverManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;
import com.bitflaker.lucidsourcekit.notification.NotificationOrderManager;
import com.bitflaker.lucidsourcekit.notification.NotificationScheduleData;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;

public class AlarmHandler {
    public static final String NEXT_UP_NOTIFICATION_CATEGORY = "NEXT_UP_NOTIFICATION_CATEGORY";
    public static final int SNOOZING_ALARM_REQUEST_CODE_START_VALUE = 1000000000;
    public static final int NOTIFICATION_REQUEST_CODE = 2121212121;

    public static void clickAction(Context context) {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.LONG);
        System.out.println(df.format(currentTime) + " scheduled");
        MainDatabase db = MainDatabase.getInstance(context);
        db.getStoredAlarmDao().insert(new StoredAlarm("TestAlarm", System.currentTimeMillis() - 1000 * 60 * 10, System.currentTimeMillis() + 1000 * 10, new boolean[] { true, true, true, true, true, true, true }, 0, "", 1, 1000 * 30, false, false, true, -1)).subscribe((id, throwable) -> {
            scheduleAlarmRepeatedlyAt(context, id, System.currentTimeMillis() + 1000 * 10, new boolean[] { true, true, true, true, true, true, true }, 1000 * 10);
        });
    }

    /**
     * schedules an exact alarm to go off at a specific time and repeat at the specified
     * interval according to the specified pattern. The pattern gets repeated after it reaches the end.
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to set
     * @param firstAlarmTime the time for the alarm to go off initially in milliseconds
     * @param repetitionPattern the pattern at which the alarm will go off (setting a value to false skips the alarm call and just adds the delay). The pattern repeats after reaching the end
     * @param interval the interval to repeat the alarm at after the initial alarm went off
     */
    public static Completable scheduleAlarmRepeatedlyAt(Context context, long storedAlarmId, long firstAlarmTime, boolean[] repetitionPattern, int interval) {
        return scheduleAlarmRepeatedlyAt(context, storedAlarmId, firstAlarmTime, repetitionPattern, 0, interval);
    }

    /**
     * schedules an exact alarm to go off at a specific time and repeat at the specified
     * interval according to the specified pattern. The pattern gets repeated after it reaches the end.
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to set
     * @param firstAlarmTime the time for the alarm to go off initially in milliseconds
     * @param repetitionPattern the pattern at which the alarm will go off (setting a value to false skips the alarm call and just adds the delay). The pattern repeats after reaching the end
     * @param repetitionPatternCurrentIndex the current index in the pattern
     * @param interval the interval to repeat the alarm at after the initial alarm went off
     */
    public static Completable scheduleAlarmRepeatedlyAt(Context context, long storedAlarmId, long firstAlarmTime, boolean[] repetitionPattern, int repetitionPatternCurrentIndex, int interval) {
        return updateScheduledRepeatingAlarmTo(context, storedAlarmId, firstAlarmTime, repetitionPattern, repetitionPatternCurrentIndex, interval, -1);
    }

    /**
     * updates or creates an exact alarm to go off at a specific time and repeat at the specified
     * interval according to the specified pattern. The request code for the alarm to update or
     * create also has to be specified. The pattern gets repeated after it reaches the end.
     * If data for a one time alarm was provided, a one time alarm will be scheduled instead.
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to set
     * @param firstAlarmTime the time for the alarm to go off initially in milliseconds
     * @param repetitionPattern the pattern at which the alarm will go off (setting a value to false skips the alarm call and just adds the delay). The pattern repeats after reaching the end
     * @param repetitionPatternCurrentIndex the current index in the pattern
     * @param interval the interval to repeat the alarm at after the initial alarm went off
     * @param requestCode the request code for the alarm to update or create
     */
    public static Completable updateScheduledRepeatingAlarmTo(Context context, long storedAlarmId, long firstAlarmTime, boolean[] repetitionPattern, int repetitionPatternCurrentIndex, int interval, int requestCode) {
        return Completable.fromAction(() -> {
            long finalFirstAlarmTime = firstAlarmTime;
            int finalRepetitionPatternCurrentIndex = repetitionPatternCurrentIndex;

            // if the alarm has no true values in the pattern array,
            // it will be assumed to be a one time alarm
            if(interval == -1 || isAllFalseValues(repetitionPattern)){
                scheduleAlarmAt(context, storedAlarmId, firstAlarmTime, requestCode).blockingSubscribe();
                return;
            }

            // repeat pattern from beginning if end was reached
            finalRepetitionPatternCurrentIndex %= repetitionPattern.length;

            // Skip calling the alarm for all entries marked with false until next true.
            // If the alarm was set to a timestamp in the past, skip forward to the next enabled timestamp,
            // which has to be at least 7 seconds in the future (as the alarm has to be scheduled at least
            // a couple of seconds in the future, during testing the time seemed to be 5 seconds, so to be
            // sure to not get below that time window, an extra 2 seconds for the code below were added)
            while(!repetitionPattern[finalRepetitionPatternCurrentIndex] || finalFirstAlarmTime < (System.currentTimeMillis() + 7 * 1000)) {
                finalFirstAlarmTime += interval;
                finalRepetitionPatternCurrentIndex = (finalRepetitionPatternCurrentIndex + 1) % repetitionPattern.length;
            }
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiverManager.class);

            // Put extras for being able to reschedule alarms again with the same properties
            intent.putExtra("REPETITION_PATTERN", repetitionPattern);
            intent.putExtra("REPETITION_PATTERN_INDEX", finalRepetitionPatternCurrentIndex);
            intent.putExtra("REPETITION_INTERVAL", interval);
            intent.putExtra("INITIAL_TIME", finalFirstAlarmTime);
            intent.putExtra("STORED_ALARM_ID", storedAlarmId);

            // Check if the alarm should be created or updated
            MainDatabase db = MainDatabase.getInstance(context);
            long finalfinalFirstAlarmTime = finalFirstAlarmTime;
            if(requestCode == -1) {
                int finalfinalRepetitionPatternCurrentIndex = finalRepetitionPatternCurrentIndex;
                // create an alarm
                // query first free request code and store it in the database, then schedule the alarm
                db.getActiveAlarmDao().getFirstFreeRequestCode().blockingSubscribe(reqCode -> {
                    int alarmReqCode = reqCode * 2;
                    int alarmEditorReqCode = reqCode * 2 + 1;
                    db.getActiveAlarmDao().insert(new ActiveAlarm(alarmReqCode, finalfinalFirstAlarmTime, interval, finalfinalRepetitionPatternCurrentIndex)).blockingSubscribe(() -> {
                        db.getStoredAlarmDao().updateRequestCode(storedAlarmId, alarmReqCode).blockingSubscribe(() -> {
                            db.getStoredAlarmDao().setActiveState((int) storedAlarmId, true).blockingSubscribe(() -> {
                                intent.putExtra("REQUEST_CODE", alarmReqCode);
                                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmReqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                Intent editor = new Intent(context, AlarmCreator.class);
                                editor.putExtra("ALARM_ID", storedAlarmId);
                                final PendingIntent pendingIntentEditor = PendingIntent.getBroadcast(context, alarmEditorReqCode, editor, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                manager.setAlarmClock(new AlarmManager.AlarmClockInfo(finalfinalFirstAlarmTime, pendingIntentEditor), pendingIntent);
                            });
                        });
                    });
                });
            }
            else {
                // update the alarm data in the database, then schedule the updated alarm
                db.getActiveAlarmDao().update(new ActiveAlarm(requestCode, firstAlarmTime, interval, repetitionPatternCurrentIndex)).blockingSubscribe(() -> {
                    intent.putExtra("REQUEST_CODE", requestCode);
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Intent editor = new Intent(context, AlarmCreator.class);
                    editor.putExtra("ALARM_ID", storedAlarmId);
                    final PendingIntent pendingIntentEditor = PendingIntent.getBroadcast(context, requestCode + 1, editor, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    manager.setAlarmClock(new AlarmManager.AlarmClockInfo(finalfinalFirstAlarmTime, pendingIntentEditor), pendingIntent);
                });
            }
        });
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to set
     * @param time the time for the alarm to go off at in milliseconds
     */
    public static Completable scheduleAlarmAt(Context context, long storedAlarmId, long time, int requestCode) {
        return Completable.fromAction(() -> {
            // If the alarm is less than 5 seconds in the future, it will be set for the next day, as the
            // alarm will not be able to go off in less than 5 seconds when it is scheduled exactly.
            long alarmTime = time;
            if(alarmTime < (System.currentTimeMillis() + 5 * 1000)){
                alarmTime += 24*60*60*1000;
            }
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiverManager.class);
            intent.putExtra("STORED_ALARM_ID", storedAlarmId);

            // If the requestCode was not provided, query the first free request code and
            // store it in the database and schedule the alarm
            long finalTime = alarmTime;
            MainDatabase db = MainDatabase.getInstance(context);
            if(requestCode == -1){
                db.getStoredAlarmDao().setActiveState((int) storedAlarmId, true).blockingSubscribe(() -> {
                    db.getActiveAlarmDao().getFirstFreeRequestCode().blockingSubscribe(reqCode -> {
                        int alarmReqCode = reqCode * 2;
                        int alarmEditorReqCode = reqCode * 2 + 1;
                        db.getActiveAlarmDao().insert(new ActiveAlarm(alarmReqCode, finalTime, -1, -1)).blockingSubscribe(() -> {
                            db.getStoredAlarmDao().updateRequestCode((int) storedAlarmId, alarmReqCode).blockingSubscribe(() -> {
                                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmReqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                Intent editor = new Intent(context, AlarmCreator.class);
                                editor.putExtra("ALARM_ID", storedAlarmId);
                                final PendingIntent pendingIntentEditor = PendingIntent.getBroadcast(context, alarmEditorReqCode, editor, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                                manager.setAlarmClock(new AlarmManager.AlarmClockInfo(finalTime, pendingIntentEditor), pendingIntent);
                            });
                        });
                    });
                });
            }
            else {
                db.getActiveAlarmDao().update(new ActiveAlarm(requestCode, finalTime, -1, -1)).blockingSubscribe(() -> {
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Intent editor = new Intent(context, AlarmCreator.class);
                    editor.putExtra("ALARM_ID", storedAlarmId);
                    final PendingIntent pendingIntentEditor = PendingIntent.getBroadcast(context, requestCode + 1, editor, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    manager.setAlarmClock(new AlarmManager.AlarmClockInfo(finalTime, pendingIntentEditor), pendingIntent);
                });
            }
        });
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     */
    public static Completable scheduleNextNotification(Context context) {
        return Completable.fromAction(() -> {
            MainDatabase db = MainDatabase.getInstance(context);
            db.getNotificationCategoryDao().getAll().blockingSubscribe(notificationCategories -> {
                NotificationOrderManager notificationOrderManager = NotificationOrderManager.load(notificationCategories);
                NotificationScheduleData nsd = notificationOrderManager.getNextNotification();
                scheduleNextNotification(context, nsd).blockingAwait();
            });
        });
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     * @param nsd the notification schedule
     */
    public static Completable scheduleNextNotification(Context context, NotificationScheduleData nsd) {
        return Completable.fromAction(() -> {
            DataStoreManager dsManager = DataStoreManager.getInstance();
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if(nsd != null) {
                dsManager.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, nsd.getId()).blockingSubscribe();

                Intent intent = new Intent(context, AlarmReceiverManager.class);
                intent.putExtra("NOTIFICATION_CATEGORY_ID", nsd.getId());
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                manager.set(AlarmManager.RTC_WAKEUP, nsd.getScheduleTime(), pendingIntent);
            }
            else {
                String id = dsManager.getSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY).blockingFirst();
                if(!id.equals("NONE")) {
                    Intent intent = new Intent(context, AlarmReceiverManager.class);
                    intent.putExtra("NOTIFICATION_CATEGORY_ID", id);
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    pendingIntent.cancel();
                    manager.cancel(pendingIntent);
                    dsManager.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, "NONE").blockingSubscribe();
                }
            }
        });
    }

    public static Completable createNotificationChannels(Context context) {
        return Completable.fromAction(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MainDatabase db = MainDatabase.getInstance(context);
                db.getNotificationCategoryDao().getAll().blockingSubscribe(notificationCategories -> {
                    for (NotificationCategory cat : notificationCategories) {
                        NotificationChannel channel = new NotificationChannel(cat.getId(), cat.getDescription(), NotificationManager.IMPORTANCE_DEFAULT);
                        channel.enableLights(true);
                        channel.setLightColor(Color.argb(255, 76, 59, 168));
                        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                        notificationManager.createNotificationChannel(channel);
                    }
                });
            }
        });
    }

    /**
     * cancels a repeating alarm that was previously scheduled, if a one time alarm was provided,
     * it will cancel the one time alarm instead
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to cancel
     */
    public static Completable cancelRepeatingAlarm(Context context, long storedAlarmId) {
        return Completable.fromAction(() -> {
            MainDatabase db = MainDatabase.getInstance(context);
            db.getStoredAlarmDao().getById((int)storedAlarmId).blockingSubscribe(storedAlarm -> {
                if(storedAlarm.requestCodeActiveAlarm != -1) {
                    db.getActiveAlarmDao().getById(storedAlarm.requestCodeActiveAlarm).blockingSubscribe(activeAlarm -> {
                        // If it is a one time alarm, cancel the one time alarm instead!
                        if(activeAlarm.interval == -1) {
                            cancelOneTimeAlarm(context, (int) storedAlarmId).blockingSubscribe();
                            return;
                        }
                        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(context, AlarmReceiverManager.class);

                        // Put extras for being able to reschedule alarms again with the same properties
                        intent.putExtra("REPETITION_PATTERN", storedAlarm.pattern);
                        intent.putExtra("REPETITION_PATTERN_INDEX", activeAlarm.patternIndex);
                        intent.putExtra("REPETITION_INTERVAL", activeAlarm.interval);
                        intent.putExtra("INITIAL_TIME", activeAlarm.initialTime);
                        intent.putExtra("STORED_ALARM_ID", storedAlarmId);
                        intent.putExtra("REQUEST_CODE", storedAlarm.requestCodeActiveAlarm);

                        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, storedAlarm.requestCodeActiveAlarm, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        pendingIntent.cancel();
                        manager.cancel(pendingIntent);
                        db.getActiveAlarmDao().deleteById(storedAlarm.requestCodeActiveAlarm).blockingAwait();
                        db.getStoredAlarmDao().setActiveState(storedAlarm.alarmId, false).blockingSubscribe();
                    });
                }
            });
        });
    }

    /**
     * cancels an alarm that is not set to repeat and was previously scheduled
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to cancel
     */
    public static Completable cancelOneTimeAlarm(Context context, long storedAlarmId) {
        return Completable.fromAction(() -> {
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiverManager.class);
            intent.putExtra("STORED_ALARM_ID", storedAlarmId);
            MainDatabase.getInstance(context).getStoredAlarmDao().getById(storedAlarmId).blockingSubscribe(alarm -> {
                if (alarm.requestCodeActiveAlarm != -1) {
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarm.requestCodeActiveAlarm, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    pendingIntent.cancel();
                    manager.cancel(pendingIntent);
                    MainDatabase.getInstance(context).getActiveAlarmDao().deleteById(alarm.requestCodeActiveAlarm).blockingSubscribe();
                    MainDatabase.getInstance(context).getStoredAlarmDao().setActiveState(alarm.alarmId, false).blockingSubscribe();
                }
            });
        });
    }

    public static void reEnableNotificationsIfNotRunning(Context context) {
        MainDatabase db = MainDatabase.getInstance(context);
        db.getNotificationCategoryDao().getAll().blockingSubscribe(notificationCategories -> {
            NotificationOrderManager notificationOrderManager = NotificationOrderManager.load(notificationCategories);
            NotificationScheduleData nsd = notificationOrderManager.getNextNotification();
            if(nsd != null) {
                Intent intent = new Intent(context, AlarmReceiverManager.class);
                intent.putExtra("NOTIFICATION_CATEGORY_ID", nsd.getId());
                boolean notificationsUp = PendingIntent.getBroadcast(context, NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE) != null;
                if (!notificationsUp) {
                    scheduleNextNotification(context);
                }
            }
        });
    }

    /**
     * reschedules alarms alarms provided if they are no longer running
     * @param context the current context
     * @param alarms the alarms to reschedule if they are no longer running
     */
    public static void reEnableAlarmsIfNotRunning(Context context, List<ActiveAlarmDetails> alarms) {
        for (ActiveAlarmDetails alarm : alarms) {
            // skip the unreferenced alarm entry
            if(alarm.requestCode == -1){
                continue;
            }

            // create the target broadcast receiver to check against (repetitive or one time)
            Intent intent = new Intent(context, AlarmReceiverManager.class);
            intent.putExtra("STORED_ALARM_ID", alarm.storedAlarmId);
            if(alarm.interval != -1){
                intent.putExtra("REPETITION_PATTERN", alarm.pattern);
                intent.putExtra("REPETITION_PATTERN_INDEX", alarm.patternIndex);
                intent.putExtra("REPETITION_INTERVAL", alarm.interval);
                intent.putExtra("INITIAL_TIME", alarm.initialTime);
                intent.putExtra("REQUEST_CODE", alarm.requestCode);
            }

            // Check if the alarm is still running
            // WARNING: when an alarm is set and the app gets force closed, it will still think the alarm is running
            boolean alarmUp = (PendingIntent.getBroadcast(context, alarm.requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE) != null);

            // Check if not only the pending intent still exists, but also that the alarm has not been
            // supposed to go off at least 30 seconds ago (which means the alarm did not go off for whatever reason
            // e.g. an app force stop)
            if(alarmUp && (alarm.initialTime + 30 * 1000) < Calendar.getInstance().getTimeInMillis()){
                AlarmHandler.cancelRepeatingAlarm(context, alarm.storedAlarmId).blockingSubscribe();
                alarm.requestCode = -1;
                alarmUp = false;
            }

//            boolean finalAlarmUp = alarmUp;
//            new Handler(context.getMainLooper()).post(() -> {
//                Toast.makeText(context, "RUNNING: " + finalAlarmUp, Toast.LENGTH_SHORT).show();
//            });

            // in case the alarm is no longer running, reschedule it
            if(!alarmUp){
                // TODO: check if it will always be recognized as not running
                System.out.println("NON-RUNNING ALARM FOUND! RE-ENABLING IT...");
                AlarmHandler.updateScheduledRepeatingAlarmTo(context, alarm.storedAlarmId, alarm.initialTime, alarm.pattern, alarm.patternIndex, alarm.interval, alarm.requestCode).blockingSubscribe();
            }
        }
    }

    private static boolean isAllFalseValues(boolean[] repetitionPattern) {
        for(boolean b : repetitionPattern){
            if(b) {
                return false;
            }
        }
        return true;
    }
}
