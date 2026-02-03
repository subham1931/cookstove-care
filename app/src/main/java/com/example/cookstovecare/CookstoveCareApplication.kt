package com.example.cookstovecare

import android.app.Application
import com.example.cookstovecare.data.local.CookstoveDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository

/**
 * Application class for dependency provision.
 */
class CookstoveCareApplication : Application() {

    val dataStore by lazy { CookstoveDataStore(applicationContext) }
    val authDataStore by lazy { com.example.cookstovecare.data.local.AuthDataStore(applicationContext) }
    val repository by lazy { CookstoveRepository(dataStore) }
}
