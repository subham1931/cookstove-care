package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TechnicianViewModel(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModel() {

    val assignedTasks: Flow<List<com.example.cookstovecare.data.entity.CookstoveTask>> =
        repository.getAllTasks().map { tasks ->
            android.util.Log.d("TechnicianVM", "TechnicianId: $technicianId, All tasks: ${tasks.size}")
            tasks.forEach { task ->
                android.util.Log.d("TechnicianVM", "Task ${task.id}: assignedTo=${task.assignedToTechnicianId}, status=${task.status}")
            }
            val filtered = tasks.filter { it.assignedToTechnicianId == technicianId }
            android.util.Log.d("TechnicianVM", "Filtered tasks for technician $technicianId: ${filtered.size}")
            filtered
        }

    val technicianDetails: Flow<com.example.cookstovecare.data.entity.Technician?> =
        repository.getTechnicianByIdFlow(technicianId)

    fun moveToInProgress(taskId: Long) {
        viewModelScope.launch {
            repository.moveTaskToInProgress(taskId)
        }
    }

}

class TechnicianViewModelFactory(
    private val repository: CookstoveRepository,
    private val technicianId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TechnicianViewModel::class.java)) {
            return TechnicianViewModel(repository, technicianId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
