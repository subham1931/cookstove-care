package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.RepairData
import com.example.cookstovecare.data.entity.ReplacementData
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Task Detail screen.
 */
data class TaskDetailUiState(
    val task: CookstoveTask? = null,
    val repairData: RepairData? = null,
    val replacementData: ReplacementData? = null,
    val assignedTechnicianName: String? = null,
    val isLoading: Boolean = true
)

class TaskDetailViewModel(
    private val repository: CookstoveRepository,
    taskId: Long
) : ViewModel() {

    private val _taskId: Long = taskId

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTaskDetail()
    }

    /** Call to refresh task data (e.g. when returning from Assign Task screen). */
    fun refresh() {
        loadTaskDetail()
    }

    private fun loadTaskDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val task = repository.getTaskById(_taskId)
            val repairData = task?.let { repository.getRepairDataByTaskId(it.id) }
            val replacementData = task?.let { repository.getReplacementDataByTaskId(it.id) }
            val assignedName = task?.assignedToTechnicianId?.let { id ->
                repository.getTechnicianById(id)?.name
            }
            _uiState.value = TaskDetailUiState(
                task = task,
                repairData = repairData,
                replacementData = replacementData,
                assignedTechnicianName = assignedName,
                isLoading = false
            )
        }
    }

    val canProceedToRepairOrReplacement: Boolean
        get() = _uiState.value.task?.statusEnum == TaskStatus.COLLECTED ||
                _uiState.value.task?.statusEnum == TaskStatus.IN_PROGRESS
}

class TaskDetailViewModelFactory(
    private val repository: CookstoveRepository,
    private val taskId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
            return TaskDetailViewModel(repository, taskId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
