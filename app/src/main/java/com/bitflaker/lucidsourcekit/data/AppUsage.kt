package com.bitflaker.lucidsourcekit.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
import android.util.Log
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppUsage {
    companion object {
        fun getUsageStats(context: Context, msInPast: Long): AppUsageEvents {
            var openCount = 0
            val stateMap = HashMap<String, AppUsageEvents>()
            val launchActivityClassName = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.component!!.className
            if(checkUsageStatsPermission(context)) {
                val manager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val usageEvents = manager.queryEvents(currentTime - msInPast, currentTime)

                while(usageEvents.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)
                    if (event.packageName != context.packageName) continue
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        stateMap[event.packageName] = stateMap[event.packageName] ?: AppUsageEvents(packageName = event.packageName)
                        stateMap[event.packageName]!!.appUsageEvents.add(LaunchEvent(event.className, event.timeStamp, true))
                        if (event.className == launchActivityClassName) {
                            openCount++
                        }
                    }
                    else if (event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                        val state = stateMap[event.packageName]
                        state?.appUsageEvents?.add(LaunchEvent(event.className, event.timeStamp, false))
                    }
                }
                return stateMap[context.packageName] ?: AppUsageEvents(packageName = context.packageName)
            }
            else {
                Intent(ACTION_USAGE_ACCESS_SETTINGS).apply {
                    context.startActivity(this)
                }
            }
            return AppUsageEvents(packageName = context.packageName)
        }

        private fun checkUsageStatsPermission(context: Context): Boolean {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid = context.packageManager.getApplicationInfo(context.packageName, 0).uid
            val accessMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow("android:get_usage_stats", uid, context.packageName)
            }
            else {
                appOpsManager.checkOpNoThrow("android:get_usage_stats", uid, context.packageName)
            }
            return accessMode == AppOpsManager.MODE_ALLOWED
        }
    }

    data class AppUsageEvents(
        val packageName: String,
        val appUsageEvents: ArrayList<LaunchEvent> = arrayListOf()
    ) {
        val totalTime : Long
            get() = getScreenTime(null)

        val journalTime: Long
            get() = getScreenTime(listOf(DreamJournalEntryEditor::class.java.name))

        val appOpenTimeStamps: List<AppOpenStats>
            get() = getAppOpenStats(null)

        private fun getScreenTime(classNameFilters: List<String>?): Long {
            val appOpenStats = getAppOpenStats(classNameFilters)
            return appOpenStats.sumOf { x -> x.openFor }
        }

        private fun getAppOpenStats(classNameFilters: List<String>?): ArrayList<AppOpenStats> {
            val appOpenStats: ArrayList<AppOpenStats> = arrayListOf()
            appUsageEvents.sortBy { x -> x.eventTime }
            var actCount = 0
            var appLaunchTimeStamp: Long = 0
            for (state in appUsageEvents) {
                if(!classNameFilters.isNullOrEmpty() && !classNameFilters.contains(state.className)) continue
                if (actCount == 0 && state.isRunning) {
                    appLaunchTimeStamp = state.eventTime
                } else if (actCount == 1 && !state.isRunning) {
                    appOpenStats.add(AppOpenStats(appLaunchTimeStamp, state.eventTime))
                }
                actCount += if (state.isRunning) 1 else -1
            }
            return appOpenStats
        }
    }

    data class AppOpenStats(
        val openedAt: Long,
        val closedAt: Long,
    ) {
        val openFor
            get() = closedAt - openedAt
    }

    data class LaunchEvent(
        val className: String,
        val eventTime: Long = 0,
        val isRunning: Boolean = false
    )
}