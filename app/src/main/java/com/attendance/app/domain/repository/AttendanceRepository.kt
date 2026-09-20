package com.attendance.app.domain.repository

import com.attendance.app.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun observeAttendanceForStaff(staffId: Long): Flow<List<AttendanceRecord>>

    /** Backs the one-check-in-per-day rule; call before opening the camera. */
    suspend fun hasMarkedAttendanceToday(staffId: Long): Boolean

    suspend fun recordAttendance(
        staffId: Long,
        selfiePath: String,
        latitude: Double,
        longitude: Double,
        matchConfidence: Float
    ): Long
}
