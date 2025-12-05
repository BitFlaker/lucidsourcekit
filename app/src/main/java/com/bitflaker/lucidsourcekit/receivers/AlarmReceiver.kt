package com.bitflaker.lucidsourcekit.receivers

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.database.MainDatabase
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.cancelOneTimeAlarm
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.scheduleNextNotification
import com.bitflaker.lucidsourcekit.main.alarms.AlarmHandler.updateScheduledRepeatingAlarmTo
import com.bitflaker.lucidsourcekit.main.alarms.views.AlarmViewer
import com.bitflaker.lucidsourcekit.main.notification.NotificationOrderManager
import com.bitflaker.lucidsourcekit.main.notification.visual.views.VisualNotificationActivity
import com.bitflaker.lucidsourcekit.utils.Tools
import com.bitflaker.lucidsourcekit.utils.goAsync
import com.bitflaker.lucidsourcekit.utils.isPermissionGranted
import java.util.Random
import java.util.TreeMap

private const val TAG: String = "ALARM_RECEIVER"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = goAsync { pendingIntent ->
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // TODO: Check if the alarm gets rescheduled after updating the app
            rescheduleAllStoredAlarms(context)
            scheduleNextNotification(context)
            return@goAsync
        }

        // Handle the custom alarm type
        when (intent.getStringExtra("ALARM_TYPE")) {
            "REPEATING_ALARM" -> {
                openAlarmViewer(context, intent)
                scheduleNextAlarm(context, intent)
            }
            "ONE_TIME" -> {
                val storedAlarmId = openAlarmViewer(context, intent)
                cancelOneTimeAlarm(context, storedAlarmId)
            }
            "SNOOZING_ALARM" -> {
                openAlarmViewer(context, intent)
            }
            "NOTIFICATION" -> {
                showNotificationIfEnabled(context, intent)
            }
        }
    }

    private fun openAlarmViewer(context: Context, intent: Intent): Long {
        val storedAlarmId = intent.getLongExtra("STORED_ALARM_ID", -1)
        if (storedAlarmId == -1L) {
            Log.e(TAG, "Missing stored alarm id")
            return -1
        }
        // TODO: implement a way of opening the AlarmViewer when the app currently is open (and the
        //       reason for the open app is, that it was opened by the AlarmViewer previously) and
        //       not to loose progress (not to reset and finish the previous MainActivity)
        context.startActivity(Intent(context, AlarmViewer::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("STORED_ALARM_ID", storedAlarmId)
        })
        return storedAlarmId
    }

    private suspend fun rescheduleAllStoredAlarms(context: Context) {
        MainDatabase.getInstance(context).activeAlarmDao.getAllDetails()
            .filter { it.requestCode != -1 }
            .forEach {
                updateScheduledRepeatingAlarmTo(
                    context,
                    it.storedAlarmId,
                    it.initialTime,
                    it.pattern,
                    it.patternIndex,
                    it.interval,
                    it.requestCode
                )
            }
    }

    private suspend fun scheduleNextAlarm(context: Context, intent: Intent) {
        val repetitionPattern = intent.getBooleanArrayExtra("REPETITION_PATTERN") ?: BooleanArray(0)
        val repetitionPatternCurrentIndex = intent.getIntExtra("REPETITION_PATTERN_INDEX", 0)
        val interval = intent.getIntExtra("REPETITION_INTERVAL", 0)
        val requestCode = intent.getIntExtra("REQUEST_CODE", 0)
        val initialTime = intent.getLongExtra("INITIAL_TIME", 0)
        val storedAlarmId = intent.getLongExtra("STORED_ALARM_ID", 0)
        updateScheduledRepeatingAlarmTo(
            context,
            storedAlarmId,
            initialTime + interval,
            repetitionPattern,
            repetitionPatternCurrentIndex + 1,
            interval,
            requestCode
        )
    }

    private suspend fun showNotificationIfEnabled(context: Context, intent: Intent) {
        val notificationCategoryId = intent.getStringExtra("NOTIFICATION_CATEGORY_ID")
        val categories = MainDatabase.getInstance(context).notificationCategoryDao.getAll()
        val manager = NotificationOrderManager.load(categories)
        if (manager.hasNotifications) {
            // Ensure the notification category is enabled and notifications are not paused to show it
            val allNotificationsPaused = context.getSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL)
            val categoryEnabled = categories.find { it.id == notificationCategoryId  }?.isEnabled ?: false
            if (categoryEnabled && !allNotificationsPaused) {
                showNotificationForCategory(context, notificationCategoryId!!)
            }

            // Schedule the next notification
            val nsd = manager.getNextNotification()
            scheduleNextNotification(context, nsd)
        }
    }

    private fun isScreenOff(context: Context): Boolean {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return !manager.displays.any { it.state != Display.STATE_OFF }  // TODO: Maybe check if any are ON / ON_SUSPEND / UNKNOWN instead ?
    }

    private suspend fun showNotificationForCategory(context: Context, notificationCategoryId: String) {
        val db = MainDatabase.getInstance(context)
        val notificationCategory = db.notificationCategoryDao.getById(notificationCategoryId)
        if (notificationCategory.isEnabled) {
            val category = db.notificationCategoryDao.getById(notificationCategoryId)
            val messages = db.notificationMessageDao.getAllOfCategory(category.id)
            if (messages.isEmpty()) {
                Log.e(TAG, "No notification messages available for category ${category.id}")
                return
            }

            // In case the notification is a reality check reminder, show the full screen
            // notification if it is enabled and the screen is currently off
            val fullScreenEnabled = context.getSetting(DataStoreKeys.NOTIFICATION_RC_REMINDER_FULL_SCREEN)
            if (category.id == "RCR" && fullScreenEnabled && isScreenOff(context)) {
                context.startActivity(
                    Intent(context, VisualNotificationActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                )
            }

            // Get message for category based on the notification weights and build it
            val msg = getWeightedNotificationMessage(messages) ?: return
            val notification = NotificationCompat.Builder(context, notificationCategoryId)
                .setSmallIcon(R.drawable.icon_no_bg)
                .setContentTitle("Reminder")
                .setContentText(msg.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg.message))
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .build()

            // Check if permission to post notification is granted and then show it
            if (context.isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                val notifyId = Tools.getUniqueNotificationId(notificationCategoryId)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(notifyId)
                manager.notify(notifyId, notification)
            }
        }
    }

    private fun getWeightedNotificationMessage(messages: List<NotificationMessage>): NotificationMessage? {
        val map = TreeMap<Int, NotificationMessage>()
        var totalWeight = 0
        for (msg in messages) {
            totalWeight += msg.weight
            map.put(totalWeight, msg)
        }
        val weightKey = Random().nextInt(totalWeight)
        val selected = map.higherEntry(weightKey)
        if (selected == null) {
            Log.w(TAG, "No suitable notification message found")
        }
        return selected?.value
    }
}