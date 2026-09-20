package com.attendance.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.attendance.app.data.local.AppDatabase
import com.attendance.app.data.local.DatabaseSeeder
import com.attendance.app.data.local.dao.AttendanceDao
import com.attendance.app.data.local.dao.StaffDao
import com.attendance.app.data.local.dao.UserDao
import com.attendance.app.data.local.entity.AttendanceEntity
import com.attendance.app.data.local.entity.StaffEntity
import com.attendance.app.data.repository.AttendanceRepositoryImpl
import com.attendance.app.data.repository.StaffRepositoryImpl
import com.attendance.app.data.repository.UserRepositoryImpl
import com.attendance.app.domain.model.AuthResult
import com.attendance.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Verifies the Step 2 data layer end-to-end with no UI involved: seeding, login, staff CRUD,
 * face-embedding round-trip, and the one-check-in-per-day rule.
 *
 * Run with: right-click this file in Android Studio -> Run, or `./gradlew connectedAndroidTest`.
 * Uses an in-memory Room database, so it never touches the real on-device DB.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var staffDao: StaffDao
    private lateinit var attendanceDao: AttendanceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        staffDao = db.staffDao()
        attendanceDao = db.attendanceDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun seeder_populatesAdminAndTwoStaffAccounts() = runTest {
        DatabaseSeeder(userDao, staffDao).seedIfEmpty()

        val admin = userDao.getUserByUsername("admin")
        assertNotNull(admin)
        assertEquals(Role.ADMIN, admin?.role)
        assertNull(admin?.linkedStaffId)

        val staff1 = userDao.getUserByUsername("staff1")
        assertNotNull(staff1)
        assertEquals(Role.STAFF, staff1?.role)
        assertNotNull(staff1?.linkedStaffId)

        assertEquals(2, staffDao.getStaffCount())
    }

    @Test
    fun seeder_isIdempotent_secondCallDoesNotDuplicateRows() = runTest {
        val seeder = DatabaseSeeder(userDao, staffDao)
        seeder.seedIfEmpty()
        seeder.seedIfEmpty()

        assertEquals(3, userDao.getUserCount()) // admin + staff1 + staff2, not 6
        assertEquals(2, staffDao.getStaffCount())
    }

    @Test
    fun login_withSeededCredentials_succeeds() = runTest {
        DatabaseSeeder(userDao, staffDao).seedIfEmpty()
        val repo = UserRepositoryImpl(userDao)

        val result = repo.login("admin", "admin123")

        assertTrue(result is AuthResult.Success)
        assertEquals(Role.ADMIN, (result as AuthResult.Success).user.role)
    }

    @Test
    fun login_withWrongPassword_isRejected() = runTest {
        DatabaseSeeder(userDao, staffDao).seedIfEmpty()
        val repo = UserRepositoryImpl(userDao)

        val result = repo.login("admin", "wrong-password")

        assertEquals(AuthResult.InvalidCredentials, result)
    }

    @Test
    fun login_withUnknownUsername_isRejected() = runTest {
        val repo = UserRepositoryImpl(userDao)

        val result = repo.login("nobody", "whatever")

        assertEquals(AuthResult.InvalidCredentials, result)
    }

    @Test
    fun addStaff_withDuplicateEmployeeId_throws() = runTest {
        val repo = StaffRepositoryImpl(staffDao)
        repo.addStaff("First Person", "EMP100")

        var threw = false
        try {
            repo.addStaff("Second Person", "EMP100") // duplicate employeeId -> unique index violation
        } catch (e: Exception) {
            threw = true
        }
        assertTrue("Expected duplicate employeeId to violate the unique index", threw)
    }

    @Test
    fun faceEmbedding_roundTrip_preservesValuesAndMarksEnrolled() = runTest {
        val repo = StaffRepositoryImpl(staffDao)
        val staffId = repo.addStaff("Embed Test", "EMP998")
        val embedding = floatArrayOf(0.12f, -0.34f, 0.56f, -0.78f)

        assertNull(repo.getFaceEmbedding(staffId)) // nothing enrolled yet

        repo.saveFaceEnrollment(staffId, "/fake/enroll.jpg", embedding)

        val retrieved = repo.getFaceEmbedding(staffId)
        assertNotNull(retrieved)
        assertTrue(embedding.contentEquals(retrieved))

        val staff = repo.getStaffById(staffId)
        assertTrue(staff!!.isEnrolled)
        assertEquals("/fake/enroll.jpg", staff.enrollmentPhotoPath)
    }

    @Test
    fun attendance_oneCheckInPerDayRule_reflectsInsertedRecord() = runTest {
        val staffId = staffDao.insertStaff(StaffEntity(name = "Kiosk Test", employeeId = "EMP999"))
        val repo = AttendanceRepositoryImpl(attendanceDao)

        assertFalse(repo.hasMarkedAttendanceToday(staffId))

        repo.recordAttendance(
            staffId = staffId,
            selfiePath = "/fake/selfie.jpg",
            latitude = 28.6139,
            longitude = 77.2090,
            matchConfidence = 0.87f
        )

        assertTrue(repo.hasMarkedAttendanceToday(staffId))
    }

    @Test
    fun attendanceHistory_returnsRecordsNewestFirst() = runTest {
        val staffId = staffDao.insertStaff(StaffEntity(name = "History Test", employeeId = "EMP997"))
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        attendanceDao.insertAttendance(
            AttendanceEntity(
                staffId = staffId, date = yesterday, time = "09:00:00",
                selfiePath = "/fake/1.jpg", latitude = 1.0, longitude = 1.0, matchConfidence = 0.9f
            )
        )
        attendanceDao.insertAttendance(
            AttendanceEntity(
                staffId = staffId, date = today, time = "09:05:00",
                selfiePath = "/fake/2.jpg", latitude = 1.0, longitude = 1.0, matchConfidence = 0.9f
            )
        )

        val repo = AttendanceRepositoryImpl(attendanceDao)
        // Room Flows stay open (they re-emit on table changes), so grab just the first
        // emission rather than collect{} — collecting the whole Flow here would hang forever.
        val history = repo.observeAttendanceForStaff(staffId).first()

        assertEquals(2, history.size)
        assertEquals(today, history.first().date) // newest first
    }
}
