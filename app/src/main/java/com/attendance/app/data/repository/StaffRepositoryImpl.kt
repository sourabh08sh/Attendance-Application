package com.attendance.app.data.repository

import com.attendance.app.data.local.dao.StaffDao
import com.attendance.app.data.local.entity.StaffEntity
import com.attendance.app.domain.model.Staff
import com.attendance.app.domain.repository.StaffRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepositoryImpl @Inject constructor(
    private val staffDao: StaffDao
) : StaffRepository {

    override fun observeAllStaff(): Flow<List<Staff>> =
        staffDao.observeAllStaff().map { list -> list.map { it.toDomain() } }

    override suspend fun getStaffById(staffId: Long): Staff? =
        staffDao.getStaffById(staffId)?.toDomain()

    override suspend fun addStaff(name: String, employeeId: String): Long =
        staffDao.insertStaff(StaffEntity(name = name, employeeId = employeeId))

    override suspend fun saveFaceEnrollment(staffId: Long, photoPath: String, embedding: FloatArray) {
        val existing = staffDao.getStaffById(staffId) ?: return
        staffDao.updateStaff(
            existing.copy(
                enrollmentPhotoPath = photoPath,
                faceEmbedding = embedding.toByteArray(),
                isEnrolled = true
            )
        )
    }

    override suspend fun getFaceEmbedding(staffId: Long): FloatArray? =
        staffDao.getStaffById(staffId)?.faceEmbedding?.toFloatArray()
}

private fun StaffEntity.toDomain() = Staff(
    staffId = staffId,
    name = name,
    employeeId = employeeId,
    enrollmentPhotoPath = enrollmentPhotoPath,
    isEnrolled = isEnrolled,
    createdAt = createdAt
)

// --- FloatArray <-> ByteArray marshalling for persisting the face embedding vector as a
// Room BLOB column. Isolated here (not in the entity) so the entity stays a plain data holder. ---

private fun FloatArray.toByteArray(): ByteArray {
    val buffer = ByteBuffer.allocate(size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
    forEach { buffer.putFloat(it) }
    return buffer.array()
}

private fun ByteArray.toFloatArray(): FloatArray {
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.nativeOrder())
    return FloatArray(size / Float.SIZE_BYTES) { buffer.float }
}
