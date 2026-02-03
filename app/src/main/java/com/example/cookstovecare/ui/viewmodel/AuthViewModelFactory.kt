package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cookstovecare.data.local.AuthDataStore

class AuthViewModelFactory(
    private val authDataStore: AuthDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
