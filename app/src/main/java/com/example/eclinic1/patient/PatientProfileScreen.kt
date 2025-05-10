package com.example.eclinic1.patient

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eclinic.R
import com.google.firebase.auth.FirebaseAuth
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: PatientProfileViewModel = viewModel()
) {
    val patientData by viewModel.patientData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Show error toast
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    IconButton(onClick = { viewModel.loadPatientData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.loadPatientData() },
            modifier = Modifier.padding(padding)
        ) {
            if (isLoading && patientData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp) // Add padding around content
                ) {
                    item { PatientInfoSection(patientData) }
                    item { MedicalDataSection(patientData, context) }

                    // Add action buttons as the last items
                    item {
                        Spacer(modifier = Modifier.height(24.dp)) // Add space before buttons
                        ActionButtons(
                            navController = navController,
                            onLogoutClicked = { showLogoutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun PatientInfoSection(patientData: PatientData?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleLarge
            )
            Divider()

            patientData?.let { data ->
                // Display user information from Firestore
                InfoRow(label = "Name", value = data.fullName)
                InfoRow(label = "Email", value = data.email ?: "Not provided")
                InfoRow(label = "User ID", value = data.userId.take(8))
            } ?: Text("No personal data available")
        }
    }
}

@Composable
private fun MedicalDataSection(patientData: PatientData?, context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Medical Information",
                style = MaterialTheme.typography.titleLarge
            )
            Divider()

            patientData?.let { data ->
                InfoRow(label = "Date of Birth", value = data.dob ?: "Not specified")
                InfoRow(label = "Medical History", value = data.medicalHistory ?: "None")
                InfoRow(label = "Height", value = data.height?.let { "$it cm" } ?: "Not specified")
                InfoRow(label = "Weight", value = data.weight?.let { "$it kg" } ?: "Not specified")

                if (data.uploadedFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Medical Documents:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    data.uploadedFiles.forEach { fileUrl ->
                        Text(
                            text = fileUrl.takeLast(40),
                            color = Color.Blue,
                            modifier = Modifier
                                .clickable {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(fileUrl)
                                            )
                                        )
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Cannot open file",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            } ?: Text("No medical data available")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionButtons(
    navController: NavController,
    onLogoutClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { navController.navigate("patientData") },
            modifier = Modifier.weight(1f)
        ) {
            Text("Edit Medical Data")
        }

        Button(
            onClick = onLogoutClicked,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Log Out")
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Logout") },
        text = { Text("Are you sure you want to sign out?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Log Out", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
private fun FullScreenLoader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}