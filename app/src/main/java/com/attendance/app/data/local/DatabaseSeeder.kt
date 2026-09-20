package com.attendance.app.data.local

import com.attendance.app.data.local.dao.StaffDao
import com.attendance.app.data.local.dao.UserDao
import com.attendance.app.data.local.entity.StaffEntity
import com.attendance.app.data.local.entity.UserEntity
import com.attendance.app.domain.model.Role
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds dummy login/staff data on first run only. Safe to call on every app launch —
 * seedIfEmpty() is a no-op once the users table has anything in it.
 *
 * Seeded logins (all dummy, local-only, plaintext — see UserEntity for why that's OK here):
 *   admin  / admin123   -> ADMIN
 *   staff1 / staff123   -> STAFF, linked to "John Doe" (EMP001), not yet face-enrolled
 *   staff2 / staff123   -> STAFF, linked to "Jane Smith" (EMP002), not yet face-enrolled
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val userDao: UserDao,
    private val staffDao: StaffDao
) {
    suspend fun seedIfEmpty() {
        if (userDao.getUserCount() > 0) return

        val johnId = staffDao.insertStaff(StaffEntity(name = "John Doe", employeeId = "EMP001"))
        val janeId = staffDao.insertStaff(StaffEntity(name = "Jane Smith", employeeId = "EMP002"))

        userDao.insertUsers(
            listOf(
                UserEntity(username = "admin", password = "admin123", role = Role.ADMIN, linkedStaffId = null),
                UserEntity(username = "staff1", password = "staff123", role = Role.STAFF, linkedStaffId = johnId),
                UserEntity(username = "staff2", password = "staff123", role = Role.STAFF, linkedStaffId = janeId)
            )
        )
    }
}
