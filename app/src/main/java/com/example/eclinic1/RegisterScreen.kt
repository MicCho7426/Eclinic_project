package com.example.eclinic1

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterClick: (String, String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstname by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp)) // Add spacing between fields

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = firstname,
            onValueChange = { firstname = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Surname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))


        // Register Button
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank() && firstname.isNotBlank() && surname.isNotBlank()) {
                    onRegisterClick(email, password, firstname, surname )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Register")
        }

        // Go to Login
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Log in")
        }
    }
}
fun registerUser(
    email: String,
    password: String,
    firstname: String,
    surname: String,

    navController: NavController
) {
    Log.d("Register", "registerUser() called") // ADD THIS

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Register", "User created successfully") // ADD THIS

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val user = mapOf(
                        "firstname" to firstname,
                        "surname" to surname,
                        "email" to email,
                        "uid" to userId
                    )

                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            Log.d("Register", "User data saved to Firestore") // ADD THIS
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Register", "Error saving user: ${e.message}")
                        }
                }
            } else {
                Log.e("Register", "Registration Failed: ${task.exception?.message}")
            }
        }
}
