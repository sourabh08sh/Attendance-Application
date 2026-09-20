package com.attendance.app.location

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data object PermissionDenied : LocationResult()
    data object LocationUnavailable : LocationResult()
    data class Error(val throwable: Throwable) : LocationResult()
}
