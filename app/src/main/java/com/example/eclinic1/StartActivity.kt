package com.example.eclinic1

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
import com.example.eclinic1.admin.AdminActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : ComponentActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                auth.currentUser?.let { user ->
                    navigateToRoleScreen(user.uid)
                } ?: run {
                    startActivity(Intent(this@StartActivity, LoginActivity::class.java))
                    finish()
                }
            }

            if (isLoading) {
                SplashScreen() // Your existing splash
            }
        }
    }

    private fun navigateToRoleScreen(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val intent = when (doc.getString("role")) {
                    "admin" -> Intent(this, MainActivity::class.java).apply {
                        putExtra("startDestination", "adminHome")
                    }
                    "doctor" -> Intent(this, MainActivity::class.java).apply {
                        putExtra("startDestination", "doctorHome")
                    }
                    else -> Intent(this, MainActivity::class.java).apply {
                        putExtra("startDestination", "patientHome/$userId")
                    }
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                auth.signOut()
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
