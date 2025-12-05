package com.bitflaker.lucidsourcekit.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

const val DATA_STORE_FILE_NAME: String = "settings"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_FILE_NAME)

suspend fun <T : Any> Context.getSetting(key: DefaultPreferenceKey<T>): T {
    return dataStore.data.map { preferences ->
        if (preferences.contains(key.preferenceKey)) {
            return@map preferences[key.preferenceKey]
        }
        updateSetting(key.preferenceKey, key.default)
        return@map key.default
    }.firstOrNull() ?: throw IllegalArgumentException("Unable to find requested setting '${key.name}'")
}

suspend fun <T> Context.updateSetting(preferenceKey: DefaultPreferenceKey<T>, value: T): Preferences {
    return updateSetting(preferenceKey.preferenceKey, value)
}

suspend fun <T> Context.updateSetting(preferenceKey: Preferences.Key<T>, value: T): Preferences {
    return dataStore.updateData { prefs ->
        val mutablePreferences = prefs.toMutablePreferences()
        mutablePreferences[preferenceKey] = value
        mutablePreferences
    }
}
