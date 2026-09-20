package com.attendance.app.data.local

import androidx.room.TypeConverter
import com.attendance.app.domain.model.Role

class Converters {
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)
}
