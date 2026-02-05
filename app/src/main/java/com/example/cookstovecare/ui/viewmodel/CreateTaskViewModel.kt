package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Create Task screen.
 * Validation logic delegated to repository; ViewModel only coordinates.
 */
data class CreateTaskUiState(
    val cookstoveNumber: String = "",
    val customerName: String = "",
    val isCustomerNameAutoFilled: Boolean = false,
    val collectionDateMillis: Long = System.currentTimeMillis(),
    val receivedProductImageUri: String? = null,
    val typeOfProcess: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isLookingUpCustomer: Boolean = false,
    val createdTaskId: Long? = null
)

class CreateTaskViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    // Static mock data for customer lookup - replace with backend API call later
    private val mockCustomerDatabase = mapOf(
        "123454321" to "Rahul Kumar",
        "987654321" to "Priya Sharma",
        "111222333" to "Amit Singh",
        "444555666" to "Sneha Patel",
        "777888999" to "Vijay Reddy"
    )

    fun updateCookstoveNumber(value: String) {
        _uiState.value = _uiState.value.copy(cookstoveNumber = value, errorMessage = null)
        // Lookup customer name when cookstove number is entered
        lookupCustomerName(value)
    }

    private fun lookupCustomerName(cookstoveNumber: String) {
        if (cookstoveNumber.isBlank()) {
            // Clear auto-filled name if cookstove number is cleared
            if (_uiState.value.isCustomerNameAutoFilled) {
                _uiState.value = _uiState.value.copy(
                    customerName = "",
                    isCustomerNameAutoFilled = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLookingUpCustomer = true)
            
            // Simulate network delay (remove when connecting to real backend)
            kotlinx.coroutines.delay(300)
            
            // TODO: Replace with actual backend API call
            // val customerName = api.getCustomerByCookstoveNumber(cookstoveNumber)
            val customerName = mockCustomerDatabase[cookstoveNumber]
            
            _uiState.value = _uiState.value.copy(isLookingUpCustomer = false)
            
            if (customerName != null) {
                _uiState.value = _uiState.value.copy(
                    customerName = customerName,
                    isCustomerNameAutoFilled = true
                )
            }
        }
    }

    fun updateCustomerName(value: String) {
        _uiState.value = _uiState.value.copy(
            customerName = value,
            isCustomerNameAutoFilled = false // User is manually editing
        )
    }

    fun updateCollectionDate(millis: Long) {
        _uiState.value = _uiState.value.copy(collectionDateMillis = millis)
    }

    fun setReceivedProductImageUri(uri: String?) {
        _uiState.value = _uiState.value.copy(receivedProductImageUri = uri?.takeIf { it.isNotBlank() })
    }

    fun setTypeOfProcess(type: String?) {
        _uiState.value = _uiState.value.copy(typeOfProcess = type?.takeIf { it.isNotBlank() })
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveTask(onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.clearCompletedData()
            val state = _uiState.value
            val result = repository.createTask(
                cookstoveNumber = state.cookstoveNumber,
                customerName = state.customerName.ifBlank { null },
                collectionDate = state.collectionDateMillis,
                receivedProductImageUri = state.receivedProductImageUri,
                typeOfProcess = state.typeOfProcess
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { taskId ->
                    // Show Repair/Replacement options on same screen
                    _uiState.value = _uiState.value.copy(createdTaskId = taskId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "unknown_error")
                    onError(e.message ?: "unknown_error")
                }
            )
        }
    }

    fun resetAfterNavigation() {
        _uiState.value = CreateTaskUiState()
    }
}

class CreateTaskViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTaskViewModel::class.java)) {
            return CreateTaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
