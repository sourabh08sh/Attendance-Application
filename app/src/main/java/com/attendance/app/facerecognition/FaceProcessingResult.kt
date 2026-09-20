package com.attendance.app.facerecognition

/**
 * Outcome of running one bitmap through the face recognition pipeline (detect -> validate ->
 * crop -> embed). Deliberately fine-grained rather than a single boolean, so calling screens can
 * show the person a specific, actionable message ("eyes closed", "move closer") instead of a
 * generic failure.
 */
sealed class FaceProcessingResult {
    data class Success(val embedding: FloatArray) : FaceProcessingResult()
    data object NoFaceDetected : FaceProcessingResult()
    data object MultipleFacesDetected : FaceProcessingResult()
    data object FaceTooSmall : FaceProcessingResult()
    data object EyesClosed : FaceProcessingResult()

    /** Detection succeeded but mobilefacenet.tflite isn't in assets/ yet — see PLACE_MODEL_HERE.txt. */
    data object EmbeddingModelUnavailable : FaceProcessingResult()

    data class Error(val throwable: Throwable) : FaceProcessingResult()
}
