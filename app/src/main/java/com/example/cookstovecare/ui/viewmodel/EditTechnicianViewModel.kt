package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.entity.Technician
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditTechnicianUiState(
    val technician: Technician? = null,
    val name: String = "",
    val phoneNumber: String = "",
    val error: String? = null,
    val isLoading: Boolean = false
)

class EditTechnicianViewModel(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTechnicianUiState())
    val uiState: StateFlow<EditTechnicianUiState> = _uiState.asStateFlow()

    init {
        loadTechnician()
    }

    private fun loadTechnician() {
        viewModelScope.launch {
            val tech = repository.getTechnicianById(technicianId)
            tech?.let {
                _uiState.value = _uiState.value.copy(
                    technician = it,
                    name = it.name,
                    phoneNumber = it.phoneNumber
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phone, error = null)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val tech = state.technician ?: return
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
            val updatedTech = tech.copy(
                name = state.name.trim(),
                phoneNumber = state.phoneNumber.trim(),
                skillType = tech.skillType, // preserve; no longer editable
                isActive = tech.isActive // preserve current status; change only via detail page
            )
            repository.updateTechnician(updatedTech).fold(
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

class EditTechnicianViewModelFactory(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTechnicianViewModel::class.java)) {
            return EditTechnicianViewModel(repository, technicianId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
