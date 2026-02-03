package com.example.cookstovecare.data.entity

import com.example.cookstovecare.data.TaskStatus

/**
 * Entity representing a cookstove repair/replacement task.
 * Stored locally via DataStore for offline support.
 */
data class CookstoveTask(
    val id: Long = 0,
    val cookstoveNumber: String,
    val customerName: String? = null,
    val collectionDate: Long,
    val status: String = "COLLECTED",
    val receivedProductImageUri: String? = null,
    val typeOfProcess: String? = null,
    val assignedToTechnicianId: Long? = null,
    val workStartedAt: Long? = null,
    val returnDate: Long? = null,
    val returnImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: TaskStatus
        get() = TaskStatus.valueOf(status)
}
