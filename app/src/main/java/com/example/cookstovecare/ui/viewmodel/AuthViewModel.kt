package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.local.AuthDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthError {
    PHONE_REQUIRED,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    PHONE_ALREADY_REGISTERED,
    LOGIN_FAILED
}

data class AuthUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val centerName: String = "",
    val confirmPassword: String = "",
    val selectedRole: UserRole = UserRole.FIELD_OFFICER,
    val isLoading: Boolean = false,
    val error: AuthError? = null
)

/**
 * ViewModel for repair center authentication.
 * Supports login and sign up.
 */
class AuthViewModel(
    private val authDataStore: AuthDataStore,
    private val repository: com.example.cookstovecare.data.repository.CookstoveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun updateCenterName(centerName: String) {
        _uiState.value = _uiState.value.copy(centerName = centerName, error = null)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, error = null)
    }

    fun updateSelectedRole(role: UserRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role, error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun login(onSuccess: (UserRole) -> Unit) {
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
                    val techId = if (state.selectedRole == UserRole.TECHNICIAN) {
                        repository.getTechnicianByPhoneNumber(phone)?.id
                    } else null
                    authDataStore.setLoggedIn(phoneNumber = phone, role = state.selectedRole, technicianId = techId)
                    _uiState.value = state.copy(isLoading = false)
                    onSuccess(state.selectedRole)
                }
            }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val state = _uiState.value
        val phone = state.phoneNumber.trim()
        val password = state.password.trim()
        val confirmPassword = state.confirmPassword.trim()
        val centerName = state.centerName.trim().takeIf { it.isNotBlank() }

        when {
            phone.isBlank() -> _uiState.value = state.copy(error = AuthError.PHONE_REQUIRED)
            password.isBlank() -> _uiState.value = state.copy(error = AuthError.PASSWORD_REQUIRED)
            password.length < 6 -> _uiState.value = state.copy(error = AuthError.PASSWORD_TOO_SHORT)
            password != confirmPassword -> _uiState.value = state.copy(error = AuthError.PASSWORD_MISMATCH)
            else -> {
                _uiState.value = state.copy(isLoading = true, error = null)
                viewModelScope.launch {
                    authDataStore.registerUser(
                        phoneNumber = phone,
                        password = password,
                        centerName = centerName
                    ).fold(
                        onSuccess = {
                            authDataStore.setLoggedIn(phoneNumber = phone, centerName = centerName)
                            _uiState.value = state.copy(isLoading = false)
                            onSuccess()
                        },
                        onFailure = {
                            _uiState.value = state.copy(
                                isLoading = false,
                                error = AuthError.PHONE_ALREADY_REGISTERED
                            )
                        }
                    )
                }
            }
        }
    }
}
