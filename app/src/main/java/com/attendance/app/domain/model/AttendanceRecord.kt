package com.attendance.app.domain.model

data class AttendanceRecord(
    val attendanceId: Long,
    val staffId: Long,
    val date: String,       // yyyy-MM-dd
    val time: String,       // HH:mm:ss
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val matchConfidence: Float
)
