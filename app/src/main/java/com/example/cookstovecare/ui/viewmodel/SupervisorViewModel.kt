package com.example.cookstovecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cookstovecare.data.repository.CookstoveRepository
import kotlinx.coroutines.flow.StateFlow

class SupervisorViewModel(
    private val repository: CookstoveRepository
) : ViewModel() {

    val tasks = repository.getAllTasks()
}

class SupervisorViewModelFactory(
    private val repository: CookstoveRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupervisorViewModel::class.java)) {
            return SupervisorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
