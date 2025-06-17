package com.example.eclinic1

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit) {
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
                    navController.navigate("patientData") // tam będzie redirect do DoctorScheduleScreen
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
