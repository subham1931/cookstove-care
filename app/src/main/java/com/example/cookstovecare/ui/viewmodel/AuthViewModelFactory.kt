package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cookstovecare.data.local.AuthDataStore

class AuthViewModelFactory(
    private val authDataStore: AuthDataStore,
    private val repository: com.example.cookstovecare.data.repository.CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authDataStore, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
