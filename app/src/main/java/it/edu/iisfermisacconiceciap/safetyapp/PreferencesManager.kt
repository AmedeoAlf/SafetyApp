package it.edu.iisfermisacconiceciap.safetyapp

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.sql.Timestamp
import java.time.Instant

const val PREFERENCES_NAME = "prefs"

val Context.dataStore by preferencesDataStore(PREFERENCES_NAME)

class PreferencesManager(private val ctx: Context) {
    companion object {
        var lastUpdate: Instant by mutableStateOf(Instant.now())
            private set
    }

    private suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        runCatching {
            ctx.dataStore.edit {
                transform(it)
                lastUpdate = Instant.now()
            }
        }
    }

    suspend fun getInt(name: String): Int? =
        get(intPreferencesKey(name))

    suspend fun setInt(name: String, value: Int) =
        set(intPreferencesKey(name), value)

    suspend fun getString(name: String): String? =
        get(stringPreferencesKey(name))

    suspend fun setString(name: String, value: String) =
        set(stringPreferencesKey(name), value)

    suspend fun getInstant(name: String): Instant? {
        val millis = get(longPreferencesKey(name))
        return if (millis != null) Timestamp(millis).toInstant() else null
    }

    suspend fun setInstant(name: String, timestamp: Instant) =
        set(longPreferencesKey(name), timestamp.toEpochMilli())

    suspend fun incrementInt(name: String) {
        val key = intPreferencesKey(name)
        edit { it[key] = (it[key] ?: 0) + 1 }
    }

    suspend fun <T> get(key: Preferences.Key<T>): T? =
        runCatching { ctx.dataStore.data.first()[key] }.getOrNull()

    suspend fun <T> set(key: Preferences.Key<T>, value: T) = edit { it[key] = value }
}