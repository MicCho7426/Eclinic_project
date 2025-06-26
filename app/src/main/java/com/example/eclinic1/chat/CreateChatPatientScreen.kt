package com.example.eclinic1.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class DoctorInfo(
    val doctorId: String,
    val fullName: String,
    val specializations: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatPatientScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    var doctorList by remember { mutableStateOf<List<DoctorInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { result ->
                doctorList = result.documents.mapNotNull { doc ->
                    DoctorInfo(
                        doctorId = doc.getString("DoctorId") ?: doc.id,
                        fullName = "${doc.getString("firstname") ?: ""} ${doc.getString("surname") ?: ""}",
                        specializations = doc.get("Specialization") as? List<String> ?: emptyList()
                    )
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select Doctor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search doctors") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        doctorList.filter {
                            it.fullName.contains(searchQuery, ignoreCase = true) ||
                                    it.specializations.any { spec ->
                                        spec.contains(searchQuery, ignoreCase = true)
                                    }
                        }
                    ) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            onClick = {
                                createChat(currentUserId, doctor.doctorId, navController)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorCard(doctor: DoctorInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                doctor.fullName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Specializations: ${doctor.specializations.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun createChat(patientId: String, doctorId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val newChat = hashMapOf(
        "patientId" to patientId,
        "doctorId" to doctorId,
        "createdAt" to System.currentTimeMillis()
    )

    db.collection("chats")
        .add(newChat)
        .addOnSuccessListener { documentReference ->
            val newChatId = documentReference.id
            navController.navigate("chatDetail/$newChatId")
        }
}