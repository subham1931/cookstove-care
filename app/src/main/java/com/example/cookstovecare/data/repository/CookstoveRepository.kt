package com.example.cookstovecare.data.repository

import com.example.cookstovecare.data.TaskStatus
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.RepairData
import com.example.cookstovecare.data.entity.ReplacementData
import com.example.cookstovecare.data.entity.Technician
import com.example.cookstovecare.data.local.CookstoveDataStore
import com.example.cookstovecare.data.local.TechnicianDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Technician with assigned task count for supervisor display.
 */
data class TechnicianWithCount(
    val technician: Technician,
    val assignedTaskCount: Int
)

/**
 * Repository for cookstove tasks, repair and replacement data.
 * All operations work offline via DataStore.
 */
class CookstoveRepository(
    private val dataStore: CookstoveDataStore,
    private val technicianDataStore: TechnicianDataStore
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

    /** Removes all completed tasks and their repair/replacement data from storage. */
    suspend fun clearCompletedData() {
        dataStore.clearCompletedData()
    }

    /** Removes all tasks and their repair/replacement data. Use for starting fresh. */
    suspend fun clearAllData() {
        dataStore.clearAllData()
    }

    suspend fun updateTaskStatus(taskId: Long, status: String) {
        dataStore.updateTaskStatus(taskId, status)
    }

    /** Move task to IN_PROGRESS and save start timestamp. */
    suspend fun moveTaskToInProgress(taskId: Long) {
        dataStore.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS.name, workStartedAt = System.currentTimeMillis())
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
        // Image is now optional for technicians
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
        dataStore.updateTaskStatus(taskId, TaskStatus.REPAIR_COMPLETED.name, completedAt = System.currentTimeMillis())
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
        dataStore.updateTaskStatus(taskId, TaskStatus.REPLACEMENT_COMPLETED.name, completedAt = System.currentTimeMillis())
        return Result.success(Unit)
    }

    suspend fun assignTaskToTechnician(taskId: Long, technicianId: Long) {
        dataStore.assignTaskToTechnician(taskId, technicianId)
    }

    suspend fun updateTaskReturn(taskId: Long, returnDate: Long, returnImageUri: String?) {
        dataStore.updateTaskReturn(taskId, returnDate, returnImageUri)
    }

    suspend fun completeOrderDistribution(taskId: Long, distributionImageUri: String?) {
        dataStore.updateTaskDistribution(taskId, System.currentTimeMillis(), distributionImageUri)
    }

    fun getAllTechnicians(): Flow<List<Technician>> = technicianDataStore.allTechnicians

    fun getActiveTechnicians(): Flow<List<Technician>> = technicianDataStore.activeTechnicians

    fun getTechniciansWithAssignedCounts(): Flow<List<TechnicianWithCount>> =
        combine(technicianDataStore.allTechnicians, dataStore.allTasks) { techs, tasks ->
            techs.map { tech ->
                val count = tasks.count {
                    it.assignedToTechnicianId == tech.id &&
                        it.statusEnum != TaskStatus.REPAIR_COMPLETED &&
                        it.statusEnum != TaskStatus.REPLACEMENT_COMPLETED
                }
                TechnicianWithCount(technician = tech, assignedTaskCount = count)
            }
        }

    suspend fun getTechnicianById(id: Long): Technician? = technicianDataStore.getTechnicianById(id)

    fun getTechnicianByIdFlow(id: Long): Flow<Technician?> =
        technicianDataStore.allTechnicians.map { techs -> techs.find { it.id == id } }

    suspend fun getTechnicianByPhoneNumber(phoneNumber: String): Technician? =
        technicianDataStore.getTechnicianByPhoneNumber(phoneNumber)

    suspend fun getAssignedTaskCount(technicianId: Long): Int {
        val tasks = dataStore.allTasks.first()
        return tasks.count { it.assignedToTechnicianId == technicianId && it.statusEnum != com.example.cookstovecare.data.TaskStatus.REPAIR_COMPLETED && it.statusEnum != com.example.cookstovecare.data.TaskStatus.REPLACEMENT_COMPLETED }
    }

    suspend fun createTechnician(
        name: String,
        phoneNumber: String,
        skillType: com.example.cookstovecare.data.TechnicianSkillType = com.example.cookstovecare.data.TechnicianSkillType.BOTH,
        isActive: Boolean = true
    ): Result<Long> {
        val trimmedName = name.trim()
        val trimmedPhone = phoneNumber.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("empty_name"))
        if (trimmedPhone.isEmpty()) return Result.failure(IllegalArgumentException("empty_phone"))
        val technician = Technician(name = trimmedName, phoneNumber = trimmedPhone, skillType = skillType, isActive = isActive)
        val id = technicianDataStore.insertTechnician(technician)
        return Result.success(id)
    }

    suspend fun updateTechnician(technician: Technician): Result<Unit> {
        val trimmedName = technician.name.trim()
        val trimmedPhone = technician.phoneNumber.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("empty_name"))
        if (trimmedPhone.isEmpty()) return Result.failure(IllegalArgumentException("empty_phone"))
        technicianDataStore.updateTechnician(technician)
        return Result.success(Unit)
    }

    suspend fun setTechnicianActive(technicianId: Long, isActive: Boolean): Result<Unit> {
        val activeCount = getAssignedTaskCount(technicianId)
        if (!isActive && activeCount > 0) {
            return Result.failure(IllegalArgumentException("cannot_disable_technician_with_active_tasks"))
        }
        technicianDataStore.setTechnicianActive(technicianId, isActive)
        return Result.success(Unit)
    }
}
