package com.thc.safewords.service

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.thc.safewords.SafewordsApp

/**
 * Encrypted storage for per-group seeds + opaque blobs (drill history, etc.).
 *
 * Backed by Android Keystore via MasterKey — values are encrypted at rest and
 * tied to the device. saveSeed returns false if the underlying commit fails.
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

    /** Save a seed for a group. Returns true on success. */
    fun saveSeed(groupId: String, seedHex: String): Boolean =
        prefs.edit().putString("$SEED_PREFIX$groupId", seedHex).commit()

    fun getSeed(groupId: String): String? =
        prefs.getString("$SEED_PREFIX$groupId", null)

    fun deleteSeed(groupId: String) {
        prefs.edit().remove("$SEED_PREFIX$groupId").apply()
    }

    fun hasSeed(groupId: String): Boolean =
        prefs.contains("$SEED_PREFIX$groupId")

    // ─── Generic string storage (drill history, etc.) ──────────────────

    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? =
        prefs.getString(key, null)
}
