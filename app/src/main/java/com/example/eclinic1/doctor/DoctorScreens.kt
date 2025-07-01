package com.example.eclinic1.doctor

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
import com.example.eclinic1.SearchScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberAsyncImagePainter
import com.example.eclinic1.chat.UserData
import com.example.eclinic1.patient.Appointment
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Calendar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorHomeContent(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentDoctorId = auth.currentUser?.uid
    val context = LocalContext.current
    val now = remember { Date() }

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var todaysAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var upcomingAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var patientCount by remember { mutableStateOf(6) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch all necessary data
    LaunchedEffect(currentDoctorId) {
        if (currentDoctorId == null) {
            errorMessage = "Doctor not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // 1. Fetch future appointments with error handling
            val meetingsSnapshot = try {
                db.collection("meetings")
                    .whereEqualTo("doctorId", currentDoctorId)
                    .whereEqualTo("status", "scheduled")
                    .whereGreaterThanOrEqualTo("date", Timestamp.now())
                    .orderBy("date")
                    .orderBy("startTime")
                    .get()
                    .await()
            } catch (e: Exception) {
                errorMessage = "Failed to load appointments"
                Log.e("DoctorHome", "Appointments query failed", e)
                return@LaunchedEffect
            }

            // 2. Safely convert documents to Appointment objects
            val tempAppointments = meetingsSnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: run {
                        Log.w("FirestoreData", "Document ${doc.id} has null data")
                        return@mapNotNull null
                    }

                    // Safely extract all fields with fallbacks
                    val patientId = data["patientId"]?.toString()?.takeIf { it.isNotBlank() }
                        ?: run {
                            Log.w("InvalidData", "Document ${doc.id} has blank patientId")
                            return@mapNotNull null
                        }

                    Appointment(
                        id = doc.id,
                        patientId = patientId,
                        doctorId = data["doctorId"]?.toString() ?: "",
                        date = when (val dateField = data["date"]) {
                            is Timestamp -> dateField
                            is com.google.firebase.Timestamp -> dateField
                            else -> Timestamp.now().also {
                                Log.w("DateConversion", "Invalid date in doc ${doc.id}")
                            }
                        },
                        patientName = data["patientName"]?.toString() ?: "",
                        doctorName = data["doctorName"]?.toString() ?: "",
                        startTime = data["startTime"]?.toString() ?: "",
                        endTime = data["endTime"]?.toString() ?: "",
                        status = data["status"]?.toString() ?: "scheduled"
                    )
                } catch (e: Exception) {
                    Log.e("ManualConversion", "Failed to convert doc ${doc.id}", e)
                    null
                }
            }

            // 3. Enrich with patient names
            appointments = tempAppointments.map { appointment ->
                try {
                    val patientDoc = db.collection("users")
                        .document(appointment.patientId)
                        .get()
                        .await()

                    val firstName = patientDoc.getString("firstname") ?: ""
                    val surname = patientDoc.getString("surname") ?: ""
                    appointment.copy(patientName = "$firstName $surname".trim())
                } catch (e: Exception) {
                    Log.e("PatientError", "Failed to load patient ${appointment.patientId}", e)
                    appointment.copy(patientName = "Patient ${appointment.patientId}")
                }
            }

            // 4. Categorize appointments
            todaysAppointments = appointments.filter { appointment ->
                val cal1 = Calendar.getInstance().apply { time = now }
                val cal2 = Calendar.getInstance().apply { time = appointment.date.toDate() }
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
            }

            upcomingAppointments = appointments.filter { appointment ->
                appointment.date.toDate().after(now) && !todaysAppointments.contains(appointment)
            }

            // 5. Get patient count
            patientCount = try {
                db.collection("meetings")
                    .whereEqualTo("primaryDoctorId", currentDoctorId)
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                    .size()
            } catch (e: Exception) {
                Log.e("PatientCount", "Failed to get patient count", e)
                0
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Unexpected error occurred"
            isLoading = false
            Log.e("DoctorHome", "Data loading error", e)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome Doctor") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                isLoading -> FullScreenLoader()
                errorMessage != null -> ErrorState(
                    message = errorMessage ?: "Unknown error",
                    onRetry = { isLoading = true; errorMessage = null }
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(
                                value = todaysAppointments.size.toString(),
                                label = "Today",
                                icon = Icons.Default.Event
                            )
                            StatCard(
                                value = upcomingAppointments.size.toString(),
                                label = "Upcoming",
                                icon = Icons.Default.Schedule
                            )
                            /*StatCard(
                                value = patientCount.toString(),
                                label = "Patients",
                                icon = Icons.Default.People
                            )*/
                        }
                    }

                    // Today's Appointments
                    if (todaysAppointments.isNotEmpty()) {
                        item {
                            Text(
                                "Today's Appointments",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        items(todaysAppointments) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onClick = { navController.navigate("appointmentDetail/${appointment.id}") }
                            )
                        }
                    }

                    // Upcoming Appointments
                    if (upcomingAppointments.isNotEmpty()) {
                        item {
                            Text(
                                "Upcoming Appointments",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        items(upcomingAppointments) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onClick = { navController.navigate("appointmentDetail/${appointment.id}") }
                            )
                        }
                    }

                    // Empty State
                    if (todaysAppointments.isEmpty() && upcomingAppointments.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("No upcoming appointments")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember { dateFormatter.format(appointment.date.toDate()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                appointment.patientName,
                style = MaterialTheme.typography.titleMedium
            )
            Text("$formattedDate • ${appointment.startTime}")
            Text("Status: ${appointment.status.replaceFirstChar { it.uppercase() }}")
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, icon: ImageVector) {
    Card(
        modifier = Modifier.width(100.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label)
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
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
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message)
        Button(onClick = onRetry) {
            Text("Retry")
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
    val userId = auth.currentUser?.uid ?: return

    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var noteText by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { avatarUri = it } }

    // Load data
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get().addOnSuccessListener {
            userData = it.data
        }
        db.collection("doctorProfiles").document(userId).get().addOnSuccessListener {
            profileData = it.data
            noteText = it.getString("note") ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("👨‍⚕️ Doctor Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        val avatarUrl = profileData?.get("avatarUrl") as? String
        if (avatarUri != null) {
            Image(
                painter = rememberAsyncImagePainter(avatarUri),
                contentDescription = null,
                modifier = Modifier.size(120.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (!avatarUrl.isNullOrEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(avatarUrl),
                contentDescription = null,
                modifier = Modifier.size(120.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(120.dp))
        }

        TextButton(onClick = { pickImageLauncher.launch("image/*") }) {
            Text("Change Avatar")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth()
        )

        userData?.let {
            val firstname = it["firstname"] as? String ?: ""
            val surname = it["surname"] as? String ?: ""
            val email = it["email"] as? String ?: ""

            Spacer(Modifier.height(12.dp))
            ProfileInfoItem(Icons.Default.Person, "Name", "$firstname $surname")
            ProfileInfoItem(Icons.Default.Email, "Email", email)
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val docRef = db.collection("doctorProfiles").document(userId)

            fun saveProfile(url: String?) {
                val data = hashMapOf(
                    "note" to noteText,
                    "avatarUrl" to (url ?: profileData?.get("avatarUrl"))
                )
                docRef.set(data).addOnSuccessListener {
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            }

            if (avatarUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("avatars/$userId.jpg")
                storageRef.putFile(avatarUri!!).continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    saveProfile(uri.toString())
                }.addOnFailureListener {
                    Log.e("Upload", "Avatar upload failed", it)
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                saveProfile(null)
            }
        }) {
            Text("💾 Save")
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("doctorSchedule") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🗓️ Manage Schedule")
        }

        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("🚪 Log Out")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Confirm Logout") },
            text = { Text("Do you really want to log out?") }
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
                is ImageVector -> {
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