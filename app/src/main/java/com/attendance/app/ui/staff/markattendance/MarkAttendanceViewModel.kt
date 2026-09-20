package com.attendance.app.ui.staff.markattendance

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.camera.CameraCaptureResult
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.StaffRepository
import com.attendance.app.facerecognition.FaceProcessingResult
import com.attendance.app.facerecognition.FaceRecognitionEngine
import com.attendance.app.location.LocationProvider
import com.attendance.app.location.LocationResult
import com.attendance.app.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class MarkAttendanceUiState(
    val staffName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null, // unrecoverable setup problem (no linked staff, etc.)
    val isEnrolled: Boolean = false,
    val alreadyCheckedInToday: Boolean = false,
    val todaysCheckInTime: String? = null,
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: String = ""
)

@HiltViewModel
class MarkAttendanceViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val staffRepository: StaffRepository,
    private val attendanceRepository: AttendanceRepository,
    private val faceRecognitionEngine: FaceRecognitionEngine,
    private val locationProvider: LocationProvider,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // The dummy-credential seed links staff1/staff2 logins to a staffId; a staff member added
    // later via Add Staff has no login account yet (out of scope for this step — see the
    // Step 8 write-up). This is the one place that gap would surface, so it's handled
    // defensively rather than assumed away.
    private val staffId: Long? = sessionManager.currentUser.value?.linkedStaffId

    private val _uiState = MutableStateFlow(MarkAttendanceUiState())
    val uiState: StateFlow<MarkAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadStaffStatus()
    }

    private fun loadStaffStatus() {
        val id = staffId
        if (id == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "No staff profile is linked to this account.")
            }
            return
        }

        viewModelScope.launch {
            val staff = staffRepository.getStaffById(id)
            if (staff == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Staff profile not found.") }
                return@launch
            }

            val alreadyChecked = attendanceRepository.hasMarkedAttendanceToday(id)
            val todaysTime = if (alreadyChecked) findTodaysCheckInTime(id) else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    staffName = staff.name,
                    isEnrolled = staff.isEnrolled,
                    alreadyCheckedInToday = alreadyChecked,
                    todaysCheckInTime = todaysTime
                )
            }
        }
    }

    private suspend fun findTodaysCheckInTime(id: Long): String? {
        val today = LocalDate.now().toString()
        return attendanceRepository.observeAttendanceForStaff(id).first()
            .firstOrNull { it.date == today }
            ?.time
    }

    fun onMarkAttendanceClick() {
        _uiState.update {
            it.copy(isCapturing = true, statusMessage = "Position your face in frame and tap Capture.")
        }
    }

    fun onCancelCapture() {
        _uiState.update { it.copy(isCapturing = false, isProcessing = false, statusMessage = "") }
    }

    fun onCameraError(message: String) {
        _uiState.update { it.copy(statusMessage = "Camera error: $message") }
    }

    fun onCaptureResult(result: CameraCaptureResult) {
        when (result) {
            is CameraCaptureResult.Error ->
                _uiState.update { it.copy(statusMessage = "Capture failed: ${result.throwable.message}") }
            is CameraCaptureResult.Success -> processCapturedFile(result.file)
        }
    }

    private fun processCapturedFile(file: File) {
        val id = staffId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Checking face...") }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                reject(file, "Could not read the captured photo. Try again.")
                return@launch
            }

            when (val result = faceRecognitionEngine.process(bitmap)) {
                is FaceProcessingResult.Success -> verifyAgainstEnrollment(file, result.embedding, id)
                FaceProcessingResult.NoFaceDetected ->
                    reject(file, "No face detected. Make sure your face is in frame.")
                FaceProcessingResult.MultipleFacesDetected ->
                    reject(file, "Multiple faces detected. Only you should be in frame.")
                FaceProcessingResult.FaceTooSmall ->
                    reject(file, "Face too small or too far. Move closer.")
                FaceProcessingResult.EyesClosed ->
                    reject(file, "Eyes appear closed. Try again with eyes open.")
                FaceProcessingResult.EmbeddingModelUnavailable ->
                    reject(file, "Face recognition isn't available on this device.")
                is FaceProcessingResult.Error ->
                    reject(file, "Face processing error: ${result.throwable.message}")
            }
        }
    }

    private suspend fun verifyAgainstEnrollment(file: File, capturedEmbedding: FloatArray, id: Long) {
        val enrolledEmbedding = staffRepository.getFaceEmbedding(id)
        if (enrolledEmbedding == null) {
            reject(file, "Enrollment data is missing. Contact your admin.")
            return
        }

        val similarity = faceRecognitionEngine.compare(capturedEmbedding, enrolledEmbedding)
        if (!faceRecognitionEngine.isMatch(similarity)) {
            reject(file, "Face didn't match our records. Try again.")
            return
        }

        proceedToLocationAndSave(file, similarity, id)
    }

    private suspend fun proceedToLocationAndSave(file: File, matchConfidence: Float, id: Long) {
        _uiState.update { it.copy(statusMessage = "Face verified. Getting your location...") }

        when (val locationResult = locationProvider.getCurrentLocation()) {
            is LocationResult.Success -> saveAttendance(file, id, matchConfidence, locationResult)
            LocationResult.PermissionDenied ->
                reject(file, "Location permission is required to mark attendance.")
            LocationResult.LocationUnavailable ->
                reject(file, "Couldn't get your location. Make sure GPS is on and try again.")
            is LocationResult.Error ->
                reject(file, "Location error: ${locationResult.throwable.message}")
        }
    }

    private suspend fun saveAttendance(file: File, id: Long, matchConfidence: Float, location: LocationResult.Success) {
        val permanentFile = moveToPermanentStorage(file, id)

        attendanceRepository.recordAttendance(
            staffId = id,
            selfiePath = permanentFile.absolutePath,
            latitude = location.latitude,
            longitude = location.longitude,
            matchConfidence = matchConfidence
        )

        val todaysTime = findTodaysCheckInTime(id)
        _uiState.update {
            it.copy(
                isProcessing = false,
                isCapturing = false,
                alreadyCheckedInToday = true,
                todaysCheckInTime = todaysTime,
                statusMessage = "Attendance marked successfully."
            )
        }
    }

    private fun reject(file: File, message: String) {
        file.delete() // don't leave rejected capture files lying around
        _uiState.update { it.copy(isProcessing = false, statusMessage = message) }
    }

    private fun moveToPermanentStorage(tempFile: File, id: Long): File {
        val dir = File(appContext.filesDir, "attendance").apply { mkdirs() }
        // Unlike enrollment's single overwritten photo, every attendance record needs its own
        // permanent selfie (admin's attendance history shows one per record), so the filename
        // includes a timestamp rather than being deterministic per staff member.
        val permanentFile = File(dir, "staff_${id}_${System.currentTimeMillis()}.jpg")
        tempFile.copyTo(permanentFile, overwrite = true)
        tempFile.delete()
        return permanentFile
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
