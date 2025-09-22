package com.bitflaker.lucidsourcekit.data.datastore

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File
import androidx.core.content.edit

object DataStoreMigrator {
    suspend fun migrateSharedPreferencesToDataStore(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isAlreadyMigrated = preferences.getBoolean("DATA_STORE_MIGRATION_FINISHED", false)
        if (isAlreadyMigrated) {
            return
        }

        val allPreferences = preferences.all
        for (key in allPreferences.keys) {
            val value = allPreferences[key]
            when (key) {
                "lang" -> context.updateSetting(DataStoreKeys.LANGUAGE, value as String)
                "latest_day_first_open" -> context.updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, value as Long)
                "app_open_streak" -> context.updateSetting(DataStoreKeys.APP_OPEN_STREAK, value as Long)
                "longest_app_open_streak" -> context.updateSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST, value as Long)
                "goal_difficulty_auto_adjust" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST, value as Boolean)
                "goal_difficulty_easy_value" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_EASY, value as Float)
                "goal_difficulty_normal_value" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_NORMAL, value as Float)
                "goal_difficulty_hard_value" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_HARD, value as Float)
                "goal_difficulty_value_variance" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_VARIANCE, value as Float)
                "goal_difficulty_variance" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VARIANCE, value as Float)
                "goal_difficulty_tendency" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_TENDENCY, value as Float)
                "goal_difficulty_accuracy" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_ACCURACY, value as Int)
                "goal_difficulty_count" -> context.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT, value as Int)
                "goal_function_value_a" -> context.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_A, value as Float)
                "goal_function_value_b" -> context.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_B, value as Float)
                "goal_function_value_c" -> context.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_C, value as Float)
                "auth_type" -> context.updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, value as String)
                "auth_cipher", "auth_hash" -> context.updateSetting(DataStoreKeys.AUTHENTICATION_HASH, value as String)
                "auth_key", "auth_salt" -> context.updateSetting(DataStoreKeys.AUTHENTICATION_SALT, value as String)
                "auth_use_biometrics" -> context.updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, value == "true")
                "NOTIFICATION_PAUSED_ALL" -> context.updateSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL, value as Boolean)
                "NEXT_UP_NOTIFICATION_CATEGORY" -> context.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, value as String)
                else -> Log.w("DataStoreMigrator", "Unknown preference key \"$key\" with value \"$value\"")
            }
        }

        preferences.edit {
            putBoolean("DATA_STORE_MIGRATION_FINISHED", true)
        }
    }

    suspend fun migrateSetupFinishedToDataStore(context: Context) {
        val path = context.filesDir.absolutePath + "/.app_setup_done"
        val file = File(path)
        if (file.exists() && file.delete()) {
            context.updateSetting(DataStoreKeys.APP_SETUP_FINISHED, true)
        }
    }
}
