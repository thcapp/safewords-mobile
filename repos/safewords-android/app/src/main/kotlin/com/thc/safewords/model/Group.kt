package com.thc.safewords.model

import com.thc.safewords.data.Primitives
import com.thc.safewords.data.RotatingWordPrimitive
import com.thc.safewords.data.WordFormat
import java.util.UUID

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val interval: RotationInterval,
    val members: List<Member> = emptyList(),
    val createdAt: Long = System.currentTimeMillis() / 1000,
    val schemaVersion: Int = 2,
    val primitives: Primitives? = null,
) {
    /**
     * Returns the canonical v1.3 primitives for this group. If the group was
     * loaded from v1.2 storage and has no primitives, expand from `interval`.
     */
    fun primitivesOrDefault(): Primitives = primitives ?: Primitives(
        rotatingWord = RotatingWordPrimitive(
            enabled = true,
            intervalSeconds = interval.seconds,
            wordFormat = WordFormat.ADJECTIVE_NOUN_NUMBER,
        ),
    )
}

enum class RotationInterval(val key: String, val seconds: Int, val displayName: String) {
    HOURLY("hourly", 3600, "Every Hour"),
    DAILY("daily", 86400, "Every Day"),
    WEEKLY("weekly", 604800, "Every Week"),
    MONTHLY("monthly", 2592000, "Every Month");

    companion object {
        fun fromKey(key: String): RotationInterval {
            return entries.firstOrNull { it.key == key } ?: DAILY
        }
    }
}
