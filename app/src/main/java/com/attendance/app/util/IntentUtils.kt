package com.attendance.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens this app's page in the system Settings app (where permission toggles live). Used when a
 * permission has been permanently denied — at that point requesting it again does nothing, and
 * Settings is the only remaining path for the user to grant it.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
