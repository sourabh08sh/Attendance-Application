package com.attendance.app.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import java.io.File

sealed class CameraCaptureResult {
    data class Success(val file: File) : CameraCaptureResult()
    data class Error(val throwable: Throwable) : CameraCaptureResult()
}

/**
 * Holds the CameraX ImageCapture use case once CameraPreview has bound it, and exposes a single
 * capability: take a picture and save it to the file the caller provides. Deliberately knows
 * nothing about where that file should live, what it's for (enrollment vs. attendance), or how
 * many shots the caller wants — that policy belongs to whichever screen uses this.
 */
class CameraCaptureController {

    // Set by CameraPreview once camera binding completes; null before that (or after dispose).
    internal var imageCapture: ImageCapture? = null

    val isReady: Boolean
        get() = imageCapture != null

    fun captureImage(
        context: Context,
        outputFile: File,
        onResult: (CameraCaptureResult) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onResult(CameraCaptureResult.Error(IllegalStateException("Camera is not ready yet")))
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onResult(CameraCaptureResult.Success(outputFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(CameraCaptureResult.Error(exception))
                }
            }
        )
    }
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController =
    remember { CameraCaptureController() }
