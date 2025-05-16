package com.example.eclinic1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController() // Ensure correct NavController initialization
            AppNavHost(
                navController,
                startDestination = "login"
            ) // Pass it to the AppNavHost
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        // Email Input
        OutlinedTextField(
            value = emailState.value,
            onValueChange = { emailState.value = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Password Input
        OutlinedTextField(
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Login Button
        Button(
            onClick = {
                auth.signInWithEmailAndPassword(emailState.value, passwordState.value)
                    .addOnSuccessListener {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val role = document.getString("role") ?: "user"
                                        Toast.makeText(context, "Zalogowano jako: $role", Toast.LENGTH_SHORT).show()
                                        Log.d("Login", "User role: $role")

                                        if (role == "patient") {
                                            // Check if medical data exists
                                            db.collection("patients").document(userId).get()
                                                .addOnSuccessListener { patientDoc ->
                                                    val hasMedicalData = patientDoc.exists()



                                                    navController.navigate("patientHome") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Error checking medical data", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            // Navigate for admin and doctor
                                            val destination = when (role) {
                                                "admin" -> "admin"
                                                "doctor" -> "doctorHome"
                                                else -> "main"
                                            }
                                            navController.navigate(destination) {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error fetching user role", Toast.LENGTH_SHORT).show()
                                }
                        }else{
                            Toast.makeText(context, "Brak danych użytkownika w Firestore", Toast.LENGTH_SHORT).show()
                        }
                        Log.w("Login", "Document doesn't exist for userId: $userId")
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        ) {
            Text("Login")
        }



        Spacer(modifier = Modifier.height(10.dp))

        // Register Button
        TextButton(
            onClick = { navController.navigate("register") }
        ) {
            Text("Don't have an account? Register Here")
        }
    }
}







