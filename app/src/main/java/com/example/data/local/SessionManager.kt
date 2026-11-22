package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "twincash_session")

class SessionManager(private val context: Context) {
    private val currentUidKey = stringPreferencesKey("current_uid")

    val currentUidFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[currentUidKey]
    }

    suspend fun saveSession(uid: String) {
        context.dataStore.edit { prefs ->
            prefs[currentUidKey] = uid
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(currentUidKey)
        }
    }
}
