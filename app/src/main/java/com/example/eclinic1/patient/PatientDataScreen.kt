package com.example.eclinic1.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eclinic1.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDataScreen(
    navController: NavController,
    initialData: PatientData? = null
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Initialize state with existing data or empty
    var dob by remember { mutableStateOf(initialData?.dob ?: "") }
    var medicalHistory by remember { mutableStateOf(initialData?.medicalHistory ?: "") }
    var height by remember { mutableStateOf(initialData?.height ?: "") }
    var weight by remember { mutableStateOf(initialData?.weight ?: "") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedFileUri = uri }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Medical Information") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // All fields are optional - no validation errors
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Optional") }
            )

            OutlinedTextField(
                value = medicalHistory,
                onValueChange = { medicalHistory = it },
                label = { Text("Medical History") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = { Text("Optional") }
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Optional") }
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Optional") }
                )
            }

            // File upload section
            Button(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Medical File")
            }

            selectedFileUri?.let { uri ->
                Text(uri.lastPathSegment ?: "File selected")
                Button(onClick = { selectedFileUri = null }) {
                    Text("Remove File")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save button - saves only what was entered
            Button(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    val userId = auth.currentUser?.uid ?: run {
                        Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updates = mutableMapOf<String, Any?>().apply {
                        if (dob.isNotBlank()) put("dob", dob)
                        if (medicalHistory.isNotBlank()) put("medicalHistory", medicalHistory)
                        if (height.isNotBlank()) put("height", height)
                        if (weight.isNotBlank()) put("weight", weight)
                    }

                    // Validate at least one change
                    if (updates.isEmpty() && selectedFileUri == null) {
                        Toast.makeText(context, "No changes to save", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Save data
                    db.collection("patients").document(userId)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()

                            // Handle file upload if exists
                            selectedFileUri?.let { uri ->
                                uploadFileToFirebase(uri, userId, context, db) {
                                    navController.popBackStack()
                                }
                            } ?: navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator()
                else Text("Save Changes")
            }
        }
    }
}

private fun uploadFileToFirebase(
    uri: Uri,
    userId: String,
    context: Context,
    db: FirebaseFirestore,
    onComplete: () -> Unit
) {
    val filename = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
    val ref = FirebaseStorage.getInstance()
        .reference
        .child("patient_files/$userId/$filename")

    ref.putFile(uri)
        .addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                db.collection("patients").document(userId)
                    .update("uploadedFiles", FieldValue.arrayUnion(downloadUrl.toString()))
                    .addOnCompleteListener { onComplete() }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete()
        }
}