package com.example.eclinic1.doctor

import android.R.attr.data
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.storage.FirebaseStorage



@Composable
fun DoctorHomeContent(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentDoctorId = auth.currentUser?.uid
    val context = LocalContext.current

    var appointments by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                // Sort lokalnie po dacie i czasie
                val sortedAppointments = fetchedAppointments.sortedWith(compareBy(
                    { (it.second["date"] as? Timestamp)?.toDate() },
                    { it.second["startTime"] as? String }
                ))

                // Dodaj nazwy pacjentów
                sortedAppointments.forEach { (meetingId, meetingData) ->
                    val patientId = meetingData["patientId"] as? String ?: return@forEach

                    db.collection("users").document(patientId).get()
                        .addOnSuccessListener { patientDoc ->
                            val firstName = patientDoc.getString("firstname") ?: ""
                            val surname = patientDoc.getString("surname") ?: ""
                            val patientName = "$firstName $surname".trim().ifEmpty { "Patient $patientId" }

                            appointments = appointments.map { (id, data) ->
                                if (id == meetingId) {
                                    id to (data + ("patientName" to patientName))
                                } else id to data
                            }
                        }
                        .addOnFailureListener {
                            appointments = appointments.map { (id, data) ->
                                if (id == meetingId) {
                                    id to (data + ("patientName" to "Patient $patientId"))
                                } else id to data
                            }
                        }
                }

                appointments = sortedAppointments
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
                Text("👨‍⚕️ Welcome, Doctor!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Today's Schedule", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            errorMessage != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Error loading appointments", color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "Unknown error")
                }
            }

            appointments.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 No appointments scheduled", fontSize = 18.sp)
                    Text("You're all caught up!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appointments) { (meetingId, meetingData) ->
                    val patientName = meetingData["patientName"] as? String
                        ?: "Patient ${meetingData["patientId"]}"
                    val timestamp = meetingData["date"] as? Timestamp ?: Timestamp.now()
                    val startTime = meetingData["startTime"] as? String ?: ""
                    val endTime = meetingData["endTime"] as? String ?: ""
                    val note = meetingData["note"] as? String ?: ""

                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timestamp.toDate())
                    val time = "$startTime - $endTime"
                    val isPast = timestamp.toDate().before(Date())

                    val appointmentText = buildString {
                        appendLine("${if (isPast) "⏰ " else ""}$patientName")
                        appendLine("$date at $time")
                        if (note.isNotEmpty()) appendLine("✉️ Note: $note")
                    }

                    AppointmentCard(
                        appointment = appointmentText,
                        isPast = isPast,
                        onClick = { navController.navigate("appointmentDetail/$meetingId") }
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: String, isPast: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.surfaceVariant
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
                fontSize = 16.sp,
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