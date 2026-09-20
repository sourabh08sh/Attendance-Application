package com.attendance.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attendance.app.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    // IGNORE (not ABORT/REPLACE): lets DatabaseSeeder be called defensively without
    // needing its own transaction-level duplicate check.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
