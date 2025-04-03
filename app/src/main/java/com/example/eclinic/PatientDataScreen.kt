package com.example.eclinic

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@Composable
fun PatientDataScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: return

    var dob by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Fetch existing user data
    LaunchedEffect(userId) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                document?.let {
                    dob = it.getString("dob") ?: ""
                    height = it.getDouble("height")?.toString() ?: ""
                    weight = it.getDouble("weight")?.toString() ?: ""
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error loading data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = { navController.navigateUp() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
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

        Button(onClick = { launcher.launch("*/*") }) {
            Text("Upload Medical File")
        }

        selectedFileUri?.let { uri ->
            Text("Selected file: ${uri.lastPathSegment}")
            Button(onClick = { selectedFileUri = null }) {
                Text("Remove File")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val updates = mutableMapOf<String, Any>()

                if (dob.isNotBlank()) updates["dob"] = dob
                height.toDoubleOrNull()?.takeIf { it > 0 }?.let { updates["height"] = it }
                weight.toDoubleOrNull()?.takeIf { it > 0 }?.let { updates["weight"] = it }

                if (updates.isEmpty()) {
                    Toast.makeText(context, "Please fill at least one valid field", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                db.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data saved!", Toast.LENGTH_SHORT).show()
                        navController.navigate("main") // lub "profile" jeśli masz taką trasę
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Save failed: ${it.message}", Toast.LENGTH_LONG).show()
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
                db.collection("users").document(userId)
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
