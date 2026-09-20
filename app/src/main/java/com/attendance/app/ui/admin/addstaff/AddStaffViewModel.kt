package com.attendance.app.ui.admin.addstaff

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.domain.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddStaffUiState(
    val name: String = "",
    val employeeId: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddStaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddStaffUiState())
    val uiState: StateFlow<AddStaffUiState> = _uiState.asStateFlow()

    // One-time "saved, go to face enrollment" signal — not state, so it can't accidentally
    // re-fire on recomposition/config change the way a StateFlow value could.
    private val _saveSuccess = Channel<Long>(Channel.BUFFERED)
    val saveSuccess = _saveSuccess.receiveAsFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onEmployeeIdChange(value: String) {
        _uiState.update { it.copy(employeeId = value, errorMessage = null) }
    }

    fun onSaveClick() {
        val name = _uiState.value.name.trim()
        val employeeId = _uiState.value.employeeId.trim()

        if (name.isBlank() || employeeId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both staff name and employee ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val newStaffId = staffRepository.addStaff(name, employeeId)
                _uiState.update { it.copy(isSaving = false) }
                _saveSuccess.send(newStaffId)
            } catch (e: SQLiteConstraintException) {
                // Thrown by Room when the unique index on StaffEntity.employeeId is violated.
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Employee ID \"$employeeId\" is already in use")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Could not save staff member. Please try again.")
                }
            }
        }
    }
}
