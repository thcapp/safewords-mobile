package com.thc.safewords.model

import java.util.UUID

data class Member(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: Role,
    val joinedAt: Long = System.currentTimeMillis() / 1000
)

enum class Role {
    CREATOR,
    MEMBER
}
