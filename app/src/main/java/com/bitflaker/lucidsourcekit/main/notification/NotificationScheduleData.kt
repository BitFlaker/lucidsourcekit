package com.bitflaker.lucidsourcekit.main.notification

import com.bitflaker.lucidsourcekit.utils.Tools
import java.util.Calendar

class NotificationScheduleData(var timestamp: Long, var id: String?) {
    var isNextDay: Boolean = false

    val scheduleTime: Long
        get() {
            val cal = Calendar.getInstance()
            cal.timeInMillis = Tools.getMidnightTime() + timestamp
            if (isNextDay) {
                cal.add(Calendar.HOUR_OF_DAY, 24)
            }
            return cal.getTimeInMillis()
        }
}
