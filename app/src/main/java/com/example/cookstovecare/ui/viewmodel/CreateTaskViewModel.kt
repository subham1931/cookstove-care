package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cookstovecare.data.local.AuthDataStore
import com.example.cookstovecare.data.local.FieldOfficerInfo
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val deliveryAddress: String = "",
    val isAddressAutoFilled: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isLookingUpCustomer: Boolean = false,
    val createdTaskId: Long? = null,
    val fieldOfficers: List<FieldOfficerInfo> = emptyList(),
    val selectedFieldOfficerId: String? = null // Phone number of selected field officer
)

class CreateTaskViewModel(
    private val repository: CookstoveRepository,
    private val authDataStore: AuthDataStore
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

    // Static mock data for address lookup - replace with backend API call later
    private val mockAddressDatabase = mapOf(
        "123454321" to "Tikabali, Kandhamal, Odisha 762002",
        "987654321" to "Rairakhol, Sambalpur, Odisha 768113",
        "111222333" to "Phulbani, Kandhamal, Odisha 762001",
        "444555666" to "Joda, Keonjhar, Odisha 758034",
        "777888999" to "Baliguda, Kandhamal, Odisha 762103"
    )

    fun updateCookstoveNumber(value: String) {
        _uiState.value = _uiState.value.copy(cookstoveNumber = value, errorMessage = null)
        // Lookup customer name when cookstove number is entered
        lookupCustomerName(value)
    }

    private fun lookupCustomerName(cookstoveNumber: String) {
        if (cookstoveNumber.isBlank()) {
            // Clear auto-filled fields if cookstove number is cleared
            val current = _uiState.value
            _uiState.value = current.copy(
                customerName = if (current.isCustomerNameAutoFilled) "" else current.customerName,
                isCustomerNameAutoFilled = false,
                deliveryAddress = if (current.isAddressAutoFilled) "" else current.deliveryAddress,
                isAddressAutoFilled = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLookingUpCustomer = true)
            
            // Simulate network delay (remove when connecting to real backend)
            kotlinx.coroutines.delay(300)
            
            // TODO: Replace with actual backend API call
            val customerName = mockCustomerDatabase[cookstoveNumber]
            val address = mockAddressDatabase[cookstoveNumber]
            
            _uiState.value = _uiState.value.copy(isLookingUpCustomer = false)
            
            if (customerName != null) {
                _uiState.value = _uiState.value.copy(
                    customerName = customerName,
                    isCustomerNameAutoFilled = true
                )
            }
            if (address != null) {
                _uiState.value = _uiState.value.copy(
                    deliveryAddress = address,
                    isAddressAutoFilled = true
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

    fun updateDeliveryAddress(value: String) {
        _uiState.value = _uiState.value.copy(
            deliveryAddress = value,
            isAddressAutoFilled = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Load all registered field officers for the dropdown selector.
     * Called when the form is opened by a Field Coordinator.
     */
    fun loadFieldOfficers() {
        viewModelScope.launch {
            val officers = authDataStore.getAllFieldOfficers()
            _uiState.value = _uiState.value.copy(fieldOfficers = officers)
        }
    }

    fun setSelectedFieldOfficer(phoneNumber: String?) {
        _uiState.value = _uiState.value.copy(selectedFieldOfficerId = phoneNumber)
    }

    fun saveTask(onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.clearCompletedData()
            val state = _uiState.value
            // Use selected field officer if set (Field Coordinator flow), otherwise use current user
            val fieldOfficerPhone = state.selectedFieldOfficerId
                ?: authDataStore.phoneNumber.first().takeIf { it.isNotBlank() }
            val result = repository.createTask(
                cookstoveNumber = state.cookstoveNumber,
                customerName = state.customerName.ifBlank { null },
                collectionDate = state.collectionDateMillis,
                receivedProductImageUri = state.receivedProductImageUri,
                typeOfProcess = state.typeOfProcess,
                createdByFieldOfficer = fieldOfficerPhone,
                deliveryAddress = state.deliveryAddress.ifBlank { null }
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
    private val repository: CookstoveRepository,
    private val authDataStore: AuthDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateTaskViewModel::class.java)) {
            return CreateTaskViewModel(repository, authDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
