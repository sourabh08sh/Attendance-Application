package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Note: this data class contains a ByteArray (faceEmbedding), so the generated equals()/
 * hashCode() do reference-style comparison on that field rather than content comparison.
 * That's a known, accepted trade-off for an MVP — it only matters if this entity is ever
 * put in a Set/used as a Map key, which it currently isn't.
 */
@Entity(
    tableName = "staff",
    indices = [Index(value = ["employeeId"], unique = true)]
)
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val staffId: Long = 0,
    val name: String,
    val employeeId: String,
    val enrollmentPhotoPath: String? = null,
    val faceEmbedding: ByteArray? = null,
    val isEnrolled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
