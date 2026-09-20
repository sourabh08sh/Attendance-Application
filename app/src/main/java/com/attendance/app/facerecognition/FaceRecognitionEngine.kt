package com.attendance.app.facerecognition

import android.graphics.Bitmap
import android.graphics.Rect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single contract Face Enrollment and Mark Attendance will depend on. Neither of those
 * screens (nor their ViewModels) should ever import ML Kit or TFLite types directly — everything
 * about detection, quality checks, cropping, and embedding is behind this interface, so the
 * underlying model(s) can be swapped later without touching consumers.
 */
interface FaceRecognitionEngine {
    /** Detects a single face in [bitmap], validates it, and extracts its embedding if valid. */
    suspend fun process(bitmap: Bitmap): FaceProcessingResult

    fun compare(embeddingA: FloatArray, embeddingB: FloatArray): Float

    fun isMatch(similarity: Float): Boolean
}

@Singleton
class MlKitTfLiteFaceRecognitionEngine @Inject constructor(
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder
) : FaceRecognitionEngine {

    override suspend fun process(bitmap: Bitmap): FaceProcessingResult {
        val faces = try {
            faceDetector.detectFaces(bitmap)
        } catch (e: Exception) {
            return FaceProcessingResult.Error(e)
        }

        if (faces.isEmpty()) return FaceProcessingResult.NoFaceDetected
        if (faces.size > 1) return FaceProcessingResult.MultipleFacesDetected

        val face = faces.first()

        val minDimension = minOf(bitmap.width, bitmap.height)
        if (face.boundingBox.width() < minDimension * MIN_FACE_SIZE_RATIO) {
            return FaceProcessingResult.FaceTooSmall
        }

        // Only meaningful when classification mode is enabled (it is, in MlKitFaceDetector);
        // null (not enabled) is treated as "open" so this check never blocks a valid capture.
        val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
        if (leftEyeOpen < EYE_OPEN_THRESHOLD || rightEyeOpen < EYE_OPEN_THRESHOLD) {
            return FaceProcessingResult.EyesClosed
        }

        if (!faceEmbedder.isModelLoaded) return FaceProcessingResult.EmbeddingModelUnavailable

        val cropped = cropToBoundingBox(bitmap, face.boundingBox)
            ?: return FaceProcessingResult.NoFaceDetected

        val embedding = faceEmbedder.embed(cropped)
            ?: return FaceProcessingResult.EmbeddingModelUnavailable

        return FaceProcessingResult.Success(embedding)
    }

    override fun compare(embeddingA: FloatArray, embeddingB: FloatArray): Float =
        FaceMatcher.cosineSimilarity(embeddingA, embeddingB)

    override fun isMatch(similarity: Float): Boolean = FaceMatcher.isMatch(similarity)

    private fun cropToBoundingBox(bitmap: Bitmap, box: Rect): Bitmap? {
        val left = box.left.coerceIn(0, bitmap.width)
        val top = box.top.coerceIn(0, bitmap.height)
        val right = box.right.coerceIn(0, bitmap.width)
        val bottom = box.bottom.coerceIn(0, bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private companion object {
        const val MIN_FACE_SIZE_RATIO = 0.15f
        const val EYE_OPEN_THRESHOLD = 0.4f
    }
}
