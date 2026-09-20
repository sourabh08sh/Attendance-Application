package com.attendance.app.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Small reusable holder for "does this screen have CAMERA permission, and a way to request it."
 * Extracted here because every camera-using screen needs the identical check-then-request
 * pattern — permission handling is still each screen's own responsibility (per the camera
 * component's isolation contract), this just avoids writing the same boilerplate repeatedly.
 *
 * Also tracks [isPermanentlyDenied]: once the user has refused once and Android stops showing
 * the system permission dialog, calling requestPermission() again silently does nothing — the
 * only way forward is the app's system Settings page. Screens should branch on this rather than
 * showing an endlessly unresponsive "Grant Permission" button.
 */
class CameraPermissionState(
    val hasPermission: Boolean,
    val isPermanentlyDenied: Boolean,
    val requestPermission: () -> Unit
)

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // Survives recomposition/rotation so "never asked" and "asked and refused" stay distinct.
    var hasRequestedBefore by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        hasRequestedBefore = true
    }

    // shouldShowRequestPermissionRationale is false both before the first request AND after a
    // permanent denial — hasRequestedBefore is what distinguishes the two.
    val isPermanentlyDenied = !hasPermission && hasRequestedBefore && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    return CameraPermissionState(
        hasPermission = hasPermission,
        isPermanentlyDenied = isPermanentlyDenied,
        requestPermission = { launcher.launch(Manifest.permission.CAMERA) }
    )
}
