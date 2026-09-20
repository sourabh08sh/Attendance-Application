package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.entity.AttendanceEntity
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceDao: AttendanceDao
) : AttendanceRepository {

    override fun observeAttendanceForStaff(staffId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.observeAttendanceForStaff(staffId).map { list -> list.map { it.toDomain() } }

    override suspend fun hasMarkedAttendanceToday(staffId: Long): Boolean =
        attendanceDao.hasAttendanceForDate(staffId, LocalDate.now().toString())

    override suspend fun recordAttendance(
        staffId: Long,
        selfiePath: String,
        latitude: Double,
        longitude: Double,
        matchConfidence: Float
    ): Long {
        val now = LocalDateTime.now()
        val entity = AttendanceEntity(
            staffId = staffId,
            date = now.toLocalDate().toString(),
            time = now.toLocalTime().withNano(0).toString(),
            selfiePath = selfiePath,
            latitude = latitude,
            longitude = longitude,
            matchConfidence = matchConfidence
        )
        return attendanceDao.insertAttendance(entity)
    }
}

private fun AttendanceEntity.toDomain() = AttendanceRecord(
    attendanceId = attendanceId,
    staffId = staffId,
    date = date,
    time = time,
    selfiePath = selfiePath,
    latitude = latitude,
    longitude = longitude,
    matchConfidence = matchConfidence
)
