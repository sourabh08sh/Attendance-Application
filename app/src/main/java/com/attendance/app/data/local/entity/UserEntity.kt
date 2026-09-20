package com.attendance.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attendance.app.domain.model.Role

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["linkedStaffId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["staffId"],
            childColumns = ["linkedStaffId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val username: String,
    // Plaintext dummy password — acceptable ONLY for this local-only MVP with seeded test
    // credentials. See the comment in UserRepositoryImpl for what changes before production.
    val password: String,
    val role: Role,
    val linkedStaffId: Long? = null // null for ADMIN; points at StaffEntity.staffId for STAFF
)
