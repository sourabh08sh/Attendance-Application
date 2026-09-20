package com.attendance.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin wrapper around FusedLocationProviderClient for a single one-shot location fetch —
 * mirrors the isolation pattern used for the camera and face recognition components. Checks
 * the permission itself and reports PermissionDenied rather than crashing, so a caller that
 * forgot to check still fails safely; the calling screen should still request the permission
 * proactively for a decent UX (see LocationPermissionState).
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // permission is checked explicitly below before this runs
    suspend fun getCurrentLocation(): LocationResult {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return LocationResult.PermissionDenied

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    val result = if (location != null) {
                        LocationResult.Success(location.latitude, location.longitude)
                    } else {
                        LocationResult.LocationUnavailable
                    }
                    continuation.resume(result)
                }
                .addOnFailureListener { e ->
                    continuation.resume(LocationResult.Error(e))
                }

            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
        }
    }
}
