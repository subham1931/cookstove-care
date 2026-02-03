package com.example.cookstovecare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cookstovecare.data.entity.CookstoveTask
import com.example.cookstovecare.data.entity.RepairData
import com.example.cookstovecare.data.entity.ReplacementData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cookstovecare_data")

/**
 * DataStore-based local storage for cookstove tasks.
 * All operations work offline. Stores JSON via Gson.
 */
class CookstoveDataStore(private val context: Context) {

    private val gson = Gson()
    private val taskListType = object : TypeToken<ArrayList<CookstoveTaskDto>>() {}.type
    private val repairMapType = object : TypeToken<LinkedHashMap<String, RepairDataDto>>() {}.type
    private val replacementMapType = object : TypeToken<LinkedHashMap<String, ReplacementDataDto>>() {}.type

    private val tasksKey = stringPreferencesKey("tasks")
    private val repairsKey = stringPreferencesKey("repairs")
    private val replacementsKey = stringPreferencesKey("replacements")

    val allTasks: Flow<List<CookstoveTask>> = context.dataStore.data.map { prefs ->
        val json = prefs[tasksKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        val dtos: List<CookstoveTaskDto> = (gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()
        dtos.map { it.toEntity() }.sortedByDescending { it.createdAt }
    }

    suspend fun getTaskById(id: Long): CookstoveTask? {
        val prefs = context.dataStore.data.first()
        val json = prefs[tasksKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        val dtos: List<CookstoveTaskDto> = (gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()
        return dtos.find { it.id == id }?.toEntity()
    }

    suspend fun hasActiveTaskWithNumber(number: String): Boolean {
        val prefs = context.dataStore.data.first()
        val json = prefs[tasksKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        val dtos: List<CookstoveTaskDto> = (gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()
        return dtos.any { it.cookstoveNumber == number && it.status == "COLLECTED" }
    }

    suspend fun hasTaskWithNumber(number: String): Boolean {
        val prefs = context.dataStore.data.first()
        val json = prefs[tasksKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        val dtos: List<CookstoveTaskDto> = (gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()
        return dtos.any { it.cookstoveNumber == number }
    }

    suspend fun insertTask(task: CookstoveTask): Long {
        val prefs = context.dataStore.data.first()
        val json = prefs[tasksKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        val dtos: List<CookstoveTaskDto> = (gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()
        val maxId = dtos.maxOfOrNull { it.id } ?: 0L
        val newId = maxId + 1
        val taskWithId = task.copy(id = newId)
        context.dataStore.edit { editPrefs ->
            val editJson = editPrefs[tasksKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val editDtos: MutableList<CookstoveTaskDto> = ((gson.fromJson(editJson, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()).toMutableList()
            editDtos.add(CookstoveTaskDto.from(taskWithId))
            editPrefs[tasksKey] = gson.toJson(editDtos)
        }
        return taskWithId.id
    }

    suspend fun updateTask(
        taskId: Long,
        cookstoveNumber: String,
        customerName: String?,
        collectionDate: Long,
        receivedProductImageUri: String? = null
    ) {
        context.dataStore.edit { editPrefs ->
            val json = editPrefs[tasksKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val dtos: MutableList<CookstoveTaskDto> = ((gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()).toMutableList()
            val idx = dtos.indexOfFirst { it.id == taskId }
            if (idx >= 0) {
                val existing = dtos[idx]
                val newImageUri = receivedProductImageUri?.takeIf { it.isNotBlank() }
                dtos[idx] = existing.copy(
                    cookstoveNumber = cookstoveNumber,
                    customerName = customerName,
                    collectionDate = collectionDate,
                    receivedProductImageUri = newImageUri ?: existing.receivedProductImageUri
                )
                editPrefs[tasksKey] = gson.toJson(dtos)
            }
        }
    }

    suspend fun updateTaskStatus(taskId: Long, status: String) {
        context.dataStore.edit { editPrefs ->
            val json = editPrefs[tasksKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val dtos: MutableList<CookstoveTaskDto> = ((gson.fromJson(json, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()).toMutableList()
            val idx = dtos.indexOfFirst { it.id == taskId }
            if (idx >= 0) {
                dtos[idx] = dtos[idx].copy(status = status)
                editPrefs[tasksKey] = gson.toJson(dtos)
            }
        }
    }

    suspend fun getRepairDataByTaskId(taskId: Long): RepairData? {
        val prefs = context.dataStore.data.first()
        val json = prefs[repairsKey] ?: "{}"
        @Suppress("UNCHECKED_CAST")
        val map: Map<String, RepairDataDto> = (gson.fromJson(json, repairMapType) as? Map<String, RepairDataDto>) ?: emptyMap()
        return map[taskId.toString()]?.toEntity()
    }

    suspend fun getReplacementDataByTaskId(taskId: Long): ReplacementData? {
        val prefs = context.dataStore.data.first()
        val json = prefs[replacementsKey] ?: "{}"
        @Suppress("UNCHECKED_CAST")
        val map: Map<String, ReplacementDataDto> = (gson.fromJson(json, replacementMapType) as? Map<String, ReplacementDataDto>) ?: emptyMap()
        return map[taskId.toString()]?.toEntity()
    }

    suspend fun hasReplacementWithNewNumber(number: String): Boolean {
        val prefs = context.dataStore.data.first()
        val json = prefs[replacementsKey] ?: "{}"
        @Suppress("UNCHECKED_CAST")
        val map: Map<String, ReplacementDataDto> = (gson.fromJson(json, replacementMapType) as? Map<String, ReplacementDataDto>) ?: emptyMap()
        return map.values.any { it.newCookstoveNumber == number }
    }

    suspend fun insertRepair(repair: RepairData) {
        context.dataStore.edit { editPrefs ->
            val json = editPrefs[repairsKey] ?: "{}"
            @Suppress("UNCHECKED_CAST")
            val map: MutableMap<String, RepairDataDto> = ((gson.fromJson(json, repairMapType) as? Map<String, RepairDataDto>) ?: emptyMap()).toMutableMap()
            map[repair.taskId.toString()] = RepairDataDto.from(repair)
            editPrefs[repairsKey] = gson.toJson(map)
        }
    }

    suspend fun insertReplacement(replacement: ReplacementData) {
        context.dataStore.edit { editPrefs ->
            val json = editPrefs[replacementsKey] ?: "{}"
            @Suppress("UNCHECKED_CAST")
            val map: MutableMap<String, ReplacementDataDto> = ((gson.fromJson(json, replacementMapType) as? Map<String, ReplacementDataDto>) ?: emptyMap()).toMutableMap()
            map[replacement.taskId.toString()] = ReplacementDataDto.from(replacement)
            editPrefs[replacementsKey] = gson.toJson(map)
        }
    }

    suspend fun deleteTask(taskId: Long) {
        context.dataStore.edit { editPrefs ->
            val tasksJson = editPrefs[tasksKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val dtos: MutableList<CookstoveTaskDto> = ((gson.fromJson(tasksJson, taskListType) as? List<CookstoveTaskDto>) ?: emptyList()).toMutableList()
            dtos.removeAll { it.id == taskId }
            editPrefs[tasksKey] = gson.toJson(dtos)
            val repairsJson = editPrefs[repairsKey] ?: "{}"
            @Suppress("UNCHECKED_CAST")
            val repairMap: MutableMap<String, RepairDataDto> = ((gson.fromJson(repairsJson, repairMapType) as? Map<String, RepairDataDto>) ?: emptyMap()).toMutableMap()
            repairMap.remove(taskId.toString())
            editPrefs[repairsKey] = gson.toJson(repairMap)
            val replacementsJson = editPrefs[replacementsKey] ?: "{}"
            @Suppress("UNCHECKED_CAST")
            val replacementMap: MutableMap<String, ReplacementDataDto> = ((gson.fromJson(replacementsJson, replacementMapType) as? Map<String, ReplacementDataDto>) ?: emptyMap()).toMutableMap()
            replacementMap.remove(taskId.toString())
            editPrefs[replacementsKey] = gson.toJson(replacementMap)
        }
    }
}

private data class CookstoveTaskDto(
    val id: Long,
    val cookstoveNumber: String,
    val customerName: String?,
    val collectionDate: Long,
    val status: String,
    val receivedProductImageUri: String? = null,
    val typeOfProcess: String? = null,
    val createdAt: Long
) {
    fun toEntity() = CookstoveTask(
        id = id,
        cookstoveNumber = cookstoveNumber,
        customerName = customerName,
        collectionDate = collectionDate,
        status = status,
        receivedProductImageUri = receivedProductImageUri,
        typeOfProcess = typeOfProcess,
        createdAt = createdAt
    )
    companion object {
        fun from(t: CookstoveTask) = CookstoveTaskDto(
            id = t.id,
            cookstoveNumber = t.cookstoveNumber,
            customerName = t.customerName,
            collectionDate = t.collectionDate,
            status = t.status,
            receivedProductImageUri = t.receivedProductImageUri,
            typeOfProcess = t.typeOfProcess,
            createdAt = t.createdAt
        )
    }
}

private data class RepairDataDto(
    val id: Long,
    val taskId: Long,
    val repairCompletionDate: Long,
    val partsReplacedRaw: String,
    val repairNotes: String?,
    val typesOfRepairRaw: String = "",
    val typeOfRepair: String? = null,
    val beforeRepairImageUri: String,
    val afterRepairImageUri: String,
    val createdAt: Long
) {
    fun toEntity() = RepairData(
        id = id,
        taskId = taskId,
        repairCompletionDate = repairCompletionDate,
        partsReplacedRaw = partsReplacedRaw,
        repairNotes = repairNotes,
        typesOfRepairRaw = typesOfRepairRaw.ifBlank { typeOfRepair?.takeIf { it.isNotBlank() } ?: "" },
        beforeRepairImageUri = beforeRepairImageUri,
        afterRepairImageUri = afterRepairImageUri,
        createdAt = createdAt
    )
    companion object {
        fun from(r: RepairData) = RepairDataDto(
            id = r.id,
            taskId = r.taskId,
            repairCompletionDate = r.repairCompletionDate,
            partsReplacedRaw = r.partsReplacedRaw,
            repairNotes = r.repairNotes,
            typesOfRepairRaw = r.typesOfRepair.joinToString(separator = "|||"),
            beforeRepairImageUri = r.beforeRepairImageUri,
            afterRepairImageUri = r.afterRepairImageUri,
            createdAt = r.createdAt
        )
    }
}

private data class ReplacementDataDto(
    val id: Long,
    val taskId: Long,
    val oldCookstoveNumber: String,
    val newCookstoveNumber: String,
    val replacementDate: Long,
    val oldCookstoveImageUri: String,
    val newCookstoveImageUri: String,
    val createdAt: Long
) {
    fun toEntity() = ReplacementData(
        id = id,
        taskId = taskId,
        oldCookstoveNumber = oldCookstoveNumber,
        newCookstoveNumber = newCookstoveNumber,
        replacementDate = replacementDate,
        oldCookstoveImageUri = oldCookstoveImageUri,
        newCookstoveImageUri = newCookstoveImageUri,
        createdAt = createdAt
    )
    companion object {
        fun from(r: ReplacementData) = ReplacementDataDto(
            id = r.id,
            taskId = r.taskId,
            oldCookstoveNumber = r.oldCookstoveNumber,
            newCookstoveNumber = r.newCookstoveNumber,
            replacementDate = r.replacementDate,
            oldCookstoveImageUri = r.oldCookstoveImageUri,
            newCookstoveImageUri = r.newCookstoveImageUri,
            createdAt = r.createdAt
        )
    }
}
