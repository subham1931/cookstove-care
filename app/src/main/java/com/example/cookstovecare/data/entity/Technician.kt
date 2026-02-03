package com.example.cookstovecare.data.entity

import com.example.cookstovecare.data.TechnicianSkillType

/**
 * Entity representing a technician who can be assigned repair/replacement tasks.
 */
data class Technician(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val skillType: TechnicianSkillType = TechnicianSkillType.BOTH,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
