package com.example.eclinic1.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.patient.PatientBottomNavigation
import com.example.eclinic1.patient.PatientNavHost
import com.example.eclinic1.patient.PatientNavItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ChatEntry(
    val chatId: String,
    val doctorId: String,
    val patientId: String
)

@Composable
fun ChatScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser ?: return
    val currentUserUid = currentUser.uid
    val chatNavController = rememberNavController()

    var userRole by remember { mutableStateOf("patient") }
    var userIdToQuery by remember { mutableStateOf(currentUserUid) }
    var chatList by remember { mutableStateOf<List<ChatEntry>>(emptyList()) }


    fun loadChats() {
            fetchChats(db, userIdToQuery, userRole) { list ->
                chatList = list
            }
        }

        LaunchedEffect(Unit) {
            db.collection("users").document(currentUserUid).get()
                .addOnSuccessListener { doc ->
                    userRole = doc.getString("role") ?: "patient"
                    userIdToQuery = if (userRole == "doctor") {
                        doc.getString("DoctorId") ?: currentUserUid
                    } else {
                        currentUserUid
                    }
                    loadChats()
                }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = {
                    if (userRole == "patient") {
                        navController.navigate("createChatPatient")
                    } else if (userRole == "doctor") {
                        navController.navigate("createChatDoctor")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Chat")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(chatList) { chat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Chat ID: ${chat.chatId}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "Doctor ID: ${chat.doctorId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Patient ID: ${chat.patientId}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = {
                                    navController.navigate("chatDetail/${chat.chatId}")
                                }) {
                                    Text("Open")
                                }

                                TextButton(onClick = {
                                    deleteChat(db, chat.chatId) {
                                        loadChats() // Refresh list after deletion
                                    }
                                }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
}

fun fetchChats(
    db: FirebaseFirestore,
    userId: String,
    role: String,
    onResult: (List<ChatEntry>) -> Unit
) {
    val field = if (role == "patient") "patientId" else "doctorId"
    db.collection("chats").whereEqualTo(field, userId).get()
        .addOnSuccessListener { result ->
            val list = result.documents.mapNotNull {
                ChatEntry(
                    chatId = it.id,
                    doctorId = it.getString("doctorId") ?: "",
                    patientId = it.getString("patientId") ?: ""
                )
            }
            onResult(list)
        }
}

fun deleteChat(
    db: FirebaseFirestore,
    chatId: String,
    onComplete: () -> Unit
) {
    db.collection("chats").document(chatId)
        .delete()
        .addOnSuccessListener { onComplete() }
}