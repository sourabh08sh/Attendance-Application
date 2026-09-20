package com.attendance.app.domain.model

/**
 * The logged-in user. Deliberately does NOT carry the password field from the entity —
 * once auth succeeds there's no reason for the credential to travel any further up the stack.
 */
data class User(
    val userId: Long,
    val username: String,
    val role: Role,
    val linkedStaffId: Long? // set for STAFF accounts, null for ADMIN
)
