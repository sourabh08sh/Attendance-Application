package com.attendance.app.ui.admin.staffprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.domain.model.AttendanceRecord
import com.attendance.app.domain.model.Staff
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffProfileUiState(
    val isLoading: Boolean = true,
    val staffName: String = "",
    val employeeId: String = "",
    val isEnrolled: Boolean = false,
    val attendanceRecords: List<AttendanceRecord> = emptyList()
)

@HiltViewModel
class StaffProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val staffRepository: StaffRepository,
    attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val staffId: Long = checkNotNull(savedStateHandle["staffId"]) {
        "StaffProfileScreen requires a staffId nav argument"
    }

    // One-shot load — fine because every path that changes enrollment status (Face Enrollment)
    // always returns here via a freshly-created instance of this screen rather than popping
    // back to a cached one; see AppNavGraph's FACE_ENROLLMENT wiring for why.
    private val staffInfo = MutableStateFlow<Staff?>(null)

    private val attendanceRecords: StateFlow<List<AttendanceRecord>> =
        attendanceRepository.observeAttendanceForStaff(staffId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<StaffProfileUiState> =
        combine(staffInfo, attendanceRecords) { staff, records ->
            StaffProfileUiState(
                isLoading = staff == null,
                staffName = staff?.name ?: "",
                employeeId = staff?.employeeId ?: "",
                isEnrolled = staff?.isEnrolled == true,
                attendanceRecords = records
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StaffProfileUiState())

    init {
        viewModelScope.launch {
            staffInfo.value = staffRepository.getStaffById(staffId)
        }
    }
}
