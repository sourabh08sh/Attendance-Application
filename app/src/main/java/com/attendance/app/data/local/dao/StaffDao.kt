package com.attendance.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendance.app.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {

    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun observeAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE staffId = :staffId LIMIT 1")
    suspend fun getStaffById(staffId: Long): StaffEntity?

    // ABORT (default/explicit here): adding a staff member with a duplicate employeeId should
    // fail loudly (unique index in the entity) rather than silently overwrite an existing one.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStaffList(staff: List<StaffEntity>): List<Long>

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("SELECT COUNT(*) FROM staff")
    suspend fun getStaffCount(): Int
}
