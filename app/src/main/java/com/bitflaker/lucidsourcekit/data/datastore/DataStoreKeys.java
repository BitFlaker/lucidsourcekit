package com.bitflaker.lucidsourcekit.data.datastore;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;

import java.util.HashMap;

public class DataStoreKeys {
    // Preferences
    public static final Preferences.Key<String> LANGUAGE = PreferencesKeys.stringKey("LANGUAGE");

    // App open streak
    public static final Preferences.Key<Long> FIRST_OPEN_TIME_TODAY_DAY = PreferencesKeys.longKey("FIRST_OPEN_TIME_TODAY_DAY");
    public static final Preferences.Key<Long> FIRST_OPEN_LATEST_DAY = PreferencesKeys.longKey("FIRST_OPEN_LATEST_DAY");
    public static final Preferences.Key<Long> APP_OPEN_STREAK = PreferencesKeys.longKey("APP_OPEN_STREAK");
    public static final Preferences.Key<Long> APP_OPEN_STREAK_LONGEST = PreferencesKeys.longKey("APP_OPEN_STREAK_LONGEST");

    // Goals settings
    public static final Preferences.Key<Boolean> GOAL_DIFFICULTY_AUTO_ADJUST = PreferencesKeys.booleanKey("GOAL_DIFFICULTY_AUTO_ADJUST");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_EASY = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_EASY");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_NORMAL = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_NORMAL");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_HARD = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_HARD");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_COMMON = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_COMMON");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_UNCOMMON = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_UNCOMMON");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_RARE = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_RARE");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VALUE_VARIANCE = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VALUE_VARIANCE");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_VARIANCE = PreferencesKeys.floatKey("GOAL_DIFFICULTY_VARIANCE");
    public static final Preferences.Key<Float> GOAL_DIFFICULTY_TENDENCY = PreferencesKeys.floatKey("GOAL_DIFFICULTY_TENDENCY");
    public static final Preferences.Key<Integer> GOAL_DIFFICULTY_ACCURACY = PreferencesKeys.intKey("GOAL_DIFFICULTY_ACCURACY");
    public static final Preferences.Key<Integer> GOAL_DIFFICULTY_COUNT = PreferencesKeys.intKey("GOAL_DIFFICULTY_COUNT");
    public static final Preferences.Key<Float> GOAL_FUNCTION_VALUE_A = PreferencesKeys.floatKey("GOAL_FUNCTION_VALUE_A");
    public static final Preferences.Key<Float> GOAL_FUNCTION_VALUE_B = PreferencesKeys.floatKey("GOAL_FUNCTION_VALUE_B");
    public static final Preferences.Key<Float> GOAL_FUNCTION_VALUE_C = PreferencesKeys.floatKey("GOAL_FUNCTION_VALUE_C");

    // Authentication
    public static final Preferences.Key<String> AUTHENTICATION_TYPE = PreferencesKeys.stringKey("AUTHENTICATION_TYPE");
    public static final Preferences.Key<String> AUTHENTICATION_HASH = PreferencesKeys.stringKey("AUTHENTICATION_HASH");
    public static final Preferences.Key<String> AUTHENTICATION_SALT = PreferencesKeys.stringKey("AUTHENTICATION_SALT");
    public static final Preferences.Key<Boolean> AUTHENTICATION_USE_BIOMETRICS = PreferencesKeys.booleanKey("AUTHENTICATION_USE_BIOMETRICS");

    // App setup
    public static final Preferences.Key<Boolean> APP_SETUP_FINISHED = PreferencesKeys.booleanKey("APP_SETUP_FINISHED");

    // Notifications
    public static final Preferences.Key<Boolean> NOTIFICATION_PAUSED_ALL = PreferencesKeys.booleanKey("NOTIFICATION_PAUSED_ALL");
    public static final Preferences.Key<String> NOTIFICATION_NEXT_CATEGORY = PreferencesKeys.stringKey("NOTIFICATION_NEXT_CATEGORY");
    public static final Preferences.Key<Boolean> NOTIFICATION_RC_REMINDER_FULL_SCREEN = PreferencesKeys.booleanKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN");
    public static final Preferences.Key<byte[]> NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS = PreferencesKeys.byteArrayKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS");
    public static final Preferences.Key<Long> NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME = PreferencesKeys.longKey("NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME");

    // Default settings values
    public static final HashMap<Preferences.Key<?>, Object> DEFAULT_VALUES = new HashMap<>() {{
        this.put(LANGUAGE, "en");
        this.put(FIRST_OPEN_LATEST_DAY, 0L);
        this.put(APP_OPEN_STREAK, 0L);
        this.put(APP_OPEN_STREAK_LONGEST, 0L);
        this.put(GOAL_DIFFICULTY_AUTO_ADJUST, true);
        this.put(GOAL_DIFFICULTY_VALUE_EASY, 100.0f);
        this.put(GOAL_DIFFICULTY_VALUE_NORMAL, 100.0f);
        this.put(GOAL_DIFFICULTY_VALUE_HARD, 100.0f);
        this.put(GOAL_DIFFICULTY_VALUE_COMMON, 0.3333333f);
        this.put(GOAL_DIFFICULTY_VALUE_UNCOMMON, 0.3333333f);
        this.put(GOAL_DIFFICULTY_VALUE_RARE, 0.3333333f);
        this.put(GOAL_DIFFICULTY_VALUE_VARIANCE, 10.0f);
        this.put(GOAL_DIFFICULTY_VARIANCE, 0.15f);
        this.put(GOAL_DIFFICULTY_TENDENCY, 1.8f);
        this.put(GOAL_DIFFICULTY_ACCURACY, 100);
        this.put(GOAL_DIFFICULTY_COUNT, 3);
        this.put(GOAL_FUNCTION_VALUE_A, 0.0f);
        this.put(GOAL_FUNCTION_VALUE_B, 0.0f);
        this.put(GOAL_FUNCTION_VALUE_C, 100.0f);
        this.put(AUTHENTICATION_TYPE, "none");
        this.put(AUTHENTICATION_HASH, "");
        this.put(AUTHENTICATION_SALT, "");
        this.put(AUTHENTICATION_USE_BIOMETRICS, false);
        this.put(NOTIFICATION_PAUSED_ALL, false);
        this.put(NOTIFICATION_NEXT_CATEGORY, "NONE");
        this.put(NOTIFICATION_RC_REMINDER_FULL_SCREEN, false);
        this.put(NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_DIGITS, new byte[0]);
        this.put(NOTIFICATION_RC_REMINDER_FULL_SCREEN_CONFIRM_TIME, 0L);
        this.put(APP_SETUP_FINISHED, false);
    }};
}
