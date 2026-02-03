package com.example.cookstovecare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cookstovecare.data.TechnicianSkillType
import com.example.cookstovecare.data.entity.Technician
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.technicianDataStore: DataStore<Preferences> by preferencesDataStore(name = "technicians_data")

private data class TechnicianDto(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val skillType: String = TechnicianSkillType.BOTH.name,
    val isActive: Boolean = true,
    val createdAt: Long
) {
    fun toEntity() = Technician(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        skillType = try { TechnicianSkillType.valueOf(skillType) } catch (_: Exception) { TechnicianSkillType.BOTH },
        isActive = isActive,
        createdAt = createdAt
    )
}

class TechnicianDataStore(private val context: Context) {
    private val gson = Gson()
    private val listType = object : TypeToken<ArrayList<TechnicianDto>>() {}.type
    private val techniciansKey = stringPreferencesKey("technicians")

    val allTechnicians: Flow<List<Technician>> = context.technicianDataStore.data.map { prefs ->
        val json = prefs[techniciansKey] ?: "[]"
        val dtos = parseDtos(json)
        dtos.map { it.toEntity() }.sortedBy { it.name }
    }

    val activeTechnicians: Flow<List<Technician>> = context.technicianDataStore.data.map { prefs ->
        val json = prefs[techniciansKey] ?: "[]"
        val dtos = parseDtos(json)
        dtos.filter { it.isActive }.map { it.toEntity() }.sortedBy { it.name }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDtos(json: String): List<TechnicianDto> {
        val rawList = gson.fromJson(json, object : TypeToken<ArrayList<Map<String, Any?>>>() {}.type) as? List<Map<String, Any?>>
            ?: return emptyList()
        return rawList.map { map ->
            TechnicianDto(
                id = (map["id"] as? Number)?.toLong() ?: 0L,
                name = map["name"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                skillType = map["skillType"] as? String ?: TechnicianSkillType.BOTH.name,
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun getTechnicianById(id: Long): Technician? {
        val prefs = context.technicianDataStore.data.first()
        val json = prefs[techniciansKey] ?: "[]"
        val dtos = parseDtos(json)
        return dtos.find { it.id == id }?.toEntity()
    }

    suspend fun getTechnicianByPhoneNumber(phoneNumber: String): Technician? {
        val prefs = context.technicianDataStore.data.first()
        val json = prefs[techniciansKey] ?: "[]"
        val dtos = parseDtos(json)
        return dtos.find { it.phoneNumber == phoneNumber }?.toEntity()
    }

    suspend fun insertTechnician(technician: Technician): Long {
        val prefs = context.technicianDataStore.data.first()
        val json = prefs[techniciansKey] ?: "[]"
        val dtos = parseDtos(json).toMutableList()
        val maxId = dtos.maxOfOrNull { it.id } ?: 0L
        val newId = maxId + 1
        val dto = TechnicianDto(
            id = newId,
            name = technician.name,
            phoneNumber = technician.phoneNumber,
            skillType = technician.skillType.name,
            isActive = technician.isActive,
            createdAt = technician.createdAt
        )
        dtos.add(dto)
        context.technicianDataStore.edit { it[techniciansKey] = gson.toJson(dtos.map { toMap(it) }) }
        return newId
    }

    suspend fun updateTechnician(technician: Technician) {
        context.technicianDataStore.edit { editPrefs ->
            val json = editPrefs[techniciansKey] ?: "[]"
            val dtos = parseDtos(json).toMutableList()
            val idx = dtos.indexOfFirst { it.id == technician.id }
            if (idx >= 0) {
                dtos[idx] = TechnicianDto(
                    id = technician.id,
                    name = technician.name,
                    phoneNumber = technician.phoneNumber,
                    skillType = technician.skillType.name,
                    isActive = technician.isActive,
                    createdAt = technician.createdAt
                )
                editPrefs[techniciansKey] = gson.toJson(dtos.map { toMap(it) })
            }
        }
    }

    suspend fun setTechnicianActive(technicianId: Long, isActive: Boolean) {
        context.technicianDataStore.edit { editPrefs ->
            val json = editPrefs[techniciansKey] ?: "[]"
            val dtos = parseDtos(json).toMutableList()
            val idx = dtos.indexOfFirst { it.id == technicianId }
            if (idx >= 0) {
                dtos[idx] = dtos[idx].copy(isActive = isActive)
                editPrefs[techniciansKey] = gson.toJson(dtos.map { toMap(it) })
            }
        }
    }

    private fun toMap(dto: TechnicianDto): Map<String, Any?> = mapOf(
        "id" to dto.id,
        "name" to dto.name,
        "phoneNumber" to dto.phoneNumber,
        "skillType" to dto.skillType,
        "isActive" to dto.isActive,
        "createdAt" to dto.createdAt
    )
}
