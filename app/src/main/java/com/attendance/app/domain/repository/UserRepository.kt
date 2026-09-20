package com.attendance.app.domain.repository

import com.attendance.app.domain.model.AuthResult

interface UserRepository {
    suspend fun login(username: String, password: String): AuthResult
}
