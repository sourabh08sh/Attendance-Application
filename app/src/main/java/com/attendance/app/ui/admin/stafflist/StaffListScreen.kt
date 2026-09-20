package com.attendance.app.ui.admin.stafflist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.domain.model.Staff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    onAddStaffClick: () -> Unit,
    onStaffClick: (staffId: Long) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: StaffListViewModel = hiltViewModel()
) {
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff (${staffList.size})") },
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    }) {
                        Text("Log out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStaffClick) {
                // Plain text "+" rather than Icons.Default.Add: keeps this screen from pulling
                // in the material-icons-core artifact for a single glyph.
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        if (staffList.isEmpty()) {
            EmptyStaffList(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(staffList, key = { it.staffId }) { staff ->
                    StaffRow(staff = staff, onClick = { onStaffClick(staff.staffId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StaffRow(staff: Staff, onClick: () -> Unit) {
    // Tapping a row opens that staff member's profile (enrollment status + attendance history).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = staff.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "ID: ${staff.employeeId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        EnrollmentBadge(isEnrolled = staff.isEnrolled)
    }
}

@Composable
private fun EnrollmentBadge(isEnrolled: Boolean) {
    val label = if (isEnrolled) "Enrolled" else "Not enrolled"
    val color = if (isEnrolled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun EmptyStaffList(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No staff yet. Tap + to add your first staff member.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
