package com.attendance.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.attendance.app.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Query("SELECT * FROM attendance WHERE staffId = :staffId ORDER BY date DESC, time DESC")
    fun observeAttendanceForStaff(staffId: Long): Flow<List<AttendanceEntity>>

    // Backs the "one check-in per day" rule — checked before opening the camera on the
    // Mark Attendance screen.
    @Query("SELECT EXISTS(SELECT 1 FROM attendance WHERE staffId = :staffId AND date = :date)")
    suspend fun hasAttendanceForDate(staffId: Long, date: String): Boolean
}
