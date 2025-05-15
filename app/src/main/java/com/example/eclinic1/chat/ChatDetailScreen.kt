package com.example.eclinic1.chat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

@Composable
fun ChatDetailScreen(chatId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val currentUserId = auth.currentUser?.uid ?: return

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    // Fetch messages
    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val messageList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()
                messages = messageList
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // Back button
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("← Back to chats")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Surface(
                    color = if (msg.senderId == currentUserId)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text(
                        text = msg.text,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (messageText.isNotBlank()) {
                    val message = ChatMessage(
                        senderId = currentUserId,
                        text = messageText,
                        timestamp = Timestamp.now()
                    )
                    db.collection("chats").document(chatId)
                        .collection("messages")
                        .add(message)
                        .addOnSuccessListener { messageText = "" }
                        .addOnFailureListener {
                            Toast.makeText(context, "Send failed", Toast.LENGTH_SHORT).show()
                        }
                }
            }) {
                Text("Send")
            }
        }
    }
}
