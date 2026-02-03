package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cookstovecare.data.repository.CookstoveRepository

/**
 * ViewModel for Supervisor Task List screen.
 * Provides tasks and technicians for display and filtering.
 */
class SupervisorTaskListViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    val tasks = repository.getAllTasks()
    val technicians = repository.getAllTechnicians()
}

class SupervisorTaskListViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupervisorTaskListViewModel::class.java)) {
            return SupervisorTaskListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
