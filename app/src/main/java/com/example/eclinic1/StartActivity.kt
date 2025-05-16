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
                auth.currentUser?.let { user ->
                    navigateToRoleScreen(user.uid)
                } ?: run {
                    startActivity(Intent(this@StartActivity, LoginActivity::class.java))
                    finish()
                }
            }

            if (isLoading) {
                SplashScreen(navController)
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
