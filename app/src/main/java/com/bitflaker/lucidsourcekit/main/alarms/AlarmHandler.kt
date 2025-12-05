package com.bitflaker.lucidsourcekit.main.alarms

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.alarms.entities.ActiveAlarm
import com.bitflaker.lucidsourcekit.database.alarms.entities.ActiveAlarmDetails
import com.bitflaker.lucidsourcekit.database.alarms.entities.StoredAlarm
import com.bitflaker.lucidsourcekit.main.notification.NotificationOrderManager
import com.bitflaker.lucidsourcekit.main.notification.NotificationScheduleData
import java.util.Calendar

object AlarmHandler {
    const val NEXT_UP_NOTIFICATION_CATEGORY: String = "NEXT_UP_NOTIFICATION_CATEGORY"
    const val SNOOZING_ALARM_REQUEST_CODE_START_VALUE: Int = 1000000000
    const val NOTIFICATION_REQUEST_CODE: Int = 2121212121

    suspend fun scheduleTestAlarm(context: Context) {
        val millis = Calendar.getInstance().timeInMillis
        val id = MainDatabase.getInstance(context).storedAlarmDao.insert(StoredAlarm(
            "TestAlarm",
            millis - 600000,
            millis + 10000,
            booleanArrayOf(true, true, true, true, true, true, true),
            0,
            "",
            1f,
            (30000).toLong(),
            false,
            false,
            true,
            -1
        ))
        scheduleAlarmRepeatedlyAt(
            context,
            id,
            millis + 10000,
            booleanArrayOf(true, true, true, true, true, true, true),
            10000
        )
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
    suspend fun scheduleAlarmRepeatedlyAt(
        context: Context,
        storedAlarmId: Long,
        firstAlarmTime: Long,
        repetitionPattern: BooleanArray,
        interval: Int
    ) {
        return scheduleAlarmRepeatedlyAt(
            context,
            storedAlarmId,
            firstAlarmTime,
            repetitionPattern,
            0,
            interval
        )
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
    suspend fun scheduleAlarmRepeatedlyAt(
        context: Context,
        storedAlarmId: Long,
        firstAlarmTime: Long,
        repetitionPattern: BooleanArray,
        repetitionPatternCurrentIndex: Int,
        interval: Int
    ) {
        return updateScheduledRepeatingAlarmTo(
            context,
            storedAlarmId,
            firstAlarmTime,
            repetitionPattern,
            repetitionPatternCurrentIndex,
            interval,
            -1
        )
    }

    /**
     * Updates or creates an exact alarm to go off at a specific time and repeat at the specified
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
    suspend fun updateScheduledRepeatingAlarmTo(
        context: Context,
        storedAlarmId: Long,
        firstAlarmTime: Long,
        repetitionPattern: BooleanArray,
        repetitionPatternCurrentIndex: Int,
        interval: Int,
        requestCode: Int
    ) {
        var checkedFirstAlarmTime = firstAlarmTime
        var freeRepetitionPatternCurrentIndex = repetitionPatternCurrentIndex

        // If the alarm has no true values in the pattern array,
        // it will be assumed to be a one time alarm
        if (interval == -1 || repetitionPattern.all { !it }) {
            scheduleAlarmAt(context, storedAlarmId, firstAlarmTime, requestCode)
            return
        }

        // Repeat pattern from the beginning if the end was reached
        freeRepetitionPatternCurrentIndex %= repetitionPattern.size

        // Skip calling the alarm for all entries marked with false until next true.
        // If the alarm was set to a timestamp in the past, skip forward to the next enabled timestamp,
        // which has to be at least 7 seconds in the future (as the alarm has to be scheduled at least
        // a couple of seconds in the future, during testing the time seemed to be 5 seconds. To be
        // sure not to get below that time window, an extra 2 seconds for the code below were added)
        while (!repetitionPattern[freeRepetitionPatternCurrentIndex] || checkedFirstAlarmTime < (System.currentTimeMillis() + 7 * 1000)) {
            checkedFirstAlarmTime += interval.toLong()
            freeRepetitionPatternCurrentIndex = (freeRepetitionPatternCurrentIndex + 1) % repetitionPattern.size
        }
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Put extras for being able to reschedule alarms again with the same properties
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", "REPEATING_ALARM")
            putExtra("REPETITION_PATTERN", repetitionPattern)
            putExtra("REPETITION_PATTERN_INDEX", freeRepetitionPatternCurrentIndex)
            putExtra("REPETITION_INTERVAL", interval)
            putExtra("INITIAL_TIME", checkedFirstAlarmTime)
            putExtra("STORED_ALARM_ID", storedAlarmId)
        }

        // TODO: Extract the common parts from this and the function below

        // Check if the alarm should be created or updated
        val db = MainDatabase.getInstance(context)
        var operationRequestCode = requestCode
        var showIntentRequestCode = requestCode + 1

        // Update the existing alarm or create a new one
        if (requestCode == -1) {
            operationRequestCode = db.activeAlarmDao.getFirstFreeRequestCode()
            db.activeAlarmDao.insert(ActiveAlarm(operationRequestCode, checkedFirstAlarmTime, interval, freeRepetitionPatternCurrentIndex))
            db.storedAlarmDao.updateRequestCode(storedAlarmId, operationRequestCode)
            db.storedAlarmDao.setActiveState(storedAlarmId, true)
            showIntentRequestCode = operationRequestCode + 10000
        } else {
            db.activeAlarmDao.update(ActiveAlarm(operationRequestCode, firstAlarmTime, interval, repetitionPatternCurrentIndex))
        }

        // Create the operation pending intent
        intent.putExtra("REQUEST_CODE", operationRequestCode)
        val operation = PendingIntent.getBroadcast(
            context,
            operationRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get the showIntent pending intent
        val showIntent = PendingIntent.getBroadcast(
            context,
            showIntentRequestCode,
            Intent(context, AlarmEditorView::class.java).apply {
                putExtra("ALARM_ID", storedAlarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm
        manager.setAlarmClock(AlarmClockInfo(checkedFirstAlarmTime, showIntent), operation)
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to set
     * @param time the time for the alarm to go off at in milliseconds
     */
    suspend fun scheduleAlarmAt(
        context: Context,
        storedAlarmId: Long,
        time: Long,
        requestCode: Int
    ) {
        // If the alarm is less than 5 seconds in the future, it will be set for the next day, as the
        // alarm will not be able to go off in less than 5 seconds when it is scheduled exactly.
        var alarmTime = time
        if (alarmTime < (System.currentTimeMillis() + 5 * 1000)) {
            alarmTime += (24 * 60 * 60 * 1000).toLong()
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", "ONE_TIME")
            putExtra("STORED_ALARM_ID", storedAlarmId)
        }

        // If the requestCode was not provided, query the first free request code and
        // store it in the database and schedule the alarm
        val db = MainDatabase.getInstance(context)
        var operationRequestCode = requestCode
        var showIntentRequestCode = requestCode + 1

        if (requestCode == -1) {
            operationRequestCode = db.activeAlarmDao.getFirstFreeRequestCode()
            db.activeAlarmDao.insert(ActiveAlarm(operationRequestCode, alarmTime, -1, -1))
            db.storedAlarmDao.updateRequestCode(storedAlarmId, operationRequestCode)
            db.storedAlarmDao.setActiveState(storedAlarmId, true)
            showIntentRequestCode = operationRequestCode + 10000
        } else {
            db.activeAlarmDao.update(ActiveAlarm(requestCode, alarmTime, -1, -1))
        }

        // Create the operation pending intent
        intent.putExtra("REQUEST_CODE", operationRequestCode)
        val operation = PendingIntent.getBroadcast(
            context,
            operationRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get the showIntent pending intent
        val showIntent = PendingIntent.getBroadcast(
            context,
            showIntentRequestCode,
            Intent(context, AlarmEditorView::class.java).apply {
                putExtra("ALARM_ID", storedAlarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm
        manager.setAlarmClock(AlarmClockInfo(alarmTime, showIntent), operation)
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     */
    suspend fun scheduleNextNotification(context: Context) {
        val categories = MainDatabase.getInstance(context).getNotificationCategoryDao().getAll()
        val manager = NotificationOrderManager.load(categories)
        scheduleNextNotification(context, manager.getNextNotification())
    }

    /**
     * schedules a one shot alarm exactly on a specific time
     * @param context the current context
     * @param nsd the notification schedule
     */
    suspend fun scheduleNextNotification(context: Context, nsd: NotificationScheduleData?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val id = nsd?.id ?: context.getSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY)
        val operation = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_TYPE", "NOTIFICATION")
                putExtra("NOTIFICATION_CATEGORY_ID", id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule next notification or cancel it if scheduleData is null
        if (nsd != null) {
            context.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, id)
            manager.set(AlarmManager.RTC_WAKEUP, nsd.scheduleTime, operation)
        } else if (id != "NONE") {
            operation.cancel()
            manager.cancel(operation)
            context.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, "NONE")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createNotificationChannels(context: Context) {
        val db = MainDatabase.getInstance(context)
    }

    /**
     * cancels a repeating alarm that was previously scheduled, if a one time alarm was provided,
     * it will cancel the one time alarm instead
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to cancel
     */
    suspend fun cancelRepeatingAlarm(context: Context, storedAlarmId: Long) {
        val db = MainDatabase.getInstance(context)
        val storedAlarm = db.storedAlarmDao.getById(storedAlarmId)
        if (storedAlarm.requestCodeActiveAlarm == -1) {
            return
        }

        val activeAlarm = db.activeAlarmDao.getById(storedAlarm.requestCodeActiveAlarm) ?: return

        // If it is a one time alarm, cancel the one time alarm instead!
        if (activeAlarm.interval == -1) {
            cancelOneTimeAlarm(context, storedAlarmId)
            return
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Put extras for being able to reschedule alarms again with the same properties
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", "REPEATING_ALARM")
            putExtra("REPETITION_PATTERN", storedAlarm.pattern)
            putExtra("REPETITION_PATTERN_INDEX", activeAlarm.patternIndex)
            putExtra("REPETITION_INTERVAL", activeAlarm.interval)
            putExtra("INITIAL_TIME", activeAlarm.initialTime)
            putExtra("STORED_ALARM_ID", storedAlarmId)
            putExtra("REQUEST_CODE", storedAlarm.requestCodeActiveAlarm)
        }

        // Cancel the operation
        val operation = PendingIntent.getBroadcast(
            context,
            storedAlarm.requestCodeActiveAlarm,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        operation.cancel()
        manager.cancel(operation)

        // Remove alarm from database
        db.activeAlarmDao.deleteById(storedAlarm.requestCodeActiveAlarm)
        db.storedAlarmDao.setActiveState(storedAlarm.alarmId, false)
    }

    /**
     * cancels an alarm that is not set to repeat and was previously scheduled
     * @param context the current context
     * @param storedAlarmId the id of the stored alarm associated to the alarm to cancel
     */
    suspend fun cancelOneTimeAlarm(context: Context, storedAlarmId: Long) {
        val db = MainDatabase.getInstance(context)
        val alarm = db.storedAlarmDao.getById(storedAlarmId)
        if (alarm.requestCodeActiveAlarm == -1) {
            return
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel the operation
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.requestCodeActiveAlarm,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_TYPE", "ONE_TIME")
                putExtra("STORED_ALARM_ID", storedAlarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent.cancel()
        manager.cancel(pendingIntent)

        // Remove alarm from database
        db.activeAlarmDao.deleteById(alarm.requestCodeActiveAlarm)
        db.storedAlarmDao.setActiveState(alarm.alarmId, false)
    }

    suspend fun reEnableNotificationsIfNotRunning(context: Context) {
        val db = MainDatabase.getInstance(context)
        val notificationCategories = db.notificationCategoryDao.getAll()
        val notificationOrderManager = NotificationOrderManager.load(notificationCategories)
        val nsd = notificationOrderManager.getNextNotification() ?: return

        // The operation will be null if it is not running
        val operation = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_TYPE", "NOTIFICATION")
                putExtra("NOTIFICATION_CATEGORY_ID", nsd.id)
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the notification again
        if (operation == null) {
            scheduleNextNotification(context)
        }
    }

    /**
     * reschedules alarms alarms provided if they are no longer running
     * @param context the current context
     * @param alarms the alarms to reschedule if they are no longer running
     */
    suspend fun reEnableAlarmsIfNotRunning(context: Context, alarms: List<ActiveAlarmDetails>) {
        for (alarm in alarms) {
            // Skip unreferenced alarm entries
            if (alarm.requestCode == -1) {
                continue
            }

            // Create the target broadcast receiver to check against
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("STORED_ALARM_ID", alarm.storedAlarmId)
            }

            // Add extras for repetitive alarms in case it has repeat values set
            if (alarm.interval != -1) {
                intent.putExtra("ALARM_TYPE", "REPEATING_ALARM")
                intent.putExtra("REPETITION_PATTERN", alarm.pattern)
                intent.putExtra("REPETITION_PATTERN_INDEX", alarm.patternIndex)
                intent.putExtra("REPETITION_INTERVAL", alarm.interval)
                intent.putExtra("INITIAL_TIME", alarm.initialTime)
                intent.putExtra("REQUEST_CODE", alarm.requestCode)
            }
            else {
                intent.putExtra("ALARM_TYPE", "ONE_TIME")
            }

            // Check if the alarm is still running
            // WARNING: When an alarm is set and the app gets force closed, it will still think the alarm is running
            val operation = PendingIntent.getBroadcast(
                context,
                alarm.requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            var isAlarmRunning = operation != null

            // Check if not only the pending intent still exists, but also that the alarm has not been
            // supposed to go off at least 30 seconds ago (which means the alarm did not go off for whatever reason
            // e.g. an app force stop)
            if (isAlarmRunning && alarm.initialTime + 30000 < Calendar.getInstance().timeInMillis) {
                cancelRepeatingAlarm(context, alarm.storedAlarmId)
                alarm.requestCode = -1
                isAlarmRunning = false
            }

            // In case the alarm is no longer running, reschedule it
            if (!isAlarmRunning) {
                // TODO: Check if it will always be recognized as not running
                println("NON-RUNNING ALARM FOUND! RE-ENABLING IT...")
                updateScheduledRepeatingAlarmTo(
                    context,
                    alarm.storedAlarmId,
                    alarm.initialTime,
                    alarm.pattern,
                    alarm.patternIndex,
                    alarm.interval,
                    alarm.requestCode
                )
            }
        }
    }
}
