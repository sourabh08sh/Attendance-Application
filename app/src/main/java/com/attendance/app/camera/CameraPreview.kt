package com.attendance.app.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Renders a live camera preview and keeps [controller]'s ImageCapture use case bound while this
 * composable is on screen. This is the entire contract: preview + the ability to capture via the
 * controller. No capture button, no guidance overlay, no permission request — those are the
 * calling screen's job (see FaceEnrollmentScreen.kt or MarkAttendanceScreen.kt for the pattern).
 *
 * Caller must ensure the CAMERA permission is already granted before composing this; binding
 * will fail (and report through [onError]) otherwise.
 */
@Composable
fun CameraPreview(
    controller: CameraCaptureController,
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    onError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Keyed on lensFacing so a future front/back toggle would rebind correctly; for this app
    // lensFacing is always FRONT, so in practice this runs once per time the screen is shown.
    DisposableEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                controller.imageCapture = imageCapture
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            controller.imageCapture = null
            // The future may still be pending if the screen is left immediately; get() here
            // would block, so only unbind if binding already completed.
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { previewView }
    )
}
