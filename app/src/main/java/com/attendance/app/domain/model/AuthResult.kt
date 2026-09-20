package com.attendance.app.domain.model

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data object InvalidCredentials : AuthResult()
}
