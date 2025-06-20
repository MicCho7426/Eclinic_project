package com.example.eclinic1.patient

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.eclinic1.Appointment
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
                patientNavController
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
    Text(
        text = stringResource(R.string.no_appointments),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 16.dp)
    )
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
        items(appointments) { appointment ->
            AppointmentCard(
                appointment = appointment,
                onViewDetails = { navController.navigate("appointmentDetails/${appointment.id}") }
            )
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
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onViewDetails: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
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
                Text("Date: ${appointment.date}")
                Text("Time: ${appointment.time}")
            }

            Button(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("View Details")
            }
        }
    }
}