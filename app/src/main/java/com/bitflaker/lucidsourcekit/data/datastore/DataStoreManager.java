package com.bitflaker.lucidsourcekit.data.datastore;

import android.content.Context;
import android.util.Log;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class DataStoreManager {
    public static final String DATA_STORE_FILE_NAME = "settings";
    private static DataStoreManager instance;
    private final RxDataStore<Preferences> dataStore;

    private DataStoreManager(Context context) {
        dataStore = new RxPreferenceDataStoreBuilder(context, DATA_STORE_FILE_NAME).build();
    }

    public static void initialize(Context context) {
        if(instance == null) {
            instance = new DataStoreManager(context);
        }
        else {
            Log.w("DataStoreManager", "Tried to initialize already initialized instance of DataStoreManager, ignoring initialization request.");
        }
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static DataStoreManager getInstance() {
        if(instance == null) {
            Log.e("DataStoreManager", "DataStoreManager instance was requested, but has not yet been initialized!");
        }
        return instance;
    }

    public <T> Flowable<T> getSetting(Preferences.Key<T> preferenceKey) {
        return dataStore.data().map(preferences -> {
            if (preferences.contains(preferenceKey)) {
                return preferences.get(preferenceKey);
            }
            try {
                T value = (T) DataStoreKeys.DEFAULT_VALUES.get(preferenceKey);
                updateSetting(preferenceKey, value).blockingSubscribe();
                return value;
            }
            catch(Exception e) {
                Log.e("DataStoreManager", "Could not get setting \"" + preferenceKey.getName() + "\" as the settings was not set and no suitable default value is available!");
                return null;
            }
        });
    }

    public <T> Single<Preferences> updateSetting(Preferences.Key<T> preferenceKey, T value) {
        return dataStore.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.set(preferenceKey, value);
            return Single.just(mutablePreferences);
        });
    }
}
