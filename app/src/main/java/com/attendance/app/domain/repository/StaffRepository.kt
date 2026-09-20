package com.attendance.app.domain.repository

import com.attendance.app.domain.model.Staff
import kotlinx.coroutines.flow.Flow

interface StaffRepository {
    fun observeAllStaff(): Flow<List<Staff>>
    suspend fun getStaffById(staffId: Long): Staff?

    /** Throws if employeeId already exists (unique constraint) — caller/ViewModel handles the error. */
    suspend fun addStaff(name: String, employeeId: String): Long

    suspend fun saveFaceEnrollment(staffId: Long, photoPath: String, embedding: FloatArray)

    /** Null if the staff member hasn't been enrolled yet. */
    suspend fun getFaceEmbedding(staffId: Long): FloatArray?
}
