package com.example.eclinic1.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.TextFieldDefaults

data class PatientUser(
    val uid: String,
    val firstname: String,
    val surname: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDoctorScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val doctorId = remember { mutableStateOf<String?>(null) }
    val patients = remember { mutableStateListOf<PatientUser>() }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch doctor ID and patients
    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    doctorId.value = doc.getString("doctorId") ?: uid

                    // Fetch patients
                    db.collection("users")
                        .whereEqualTo("role", "patient")
                        .get()
                        .addOnSuccessListener { result ->
                            patients.clear()
                            patients.addAll(result.documents.mapNotNull { doc ->
                                PatientUser(
                                    uid = doc.id,
                                    firstname = doc.getString("firstname") ?: "",
                                    surname = doc.getString("surname") ?: ""
                                )
                            })
                            isLoading = false
                        }
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select Patient") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search patients") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Patients list
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        patients.filter { patient ->
                            searchQuery.isEmpty() ||
                                    patient.firstname.contains(searchQuery, ignoreCase = true) ||
                                    patient.surname.contains(searchQuery, ignoreCase = true)
                        }
                    ) { patient ->
                        PatientCard(
                            patient = patient,
                            onClick = {
                                doctorId.value?.let { docId ->
                                    createChatWithPatient(
                                        db = db,
                                        patientId = patient.uid,
                                        doctorId = docId,
                                        navController = navController
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(patient: PatientUser, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // You could add an avatar here
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.firstname.take(1).uppercase() + patient.surname.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${patient.firstname} ${patient.surname}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Patient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Start chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun createChatWithPatient(
    db: FirebaseFirestore,
    patientId: String,
    doctorId: String,
    navController: NavController
) {
    val chatData = hashMapOf(
        "patientId" to patientId,
        "doctorId" to doctorId,
        "createdAt" to System.currentTimeMillis()
    )

    db.collection("chats")
        .add(chatData)
        .addOnSuccessListener { docRef ->
            navController.navigate("chatDetail/${docRef.id}") {
                popUpTo("chatList") { inclusive = false }
            }
        }
        .addOnFailureListener {
            // Handle error (you might want to show a snackbar)
        }
}