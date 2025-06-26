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

            // BMI display when both fields have values
            if (height.isNotBlank() && weight.isNotBlank()) {
                val bmiValue = try {
                    val h = height.toDouble() / 100
                    val w = weight.toDouble()
                    w / (h * h)
                } catch (e: Exception) { null }

                bmiValue?.let {
                    Text("BMI: ${"%.1f".format(it)}",
                        color = when {
                            it < 18.5 -> Color.Blue
                            it < 25 -> Color.Green
                            it < 30 -> Color.Yellow
                            else -> Color.Red
                        },
                        modifier = Modifier.fillMaxWidth())
                }
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
                onClick = {
                    val userId = auth.currentUser?.uid ?: return@Button
                    val updates = mutableMapOf<String, Any?>()

                    if (dob.isNotBlank()) updates["dob"] = dob
                    if (medicalHistory.isNotBlank()) updates["medicalHistory"] = medicalHistory
                    if (height.isNotBlank()) updates["height"] = height
                    if (weight.isNotBlank()) updates["weight"] = weight

                    db.collection("patients").document(userId)
                        .update(updates.filterValues { it != null } as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }

                    selectedFileUri?.let { uri ->
                        uploadFileToFirebase(uri, userId, context, db)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}

private fun uploadFileToFirebase(uri: Uri, userId: String, context: Context, db: FirebaseFirestore) {
    val filename = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
    val ref = FirebaseStorage.getInstance()
        .reference
        .child("patient_files/$userId/$filename")

    ref.putFile(uri)
        .addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                db.collection("patients").document(userId)
                    .update("uploadedFiles", FieldValue.arrayUnion(downloadUrl.toString()))
            }
        }
}