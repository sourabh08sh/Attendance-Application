package com.attendance.app.ui.admin.faceenrollment

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.camera.CameraCaptureResult
import com.attendance.app.domain.repository.StaffRepository
import com.attendance.app.facerecognition.FaceProcessingResult
import com.attendance.app.facerecognition.FaceRecognitionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.sqrt

data class FaceEnrollmentUiState(
    val staffName: String? = null,
    val isReenrollment: Boolean = false,
    val capturesCollected: Int = 0,
    val statusMessage: String = "Position the face in frame and tap Capture.",
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class FaceEnrollmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val staffRepository: StaffRepository,
    private val faceRecognitionEngine: FaceRecognitionEngine,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val staffId: Long = checkNotNull(savedStateHandle["staffId"]) {
        "FaceEnrollmentScreen requires a staffId nav argument"
    }

    private val _uiState = MutableStateFlow(FaceEnrollmentUiState())
    val uiState: StateFlow<FaceEnrollmentUiState> = _uiState.asStateFlow()

    // One-time "enrollment saved, leave this screen" signal.
    private val _finished = Channel<Unit>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()

    private val collectedEmbeddings = mutableListOf<FloatArray>()

    // The most recent successfully-validated capture's temp file. Only this one ever becomes
    // permanent (on the final required capture); earlier successful captures' photos are
    // deleted once their embedding is collected — we don't need to keep all of them.
    private var lastGoodCaptureFile: File? = null

    init {
        viewModelScope.launch {
            val staff = staffRepository.getStaffById(staffId)
            _uiState.update {
                it.copy(
                    staffName = staff?.name ?: "Unknown staff",
                    isReenrollment = staff?.isEnrolled == true
                )
            }
        }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Checking face...") }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                file.delete()
                _uiState.update {
                    it.copy(isProcessing = false, statusMessage = "Could not read the captured photo. Try again.")
                }
                return@launch
            }

            when (val result = faceRecognitionEngine.process(bitmap)) {
                is FaceProcessingResult.Success -> onValidCapture(file, result.embedding)
                FaceProcessingResult.NoFaceDetected ->
                    reject(file, "No face detected. Make sure your face is in frame.")
                FaceProcessingResult.MultipleFacesDetected ->
                    reject(file, "Multiple faces detected. Only the staff member should be in frame.")
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

    private suspend fun onValidCapture(file: File, embedding: FloatArray) {
        collectedEmbeddings.add(embedding)
        lastGoodCaptureFile?.delete() // superseded — we only keep the most recent good capture's photo
        lastGoodCaptureFile = file

        val captured = collectedEmbeddings.size
        if (captured >= REQUIRED_CAPTURES) {
            finishEnrollment(file)
        } else {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    capturesCollected = captured,
                    statusMessage = "Good. Capture ${captured + 1} of $REQUIRED_CAPTURES."
                )
            }
        }
    }

    private fun reject(file: File, message: String) {
        file.delete() // don't leave rejected capture files lying around
        _uiState.update { it.copy(isProcessing = false, statusMessage = message) }
    }

    private suspend fun finishEnrollment(finalCaptureFile: File) {
        _uiState.update { it.copy(isProcessing = true, statusMessage = "Saving enrollment...") }

        val averaged = averageEmbeddings(collectedEmbeddings)
        val permanentFile = moveToPermanentStorage(finalCaptureFile)

        staffRepository.saveFaceEnrollment(staffId, permanentFile.absolutePath, averaged)

        _uiState.update {
            it.copy(
                isProcessing = false,
                isComplete = true,
                capturesCollected = REQUIRED_CAPTURES,
                statusMessage = "Enrollment complete."
            )
        }
        _finished.send(Unit)
    }

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        val size = embeddings.first().size
        val sum = FloatArray(size)
        for (embedding in embeddings) {
            for (i in 0 until size) {
                sum[i] = sum[i] + embedding[i]
            }
        }
        for (i in 0 until size) {
            sum[i] = sum[i] / embeddings.size
        }
        return l2Normalize(sum)
    }

    /**
     * Scales [embedding] to unit length. Averaging several already-normalized embeddings
     * shrinks the result's magnitude (it's no longer a point on the unit hypersphere), which
     * would skew cosine-similarity comparisons against it later — re-normalizing after
     * averaging keeps the stored embedding on the same footing as any single embedding it's
     * later compared against.
     */
    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sumOfSquares = 0f
        for (value in embedding) {
            sumOfSquares += value * value
        }
        val magnitude = sqrt(sumOfSquares)
        // Guard against a degenerate all-zero vector so this can never divide by zero / emit NaNs.
        if (magnitude == 0f) return embedding
        return FloatArray(embedding.size) { i -> embedding[i] / magnitude }
    }

    private fun moveToPermanentStorage(tempFile: File): File {
        val dir = File(appContext.filesDir, "enrollment").apply { mkdirs() }
        // Deterministic name per staff — a re-enrollment simply overwrites the old photo rather
        // than accumulating differently-named files over time.
        val permanentFile = File(dir, "staff_$staffId.jpg")
        tempFile.copyTo(permanentFile, overwrite = true)
        tempFile.delete()
        return permanentFile
    }

    override fun onCleared() {
        super.onCleared()
        // If the screen is left mid-enrollment (e.g. Cancel after some successful captures),
        // clean up the one dangling temp file. A no-op if enrollment already completed, since
        // moveToPermanentStorage() already deleted it.
        lastGoodCaptureFile?.delete()
    }

    companion object {
        const val REQUIRED_CAPTURES = 3
    }
}
