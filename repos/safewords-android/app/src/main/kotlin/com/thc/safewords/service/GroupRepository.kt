package com.thc.safewords.service

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thc.safewords.SafewordsApp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Group
import com.thc.safewords.model.Member
import com.thc.safewords.model.Role
import com.thc.safewords.model.RotationInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

/**
 * Repository for managing family groups.
 *
 * Group metadata is stored as JSON in EncryptedSharedPreferences.
 * Seeds are stored separately via SecureStorageService.
 */
object GroupRepository {

    private const val PREFS_NAME = "safewords_groups_prefs"
    private const val GROUPS_KEY = "groups_json"
    private const val DEFAULT_INTERVAL_KEY = "default_interval"

    private val gson = Gson()

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            SafewordsApp.instance,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    init {
        loadGroups()
    }

    /**
     * Create a new group with a randomly generated seed.
     *
     * @param name group display name
     * @param creatorName name of the person creating the group
     * @param interval rotation interval
     * @return the created group
     */
    fun createGroup(
        name: String,
        creatorName: String,
        interval: RotationInterval = getDefaultInterval()
    ): Group {
        val seed = generateSeed()
        val seedHex = TOTPDerivation.bytesToHex(seed)

        val creator = Member(
            name = creatorName,
            role = Role.CREATOR
        )

        val group = Group(
            name = name,
            interval = interval,
            members = listOf(creator)
        )

        SecureStorageService.saveSeed(group.id, seedHex)
        val currentGroups = _groups.value.toMutableList()
        currentGroups.add(group)
        saveGroups(currentGroups)

        return group
    }

    /**
     * Join a group from a QR code payload.
     *
     * @param name group name from QR
     * @param seedHex hex-encoded seed from QR
     * @param interval rotation interval from QR
     * @param memberName the person joining
     * @return the created group
     */
    fun joinGroup(
        name: String,
        seedHex: String,
        interval: RotationInterval,
        memberName: String
    ): Group {
        val member = Member(
            name = memberName,
            role = Role.MEMBER
        )

        val group = Group(
            name = name,
            interval = interval,
            members = listOf(member)
        )

        SecureStorageService.saveSeed(group.id, seedHex)
        val currentGroups = _groups.value.toMutableList()
        currentGroups.add(group)
        saveGroups(currentGroups)

        return group
    }

    /**
     * Update a group's properties.
     */
    fun updateGroup(group: Group) {
        val currentGroups = _groups.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            currentGroups[index] = group
            saveGroups(currentGroups)
        }
    }

    /**
     * Delete a group and its seed.
     */
    fun deleteGroup(groupId: String) {
        SecureStorageService.deleteSeed(groupId)
        val currentGroups = _groups.value.toMutableList()
        currentGroups.removeAll { it.id == groupId }
        saveGroups(currentGroups)
    }

    /**
     * Get a specific group by ID.
     */
    fun getGroup(groupId: String): Group? {
        return _groups.value.firstOrNull { it.id == groupId }
    }

    /**
     * Get the seed for a group as a byte array.
     */
    fun getGroupSeed(groupId: String): ByteArray? {
        val hex = SecureStorageService.getSeed(groupId) ?: return null
        return TOTPDerivation.hexToBytes(hex)
    }

    /**
     * Get the current safeword for a group.
     */
    fun getCurrentSafeword(groupId: String): String? {
        val group = getGroup(groupId) ?: return null
        val seed = getGroupSeed(groupId) ?: return null
        val timestamp = System.currentTimeMillis() / 1000
        return TOTPDerivation.deriveSafeword(seed, group.interval.seconds, timestamp)
    }

    /**
     * Add a member to a group.
     */
    fun addMember(groupId: String, member: Member) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.toMutableList()
        updatedMembers.add(member)
        updateGroup(group.copy(members = updatedMembers))
    }

    /**
     * Remove a member from a group.
     */
    fun removeMember(groupId: String, memberId: String) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.filter { it.id != memberId }
        updateGroup(group.copy(members = updatedMembers))
    }

    /**
     * Get the default rotation interval.
     */
    fun getDefaultInterval(): RotationInterval {
        val key = prefs.getString(DEFAULT_INTERVAL_KEY, null)
        return if (key != null) RotationInterval.fromKey(key) else RotationInterval.DAILY
    }

    /**
     * Set the default rotation interval.
     */
    fun setDefaultInterval(interval: RotationInterval) {
        prefs.edit().putString(DEFAULT_INTERVAL_KEY, interval.key).apply()
    }

    private fun generateSeed(): ByteArray {
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)
        return seed
    }

    private fun loadGroups() {
        val json = prefs.getString(GROUPS_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<Group>>() {}.type
            _groups.value = gson.fromJson(json, type) ?: emptyList()
        }
    }

    private fun saveGroups(groups: List<Group>) {
        _groups.value = groups
        val json = gson.toJson(groups)
        prefs.edit().putString(GROUPS_KEY, json).apply()
    }
}
