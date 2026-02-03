package com.example.cookstovecare.data.repository

import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.RepairData
import com.example.cookstovecare.data.entity.ReplacementData
import com.example.cookstovecare.data.local.CookstoveDataStore
import kotlinx.coroutines.flow.Flow

/**
 * Repository for cookstove tasks, repair and replacement data.
 * All operations work offline via DataStore.
 */
class CookstoveRepository(
    private val dataStore: CookstoveDataStore
) {

    fun getAllTasks(): Flow<List<CookstoveTask>> = dataStore.allTasks

    suspend fun getTaskById(taskId: Long): CookstoveTask? = dataStore.getTaskById(taskId)

    suspend fun updateTask(
        taskId: Long,
        cookstoveNumber: String,
        customerName: String?,
        collectionDate: Long,
        receivedProductImageUri: String? = null
    ): Result<Unit> {
        val trimmedNumber = cookstoveNumber.trim()
        if (trimmedNumber.isEmpty()) {
            return Result.failure(IllegalArgumentException("empty_cookstove_number"))
        }
        val existingTask = dataStore.getTaskById(taskId) ?: return Result.failure(IllegalArgumentException("task_not_found"))
        if (trimmedNumber != existingTask.cookstoveNumber && dataStore.hasActiveTaskWithNumber(trimmedNumber)) {
            return Result.failure(IllegalArgumentException("duplicate_cookstove_number"))
        }
        dataStore.updateTask(
            taskId = taskId,
            cookstoveNumber = trimmedNumber,
            customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
            collectionDate = collectionDate,
            receivedProductImageUri = receivedProductImageUri?.takeIf { it.isNotBlank() }
        )
        return Result.success(Unit)
    }

    suspend fun deleteTask(taskId: Long) {
        dataStore.deleteTask(taskId)
    }

    suspend fun getRepairDataByTaskId(taskId: Long) = dataStore.getRepairDataByTaskId(taskId)

    suspend fun getReplacementDataByTaskId(taskId: Long) =
        dataStore.getReplacementDataByTaskId(taskId)

    suspend fun createTask(
        cookstoveNumber: String,
        customerName: String?,
        collectionDate: Long,
        receivedProductImageUri: String? = null,
        typeOfProcess: String? = null
    ): Result<Long> {
        val trimmedNumber = cookstoveNumber.trim()
        if (trimmedNumber.isEmpty()) {
            return Result.failure(IllegalArgumentException("empty_cookstove_number"))
        }
        if (dataStore.hasActiveTaskWithNumber(trimmedNumber)) {
            return Result.failure(IllegalArgumentException("duplicate_cookstove_number"))
        }
        val task = CookstoveTask(
            cookstoveNumber = trimmedNumber,
            customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
            collectionDate = collectionDate,
            status = TaskStatus.COLLECTED.name,
            receivedProductImageUri = receivedProductImageUri?.takeIf { it.isNotBlank() },
            typeOfProcess = typeOfProcess?.takeIf { it.isNotBlank() }
        )
        val id = dataStore.insertTask(task)
        return Result.success(id)
    }

    suspend fun saveRepairData(
        taskId: Long,
        repairCompletionDate: Long,
        partsReplaced: List<String>,
        repairNotes: String?,
        typesOfRepair: List<String>,
        beforeRepairImageUri: String,
        afterRepairImageUri: String,
        collectionDate: Long
    ): Result<Unit> {
        if (repairCompletionDate < collectionDate) {
            return Result.failure(IllegalArgumentException("repair_date_before_collection"))
        }
        val hasPartsOrNotes = partsReplaced.isNotEmpty() || !repairNotes.isNullOrBlank()
        val hasTypesOfRepair = typesOfRepair.isNotEmpty()
        if (!hasPartsOrNotes && !hasTypesOfRepair) {
            return Result.failure(IllegalArgumentException("parts_or_notes_or_type_required"))
        }
        if (beforeRepairImageUri.isBlank() || afterRepairImageUri.isBlank()) {
            return Result.failure(IllegalArgumentException("images_required"))
        }
        val repairData = RepairData(
            taskId = taskId,
            repairCompletionDate = repairCompletionDate,
            partsReplacedRaw = partsReplaced.joinToString(separator = "|||"),
            repairNotes = repairNotes?.trim()?.takeIf { it.isNotEmpty() },
            typesOfRepairRaw = typesOfRepair.filter { it.isNotBlank() }.joinToString(separator = "|||"),
            beforeRepairImageUri = beforeRepairImageUri,
            afterRepairImageUri = afterRepairImageUri
        )
        dataStore.insertRepair(repairData)
        dataStore.updateTaskStatus(taskId, TaskStatus.REPAIR_COMPLETED.name)
        return Result.success(Unit)
    }

    suspend fun saveReplacementData(
        taskId: Long,
        oldCookstoveNumber: String,
        newCookstoveNumber: String,
        collectedDate: Long,
        replacementDate: Long,
        oldCookstoveImageUri: String,
        newCookstoveImageUri: String
    ): Result<Unit> {
        if (oldCookstoveNumber.trim() == newCookstoveNumber.trim()) {
            return Result.failure(IllegalArgumentException("old_new_numbers_same"))
        }
        if (dataStore.hasReplacementWithNewNumberExcludingTaskId(taskId, newCookstoveNumber.trim())) {
            return Result.failure(IllegalArgumentException("duplicate_new_cookstove_number"))
        }
        if (dataStore.hasTaskWithNumber(newCookstoveNumber.trim())) {
            return Result.failure(IllegalArgumentException("duplicate_new_cookstove_number"))
        }
        if (oldCookstoveImageUri.isBlank() || newCookstoveImageUri.isBlank()) {
            return Result.failure(IllegalArgumentException("images_required"))
        }
        val replacementData = ReplacementData(
            taskId = taskId,
            oldCookstoveNumber = oldCookstoveNumber.trim(),
            newCookstoveNumber = newCookstoveNumber.trim(),
            collectedDate = collectedDate,
            replacementDate = replacementDate,
            oldCookstoveImageUri = oldCookstoveImageUri,
            newCookstoveImageUri = newCookstoveImageUri
        )
        dataStore.insertReplacement(replacementData)
        dataStore.updateTaskStatus(taskId, TaskStatus.REPLACEMENT_COMPLETED.name)
        return Result.success(Unit)
    }
}
