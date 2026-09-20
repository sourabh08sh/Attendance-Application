package com.attendance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.StaffDao
import com.attendance.app.data.local.dao.UserDao
import com.attendance.app.data.local.entity.AttendanceEntity
import com.attendance.app.data.local.entity.StaffEntity
import com.attendance.app.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, StaffEntity::class, AttendanceEntity::class],
    version = 1,
    // exportSchema=false is fine while the schema is still moving fast during the MVP build.
    // Turn this on (and commit the generated schema JSON) before writing any real Room
    // migrations, so future version bumps have a baseline to diff against.
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun staffDao(): StaffDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        const val DATABASE_NAME = "attendance_app.db"
    }
}
