package com.example.eclinic1.doctor

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eclinic1.R
import com.example.eclinic1.SearchScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DoctorHomeContent(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentDoctorId = auth.currentUser?.uid
    val context = LocalContext.current

    // State for appointments and loading
    var appointments by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch appointments
    LaunchedEffect(currentDoctorId) {
        if (currentDoctorId == null) return@LaunchedEffect

        db.collection("meetings")
            .whereEqualTo("doctorId", currentDoctorId)
            .whereEqualTo("status", "scheduled")
            .orderBy("date")
            .orderBy("startTime")
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = "Error loading appointments: ${error.message}"
                    Log.e("DoctorHome", "Error loading appointments", error)
                    return@addSnapshotListener
                }

                val fetchedAppointments = mutableListOf<Pair<String, Map<String, Any>>>()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    fetchedAppointments.add(doc.id to data)
                }

                // Now fetch patient names for each appointment
                fetchedAppointments.forEach { (meetingId, meetingData) ->
                    val patientId = meetingData["patientId"] as? String ?: return@forEach

                    db.collection("users").document(patientId).get()
                        .addOnSuccessListener { patientDoc ->
                            val firstName = patientDoc.getString("firstname") ?: ""
                            val surname = patientDoc.getString("surname") ?: ""
                            val patientName = if (firstName.isNotEmpty() || surname.isNotEmpty()) {
                                "$firstName $surname".trim()
                            } else {
                                "Patient $patientId"
                            }

                            // Update the appointments list with patient names
                            appointments = appointments.map { (id, data) ->
                                if (id == meetingId) {
                                    id to (data + ("patientName" to patientName))
                                } else {
                                    id to data
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("DoctorHome", "Error fetching patient name", e)
                            appointments = appointments.map { (id, data) ->
                                if (id == meetingId) {
                                    id to (data + ("patientName" to "Patient $patientId"))
                                } else {
                                    id to data
                                }
                            }
                        }
                }

                appointments = fetchedAppointments
                Log.d("DoctorHome", "Fetched ${appointments.size} appointments")
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "👨‍⚕️ Welcome, Doctor!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today's Schedule",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚠️ Error loading appointments",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            appointments.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎉 No appointments scheduled",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "You're all caught up!",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(appointments) { (meetingId, meetingData) ->
                        val patientName = meetingData["patientName"] as? String ?:
                        "Patient ${meetingData["patientId"]}"
                        val date = meetingData["date"] as? String ?: "Unknown date"
                        val time = "${meetingData["startTime"]} - ${meetingData["endTime"]}"
                        val note = meetingData["note"] as? String ?: ""

                        AppointmentCard(
                            appointment = "$patientName\n$date at $time${if (note.isNotEmpty()) "\n✉️ Note: $note" else ""}",
                            onClick = { navController.navigate("appointmentDetail/$meetingId") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = "Appointment",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = appointment,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DoctorSearchScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            Text(
                text = "🔍 Patient Search",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        SearchScreen()
    }
}

@Composable
fun DoctorProfileScreen(navController: NavController, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val userId = auth.currentUser?.uid
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    userData = document.data?.apply {
                        Log.d("ProfileScreen", "User data: $this")
                    }
                }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile header with image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.doctor_avatar), // Add your doctor avatar image
                    contentDescription = "Doctor Profile",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "👨‍⚕️ Doctor Profile",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (userData != null) {
                    ProfileInfoItem(icon = Icons.Default.Person, title = "Name", value = userData?.get("name") as? String ?: "N/A")
                    ProfileInfoItem(icon = Icons.Default.Email, title = "Email", value = userData?.get("email") as? String ?: "N/A")
                    ProfileInfoItem(icon = Icons.Default.Work, title = "Specialization", value = userData?.get("specialization") as? String ?: "General Practitioner")
                    ProfileInfoItem(icon = Icons.Default.Phone, title = "Phone", value = userData?.get("phone") as? String ?: "N/A")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.navigate("doctorSchedule") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📅 Manage Schedule")
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🚪 Log Out")
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("🔒 Confirm Logout") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        auth.signOut()
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(icon: Any, title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (icon) {
                is androidx.compose.ui.graphics.vector.ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                is Int -> {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}