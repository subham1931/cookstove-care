package com.example.cookstovecare.data

/**
 * Sealed class representing cookstove task status.
 * Used for type-safe status handling across the app.
 */
enum class TaskStatus {
    /** Task created, cookstove collected - awaiting repair or replacement */
    COLLECTED,
    /** Repair completed for this task */
    REPAIR_COMPLETED,
    /** Replacement completed for this task */
    REPLACEMENT_COMPLETED
}
