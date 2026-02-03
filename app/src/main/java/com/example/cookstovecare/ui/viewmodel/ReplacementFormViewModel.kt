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
 * ViewModel for Replacement Form screen.
 * Validation and save logic in repository.
 */
data class ReplacementFormUiState(
    val taskId: Long = 0L,
    val oldCookstoveNumber: String = "",
    val newCookstoveNumber: String = "",
    val replacementDateMillis: Long = System.currentTimeMillis(),
    val oldCookstoveImageUri: String? = null,
    val newCookstoveImageUri: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false
)

class ReplacementFormViewModel(
    private val repository: CookstoveRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val _uiState = MutableStateFlow(ReplacementFormUiState(taskId = taskId))
    val uiState: StateFlow<ReplacementFormUiState> = _uiState.asStateFlow()

    init {
        loadTaskForOldNumber()
    }

    private fun loadTaskForOldNumber() {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            task?.let {
                _uiState.value = _uiState.value.copy(oldCookstoveNumber = it.cookstoveNumber)
            }
        }
    }

    fun updateNewCookstoveNumber(value: String) {
        _uiState.value = _uiState.value.copy(newCookstoveNumber = value, errorMessage = null)
    }

    fun setOldCookstoveImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(oldCookstoveImageUri = uri, errorMessage = null)
    }

    fun setNewCookstoveImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(newCookstoveImageUri = uri, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun submit(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val oldUri = state.oldCookstoveImageUri ?: ""
            val newUri = state.newCookstoveImageUri ?: ""

            val result = repository.saveReplacementData(
                taskId = taskId,
                oldCookstoveNumber = state.oldCookstoveNumber,
                newCookstoveNumber = state.newCookstoveNumber,
                replacementDate = state.replacementDateMillis,
                oldCookstoveImageUri = oldUri,
                newCookstoveImageUri = newUri
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

class ReplacementFormViewModelFactory(
    private val repository: CookstoveRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReplacementFormViewModel::class.java)) {
            return ReplacementFormViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
