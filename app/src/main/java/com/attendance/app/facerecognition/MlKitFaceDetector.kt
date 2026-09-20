package com.attendance.app.facerecognition

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around ML Kit's on-device face detector. Only responsible for finding faces (and
 * their classifications, e.g. eye-open probability) in a bitmap — no embedding, no matching, no
 * knowledge of enrollment/attendance. Runs fully on-device via Google Play Services; needs no
 * model file bundled with the app.
 */
class MlKitFaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // enables eye-open probabilities
            .setMinFaceSize(0.15f)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<Face> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces -> continuation.resume(faces) }
                .addOnFailureListener { e -> continuation.resumeWithException(e) }
        }
}
