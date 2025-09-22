package com.bitflaker.lucidsourcekit.data.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bitflaker.lucidsourcekit.utils.Tools
import java.util.Calendar

object DataStoreKeys {
    // Preferences
    val LANGUAGE = DefaultPreferenceKey("LANGUAGE", "en")

    // App open streak
    val FIRST_OPEN_TIME_TODAY_DAY = DefaultPreferenceKey("FIRST_OPEN_TIME_TODAY_DAY", Calendar.getInstance().timeInMillis)
    val FIRST_OPEN_LATEST_DAY = DefaultPreferenceKey("FIRST_OPEN_LATEST_DAY", Tools.getMidnightTime())
    val APP_OPEN_STREAK = DefaultPreferenceKey("APP_OPEN_STREAK", 0L)
    val APP_OPEN_STREAK_LONGEST = DefaultPreferenceKey("APP_OPEN_STREAK_LONGEST", 0L)

    // Goals settings
    val GOAL_DIFFICULTY_AUTO_ADJUST = DefaultPreferenceKey("GOAL_DIFFICULTY_AUTO_ADJUST", true)
    val GOAL_DIFFICULTY_VALUE_EASY = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_EASY", 100.0f)
    val GOAL_DIFFICULTY_VALUE_NORMAL = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_NORMAL", 100.0f)
    val GOAL_DIFFICULTY_VALUE_HARD = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_HARD", 100.0f)
    val GOAL_DIFFICULTY_VALUE_COMMON = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_COMMON", 0.3333333f)
    val GOAL_DIFFICULTY_VALUE_UNCOMMON = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_UNCOMMON", 0.3333333f)
    val GOAL_DIFFICULTY_VALUE_RARE = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_RARE", 0.3333333f)
    val GOAL_DIFFICULTY_VALUE_VARIANCE = DefaultPreferenceKey("GOAL_DIFFICULTY_VALUE_VARIANCE", 10.0f)
    val GOAL_DIFFICULTY_VARIANCE = DefaultPreferenceKey("GOAL_DIFFICULTY_VARIANCE", 0.15f)
    val GOAL_DIFFICULTY_TENDENCY = DefaultPreferenceKey("GOAL_DIFFICULTY_TENDENCY", 1.8f)
    val GOAL_DIFFICULTY_ACCURACY = DefaultPreferenceKey("GOAL_DIFFICULTY_ACCURACY", 100)
    val GOAL_DIFFICULTY_COUNT = DefaultPreferenceKey("GOAL_DIFFICULTY_COUNT", 3)
    val GOAL_FUNCTION_VALUE_A = DefaultPreferenceKey("GOAL_FUNCTION_VALUE_A", 0.0f)
    val GOAL_FUNCTION_VALUE_B = DefaultPreferenceKey("GOAL_FUNCTION_VALUE_B", 0.0f)
    val GOAL_FUNCTION_VALUE_C = DefaultPreferenceKey("GOAL_FUNCTION_VALUE_C", 100.0f)

    // Authentication
    val AUTHENTICATION_TYPE = DefaultPreferenceKey("AUTHENTICATION_TYPE", "none")
    val AUTHENTICATION_HASH = DefaultPreferenceKey("AUTHENTICATION_HASH", "")
    val AUTHENTICATION_SALT = DefaultPreferenceKey("AUTHENTICATION_SALT", "")
    val AUTHENTICATION_USE_BIOMETRICS = DefaultPreferenceKey("AUTHENTICATION_USE_BIOMETRICS", false)

    // App setup
    val APP_SETUP_FINISHED = DefaultPreferenceKey("APP_SETUP_FINISHED", false)

    // Notifications
    val NOTIFICATION_PAUSED_ALL = DefaultPreferenceKey("NOTIFICATION_PAUSED_ALL", false)
    val NOTIFICATION_NEXT_CATEGORY = DefaultPreferenceKey("NOTIFICATION_NEXT_CATEGORY", "NONE")
    val NOTIFICATION_RC_REMINDER_FULL_SCREEN = DefaultPreferenceKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN", false)
    val NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS = DefaultPreferenceKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS", ByteArray(0))
    val NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME = DefaultPreferenceKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME", 0L)

    // Usage stats
    val USAGE_STATS_PERMISSION_DISMISSED = DefaultPreferenceKey("USAGE_STATS_PERMISSION_DISMISSED", 0)
}

class DefaultPreferenceKey<T>(val name: String, val default: T & Any) {
    val preferenceKey: Preferences.Key<T> by lazy {
        createPreferenceKey(default)
    }

    /**
     * Creates a [Preferences.Key] with the name provided in the constructor and of the
     * same type as the default value. This is a workaround for the inability to generically
     * create the keys from the actual type
     * @param defaultValue The default value to be used in case the parameter is not initialized
     * @param T The type of the key. This type has to be supported by [Preferences.Key]
     * @return The constructed preference key of the type provided by the default value
     */
    fun <T> createPreferenceKey(defaultValue: T & Any): Preferences.Key<T> = when (defaultValue::class) {
        String::class -> stringPreferencesKey(name)
        Int::class -> intPreferencesKey(name)
        Long::class -> longPreferencesKey(name)
        ByteArray::class -> byteArrayPreferencesKey(name)
        Boolean::class -> booleanPreferencesKey(name)
        Float::class -> floatPreferencesKey(name)
        else -> throw IllegalArgumentException("Unsupported default value preference")
    } as Preferences.Key<T>
}