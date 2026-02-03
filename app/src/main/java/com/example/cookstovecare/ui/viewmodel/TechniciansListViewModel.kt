package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TechniciansListViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    val techniciansWithCounts = repository.getTechniciansWithAssignedCounts()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setTechnicianActive(technicianId: Long, isActive: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.setTechnicianActive(technicianId, isActive)
                .fold(
                    onSuccess = { onSuccess() },
                    onFailure = { e ->
                        _snackbarMessage.value = when (e.message) {
                            "cannot_disable_technician_with_active_tasks" ->
                                "cannot_disable_technician_with_active_tasks"
                            else -> e.message
                        }
                    }
                )
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

class TechniciansListViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TechniciansListViewModel::class.java)) {
            return TechniciansListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
