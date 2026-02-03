package com.example.cookstovecare.data.entity

/**
 * Entity for replacement completion data.
 * Stores image URIs (not bitmaps) for offline support.
 */
data class ReplacementData(
    val id: Long = 0,
    val taskId: Long,
    val oldCookstoveNumber: String,
    val newCookstoveNumber: String,
    val collectedDate: Long,
    val replacementDate: Long,
    val oldCookstoveImageUri: String,
    val newCookstoveImageUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
