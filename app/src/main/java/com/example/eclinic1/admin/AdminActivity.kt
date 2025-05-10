package com.example.eclinic1.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity(navController: NavHostController) : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var username by remember { mutableStateOf("Admin") }

            LaunchedEffect(Unit) {
                fetchUsername { name -> username = name }
            }

            AdminScreen(navController = rememberNavController())
        }
    }

    private fun fetchUsername(onResult: (String) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val firstName = document.getString("firstname") ?: "Admin"
                    val lastName = document.getString("surname") ?: ""
                    onResult("$firstName $lastName")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch username", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

@Composable
fun AdminScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    var pendingAdmins by remember { mutableStateOf(listOf<Map<String, String>>()) }

    LaunchedEffect(Unit) {
        firestore.collection("users")
            .whereEqualTo("role", "admin")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                pendingAdmins = result.documents.map { doc ->
                    mapOf(
                        "uid" to doc.id,
                        "name" to (doc.getString("firstname") ?: "") + " " + (doc.getString("surname") ?: "")
                    )
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Admin Dashboard", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        pendingAdmins.forEach { admin ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(admin["name"] ?: "Unknown")
                Button(onClick = {
                    firestore.collection("users").document(admin["uid"] ?: "").update("status", "approved")
                        .addOnSuccessListener {
                            pendingAdmins = pendingAdmins.filter { it["uid"] != admin["uid"] }
                        }
                }) {
                    Text("Approve")
                }
            }
        }
    }
}

