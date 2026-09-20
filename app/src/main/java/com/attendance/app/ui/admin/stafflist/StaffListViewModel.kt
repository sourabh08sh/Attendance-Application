package com.attendance.app.ui.admin.stafflist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.domain.model.Staff
import com.attendance.app.domain.model.User
import com.attendance.app.domain.repository.StaffRepository
import com.attendance.app.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StaffListViewModel @Inject constructor(
    staffRepository: StaffRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // stateIn keeps the Flow hot for 5s after the screen goes away, so a quick config change
    // (rotation) doesn't re-trigger a fresh Room query and a flash of an empty list.
    val staffList: StateFlow<List<Staff>> = staffRepository.observeAllStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<User?> = sessionManager.currentUser

    fun logout() {
        sessionManager.clearSession()
    }
}
