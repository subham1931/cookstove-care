package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUri: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

class EditProfileViewModel(
    private val authDataStore: AuthDataStore,
    private val repository: CookstoveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var userRole: UserRole = UserRole.FIELD_OFFICER
    private var technicianId: Long? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRole = authDataStore.userRole.first()
            technicianId = authDataStore.technicianId.first()
            val phoneNumber = authDataStore.phoneNumber.first()
            val profileImageUri = authDataStore.profileImageUri.first()
            
            // For technicians, get name from technician entity
            val name = if (userRole == UserRole.TECHNICIAN && technicianId != null) {
                repository.getTechnicianById(technicianId!!)?.name ?: authDataStore.centerName.first()
            } else {
                authDataStore.centerName.first()
            }
            
            _uiState.value = _uiState.value.copy(
                name = name,
                phoneNumber = phoneNumber,
                profileImageUri = profileImageUri
            )
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun updateProfileImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(profileImageUri = uri, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "empty_name")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val trimmedName = state.name.trim()
                
                // For technicians, update the technician entity
                if (userRole == UserRole.TECHNICIAN && technicianId != null) {
                    val technician = repository.getTechnicianById(technicianId!!)
                    if (technician != null) {
                        repository.updateTechnician(technician.copy(name = trimmedName))
                    }
                }
                
                // Always update auth data store (for centerName and profileImageUri)
                authDataStore.updateProfile(
                    centerName = trimmedName,
                    profileImageUri = state.profileImageUri
                )
                
                _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error")
            }
        }
    }
}

class EditProfileViewModelFactory(
    private val authDataStore: AuthDataStore,
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            return EditProfileViewModel(authDataStore, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
