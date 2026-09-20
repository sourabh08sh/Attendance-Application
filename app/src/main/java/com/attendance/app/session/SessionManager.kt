package com.attendance.app.session

import com.attendance.app.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks who is currently logged in on this shared/kiosk device. Deliberately in-memory only
 * (not persisted to disk): on a shared device, a process restart landing back on the Login
 * screen is the correct, safe behavior — we don't want a stale session surviving a reboot.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun clearSession() {
        _currentUser.value = null
    }
}
