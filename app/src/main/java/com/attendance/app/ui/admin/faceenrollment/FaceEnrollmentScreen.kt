package com.attendance.app.ui.admin.faceenrollment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.camera.CameraPreview
import com.attendance.app.camera.rememberCameraCaptureController
import com.attendance.app.camera.rememberCameraPermissionState
import com.attendance.app.util.openAppSettings
import java.io.File

/**
 * [onExit] is used identically for the Cancel button, the Done button, the automatic
 * "enrollment saved" event, and the system back gesture (via BackHandler below). All four leave
 * this screen the same way — see AppNavGraph's FACE_ENROLLMENT wiring for why that
 * consistency matters: it's what keeps the Staff Profile screen this returns to from ever
 * showing stale enrollment status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceEnrollmentScreen(
    onExit: () -> Unit,
    viewModel: FaceEnrollmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionState = rememberCameraPermissionState()
    val controller = rememberCameraCaptureController()

    BackHandler(onBack = onExit)

    LaunchedEffect(Unit) {
        if (!permissionState.hasPermission) {
            permissionState.requestPermission()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.finished.collect { onExit() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isReenrollment) "Re-enroll Face" else "Enroll Face") },
                navigationIcon = {
                    TextButton(onClick = onExit) { Text("Cancel") }
                }
            )
        }
    ) { padding ->
        if (!permissionState.hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (permissionState.isPermanentlyDenied) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Camera permission was denied. Enable it from Settings to enroll a face.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { openAppSettings(context) }) { Text("Open Settings") }
                    }
                } else {
                    Text(
                        text = "Camera permission is required to enroll a face.",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                uiState.staffName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (uiState.isComplete) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Enrollment complete", style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    CameraPreview(
                        controller = controller,
                        modifier = Modifier.weight(1f),
                        onError = { viewModel.onCameraError(it.message ?: "Unknown error") }
                    )
                }

                LinearProgressIndicator(
                    progress = { uiState.capturesCollected / FaceEnrollmentViewModel.REQUIRED_CAPTURES.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = uiState.statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (uiState.isComplete) {
                    Button(
                        onClick = onExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Done")
                    }
                } else {
                    Button(
                        onClick = {
                            val outputFile = File(context.cacheDir, "enroll_${System.currentTimeMillis()}.jpg")
                            controller.captureImage(context, outputFile) { result ->
                                viewModel.onCaptureResult(result)
                            }
                        },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Capture (${uiState.capturesCollected}/${FaceEnrollmentViewModel.REQUIRED_CAPTURES})")
                        }
                    }
                }
            }
        }
    }
}
