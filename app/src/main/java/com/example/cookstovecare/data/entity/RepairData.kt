package com.example.cookstovecare.data.entity

/**
 * Entity for repair completion data.
 * Stores image URIs (not bitmaps) for offline support.
 * partsReplaced stored as pipe-separated string.
 */
data class RepairData(
    val id: Long = 0,
    val taskId: Long,
    val repairCompletionDate: Long,
    val partsReplacedRaw: String,
    val repairNotes: String? = null,
    val typesOfRepairRaw: String = "",
    val beforeRepairImageUri: String,
    val afterRepairImageUri: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val partsReplaced: List<String>
        get() = if (partsReplacedRaw.isEmpty()) emptyList() else partsReplacedRaw.split("|||")

    val typesOfRepair: List<String>
        get() = if (typesOfRepairRaw.isEmpty()) emptyList() else typesOfRepairRaw.split("|||")
}
