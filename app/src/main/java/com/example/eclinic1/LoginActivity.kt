package com.example.eclinic1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            AppNavHost(
                navController,
                startDestination = "login"
            )
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
    val activity = context as? ComponentActivity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("Permission", "Zgoda na powiadomienia udzielona.")
            } else {
                Log.d("Permission", "Zgoda na powiadomienia odrzucona.")
            }
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                    )
                )
        ) {
            // Górna fala
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.TopCenter)
            ) {
                val width = size.width
                val height = size.height

                val path = Path().apply {
                    moveTo(0f, height * 0.7f)
                    quadraticBezierTo(
                        width / 2,
                        height * 1.2f,
                        width,
                        height * 0.7f
                    )
                    lineTo(width, 0f)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White
                )
            }

            // Logo na górze
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = emailState.value,
                    onValueChange = { emailState.value = it },
                    label = { Text("Email") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passwordState.value,
                    onValueChange = { passwordState.value = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        auth.signInWithEmailAndPassword(emailState.value, passwordState.value)
                            .addOnSuccessListener {
                                val userId = auth.currentUser?.uid

                                if (userId != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val sharedPreferences =
                                            context.getSharedPreferences(
                                                "prefs",
                                                Context.MODE_PRIVATE
                                            )
                                        val alreadyAsked =
                                            sharedPreferences.getBoolean(
                                                "asked_notifications",
                                                false
                                            )

                                        if (!alreadyAsked) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            sharedPreferences.edit()
                                                .putBoolean("asked_notifications", true).apply()
                                        }
                                    }


                                    FirebaseMessaging.getInstance().token
                                        .addOnSuccessListener { token ->
                                            db.collection("users").document(userId)
                                                .update("fcm", token)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        "FCM",
                                                        "Token FCM zaktualizowany po logowaniu"
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    Log.e(
                                                        "FCM",
                                                        "Błąd aktualizacji tokena: ${it.message}"
                                                    )
                                                }


                                            db.collection("users").document(userId).get()
                                                .addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        val role =
                                                            document.getString("role") ?: "user"
                                                        Toast.makeText(
                                                            context,
                                                            "Zalogowano jako: $role",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        Log.d("Login", "Rola użytkownika: $role")

                                                        when (role) {
                                                            "patient" -> {
                                                                db.collection("patients")
                                                                    .document(userId)
                                                                    .get()
                                                                    .addOnSuccessListener { patientDoc ->
                                                                        val hasMedicalData =
                                                                            patientDoc.exists()
                                                                        navController.navigate("patientHome") {
                                                                            popUpTo("login") {
                                                                                inclusive = true
                                                                            }
                                                                        }
                                                                    }
                                                                    .addOnFailureListener {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Błąd sprawdzania danych medycznych",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                            }

                                                            "admin" -> navController.navigate("admin") {
                                                                popUpTo("login") {
                                                                    inclusive = true
                                                                }
                                                            }

                                                            "doctor" -> navController.navigate("doctorHome") {
                                                                popUpTo("login") {
                                                                    inclusive = true
                                                                }
                                                            }

                                                            else -> navController.navigate("main") {
                                                                popUpTo("login") {
                                                                    inclusive = true
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Log.w(
                                                            "Login",
                                                            "Dokument użytkownika nie istnieje"
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Nie znaleziono danych użytkownika",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Błąd pobierania roli użytkownika",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }

                                } else {
                                    Toast.makeText(
                                        context,
                                        "Nie udało się pobrać UID użytkownika",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Logowanie nie powiodło się: ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                ) {
                    Text("Login")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { navController.navigate("register") }
                ) {
                    Text("Don't have an account? Register Here")
                }
            }
        }
    }
}
