package com.bitflaker.lucidsourcekit.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.datastore.getSetting
import com.bitflaker.lucidsourcekit.datastore.updateSetting
import com.bitflaker.lucidsourcekit.main.dreamjournal.DreamJournalEditorView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppUsage {
    companion object {
        suspend fun getUsageStats(context: ComponentActivity, msInPast: Long): AppUsageEvents {
            var openCount = 0
            val stateMap = HashMap<String, AppUsageEvents>()
            val launchActivityClassName = context.packageManager.getLaunchIntentForPackage(context.packageName)!!.component!!.className
            val manager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val usageEvents = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.queryEventsForSelf(currentTime - msInPast, currentTime)
            } else {
                val dismissCount = context.getSetting(DataStoreKeys.USAGE_STATS_PERMISSION_DISMISSED) ?: 0
                if (!checkUsageStatsPermission(context) && dismissCount < 10) {
                    showPermissionDialog(context, dismissCount)
                }
                manager.queryEvents(currentTime - msInPast, currentTime)
            }

            while (usageEvents?.hasNextEvent() == true) {
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

        private fun showPermissionDialog(context: ComponentActivity, dismissCount: Int) {
            MaterialAlertDialogBuilder(context, R.style.Theme_LucidSourceKit_ThemedDialog)
                .setTitle("Permission")
                .setMessage("To be able to show you statistics about usage of this app, access to app usages is required. Grant the permission to proceed.")
                .setPositiveButton(context.resources.getString(R.string.ok)) { _, _ ->
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton(context.resources.getString(R.string.no)) { _, _ ->
                    context.lifecycleScope.launch(Dispatchers.IO) {
                        context.updateSetting(DataStoreKeys.USAGE_STATS_PERMISSION_DISMISSED, 99)
                    }
                }
                .setOnCancelListener {
                    showToastLong(context, "Permission required to display app usage")
                    context.lifecycleScope.launch(Dispatchers.IO) {
                        context.updateSetting(DataStoreKeys.USAGE_STATS_PERMISSION_DISMISSED, dismissCount + 1)
                    }
                }
                .show()
        }

        private fun checkUsageStatsPermission(context: Context): Boolean {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid = context.packageManager.getApplicationInfo(context.packageName, 0).uid
            val accessMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow("android:get_usage_stats", uid, context.packageName)
            } else {
                @Suppress("DEPRECATION")
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
            get() = getScreenTime(listOf(DreamJournalEditorView::class.java.name))

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

    data class AppOpenStats(val openedAt: Long, val closedAt: Long) {
        val openFor
            get() = closedAt - openedAt
    }

    data class LaunchEvent(
        val className: String,
        val eventTime: Long = 0,
        val isRunning: Boolean? = false
    )
}