package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.TechnicianSkillType
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateTechnicianUiState(
    val name: String = "",
    val phoneNumber: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
)

class CreateTechnicianViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTechnicianUiState())
    val uiState: StateFlow<CreateTechnicianUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, error = null)
    }

    fun create(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "empty_name")
            return
        }
        if (state.phoneNumber.isBlank()) {
            _uiState.value = state.copy(error = "empty_phone")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            repository.createTechnician(state.name, state.phoneNumber, TechnicianSkillType.BOTH, isActive = true)
                .fold(
                    onSuccess = {
                        _uiState.value = state.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.value = state.copy(isLoading = false, error = e.message ?: "Error")
                    }
                )
        }
    }
}

class CreateTechnicianViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTechnicianViewModel::class.java)) {
            return CreateTechnicianViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
