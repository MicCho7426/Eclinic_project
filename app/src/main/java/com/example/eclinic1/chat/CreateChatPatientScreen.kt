package com.example.eclinic1.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun CreateChatPatientScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    var doctorList by remember { mutableStateOf<List<DoctorInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val id = doc.getString("DoctorId") ?: return@mapNotNull null
                    val name = "${doc.getString("firstname") ?: ""} ${doc.getString("surname") ?: ""}"
                    val specs = doc.get("Specialization") as? List<String> ?: emptyList()
                    DoctorInfo(doctorId = id, fullName = name, specializations = specs)
                }
                doctorList = list
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose a doctor to start a chat:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(doctorList) { doctor ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            createChat(currentUserId, doctor.doctorId, navController)
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(doctor.fullName, style = MaterialTheme.typography.titleSmall)
                        Text("Specializations: ${doctor.specializations.joinToString()}")
                    }
                }
            }
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