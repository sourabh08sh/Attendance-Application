package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["staffId", "date"])],
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["staffId"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val attendanceId: Long = 0,
    val staffId: Long,
    val date: String,        // yyyy-MM-dd — indexed together with staffId for the daily-check query
    val time: String,        // HH:mm:ss
    val selfiePath: String,
    val latitude: Double,
    val longitude: Double,
    val matchConfidence: Float
)
