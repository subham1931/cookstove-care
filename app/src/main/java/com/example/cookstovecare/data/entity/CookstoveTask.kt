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
    val completedAt: Long? = null,
    val distributionDate: Long? = null,
    val distributionImageUri: String? = null,
    val distributionComment: String? = null,
    val newStoveNumber: String? = null,       // New cookstove number given during replacement delivery
    val newStoveImageUri: String? = null,     // Image of new stove given during replacement delivery
    val customerReview: String? = null,       // Customer review/feedback at delivery
    val deliveryAddress: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByFieldOfficer: String? = null // Phone number of the Field Officer who created this task
) {
    val statusEnum: TaskStatus
        get() = TaskStatus.valueOf(status)
}
