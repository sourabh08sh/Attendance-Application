package com.attendance.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.app.ui.admin.addstaff.AddStaffScreen
import com.attendance.app.ui.admin.faceenrollment.FaceEnrollmentScreen
import com.attendance.app.ui.admin.staffprofile.StaffProfileScreen
import com.attendance.app.ui.admin.stafflist.StaffListScreen
import com.attendance.app.ui.auth.LoginScreen
import com.attendance.app.ui.staff.markattendance.MarkAttendanceScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToAdmin = {
                    navController.navigate(Routes.STAFF_LIST) {
                        // Login is never a valid back-stack target once signed in.
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToStaff = {
                    navController.navigate(Routes.STAFF_HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STAFF_LIST) {
            StaffListScreen(
                onAddStaffClick = { navController.navigate(Routes.ADD_STAFF) },
                onStaffClick = { staffId -> navController.navigate(Routes.staffProfile(staffId)) },
                onLoggedOut = { navController.navigateToLoginAndClearBackStack() }
            )
        }

        composable(
            route = Routes.STAFF_PROFILE,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            StaffProfileScreen(
                onBack = { navController.popBackStack() },
                onEnrollClick = { navController.navigate(Routes.faceEnrollment(staffId)) }
            )
        }

        composable(Routes.ADD_STAFF) {
            AddStaffScreen(
                onStaffSaved = { staffId ->
                    navController.navigate(Routes.faceEnrollment(staffId)) {
                        // Staff is already saved at this point — don't leave a stale, already-
                        // submitted Add Staff form sitting on the back stack behind enrollment.
                        popUpTo(Routes.ADD_STAFF) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FACE_ENROLLMENT,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            FaceEnrollmentScreen(
                // Always lands on a FRESH Staff Profile instance for this staff, clearing
                // everything above Staff List first. This is deliberate, not just "go back":
                // this screen can be reached both via Add Staff (no Profile on the stack yet)
                // and via an existing Staff Profile (which would otherwise show stale
                // enrollment status if we simply popped back to it). Navigating forward to a
                // new instance works correctly for both cases.
                onExit = {
                    navController.navigate(Routes.staffProfile(staffId)) {
                        popUpTo(Routes.STAFF_LIST) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.STAFF_HOME) {
            MarkAttendanceScreen(
                onLoggedOut = { navController.navigateToLoginAndClearBackStack() }
            )
        }
    }
}

private fun NavHostController.navigateToLoginAndClearBackStack() {
    navigate(Routes.LOGIN) {
        // popUpTo(0) clears the entire back stack — on a shared kiosk device, the next person
        // to use it should never be able to back-button into the previous user's screen.
        popUpTo(0) { inclusive = true }
    }
}
