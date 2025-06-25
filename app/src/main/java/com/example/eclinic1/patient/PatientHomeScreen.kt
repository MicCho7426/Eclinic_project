package com.example.eclinic1.patient

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.eclinic1.R
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    navController: NavHostController,
    viewModel: PatientHomeViewModel = viewModel(),
    patientNavController: NavHostController,
    parentNavController: NavController
) {
    val context = LocalContext.current
    val userData by viewModel.userData.collectAsState()
    val patientData by viewModel.patientData.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
        return
    }

    LaunchedEffect(userId) {
        viewModel.loadData(userId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = userData?.let { "Welcome, ${it.firstName}\uD83D\uDC4B" } ?: "Welcome",
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> FullScreenLoader()
            else -> PatientContent(
                modifier = Modifier.padding(paddingValues),
                appointments = appointments,
                patientData = patientData,
                navController = navController,
                onFileClick = { url -> openFile(context, url) },
                patientNavController = patientNavController
            )
        }
    }
}

@Composable
private fun FullScreenLoader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PatientContent(
    modifier: Modifier = Modifier,
    appointments: List<Appointment>,
    patientData: PatientData?,
    navController: NavHostController,
    onFileClick: (String) -> Unit,
    patientNavController: NavHostController

) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upcoming Appointments
        AppointmentsSection(appointments, navController)

        Spacer(modifier = Modifier.height(24.dp))

        // Health Summary
        patientData?.let { data ->
            HealthSummaryCard(
                patientData = data,
                onFileClick = onFileClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Button(
            onClick = {
                patientNavController.navigate("bookAppointment") {
                }
            }
        ) {
            Text("Book Appointment")
        }
    }
}

@Composable
private fun AppointmentsSection(
    appointments: List<Appointment>,
    navController: NavHostController
) {
    Column {
        Text(
            text = stringResource(R.string.upcoming_appointments),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            appointments.isEmpty() -> NoAppointmentsMessage()
            else -> AppointmentList(appointments, navController)
        }
    }
}

@Composable
private fun NoAppointmentsMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No upcoming appointments",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You don't have any scheduled appointments",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppointmentList(
    appointments: List<Appointment>,
    navController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = appointments,
            key = { it.id }
        ) { appointment ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AppointmentCard(
                    appointment = appointment,
                    onViewDetails = { navController.navigate("appointmentDetails/${appointment.id}") }
                )
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(
    patientData: PatientData,
    onFileClick: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            HealthSummaryHeader()
            Spacer(modifier = Modifier.height(12.dp))
            PatientDetails(patientData)
            MedicalFilesSection(patientData.uploadedFiles, onFileClick)
        }
    }
}

@Composable
private fun HealthSummaryHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.MonitorHeart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.health_summary),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PatientDetails(patientData: PatientData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        patientData.dob?.let { Text("${stringResource(R.string.dob)}: $it") }
        patientData.height?.let { Text("${stringResource(R.string.height)}: $it cm") }
        patientData.weight?.let { Text("${stringResource(R.string.weight)}: $it kg") }
        patientData.medicalHistory?.takeIf { it.isNotBlank() }?.let {
            Text("${stringResource(R.string.medical_history)}: ${it.take(50)}...")
        }
    }
}

@Composable
private fun MedicalFilesSection(
    files: List<String>,
    onFileClick: (String) -> Unit
) {
    if (files.isNotEmpty()) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${stringResource(R.string.medical_files)}:",
                style = MaterialTheme.typography.labelMedium
            )
            files.forEachIndexed { index, fileUrl ->
                Text(
                    text = "${index + 1}. ${fileUrl.takeLast(30)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onFileClick(fileUrl) }
                )
            }
        }
    }
}

private fun openFile(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
    val formattedDate = remember(appointment.date) {
        dateFormatter.format(appointment.date.toDate())

    }
    val statusColor = when (appointment.status.lowercase()) {
        "scheduled" -> Color(0xFF2196F3)  // Blue
        "completed" -> Color(0xFF4CAF50)   // Green
        "cancelled" -> Color(0xFFF44336)   // Red
        else -> Color(0xFF9E9E9E)          // Gray
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appointment",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("With: ${appointment.doctorName}")
                Text("Date: $formattedDate")
                Text("Time: ${appointment.startTime} - ${appointment.endTime}")
                Text("Status: ${appointment.status.replaceFirstChar { it.uppercase() }}")
            }
            Box(
                modifier = Modifier
                    .background(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = appointment.status.replaceFirstChar { it.uppercase() },
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}