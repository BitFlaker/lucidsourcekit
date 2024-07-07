package com.bitflaker.lucidsourcekit.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEntryEditor

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
                    else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                        val state = stateMap[event.packageName]
                        state?.appUsageEvents?.add(LaunchEvent(event.className, event.timeStamp, null))
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
            var runningActivityCount = 0
            var appLaunchTimeStamp: Long = 0
            val appPauseEvents: HashMap<String, Long> = hashMapOf()
            val appOpenStats: ArrayList<AppOpenStats> = arrayListOf()
            appUsageEvents.sortBy { x -> x.eventTime }
            for (state in appUsageEvents) {
                // Only process events complying with the provided class name filter
                if(!classNameFilters.isNullOrEmpty() && !classNameFilters.contains(state.className)) continue

                if (state.isRunning == true) {
                    if (appPauseEvents.containsKey(state.className)) {
                        // In case the activity was paused before and is launched again, count the
                        // usage time excluding the time the app was paused and without incrementing
                        // the runningActivityCount as the activity was just unpaused
                        appOpenStats.add(AppOpenStats(appLaunchTimeStamp, appPauseEvents[state.className]!!))
                        appPauseEvents.remove(state.className)
                        appLaunchTimeStamp = state.eventTime
                        continue
                    }
                    if (runningActivityCount == 0) {
                        // The first activity instance was launched. A new app usage session was started
                        appLaunchTimeStamp = state.eventTime
                    }
                    runningActivityCount++
                }
                else if (state.isRunning == false) {
                    if (appPauseEvents.containsKey(state.className)) {
                        // In case the activity was paused before it was closed, untrack the
                        // pause event timestamp and handle it like every other activity closed event
                        appPauseEvents.remove(state.className)
                    }
                    if (runningActivityCount == 1) {
                        // The last activity instance was closed, the current app usage session
                        // is now considered completed
                        appPauseEvents.clear()
                        appOpenStats.add(AppOpenStats(appLaunchTimeStamp, state.eventTime))
                    }
                    runningActivityCount--
                }
                else {
                    // In case the activity was paused, store the timestamp without modifying
                    // the runningActivityCount as the activity has not yet been closed
                    appPauseEvents[state.className] = state.eventTime
                }
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
        val isRunning: Boolean? = false
    )
}