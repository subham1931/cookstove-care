package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Repair Form screen.
 * Validation and save logic in repository.
 */
data class RepairFormUiState(
    val taskId: Long = 0L,
    val taskDataLoaded: Boolean = false,
    val cookstoveNumber: String = "",
    val collectionDateMillis: Long = 0L,
    val repairCompletionDateMillis: Long = System.currentTimeMillis(),
    val selectedParts: Set<String> = emptySet(),
    val repairNotes: String = "",
    val selectedTypesOfRepair: Set<String> = emptySet(),
    val beforeRepairImageUri: String? = null,
    val afterRepairImageUri: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false
)

class RepairFormViewModel(
    private val repository: CookstoveRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val _uiState = MutableStateFlow(RepairFormUiState(taskId = taskId))
    val uiState: StateFlow<RepairFormUiState> = _uiState.asStateFlow()

    init {
        loadTaskForCollectionDate()
    }

    private fun loadTaskForCollectionDate() {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                _uiState.value = _uiState.value.copy(
                    taskDataLoaded = true,
                    cookstoveNumber = task.cookstoveNumber,
                    collectionDateMillis = task.collectionDate,
                    repairCompletionDateMillis = maxOf(System.currentTimeMillis(), task.collectionDate),
                    beforeRepairImageUri = task.receivedProductImageUri
                )
            } else {
                _uiState.value = _uiState.value.copy(taskDataLoaded = true)
            }
        }
    }

    fun toggleTypeOfRepair(type: String) {
        val current = _uiState.value.selectedTypesOfRepair
        val updated = if (type in current) current - type else current + type
        _uiState.value = _uiState.value.copy(selectedTypesOfRepair = updated, errorMessage = null)
    }

    fun updateRepairDate(millis: Long) {
        _uiState.value = _uiState.value.copy(repairCompletionDateMillis = millis, errorMessage = null)
    }

    fun togglePart(part: String) {
        val current = _uiState.value.selectedParts
        val updated = if (part in current) current - part else current + part
        _uiState.value = _uiState.value.copy(selectedParts = updated, errorMessage = null)
    }

    fun updateRepairNotes(notes: String) {
        _uiState.value = _uiState.value.copy(repairNotes = notes, errorMessage = null)
    }

    fun setBeforeRepairImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(beforeRepairImageUri = uri, errorMessage = null)
    }

    fun setAfterRepairImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(afterRepairImageUri = uri, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun submit(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val beforeUri = state.beforeRepairImageUri ?: ""
            val afterUri = state.afterRepairImageUri ?: ""

            val result = repository.saveRepairData(
                taskId = taskId,
                repairCompletionDate = state.repairCompletionDateMillis,
                partsReplaced = state.selectedParts.toList(),
                repairNotes = state.repairNotes.ifBlank { null },
                typesOfRepair = state.selectedTypesOfRepair.toList(),
                beforeRepairImageUri = beforeUri,
                afterRepairImageUri = afterUri,
                collectionDate = state.collectionDateMillis
            )

            _uiState.value = state.copy(isLoading = false)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(saveSuccess = true)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "unknown_error")
                    onError(e.message ?: "unknown_error")
                }
            )
        }
    }
}

class RepairFormViewModelFactory(
    private val repository: CookstoveRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepairFormViewModel::class.java)) {
            return RepairFormViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
