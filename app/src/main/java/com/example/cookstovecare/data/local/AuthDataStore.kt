package com.example.cookstovecare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

/**
 * DataStore for repair center authentication state.
 */
class AuthDataStore(private val context: Context) {

    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    private val phoneNumberKey = stringPreferencesKey("phone_number")

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[isLoggedInKey] ?: false
    }

    val phoneNumber: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[phoneNumberKey] ?: ""
    }

    suspend fun setLoggedIn(phoneNumber: String) {
        context.authDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[phoneNumberKey] = phoneNumber
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(isLoggedInKey)
            prefs.remove(phoneNumberKey)
        }
    }
}
