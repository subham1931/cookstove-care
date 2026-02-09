package com.example.cookstovecare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cookstovecare.data.UserRole
import com.example.cookstovecare.data.entity.UserInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

private data class RegisteredUser(
    val phoneNumber: String,
    val password: String,
    val centerName: String? = null,
    val role: String = UserRole.FIELD_OFFICER.name,
    val profileImageUri: String? = null
)

/**
 * DataStore for repair center authentication state.
 */
class AuthDataStore(private val context: Context) {

    private val gson = Gson()
    private val userListType = object : TypeToken<ArrayList<RegisteredUser>>() {}.type

    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    private val phoneNumberKey = stringPreferencesKey("phone_number")
    private val centerNameKey = stringPreferencesKey("center_name")
    private val userRoleKey = stringPreferencesKey("user_role")
    private val technicianIdKey = stringPreferencesKey("technician_id")
    private val profileImageUriKey = stringPreferencesKey("profile_image_uri")
    private val registeredUsersKey = stringPreferencesKey("registered_users")

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[isLoggedInKey] ?: false
    }

    val phoneNumber: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[phoneNumberKey] ?: ""
    }

    val centerName: Flow<String> = context.authDataStore.data.map { prefs ->
        prefs[centerNameKey] ?: ""
    }

    val technicianId: Flow<Long?> = context.authDataStore.data.map { prefs ->
        prefs[technicianIdKey]?.toLongOrNull()
    }

    val userRole: Flow<UserRole> = context.authDataStore.data.map { prefs ->
        val roleStr = prefs[userRoleKey] ?: UserRole.FIELD_OFFICER.name
        try {
            UserRole.valueOf(roleStr)
        } catch (_: Exception) {
            UserRole.FIELD_OFFICER
        }
    }

    val profileImageUri: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[profileImageUriKey]
    }

    suspend fun setLoggedIn(phoneNumber: String, centerName: String? = null, role: UserRole = UserRole.FIELD_OFFICER, technicianId: Long? = null) {
        context.authDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[phoneNumberKey] = phoneNumber
            prefs[centerNameKey] = centerName ?: ""
            prefs[userRoleKey] = role.name
            if (technicianId != null) {
                prefs[technicianIdKey] = technicianId.toString()
            } else {
                prefs.remove(technicianIdKey)
            }
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(isLoggedInKey)
            prefs.remove(phoneNumberKey)
            prefs.remove(centerNameKey)
            prefs.remove(userRoleKey)
            prefs.remove(technicianIdKey)
            prefs.remove(profileImageUriKey)
        }
    }

    /**
     * Update the current user's profile information.
     * Also syncs the name and image to the RegisteredUser list so other roles can see it.
     */
    suspend fun updateProfile(centerName: String, profileImageUri: String?) {
        context.authDataStore.edit { prefs ->
            prefs[centerNameKey] = centerName
            if (profileImageUri != null) {
                prefs[profileImageUriKey] = profileImageUri
            } else {
                prefs.remove(profileImageUriKey)
            }
            
            // Also update the registered users list so coordinator can see the updated name
            val currentPhone = prefs[phoneNumberKey] ?: return@edit
            val json = prefs[registeredUsersKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val users: MutableList<RegisteredUser> = ((gson.fromJson(json, userListType) as? List<RegisteredUser>) ?: emptyList()).toMutableList()
            val idx = users.indexOfFirst { it.phoneNumber == currentPhone }
            if (idx >= 0) {
                users[idx] = users[idx].copy(centerName = centerName, profileImageUri = profileImageUri)
                prefs[registeredUsersKey] = gson.toJson(users)
            }
        }
    }

    suspend fun registerUser(phoneNumber: String, password: String, centerName: String?, role: UserRole = UserRole.FIELD_OFFICER): Result<Unit> {
        val users = getRegisteredUsers().toMutableList()
        if (users.any { it.phoneNumber == phoneNumber }) {
            return Result.failure(IllegalArgumentException("phone_already_registered"))
        }
        users.add(RegisteredUser(phoneNumber = phoneNumber, password = password, centerName = centerName, role = role.name))
        context.authDataStore.edit { prefs ->
            prefs[registeredUsersKey] = gson.toJson(users)
        }
        return Result.success(Unit)
    }
    
    /**
     * Register or update user with role. If user exists, updates their role.
     */
    suspend fun registerOrUpdateUser(phoneNumber: String, password: String, centerName: String?, role: UserRole) {
        val users = getRegisteredUsers().toMutableList()
        val existingIndex = users.indexOfFirst { it.phoneNumber == phoneNumber }
        
        if (existingIndex >= 0) {
            // Update existing user's role
            val existing = users[existingIndex]
            users[existingIndex] = existing.copy(role = role.name)
        } else {
            // Add new user
            users.add(RegisteredUser(phoneNumber = phoneNumber, password = password, centerName = centerName, role = role.name))
        }
        
        context.authDataStore.edit { prefs ->
            prefs[registeredUsersKey] = gson.toJson(users)
        }
    }

    suspend fun verifyLogin(phoneNumber: String, password: String): Boolean {
        val users = getRegisteredUsers()
        return users.any { it.phoneNumber == phoneNumber && it.password == password }
    }

    suspend fun getRegisteredUser(phoneNumber: String): UserInfo? {
        val user = getRegisteredUsers().find { it.phoneNumber == phoneNumber }
        return user?.let { UserInfo(phoneNumber = it.phoneNumber, centerName = it.centerName) }
    }

    private suspend fun getRegisteredUsers(): List<RegisteredUser> {
        val prefs = context.authDataStore.data.first()
        val json = prefs[registeredUsersKey] ?: "[]"
        @Suppress("UNCHECKED_CAST")
        return (gson.fromJson(json, userListType) as? List<RegisteredUser>) ?: emptyList()
    }
    
    /**
     * Remove registered users by phone numbers.
     */
    suspend fun removeRegisteredUsers(phoneNumbers: Set<String>) {
        context.authDataStore.edit { prefs ->
            val json = prefs[registeredUsersKey] ?: "[]"
            @Suppress("UNCHECKED_CAST")
            val users: MutableList<RegisteredUser> = ((gson.fromJson(json, userListType) as? List<RegisteredUser>) ?: emptyList()).toMutableList()
            val before = users.size
            users.removeAll { it.phoneNumber in phoneNumbers }
            if (users.size != before) {
                prefs[registeredUsersKey] = gson.toJson(users)
            }
        }
    }

    /**
     * Get all registered Field Officers.
     * Name falls back to phone number if not set or blank.
     */
    suspend fun getAllFieldOfficers(): List<FieldOfficerInfo> {
        return getRegisteredUsers()
            .filter { it.role == UserRole.FIELD_OFFICER.name }
            .map { FieldOfficerInfo(
                phoneNumber = it.phoneNumber,
                name = it.centerName?.takeIf { name -> name.isNotBlank() },
                profileImageUri = it.profileImageUri
            )}
    }
}

/**
 * Data class representing Field Officer information.
 * [name] is null if the field officer hasn't set a display name yet.
 * Use [displayName] for UI: shows name if available, otherwise phone number.
 */
data class FieldOfficerInfo(
    val phoneNumber: String,
    val name: String? = null,
    val profileImageUri: String? = null
) {
    val displayName: String get() = name ?: phoneNumber
}
