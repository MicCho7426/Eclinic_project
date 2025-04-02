package com.example.eclinic

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    val userId = auth.currentUser?.uid
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Fetch user data
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = document.data
                        Log.d("ProfileScreen", "User role: ${userData?.get("role")}") // Debugging
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

            if (role == "patient") { // ✅ Fixed case sensitivity
                val dob = userData?.get("dob") as? String ?: "N/A"
                val medicalHistory = userData?.get("medicalHistory") as? String ?: "N/A"
                val height = userData?.get("height") as? String ?: "N/A"
                val weight = userData?.get("weight") as? String ?: "N/A"

                //Text("Role: $role")   Debugging output
                Text("Date of Birth: $dob")
                Text("Medical History: $medicalHistory")
                Text("Height: $height cm")
                Text("Weight: $weight kg")

                // Display uploaded files if available
                val uploadedFiles = userData?.get("uploadedFiles") as? List<String>
                if (!uploadedFiles.isNullOrEmpty()) {
                    Text("Uploaded Files:")
                    uploadedFiles.forEach { fileUrl ->
                        Text(
                            text = fileUrl,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                context.startActivity(intent) // ✅ Use context variable
                            }
                        )
                    }
                }
            } else {
                Text("You are not a patient. No medical data available.") // 🔄 Should no longer appear incorrectly
            }
        } else {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ "Edit Medical Data" Button
        Button(
            onClick = {
                try {
                    navController.navigate("patientData")
                } catch (e: Exception) {
                    Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Edit Medical Data")
        }

        // ✅ Logout Button
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
}
