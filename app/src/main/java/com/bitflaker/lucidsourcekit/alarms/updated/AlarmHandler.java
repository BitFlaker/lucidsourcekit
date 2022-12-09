package com.bitflaker.lucidsourcekit.alarms.updated;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.bitflaker.lucidsourcekit.alarms.AlarmReceiverManager;
import com.bitflaker.lucidsourcekit.database.MainDatabase;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarmDetails;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;

public class AlarmHandler {
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

            // if the alarm has no true values in the pattern array, it essentially is useless as no alarm would ever be set
            if(isAllFalseValues(repetitionPattern)){
                throw new IllegalArgumentException("repetitionPattern boolean array must contain at least 1 true value!");
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
                    db.getActiveAlarmDao().insert(new ActiveAlarm(reqCode, finalfinalFirstAlarmTime, interval, finalfinalRepetitionPatternCurrentIndex)).blockingSubscribe(() -> {
                        db.getStoredAlarmDao().updateRequestCode(storedAlarmId, reqCode).blockingSubscribe(() -> {
                            intent.putExtra("REQUEST_CODE", reqCode);
                            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                            manager.setExact(AlarmManager.RTC_WAKEUP, finalfinalFirstAlarmTime, pendingIntent);
                        });
                    });
                });
            }
            else {
                // update the alarm data in the database, then schedule the updated alarm
                db.getActiveAlarmDao().update(new ActiveAlarm(requestCode, firstAlarmTime, interval, repetitionPatternCurrentIndex)).blockingSubscribe(() -> {
                    intent.putExtra("REQUEST_CODE", requestCode);
                    final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    manager.setExact(AlarmManager.RTC_WAKEUP, finalfinalFirstAlarmTime, pendingIntent);
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
    public static void scheduleAlarmAt(Context context, long storedAlarmId, long time) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiverManager.class);

        // query first free request code and store it in the database, then schedule the alarm
        MainDatabase db = MainDatabase.getInstance(context);
        db.getActiveAlarmDao().getFirstFreeRequestCode().subscribe(reqCode -> {
            // TODO: remove reqCode from stored Alarm after it was triggered
            db.getStoredAlarmDao().updateRequestCode(storedAlarmId, reqCode).subscribe(() -> {
                db.getActiveAlarmDao().insert(new ActiveAlarm(reqCode, time, -1, -1));
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                manager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            });
        });
    }

    /**
     * cancels a repeating alarm that was previously scheduled
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to cancel
     */
    public static Completable cancelRepeatingAlarm(Context context, long storedAlarmId) {
        return Completable.fromAction(() -> {
            MainDatabase db = MainDatabase.getInstance(context);
            db.getStoredAlarmDao().getById((int)storedAlarmId).blockingSubscribe(storedAlarm -> {
                if(storedAlarm.requestCodeActiveAlarm != -1) {
                    db.getActiveAlarmDao().getById(storedAlarm.requestCodeActiveAlarm).blockingSubscribe(activeAlarm -> {
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
                        db.getActiveAlarmDao().delete(activeAlarm).blockingAwait();
                    });
                }
            });
        });
    }

    /**
     * cancels an alarm that is not set to repeat and was previously scheduled
     * @param context the current context
     * @param requestCode the request code for the alarm cancel
     */
    public static void cancelOneTimeAlarm(Context context, int requestCode) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiverManager.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        pendingIntent.cancel();
        manager.cancel(pendingIntent);
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

            // create the target broadcast receiver to check against
            Intent intent = new Intent(context, AlarmReceiverManager.class);
            intent.putExtra("REPETITION_PATTERN", alarm.pattern);
            intent.putExtra("REPETITION_PATTERN_INDEX", alarm.patternIndex);
            intent.putExtra("REPETITION_INTERVAL", alarm.interval);
            intent.putExtra("INITIAL_TIME", alarm.initialTime);
            intent.putExtra("STORED_ALARM_ID", alarm.storedAlarmId);
            intent.putExtra("REQUEST_CODE", alarm.requestCode);

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
//            new Handler(context.getMainLooper()).post(() -> {
//                Toast.makeText(context, "RUNNING: " + alarmUp, Toast.LENGTH_SHORT).show();
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
