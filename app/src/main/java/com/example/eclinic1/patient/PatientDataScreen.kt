package com.example.eclinic1.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@Composable
fun PatientDataScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var dob by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
    }
    IconButton(onClick = { navController.navigateUp() }) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    )
    {
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = medicalHistory,
            onValueChange = { medicalHistory = it },
            label = { Text("Medical History") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedFileUri = uri // Store selected file
        }

        Button(onClick = { launcher.launch("*/*") }) {
            Text("Upload Medical File")
        }

// Show selected file details if a file is chosen
        selectedFileUri?.let { uri ->
            Text("Selected file: ${uri.lastPathSegment}")
            Button(onClick = { selectedFileUri = null }) {
                Text("Remove File")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val userId = auth.currentUser?.uid ?: return@Button
                val patientData = hashMapOf(
                    "dob" to dob,
                    "medicalHistory" to medicalHistory,
                    "height" to height,
                    "weight" to weight
                )

                db.collection("patients").document(userId)
                    .set(patientData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data Saved!", Toast.LENGTH_SHORT).show()
                        navController.navigate("profile")
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                    }

                selectedFileUri?.let { uri ->
                    uploadFileToFirebase(uri, userId, context, db)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

private fun uploadFileToFirebase(uri: Uri, userId: String, context: Context, db: FirebaseFirestore) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("patient_files/$userId/${uri.lastPathSegment}")

    fileRef.putFile(uri)
        .addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                db.collection("patients").document(userId)
                    .update("uploadedFiles", FieldValue.arrayUnion(downloadUrl.toString()))
                    .addOnSuccessListener {
                        Toast.makeText(context, "File Uploaded!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "File upload failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
}
