package com.example.eclinic1.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

@Composable
fun ChatDetailScreen(chatId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    val currentUserId = auth.currentUser?.uid ?: return

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = result.data?.data
            fileUri?.let { uri ->
                val filename = uri.lastPathSegment ?: "uploaded_file"
                val storageRef = storage.reference.child("chat_files/$chatId/$filename")

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            val message = ChatMessage(
                                senderId = currentUserId,
                                text = downloadUrl.toString(),
                                timestamp = Timestamp.now()
                            )
                            db.collection("chats").document(chatId)
                                .collection("messages")
                                .add(message)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "File upload failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

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

        Button(onClick = { navController.popBackStack() }) {
            Text("← Back to chats")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUrl = msg.text.startsWith("http")
                Surface(
                    color = if (msg.senderId == currentUserId)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (isUrl && (msg.text.endsWith(".jpg") || msg.text.endsWith(".png"))) {
                            Text(
                                text = "📷 Photo (tap to view)",
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.text))
                                    context.startActivity(intent)
                                },
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else if (isUrl) {
                            Text(
                                text = "📎 File (tap to download)",
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.text))
                                    context.startActivity(intent)
                                },
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = msg.text,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .shadow(elevation = 4.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )

                Spacer(modifier = Modifier.width(4.dp))

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

                Spacer(modifier = Modifier.width(4.dp))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(Intent.createChooser(intent, "Select file"))
                }) {
                    Text("📎")
                }
            }
        }
    }
}