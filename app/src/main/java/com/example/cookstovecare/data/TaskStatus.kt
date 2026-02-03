package com.example.cookstovecare.data

/**
 * Sealed class representing cookstove task status.
 * Used for type-safe status handling across the app.
 */
enum class TaskStatus {
    /** Task created, cookstove collected - awaiting assignment */
    COLLECTED,
    /** Task assigned to technician - awaiting work to start */
    ASSIGNED,
    /** Technician is working on the task */
    IN_PROGRESS,
    /** Repair completed for this task */
    REPAIR_COMPLETED,
    /** Replacement completed for this task */
    REPLACEMENT_COMPLETED
}
