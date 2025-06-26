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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(chatId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    val currentUserId = auth.currentUser?.uid ?: return

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isUploading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isUploading = true
            val filename = it.lastPathSegment ?: "file_${System.currentTimeMillis()}"
            val storageRef = storage.reference.child("chat_files/$chatId/$filename")

            storageRef.putFile(it)
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
                        isUploading = false
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
        }
    }

    // Fetch messages
    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) }
                    ?.let { messages = it }
            }
    }

    // Auto-scroll
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chat", style = MaterialTheme.typography.titleLarge) },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    ChatBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Input Area
            MessageInputSection(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
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
                    }
                },
                onAttachFile = { filePickerLauncher.launch("*/*") },
                isUploading = isUploading,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isCurrentUser: Boolean, modifier: Modifier = Modifier) {
    val bubbleColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = when {
                isCurrentUser -> MaterialTheme.shapes.medium.copy(
                    topStart = MaterialTheme.shapes.medium.topEnd,
                    topEnd = MaterialTheme.shapes.medium.topEnd,
                    bottomStart = MaterialTheme.shapes.medium.topEnd,
                    bottomEnd = MaterialTheme.shapes.medium.topStart
                )
                else -> MaterialTheme.shapes.medium.copy(
                    topStart = MaterialTheme.shapes.medium.topEnd,
                    topEnd = MaterialTheme.shapes.medium.topEnd,
                    bottomStart = MaterialTheme.shapes.medium.topStart,
                    bottomEnd = MaterialTheme.shapes.medium.topEnd
                )
            },
            color = bubbleColor,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    isImageUrl(message.text) -> {
                        Text("📷 Image", color = textColor)
                    }
                    isFileUrl(message.text) -> {
                        Text("📎 File", color = textColor)
                    }
                    else -> {
                        Text(message.text, color = textColor)
                    }
                }
                Text(
                    text = message.timestamp.toDate().formatTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun MessageInputSection(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        IconButton(
            onClick = onAttachFile,
            enabled = !isUploading
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attach file",
                tint = if (isUploading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            ),
            trailingIcon = {
                if (messageText.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = false,
            maxLines = 3
        )
    }
}

fun isImageUrl(text: String): Boolean {
    return text.startsWith("http") &&
            (text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".jpeg"))
}

fun isFileUrl(text: String): Boolean {
    return text.startsWith("http") && !isImageUrl(text)
}

fun Date.formatTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(this)
}