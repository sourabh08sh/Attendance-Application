package com.attendance.app.di

import com.attendance.app.data.repository.AttendanceRepositoryImpl
import com.attendance.app.data.repository.StaffRepositoryImpl
import com.attendance.app.data.repository.UserRepositoryImpl
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.StaffRepository
import com.attendance.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * This is the seam for a future remote data source: if the app ever moves off the single
 * shared-device model, only these bindings (and the *Impl classes) change — ViewModels depend
 * on the interfaces above and never see Room directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindStaffRepository(impl: StaffRepositoryImpl): StaffRepository

    @Binds
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository
}
