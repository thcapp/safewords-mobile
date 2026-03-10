package com.thc.safewords.model

import java.util.UUID

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val interval: RotationInterval,
    val members: List<Member> = emptyList(),
    val createdAt: Long = System.currentTimeMillis() / 1000
)

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
