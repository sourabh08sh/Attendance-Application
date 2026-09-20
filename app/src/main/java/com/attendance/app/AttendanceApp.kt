package com.attendance.app

import android.app.Application
import com.attendance.app.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root Application class. @HiltAndroidApp triggers Hilt's code generation and creates
 * the top-level DI container that all other components (Activities, ViewModels, etc.) hang off.
 *
 * Also kicks off one-time dummy data seeding on launch — see DatabaseSeeder for what gets
 * seeded and why it's safe to call on every app start.
 */
@HiltAndroidApp
class AttendanceApp : Application() {

    @Inject lateinit var databaseSeeder: DatabaseSeeder

    // Application has no lifecycle scope of its own, so this is a manual scope tied to the
    // process. SupervisorJob so a seeding failure can't take anything else down with it.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            databaseSeeder.seedIfEmpty()
        }
    }
}
