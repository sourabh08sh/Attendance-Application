package com.attendance.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.domain.model.AuthResult
import com.attendance.app.domain.model.Role
import com.attendance.app.domain.repository.UserRepository
import com.attendance.app.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // One-time event, not state: a Role emitted here should trigger exactly one navigation,
    // never replay on recomposition/config change the way a StateFlow value would.
    private val _loginSuccess = Channel<Role>(Channel.BUFFERED)
    val loginSuccess = _loginSuccess.receiveAsFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both username and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = userRepository.login(state.username, state.password)) {
                is AuthResult.Success -> {
                    sessionManager.setCurrentUser(result.user)
                    _uiState.update { it.copy(isLoading = false, password = "") }
                    _loginSuccess.send(result.user.role)
                }
                AuthResult.InvalidCredentials -> {
                    _uiState.update {
                        it.copy(isLoading = false, password = "", errorMessage = "Invalid username or password")
                    }
                }
            }
        }
    }
}
