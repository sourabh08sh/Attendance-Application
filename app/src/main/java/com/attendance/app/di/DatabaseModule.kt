package com.attendance.app.di

import android.content.Context
import androidx.room.Room
import com.attendance.app.data.local.AppDatabase
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.StaffDao
import com.attendance.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .build()
    // No fallbackToDestructiveMigration(): schema changes should be handled deliberately once
    // there's real data worth keeping. Being strict from day one is cheap; being lenient and
    // silently losing staff/attendance data later is not.

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideStaffDao(db: AppDatabase): StaffDao = db.staffDao()

    @Provides
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()
}
