package com.attendance.app.domain.model

/**
 * Staff profile as seen by the UI layer. The raw face embedding is intentionally NOT part of
 * this model — it's a large float vector that only the face-recognition module needs, fetched
 * separately via StaffRepository.getFaceEmbedding(). Keeping it out of the general-purpose
 * model stops it from silently riding along through screens (staff list, profile) that have
 * no reason to touch it.
 */
data class Staff(
    val staffId: Long,
    val name: String,
    val employeeId: String,
    val enrollmentPhotoPath: String?,
    val isEnrolled: Boolean,
    val createdAt: Long
)
