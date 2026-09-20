package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.UserDao
import com.attendance.app.data.local.entity.UserEntity
import com.attendance.app.domain.model.AuthResult
import com.attendance.app.domain.model.User
import com.attendance.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun login(username: String, password: String): AuthResult {
        val entity = userDao.getUserByUsername(username.trim()) ?: return AuthResult.InvalidCredentials

        // Plaintext comparison — acceptable ONLY because this is a local-only kiosk MVP with
        // seeded dummy credentials and no backend. Before this ever handles real staff
        // credentials, swap to a hashed-password check (e.g. BCrypt) and never store/compare
        // plaintext passwords.
        if (entity.password != password) return AuthResult.InvalidCredentials

        return AuthResult.Success(entity.toDomain())
    }
}

private fun UserEntity.toDomain() = User(
    userId = userId,
    username = username,
    role = role,
    linkedStaffId = linkedStaffId
)
