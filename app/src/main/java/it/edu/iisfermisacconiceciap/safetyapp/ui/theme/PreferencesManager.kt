package it.edu.iisfermisacconiceciap.safetyapp.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull

const val PREFERENCES_NAME = "prefs"

val Context.dataStore by preferencesDataStore(PREFERENCES_NAME)

class PreferencesManager(val context: Context) {
    suspend fun getInt(name: String): Int? =
        get(intPreferencesKey(name))

    suspend fun setInt(name: String, value: Int) =
        set(intPreferencesKey(name), value)

    suspend fun incrementInt(name: String) {
        val key = intPreferencesKey(name)
        context.dataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    suspend fun <T> get(key: Preferences.Key<T>): T? {
        return context.dataStore.data.firstOrNull { prefs ->
            prefs.contains(key)
        }?.get(key)
    }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}