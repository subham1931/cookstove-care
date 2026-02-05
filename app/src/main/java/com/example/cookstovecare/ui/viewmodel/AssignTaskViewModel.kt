package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssignTaskViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    val technicians = repository.getActiveTechnicians()

    fun assignTo(taskId: Long, technicianId: Long, onAssigned: () -> Unit) {
        viewModelScope.launch {
            android.util.Log.d("AssignTaskVM", "Assigning task $taskId to technician $technicianId")
            repository.assignTaskToTechnician(taskId, technicianId)
            android.util.Log.d("AssignTaskVM", "Task $taskId assigned successfully to technician $technicianId")
            onAssigned()
        }
    }
}

class AssignTaskViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignTaskViewModel::class.java)) {
            return AssignTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
