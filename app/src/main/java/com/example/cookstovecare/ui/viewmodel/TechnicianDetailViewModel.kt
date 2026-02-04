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

data class TechnicianDetailUiState(
    val technician: Technician? = null,
    val assignedTaskCount: Int = 0,
    val snackbarMessage: String? = null,
    val isLoading: Boolean = true
)

class TechnicianDetailViewModel(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechnicianDetailUiState())
    val uiState: StateFlow<TechnicianDetailUiState> = _uiState.asStateFlow()

    init {
        loadTechnician()
    }

    private fun loadTechnician() {
        viewModelScope.launch {
            val tech = repository.getTechnicianById(technicianId)
            val count = repository.getAssignedTaskCount(technicianId)
            _uiState.value = _uiState.value.copy(
                technician = tech,
                assignedTaskCount = count,
                isLoading = false
            )
        }
    }

    fun setTechnicianActive(isActive: Boolean) {
        val tech = _uiState.value.technician ?: return
        viewModelScope.launch {
            repository.setTechnicianActive(technicianId, isActive)
                .fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            technician = tech.copy(isActive = isActive)
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            snackbarMessage = e.message ?: "Error"
                        )
                    }
                )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

class TechnicianDetailViewModelFactory(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TechnicianDetailViewModel::class.java)) {
            return TechnicianDetailViewModel(repository, technicianId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
