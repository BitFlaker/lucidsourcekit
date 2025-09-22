package com.bitflaker.lucidsourcekit.main.notification

import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory
import com.bitflaker.lucidsourcekit.utils.Tools
import java.util.Calendar

private const val MIN_NOTIFICATION_FUTURE_TIME = 60L * 1000L

class NotificationOrderManager {
    private val scheduleData: MutableList<NotificationScheduleData> = ArrayList()
    private val markers: MutableList<Long> = ArrayList()
    var hasNotifications = false
        private set

    companion object {
        fun load(notificationCategories: List<NotificationCategory>): NotificationOrderManager {
            val manager = NotificationOrderManager()
            for (category in notificationCategories) {
                if (category.isEnabled && category.dailyNotificationCount > 0) {
                    manager.scheduleNotificationTimeSpan(
                        category.timeFrom,
                        category.timeTo,
                        category.dailyNotificationCount,
                        category.id
                    )
                }
            }
            return manager
        }
    }

    fun scheduleNotificationTimeSpan(timeFrom: Long, timeTo: Long, count: Int, id: String?) {
        hasNotifications = count > 0
        insertMarker(timeFrom)
        insertMarker(timeTo)
        val delay = (timeTo - timeFrom) / count
        for (i in 0..count - 1) {
            val time = timeFrom + (delay * i) + (delay / 2)
            var idx = scheduleData.size
            for (i in scheduleData.indices) {
                if (scheduleData[i].timestamp > time) {
                    idx = i
                    break
                }
            }
            scheduleData.add(idx, NotificationScheduleData(time, id))
        }
    }

    fun getScheduledData(): List<NotificationScheduleData> {
        val finalItems = ArrayList<NotificationScheduleData>()
        for (markerIndex in 0..markers.size - 2) {
            val markerItems = getDataInMarker(markerIndex)
            val markerStart = markers[markerIndex]
            val markerEnd = markers[markerIndex + 1]
            val markerDuration = markerEnd - markerStart
            val inMarkerDelay = if (markerItems.isEmpty()) 0 else (markerDuration / markerItems.size)

            for (i in markerItems.indices) {
                val time = markerStart + (inMarkerDelay * i) + (inMarkerDelay / 2)
                finalItems.add(NotificationScheduleData(time, markerItems[i].id))
            }
        }
        return finalItems
    }

    private fun getDataInMarker(markerIndex: Int): MutableList<NotificationScheduleData> {
        val markerStart = markers[markerIndex]
        val markerEnd = markers[markerIndex + 1]
        val isInLastMarker = markers.size == markerIndex + 1
        val items = ArrayList<NotificationScheduleData>()
        for (i in scheduleData.indices) {
            val currentTime = scheduleData[i].timestamp
            if (currentTime >= markerStart && currentTime < markerEnd || isInLastMarker && (currentTime >= markerStart && currentTime <= markerEnd)) {
                items.add(scheduleData[i])
            }
        }
        return items
    }

    private fun insertMarker(marker: Long) {
        if (markers.contains(marker)) {
            return
        }
        var idx = markers.size
        for (i in markers.indices) {
            if (markers[i] > marker) {
                idx = i
                break
            }
        }
        markers.add(idx, marker)
    }

    fun getNextNotification(): NotificationScheduleData? {
        val data = getScheduledData()
        if (data.isEmpty()) {
            return null
        }

        val currentTime = Tools.getTimeOfDayMillis(Calendar.getInstance())
        for (entry in data) {
            if (entry.timestamp > currentTime + MIN_NOTIFICATION_FUTURE_TIME) {
                return entry
            }
        }

        val nsd = data[0]
        nsd.isNextDay = true
        return nsd
    }
}
