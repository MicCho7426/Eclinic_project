package com.example.eclinic1.chat

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

data class PatientUser(
    val uid: String,
    val firstname: String,
    val surname: String
)

@Composable
fun CreateChatDoctorScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val doctorId = remember { mutableStateOf<String?>(null) }
    val patients = remember { mutableStateListOf<PatientUser>() }
    var search by remember { mutableStateOf("") }

    // Pobierz doctorId z Firestore
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    doctorId.value = doc.getString("DoctorId")
                }
        }

        // Pobierz pacjentów
        db.collection("users")
            .whereEqualTo("role", "patient")
            .get()
            .addOnSuccessListener { result ->
                patients.clear()
                for (doc in result) {
                    patients.add(
                        PatientUser(
                            uid = doc.id,
                            firstname = doc.getString("firstname") ?: "",
                            surname = doc.getString("surname") ?: ""
                        )
                    )
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search patient by name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(patients.filter {
                it.firstname.contains(search, ignoreCase = true) ||
                        it.surname.contains(search, ignoreCase = true)
            }) { patient ->
                ElevatedCard(
                    onClick = {
                        createChatWithPatient(
                            db = db,
                            patientId = patient.uid,
                            doctorId = doctorId.value,
                            navController = navController
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${patient.firstname} ${patient.surname}")
                    }
                }
            }
        }
    }
}

private fun createChatWithPatient(
    db: FirebaseFirestore,
    patientId: String,
    doctorId: String?,
    navController: NavController
) {
    if (doctorId == null) return
    val chat = hashMapOf(
        "patientId" to patientId,
        "doctorId" to doctorId,
        "createdAt" to System.currentTimeMillis()
    )
    db.collection("chats").add(chat)
        .addOnSuccessListener { documentReference ->
            val newChatId = documentReference.id
            navController.navigate("chatDetail/$newChatId")
        }
}