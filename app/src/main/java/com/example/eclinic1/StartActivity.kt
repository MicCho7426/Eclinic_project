package com.example.eclinic1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.admin.StartAdmin
import com.example.eclinic1.doctor.DoctorActivity
import com.example.eclinic1.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

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
                SplashScreen(navController)
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
                        "admin" -> Intent(this, StartAdmin::class.java)
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
fun SplashScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 sekundy opóźnienia
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}
