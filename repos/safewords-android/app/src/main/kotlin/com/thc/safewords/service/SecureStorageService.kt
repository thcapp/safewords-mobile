package com.thc.safewords.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.thc.safewords.SafewordsApp

/**
 * Secure storage for group seeds using EncryptedSharedPreferences.
 *
 * Seeds are stored as hex strings, backed by Android Keystore via
 * the MasterKey. This ensures seeds are encrypted at rest and tied
 * to the device's hardware-backed keystore.
 */
object SecureStorageService {

    private const val PREFS_NAME = "safewords_secure_prefs"
    private const val SEED_PREFIX = "seed_"

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

    /**
     * Save a seed for a group.
     *
     * @param groupId the group's unique identifier
     * @param seedHex the 256-bit seed as a hex string (64 chars)
     */
    fun saveSeed(groupId: String, seedHex: String) {
        prefs.edit().putString("$SEED_PREFIX$groupId", seedHex).apply()
    }

    /**
     * Retrieve a seed for a group.
     *
     * @param groupId the group's unique identifier
     * @return the hex-encoded seed, or null if not found
     */
    fun getSeed(groupId: String): String? {
        return prefs.getString("$SEED_PREFIX$groupId", null)
    }

    /**
     * Delete a seed for a group.
     *
     * @param groupId the group's unique identifier
     */
    fun deleteSeed(groupId: String) {
        prefs.edit().remove("$SEED_PREFIX$groupId").apply()
    }

    /**
     * Check if a seed exists for a group.
     */
    fun hasSeed(groupId: String): Boolean {
        return prefs.contains("$SEED_PREFIX$groupId")
    }
}
