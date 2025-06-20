package com.example.eclinic1.doctor

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.eclinic1.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Home Content
@Composable
fun DoctorHomeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Today's Appointments",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Add appointment list here
    }
}

// Search Screen
@Composable
fun DoctorSearchScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Patient Search",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        // Add search functionality here
    }
}

// Profile Screen
@Composable
fun DoctorProfileScreen(navController: NavController, onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val userId = auth.currentUser?.uid
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                        Log.d("ProfileScreen", "User role: ${userData?.get("role")}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileScreen", "Error fetching user data: ${e.message}")
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        if (userData != null) {
            val role = (userData?.get("role") as? String)?.lowercase() ?: "N/A"

            if (role == "patient") {
                val dob = userData?.get("dob") as? String ?: "N/A"
                val medicalHistory = userData?.get("medicalHistory") as? String ?: "N/A"
                val height = userData?.get("height") as? String ?: "N/A"
                val weight = userData?.get("weight") as? String ?: "N/A"

                Text("Date of Birth: $dob")
                Text("Medical History: $medicalHistory")
                Text("Height: $height cm")
                Text("Weight: $weight kg")

                val uploadedFiles = userData?.get("uploadedFiles") as? List<String>
                if (!uploadedFiles.isNullOrEmpty()) {
                    Text("Uploaded Files:")
                    uploadedFiles.forEach { fileUrl ->
                        Text(
                            text = fileUrl,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("patientData") }) {
                    Text("Edit Medical Data")
                }
            } else if (role == "doctor") {
                Text("You are a doctor. You can manage your work schedule.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    navController.navigate("doctorSchedule")
                }) {
                    Text("Manage Schedule")
                }
            } else {
                Text("Unknown role: $role")
            }
        } else {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = {
                auth.signOut()
                navController.navigate("login") { popUpTo("profile") { inclusive = true } }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
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
            })
    }
}