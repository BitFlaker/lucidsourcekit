package com.bitflaker.lucidsourcekit.general.datastore;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class DataStoreMigrator {
    public static void migrateSharedPreferencesToDataStore(Context context) {
        if(DataStoreManager.isInitialized()) {
            DataStoreManager dsManager = DataStoreManager.getInstance();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isAlreadyMigrated = preferences.getBoolean("DATA_STORE_MIGRATION_FINISHED", false);
            if(isAlreadyMigrated) {
                return;
            }

            Map<String, ?> allPreferences = preferences.getAll();
            for(String key : allPreferences.keySet()) {
                Object value = allPreferences.get(key);
                switch (key) {
                    case "lang":
                        dsManager.updateSetting(DataStoreKeys.LANGUAGE, (String) value).blockingSubscribe();
                        break;
                    case "latest_day_first_open":
                        dsManager.updateSetting(DataStoreKeys.FIRST_OPEN_LATEST_DAY, (Long) value).blockingSubscribe();
                        break;
                    case "app_open_streak":
                        dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK, (Long) value).blockingSubscribe();
                        break;
                    case "longest_app_open_streak":
                        dsManager.updateSetting(DataStoreKeys.APP_OPEN_STREAK_LONGEST, (Long) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_auto_adjust":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_AUTO_ADJUST, (Boolean) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_easy_value":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_EASY, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_normal_value":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_NORMAL, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_hard_value":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_HARD, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_value_variance":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VALUE_VARIANCE, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_variance":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_VARIANCE, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_tendency":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_TENDENCY, (Float) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_accuracy":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_ACCURACY, (Integer) value).blockingSubscribe();
                        break;
                    case "goal_difficulty_count":
                        dsManager.updateSetting(DataStoreKeys.GOAL_DIFFICULTY_COUNT, (Integer) value).blockingSubscribe();
                        break;
                    case "goal_function_value_a":
                        dsManager.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_A, (Float) value).blockingSubscribe();
                        break;
                    case "goal_function_value_b":
                        dsManager.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_B, (Float) value).blockingSubscribe();
                        break;
                    case "goal_function_value_c":
                        dsManager.updateSetting(DataStoreKeys.GOAL_FUNCTION_VALUE_C, (Float) value).blockingSubscribe();
                        break;
                    case "auth_type":
                        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, (String) value).blockingSubscribe();
                        break;
                    case "auth_cipher":
                    case "auth_hash":
                        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_HASH, (String) value).blockingSubscribe();
                        break;
                    case "auth_key":
                    case "auth_salt":
                        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_SALT, (String) value).blockingSubscribe();
                        break;
                    case "auth_use_biometrics":
                        Boolean transformedValue = Objects.equals(value, "true");
                        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, transformedValue).blockingSubscribe();
                        break;
                    case "NOTIFICATION_PAUSED_ALL":
                        dsManager.updateSetting(DataStoreKeys.NOTIFICATION_PAUSED_ALL, (Boolean) value).blockingSubscribe();
                        break;
                    case "NEXT_UP_NOTIFICATION_CATEGORY":
                        dsManager.updateSetting(DataStoreKeys.NOTIFICATION_NEXT_CATEGORY, (String) value).blockingSubscribe();
                        break;
                    default:
                        Log.w("DataStoreMigrator", "Unknown preference key \"" + key + "\" with value \"" + value + "\"");
                }
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("DATA_STORE_MIGRATION_FINISHED", true);
            editor.apply();
            return;
        }
        Log.e("DataStoreMigrator", "Could not migrate SharedPreferences to DataStore as the DataStoreManager has not yet been initialized!");
    }

    public static void migrateSetupFinishedToDataStore(Context context) {
        String path = context.getFilesDir().getAbsolutePath() + "/.app_setup_done";
        File file = new File(path);
        if(file.exists() && file.delete()) {
            DataStoreManager.getInstance().updateSetting(DataStoreKeys.APP_SETUP_FINISHED, true).blockingSubscribe();
        }
    }
}
