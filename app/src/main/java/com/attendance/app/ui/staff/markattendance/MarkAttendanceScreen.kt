package com.attendance.app.ui.staff.markattendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.attendance.app.camera.CameraCaptureController
import com.attendance.app.camera.CameraPreview
import com.attendance.app.camera.rememberCameraCaptureController
import com.attendance.app.camera.rememberCameraPermissionState
import com.attendance.app.location.rememberLocationPermissionState
import com.attendance.app.util.openAppSettings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceScreen(
    onLoggedOut: () -> Unit,
    viewModel: MarkAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraPermission = rememberCameraPermissionState()
    val locationPermission = rememberLocationPermissionState()
    val controller = rememberCameraCaptureController()

    // Request both permissions together as soon as the person taps "Mark Attendance", rather
    // than surprising them with a location prompt only after they've already taken a selfie.
    LaunchedEffect(uiState.isCapturing) {
        if (uiState.isCapturing) {
            if (!cameraPermission.hasPermission) cameraPermission.requestPermission()
            if (!locationPermission.hasPermission) locationPermission.requestPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff") },
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    }) {
                        Text("Log out")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(padding)
            uiState.errorMessage != null -> ErrorState(padding, uiState.errorMessage!!)
            uiState.isCapturing -> CapturingContent(
                padding = padding,
                statusMessage = uiState.statusMessage,
                isProcessing = uiState.isProcessing,
                hasCameraPermission = cameraPermission.hasPermission,
                hasLocationPermission = locationPermission.hasPermission,
                cameraPermanentlyDenied = cameraPermission.isPermanentlyDenied,
                locationPermanentlyDenied = locationPermission.isPermanentlyDenied,
                onRequestCameraPermission = cameraPermission.requestPermission,
                onRequestLocationPermission = locationPermission.requestPermission,
                onOpenSettings = { openAppSettings(context) },
                controller = controller,
                onCameraError = { viewModel.onCameraError(it.message ?: "Unknown error") },
                onCapture = {
                    val outputFile = File(context.cacheDir, "attendance_${System.currentTimeMillis()}.jpg")
                    controller.captureImage(context, outputFile) { result -> viewModel.onCaptureResult(result) }
                },
                onCancel = viewModel::onCancelCapture
            )
            else -> IdleContent(
                padding = padding,
                uiState = uiState,
                onMarkAttendanceClick = viewModel::onMarkAttendanceClick
            )
        }
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(padding: PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}

@Composable
private fun IdleContent(
    padding: PaddingValues,
    uiState: MarkAttendanceUiState,
    onMarkAttendanceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome, ${uiState.staffName ?: ""}", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(24.dp))

        when {
            !uiState.isEnrolled -> Text(
                text = "Your face hasn't been enrolled yet. Contact your admin.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            uiState.alreadyCheckedInToday -> Text(
                text = uiState.statusMessage.ifBlank {
                    "You've already checked in today" +
                        (uiState.todaysCheckInTime?.let { " at $it" } ?: "") + "."
                },
                textAlign = TextAlign.Center
            )
            else -> Button(
                onClick = onMarkAttendanceClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark Attendance")
            }
        }
    }
}

@Composable
private fun CapturingContent(
    padding: PaddingValues,
    statusMessage: String,
    isProcessing: Boolean,
    hasCameraPermission: Boolean,
    hasLocationPermission: Boolean,
    cameraPermanentlyDenied: Boolean,
    locationPermanentlyDenied: Boolean,
    onRequestCameraPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    controller: CameraCaptureController,
    onCameraError: (Throwable) -> Unit,
    onCapture: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (!hasCameraPermission || !hasLocationPermission) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera and location permissions are required to mark attendance.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Once a permission is permanently denied, requesting it again does nothing
                    // (the system dialog no longer appears) — Settings is the only way forward,
                    // and one Settings page covers both permissions regardless of which is denied.
                    if (cameraPermanentlyDenied || locationPermanentlyDenied) {
                        Text(
                            text = "One or more permissions were denied. Enable them from Settings.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                        Button(onClick = onOpenSettings) { Text("Open Settings") }
                    } else {
                        if (!hasCameraPermission) {
                            Button(onClick = onRequestCameraPermission) { Text("Grant Camera Permission") }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (!hasLocationPermission) {
                            Button(onClick = onRequestLocationPermission) { Text("Grant Location Permission") }
                        }
                    }
                }
            }
        } else {
            CameraPreview(
                controller = controller,
                modifier = Modifier.weight(1f),
                onError = onCameraError
            )
        }

        Text(text = statusMessage, modifier = Modifier.padding(16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TextButton(onClick = onCancel, enabled = !isProcessing) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onCapture,
                enabled = hasCameraPermission && hasLocationPermission && !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Capture")
                }
            }
        }
    }
}
