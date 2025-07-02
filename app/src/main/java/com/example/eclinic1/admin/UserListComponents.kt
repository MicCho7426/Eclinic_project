package com.example.eclinic1.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eclinic1.firebase.FetchAllUsers
import com.example.eclinic1.firebase.FetchFcmToken
import androidx.lifecycle.viewmodel.compose.viewModel as viewModel

@Composable
fun UserListFab(
    fabExpanded: Boolean,
    onFabExpandChange: (Boolean) -> Unit,
    onCreateUserClick: () -> Unit,
    onSendMessageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = {
                        onCreateUserClick()
                        onFabExpandChange(false)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Create User")
                }
            }

            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = {
                        onSendMessageClick()
                        onFabExpandChange(false)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Message")
                }
            }

            // Główny FAB
            FloatingActionButton(
                onClick = { onFabExpandChange(!fabExpanded) }
            ) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Expand FAB"
                )
            }
        }
    }

}
@Composable
fun SendPushMessageDialog(
    viewModel: FetchAllUsers = viewModel(),
    users: List<SimpleUser>,
    onDismiss: () -> Unit,
    onSend: (recipientToken: String?, message: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedUser by remember { mutableStateOf<SimpleUser?>(null) }
    var messageText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val fetchFcmToken = FetchFcmToken()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Message") },
        text = {
            Column {
                Text("Select recipient:")
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(
                            selectedUser?.let { "${it.firstname} ${it.secondname}" } ?: "All Users"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Users") },
                            onClick = {
                                selectedUser = null
                                expanded = false
                            }
                        )
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text("${user.firstname} ${user.secondname}") },
                                onClick = {
                                    selectedUser = user
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        if (selectedUser != null) {
                            fetchFcmToken.getToken(selectedUser!!.uid) { token ->
                                viewModel.sendPushNotification(token, messageText) { success ->
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "Wysłano wiadomość",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Błąd wysyłania",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            // Wysyłanie do wszystkich
                            fetchFcmToken.getAllTokens { tokens ->
                                if (tokens.isNotEmpty()) {
                                    tokens.forEach { token ->
                                        viewModel.sendPushNotification(
                                            token,
                                            messageText
                                        ) { success ->
                                            if (success) {

                                                println("Wysłano do: $token")
                                            } else {
                                                println("Błąd wysyłania do: $token")
                                            }
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        "Wysłano do wszystkich",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Brak tokenów FCM", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }
                }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
