package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.local.AuthDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthError {
    PHONE_REQUIRED,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT
}

data class AuthUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null
)

/**
 * ViewModel for repair center authentication.
 * Phone number and password only; login persists until logout.
 */
class AuthViewModel(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        val phone = state.phoneNumber.trim()
        val password = state.password.trim()

        when {
            phone.isBlank() -> _uiState.value = state.copy(error = AuthError.PHONE_REQUIRED)
            password.isBlank() -> _uiState.value = state.copy(error = AuthError.PASSWORD_REQUIRED)
            password.length < 6 -> _uiState.value = state.copy(error = AuthError.PASSWORD_TOO_SHORT)
            else -> {
                _uiState.value = state.copy(isLoading = true, error = null)
                viewModelScope.launch {
                    authDataStore.setLoggedIn(phoneNumber = phone)
                    _uiState.value = state.copy(isLoading = false)
                    onSuccess()
                }
            }
        }
    }
}
