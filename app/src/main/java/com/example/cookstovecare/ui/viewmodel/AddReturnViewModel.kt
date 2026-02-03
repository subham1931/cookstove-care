package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddReturnUiState(
    val returnDateMillis: Long = System.currentTimeMillis(),
    val returnImageUri: String? = null,
    val isLoading: Boolean = false
)

class AddReturnViewModel(
    private val repository: CookstoveRepository,
    private val taskId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddReturnUiState())
    val uiState: StateFlow<AddReturnUiState> = _uiState.asStateFlow()

    fun updateReturnDate(millis: Long) {
        _uiState.value = _uiState.value.copy(returnDateMillis = millis)
    }

    fun setReturnImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(returnImageUri = uri)
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val uri = state.returnImageUri ?: return
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            repository.updateTaskReturn(taskId, state.returnDateMillis, uri)
            _uiState.value = state.copy(isLoading = false)
            onSuccess()
        }
    }
}

class AddReturnViewModelFactory(
    private val repository: CookstoveRepository,
    private val taskId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddReturnViewModel::class.java)) {
            return AddReturnViewModel(repository, taskId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
