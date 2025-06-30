package com.example.eclinic1.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

data class ChatEntry(
    val chatId: String,
    val doctorId: String,
    val patientId: String,
    val doctorName: String,
    val patientName: String
) {
    fun getOtherUserName(currentUserId: String): String {
        return when (currentUserId) {
            doctorId -> patientName
            patientId -> doctorName
            else -> "Unknown User"
        }
    }
}

data class UserData(
    val uid: String,
    val firstname: String,
    val surname: String,
    val role: String,
    val DoctorId: String? = null  // Note: This matches your Firestore field name
) {
    val fullName: String
        get() = "$firstname $surname".trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return
    val context = LocalContext.current

    var chatList by remember { mutableStateOf<List<ChatEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var userRole by remember { mutableStateOf("patient") }
    //var modifier = Modifier.fillMaxSize()
    Box(modifier = Modifier.fillMaxSize()) {
    LaunchedEffect(Unit) {
        try {
            // First get current user's role
            val currentUserDoc = db.collection("users").document(currentUserId).get().await()
            userRole = currentUserDoc.getString("role") ?: "patient"

            // Then fetch chats with names
            chatList = fetchChatsWithNames(db, currentUserId, userRole)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("My Chats") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (userRole == "patient") {
                            navController.navigate("createChatPatient")
                        } else {
                            navController.navigate("createChatDoctor")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "New chat")
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (chatList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No chats available", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(chatList) { chat ->
                        ChatListItem(
                            chat = chat,
                            currentUserId = currentUserId,
                            onChatClick = { navController.navigate("chatDetail/${chat.chatId}") },
                            onDeleteClick = {
                                // Handle delete
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatEntry,
    currentUserId: String,
    onChatClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otherUserName = remember(chat, currentUserId) {
        chat.getOtherUserName(currentUserId)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        onClick = onChatClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    otherUserName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Chat ID: ${chat.chatId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            deleteChat(db = FirebaseFirestore.getInstance(), chatId = chat.chatId)
                            onDeleteClick()
                            Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete chat",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

suspend fun deleteChat(db: FirebaseFirestore, chatId: String) {
    db.collection("chats").document(chatId).delete().await()
}

suspend fun fetchChatsWithNames(
    db: FirebaseFirestore,
    currentUserId: String,
    userRole: String
): List<ChatEntry> {
    val field = if (userRole == "patient") "patientId" else "doctorId"
    val chatsSnapshot = db.collection("chats")
        .whereEqualTo(field, currentUserId)
        .get()
        .await()

    return chatsSnapshot.documents.mapNotNull { doc ->
        try {
            // Get raw IDs from chat document
            val chatDoctorId = doc.getString("doctorId") ?: ""
            val chatPatientId = doc.getString("patientId") ?: ""

            // Fetch user data - handling both doctor and patient cases
            val (doctorData, patientData) = coroutineScope {
                val doctorDeferred = async {
                    if (userRole == "doctor") {
                        // For doctor's view, we're the doctor - get our own data
                        fetchUserData(db, currentUserId)
                    } else {
                        // For patient's view, get doctor's data using the chat's doctorId (which is uid)
                        fetchUserData(db, chatDoctorId)
                    }
                }

                val patientDeferred = async {
                    if (userRole == "patient") {
                        // For patient's view, we're the patient - get our own data
                        fetchUserData(db, currentUserId)
                    } else {
                        // For doctor's view, get patient's data using the chat's patientId
                        fetchUserData(db, chatPatientId)
                    }
                }

                Pair(doctorDeferred.await(), patientDeferred.await())
            }

            ChatEntry(
                chatId = doc.id,
                doctorId = chatDoctorId,
                patientId = chatPatientId,
                doctorName = doctorData?.fullName ?: "Unknown Doctor",
                patientName = patientData?.fullName ?: "Unknown Patient"
            )
        } catch (e: Exception) {
            Log.e("ChatDebug", "Error processing chat ${doc.id}", e)
            null
        }
    }
}

suspend fun fetchUserData(db: FirebaseFirestore, userId: String): UserData? {
    if (userId.isBlank()) return null

    return try {
        val userDoc = db.collection("users").document(userId).get().await()
        if (userDoc.exists()) {
            UserData(
                uid = userId,
                firstname = userDoc.getString("firstname") ?: "",
                surname = userDoc.getString("surname") ?: "",
                role = userDoc.getString("role") ?: "patient",
                DoctorId = userDoc.getString("DoctorId") // This might be null for patients
            )
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("ChatDebug", "Error fetching user $userId", e)
        null
    }
}
