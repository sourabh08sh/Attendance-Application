package com.attendance.app.ui.admin.staffprofile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.attendance.app.domain.model.AttendanceRecord
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    onBack: () -> Unit,
    onEnrollClick: () -> Unit,
    viewModel: StaffProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.staffName.ifBlank { "Staff Profile" }) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item {
                    ProfileHeader(
                        employeeId = uiState.employeeId,
                        isEnrolled = uiState.isEnrolled,
                        onEnrollClick = onEnrollClick
                    )
                    HorizontalDivider()
                    Text(
                        text = "Attendance History (${uiState.attendanceRecords.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (uiState.attendanceRecords.isEmpty()) {
                    item {
                        Text(
                            text = "No attendance records yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(uiState.attendanceRecords, key = { it.attendanceId }) { record ->
                        AttendanceRecordRow(record)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    employeeId: String,
    isEnrolled: Boolean,
    onEnrollClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "Employee ID: $employeeId", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isEnrolled) "Face enrolled" else "Face not enrolled",
            color = if (isEnrolled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onEnrollClick, modifier = Modifier.fillMaxWidth()) {
            Text(if (isEnrolled) "Re-enroll Face" else "Enroll Face")
        }
    }
}

@Composable
private fun AttendanceRecordRow(record: AttendanceRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = File(record.selfiePath),
            contentDescription = "Attendance selfie from ${record.date}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = "${record.date}  ${record.time}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Lat: %.5f, Lng: %.5f".format(record.latitude, record.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
