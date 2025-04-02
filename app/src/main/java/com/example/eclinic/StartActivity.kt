package com.example.eclinic

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var isLoading by remember { mutableStateOf(true) }
            val currentAuth = rememberUpdatedState(auth)

            LaunchedEffect(Unit) {
                val user = currentAuth.value.currentUser
                Log.d("StartActivity", "Checking user authentication...")

                if (user != null) {
                    Log.d("StartActivity", "User found: ${user.uid}")
                    navigateToCorrectScreen(user.uid)
                } else {
                    Log.d("StartActivity", "No user found, redirecting to login")
                    startActivity(Intent(this@StartActivity, LoginActivity::class.java))
                    finish()
                }
            }

            if (isLoading) {
                SplashScreen()
            }
        }
    }

    private fun navigateToCorrectScreen(userId: String) {
        Log.d("StartActivity", "Fetching user role for $userId")

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: "user"
                    val status = document.getString("status") ?: "pending"
                    Log.d("StartActivity", "User role: $role, Status: $status")

                    if (role == "admin" && status != "approved") {
                        Toast.makeText(this, "Admin approval pending", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                        return@addOnSuccessListener
                    }

                    // Redirect based on role
                    val intent = when (role) {
                        "admin" -> Intent(this, AdminActivity::class.java)
                        "doctor" -> Intent(this, DoctorActivity::class.java)
                        "user" -> Intent(this, HomeActivity::class.java)
                        else -> Intent(this, LoginActivity::class.java)
                    }

                    startActivity(intent)
                } else {
                    Log.d("StartActivity", "User document not found, redirecting to login")
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("StartActivity", "Error fetching user role", exception)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
