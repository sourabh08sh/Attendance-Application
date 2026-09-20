package com.attendance.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val STAFF_LIST = "staff_list"
    const val ADD_STAFF = "add_staff"
    const val STAFF_HOME = "staff_home"

    const val STAFF_PROFILE = "staff_profile/{staffId}"
    fun staffProfile(staffId: Long) = "staff_profile/$staffId"

    const val FACE_ENROLLMENT = "face_enrollment/{staffId}"
    fun faceEnrollment(staffId: Long) = "face_enrollment/$staffId"
}
