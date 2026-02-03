package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditTaskUiState(
    val taskId: Long = 0L,
    val cookstoveNumber: String = "",
    val customerName: String = "",
    val collectionDateMillis: Long = System.currentTimeMillis(),
    val receivedProductImageUri: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val updateSuccess: Boolean = false
)

class EditTaskViewModel(
    private val repository: CookstoveRepository,
    taskId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTaskUiState(taskId = taskId))
    val uiState: StateFlow<EditTaskUiState> = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val task = repository.getTaskById(_uiState.value.taskId)
            task?.let {
                _uiState.value = EditTaskUiState(
                    taskId = it.id,
                    cookstoveNumber = it.cookstoveNumber,
                    customerName = it.customerName ?: "",
                    collectionDateMillis = it.collectionDate,
                    receivedProductImageUri = it.receivedProductImageUri,
                    isLoading = false
                )
            } ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateCookstoveNumber(value: String) {
        _uiState.value = _uiState.value.copy(cookstoveNumber = value, errorMessage = null)
    }

    fun updateCustomerName(value: String) {
        _uiState.value = _uiState.value.copy(customerName = value)
    }

    fun updateCollectionDate(millis: Long) {
        _uiState.value = _uiState.value.copy(collectionDateMillis = millis, errorMessage = null)
    }

    fun setReceivedProductImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(receivedProductImageUri = uri?.takeIf { it.isNotBlank() })
    }

    fun updateTask(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = repository.updateTask(
                taskId = state.taskId,
                cookstoveNumber = state.cookstoveNumber,
                customerName = state.customerName.ifBlank { null },
                collectionDate = state.collectionDateMillis,
                receivedProductImageUri = state.receivedProductImageUri
            )
            _uiState.value = state.copy(isLoading = false)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(updateSuccess = true)
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

class EditTaskViewModelFactory(
    private val repository: CookstoveRepository,
    private val taskId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTaskViewModel::class.java)) {
            return EditTaskViewModel(repository, taskId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
