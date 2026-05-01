package com.thc.safewords.service

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thc.safewords.SafewordsApp
import com.thc.safewords.crypto.Primitives as CryptoPrimitives
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.data.Primitives
import com.thc.safewords.data.RotatingWordPrimitive
import com.thc.safewords.data.WordFormat
import com.thc.safewords.model.Group
import com.thc.safewords.model.Member
import com.thc.safewords.model.Role
import com.thc.safewords.model.RotationInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

/**
 * Repository for managing family groups + global preferences.
 *
 * Group metadata is stored as JSON in EncryptedSharedPreferences.
 * Per-group seeds live in SecureStorageService.
 * Boolean prefs (notifications, biometrics, widget visibility, etc.) live in
 * EncryptedSharedPreferences alongside the group JSON.
 */
object GroupRepository {

    private const val PREFS_NAME = "safewords_groups_prefs"
    private const val GROUPS_KEY = "groups_json"
    private const val DEFAULT_INTERVAL_KEY = "default_interval"
    private const val ACTIVE_GROUP_KEY = "active_group_id"
    private const val PLAIN_MODE_KEY = "plain_mode"
    private const val ADVANCED_VIEW_KEY = "advanced_view_enabled"
    private const val DEMO_MODE_KEY = "demo_mode"
    private val DEMO_GROUP_ID = "00000000-0000-0000-0000-00000000d013"

    /**
     * Stable seed used for the demo group. Spells "TIGER-DEMO-SAFEWORDS-V13-DEMO-!!"
     * in ASCII so anyone reverse-engineering can see this isn't a real seed.
     * Identical to iOS so cross-platform parity holds for the demo group.
     */
    private val DEMO_SEED_BYTES = byteArrayOf(
        0x54, 0x49, 0x47, 0x45, 0x52, 0x2d, 0x44, 0x45,
        0x4d, 0x4f, 0x2d, 0x53, 0x41, 0x46, 0x45, 0x57,
        0x4f, 0x52, 0x44, 0x53, 0x2d, 0x56, 0x31, 0x33,
        0x2d, 0x44, 0x45, 0x4d, 0x4f, 0x2d, 0x21, 0x21,
    )
    private const val BIOMETRIC_REQUIRED_KEY = "biometric_required"
    private const val NOTIFY_ROTATION_KEY = "notify_on_rotation"
    private const val PREVIEW_NEXT_WORD_KEY = "preview_next_word"
    private const val LOCK_SCREEN_GLANCE_KEY = "lock_screen_glance"
    private const val HIDE_UNTIL_UNLOCK_KEY = "hide_until_unlock"
    private const val EMERGENCY_OVERRIDE_KEY_PREFIX = "emergency_override_"

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

    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    private val _demoMode = MutableStateFlow(false)
    val demoMode: StateFlow<Boolean> = _demoMode.asStateFlow()

    init {
        loadGroups()
        loadActiveGroupId()
        loadDemoMode()
    }

    // ─── Group lifecycle ────────────────────────────────────────────────

    /**
     * Create a new group with a randomly generated seed (or a provided hex seed).
     * Returns the created group on success, or null if seed storage failed.
     */
    fun createGroup(
        name: String,
        creatorName: String,
        interval: RotationInterval = getDefaultInterval(),
        seedHex: String? = null
    ): Group? {
        exitDemoMode()
        val finalSeedHex = seedHex ?: TOTPDerivation.bytesToHex(generateSeed())

        val creator = Member(name = creatorName, role = Role.CREATOR)
        val group = Group(
            name = name,
            interval = interval,
            members = listOf(creator),
            primitives = defaultPrimitives(interval),
        )

        if (!SecureStorageService.saveSeed(group.id, finalSeedHex)) return null

        val updated = _groups.value.toMutableList().apply { add(group) }
        saveGroups(updated)
        if (_activeGroupId.value == null) setActiveGroup(group.id)
        return group
    }

    /**
     * Join an existing group from a QR or recovery payload.
     * Returns the created group on success, or null if seed storage failed.
     */
    fun joinGroup(
        name: String,
        seedHex: String,
        interval: RotationInterval,
        memberName: String
    ): Group? {
        exitDemoMode()
        val member = Member(name = memberName, role = Role.MEMBER)
        val group = Group(
            name = name,
            interval = interval,
            members = listOf(member),
            primitives = defaultPrimitives(interval),
        )

        if (!SecureStorageService.saveSeed(group.id, seedHex)) return null

        val updated = _groups.value.toMutableList().apply { add(group) }
        saveGroups(updated)
        if (_activeGroupId.value == null) setActiveGroup(group.id)
        return group
    }

    fun updateGroup(group: Group) {
        val updated = _groups.value.toMutableList()
        val idx = updated.indexOfFirst { it.id == group.id }
        if (idx >= 0) {
            updated[idx] = group
            saveGroups(updated)
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(name = newName))
    }

    fun setRotationInterval(groupId: String, interval: RotationInterval) {
        val group = getGroup(groupId) ?: return
        val primitives = group.primitivesOrDefault()
        updateGroup(group.copy(
            interval = interval,
            primitives = primitives.copy(
                rotatingWord = primitives.rotatingWord.copy(
                    intervalSeconds = interval.seconds,
                ),
            ),
        ))
    }

    fun setWordFormat(groupId: String, format: WordFormat) {
        val group = getGroup(groupId) ?: return
        val primitives = group.primitivesOrDefault()
        updateGroup(group.copy(
            primitives = primitives.copy(
                rotatingWord = primitives.rotatingWord.copy(wordFormat = format),
            ),
        ))
    }

    fun setStaticOverrideEnabled(groupId: String, enabled: Boolean) {
        val group = getGroup(groupId) ?: return
        val primitives = group.primitivesOrDefault()
        updateGroup(group.copy(
            primitives = primitives.copy(
                staticOverride = primitives.staticOverride.copy(enabled = enabled),
            ),
        ))
    }

    fun setChallengeAnswerEnabled(groupId: String, enabled: Boolean) {
        val group = getGroup(groupId) ?: return
        val primitives = group.primitivesOrDefault()
        updateGroup(group.copy(
            primitives = primitives.copy(
                challengeAnswer = primitives.challengeAnswer.copy(enabled = enabled),
            ),
        ))
    }

    fun markStaticOverridePrinted(groupId: String) {
        val group = getGroup(groupId) ?: return
        val primitives = group.primitivesOrDefault()
        updateGroup(group.copy(
            primitives = primitives.copy(
                staticOverride = primitives.staticOverride.copy(
                    printedAt = System.currentTimeMillis() / 1000,
                ),
            ),
        ))
    }

    fun deleteGroup(groupId: String) {
        if (groupId == DEMO_GROUP_ID) {
            exitDemoMode()
            return
        }
        SecureStorageService.deleteSeed(groupId)
        val updated = _groups.value.filterNot { it.id == groupId }
        saveGroups(updated)
        if (_activeGroupId.value == groupId) {
            setActiveGroup(updated.firstOrNull()?.id)
        }
        // Clear per-group emergency override if any.
        prefs.edit().remove(EMERGENCY_OVERRIDE_KEY_PREFIX + groupId).apply()
    }

    fun getGroup(groupId: String): Group? = _groups.value.firstOrNull { it.id == groupId }

    // ─── Active-group selection ─────────────────────────────────────────

    fun setActiveGroup(groupId: String?) {
        _activeGroupId.value = groupId
        if (groupId == null) {
            prefs.edit().remove(ACTIVE_GROUP_KEY).apply()
        } else {
            prefs.edit().putString(ACTIVE_GROUP_KEY, groupId).apply()
        }
    }

    /** The currently-active group, or the first group as fallback. */
    fun activeGroup(): Group? {
        val id = _activeGroupId.value
        return if (id != null) getGroup(id) ?: _groups.value.firstOrNull()
               else _groups.value.firstOrNull()
    }

    private fun loadActiveGroupId() {
        _activeGroupId.value = prefs.getString(ACTIVE_GROUP_KEY, null)
    }

    // ─── Member operations ──────────────────────────────────────────────

    fun addMember(groupId: String, member: Member) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(members = group.members + member))
    }

    fun removeMember(groupId: String, memberId: String) {
        val group = getGroup(groupId) ?: return
        updateGroup(group.copy(members = group.members.filterNot { it.id == memberId }))
    }

    // ─── Seed access + safeword derivation ──────────────────────────────

    fun getGroupSeed(groupId: String): ByteArray? {
        if (_demoMode.value && groupId == DEMO_GROUP_ID) {
            return DEMO_SEED_BYTES.copyOf()
        }
        val hex = SecureStorageService.getSeed(groupId) ?: return null
        return TOTPDerivation.hexToBytes(hex)
    }

    fun getCurrentSafeword(groupId: String): String? {
        val group = getGroup(groupId) ?: return null
        val seed = getGroupSeed(groupId) ?: return null
        val timestamp = System.currentTimeMillis() / 1000
        val rotating = group.primitivesOrDefault().rotatingWord
        return when (rotating.wordFormat) {
            WordFormat.NUMERIC -> CryptoPrimitives.numeric(seed, rotating.intervalSeconds, timestamp)
            WordFormat.ADJECTIVE_NOUN_NUMBER ->
                TOTPDerivation.deriveSafeword(seed, rotating.intervalSeconds, timestamp)
        }
    }

    /**
     * Deterministic group static override word derived from the seed. Returns
     * null if the group is unknown or the seed is unavailable. The override
     * is the same word every call for a given seed; rotate the seed to change.
     */
    fun getStaticOverride(groupId: String): String? {
        val seed = getGroupSeed(groupId) ?: return null
        return CryptoPrimitives.staticOverride(seed)
    }

    /**
     * Deterministic challenge/answer table for a group. Returns the requested
     * row count starting at index 0 (24 for wallet excerpt, 100 for full
     * protocol card).
     */
    fun getChallengeAnswerTable(groupId: String, rowCount: Int): List<CryptoPrimitives.ChallengeAnswerRow>? {
        val group = getGroup(groupId) ?: return null
        val seed = getGroupSeed(groupId) ?: return null
        val tableVersion = group.primitivesOrDefault().challengeAnswer.tableVersion
        return CryptoPrimitives.challengeAnswerTable(seed, tableVersion, rowCount)
    }

    /**
     * Replace a group's seed with a new random one. Members keep their identity
     * but the QR / safeword stream changes. Returns false if storage fails.
     */
    fun rotateGroupSeed(groupId: String): Boolean {
        val newHex = TOTPDerivation.bytesToHex(generateSeed())
        return SecureStorageService.saveSeed(groupId, newHex)
    }

    // ─── Default interval ───────────────────────────────────────────────

    fun getDefaultInterval(): RotationInterval {
        val key = prefs.getString(DEFAULT_INTERVAL_KEY, null)
        return if (key != null) RotationInterval.fromKey(key) else RotationInterval.DAILY
    }

    fun setDefaultInterval(interval: RotationInterval) {
        prefs.edit().putString(DEFAULT_INTERVAL_KEY, interval.key).apply()
    }

    // ─── Boolean prefs ──────────────────────────────────────────────────

    fun isPlainMode(): Boolean = prefs.getBoolean(PLAIN_MODE_KEY, false)
    fun setPlainMode(enabled: Boolean) { prefs.edit().putBoolean(PLAIN_MODE_KEY, enabled).apply() }

    /**
     * v1.3 view preference. Default is Plain (Advanced disabled). Sticky once
     * the user opts into Advanced — they stay until they toggle back.
     */
    fun isAdvancedView(): Boolean = prefs.getBoolean(ADVANCED_VIEW_KEY, false)
    fun setAdvancedView(enabled: Boolean) { prefs.edit().putBoolean(ADVANCED_VIEW_KEY, enabled).apply() }

    fun isBiometricRequired(): Boolean = prefs.getBoolean(BIOMETRIC_REQUIRED_KEY, false)
    fun setBiometricRequired(enabled: Boolean) { prefs.edit().putBoolean(BIOMETRIC_REQUIRED_KEY, enabled).apply() }

    fun isNotifyOnRotation(): Boolean = prefs.getBoolean(NOTIFY_ROTATION_KEY, true)
    fun setNotifyOnRotation(enabled: Boolean) { prefs.edit().putBoolean(NOTIFY_ROTATION_KEY, enabled).apply() }

    fun isPreviewNextWord(): Boolean = prefs.getBoolean(PREVIEW_NEXT_WORD_KEY, false)
    fun setPreviewNextWord(enabled: Boolean) { prefs.edit().putBoolean(PREVIEW_NEXT_WORD_KEY, enabled).apply() }

    fun isLockScreenGlance(): Boolean = prefs.getBoolean(LOCK_SCREEN_GLANCE_KEY, true)
    fun setLockScreenGlance(enabled: Boolean) { prefs.edit().putBoolean(LOCK_SCREEN_GLANCE_KEY, enabled).apply() }

    fun isHideUntilUnlock(): Boolean = prefs.getBoolean(HIDE_UNTIL_UNLOCK_KEY, false)
    fun setHideUntilUnlock(enabled: Boolean) { prefs.edit().putBoolean(HIDE_UNTIL_UNLOCK_KEY, enabled).apply() }

    // ─── Per-group emergency override word ──────────────────────────────

    fun getEmergencyOverrideWord(groupId: String): String? =
        prefs.getString(EMERGENCY_OVERRIDE_KEY_PREFIX + groupId, null)?.takeIf { it.isNotBlank() }

    fun setEmergencyOverrideWord(groupId: String, word: String?) {
        val key = EMERGENCY_OVERRIDE_KEY_PREFIX + groupId
        if (word.isNullOrBlank()) prefs.edit().remove(key).apply()
        else prefs.edit().putString(key, word.trim()).apply()
    }

    // ─── Reset ──────────────────────────────────────────────────────────

    /** Wipe every group, every seed, every preference. Only callable from danger zone. */
    fun resetAllData() {
        _groups.value.forEach {
            if (it.id != DEMO_GROUP_ID) SecureStorageService.deleteSeed(it.id)
        }
        prefs.edit().clear().apply()
        _groups.value = emptyList()
        _activeGroupId.value = null
        _demoMode.value = false
    }

    // ─── Demo mode ──────────────────────────────────────────────────────

    /**
     * Drop the user into a non-persisted demo group so they can explore the
     * app before committing to creating or joining a real group. The demo
     * group's seed is hardcoded and identical across iOS/Android.
     */
    fun enterDemoMode() {
        if (_demoMode.value) return
        // Don't shadow real groups.
        if (_groups.value.any { it.id != DEMO_GROUP_ID }) return
        _demoMode.value = true
        prefs.edit().putBoolean(DEMO_MODE_KEY, true).apply()
        val demo = demoGroup()
        _groups.value = listOf(demo)
        setActiveGroup(demo.id)
    }

    fun exitDemoMode() {
        if (!_demoMode.value && _groups.value.none { it.id == DEMO_GROUP_ID }) return
        _demoMode.value = false
        prefs.edit().putBoolean(DEMO_MODE_KEY, false).apply()
        _groups.value = _groups.value.filterNot { it.id == DEMO_GROUP_ID }
        if (_activeGroupId.value == DEMO_GROUP_ID) {
            setActiveGroup(_groups.value.firstOrNull()?.id)
        }
    }

    private fun loadDemoMode() {
        val on = prefs.getBoolean(DEMO_MODE_KEY, false)
        if (on) {
            _demoMode.value = true
            // Inject the demo group on launch if it isn't already present.
            if (_groups.value.none { it.id == DEMO_GROUP_ID }) {
                _groups.value = _groups.value + demoGroup()
            }
            if (_activeGroupId.value == null) {
                _activeGroupId.value = DEMO_GROUP_ID
            }
        }
    }

    private fun demoGroup(): Group = Group(
        id = DEMO_GROUP_ID,
        name = "Demo · TIGER",
        interval = RotationInterval.DAILY,
        members = listOf(Member(name = "Demo User", role = Role.CREATOR)),
        createdAt = 0L,
        primitives = Primitives(
            rotatingWord = RotatingWordPrimitive(
                enabled = true,
                intervalSeconds = RotationInterval.DAILY.seconds,
                wordFormat = WordFormat.ADJECTIVE_NOUN_NUMBER,
            ),
            challengeAnswer = com.thc.safewords.data.ChallengeAnswerPrimitive(
                enabled = true,
                tableVersion = 1,
                rowCount = 100,
            ),
            staticOverride = com.thc.safewords.data.StaticOverridePrimitive(
                enabled = true,
                derivationVersion = 1,
            ),
        ),
    )

    // ─── Internal helpers ───────────────────────────────────────────────

    private fun generateSeed(): ByteArray {
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)
        return seed
    }

    private fun defaultPrimitives(interval: RotationInterval): Primitives = Primitives(
        rotatingWord = RotatingWordPrimitive(
            enabled = true,
            intervalSeconds = interval.seconds,
            wordFormat = WordFormat.ADJECTIVE_NOUN_NUMBER,
        ),
    )

    private fun loadGroups() {
        val json = prefs.getString(GROUPS_KEY, null) ?: return
        val type = object : TypeToken<List<Group>>() {}.type
        _groups.value = gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveGroups(groups: List<Group>) {
        _groups.value = groups
        prefs.edit().putString(GROUPS_KEY, gson.toJson(groups)).apply()
    }
}
