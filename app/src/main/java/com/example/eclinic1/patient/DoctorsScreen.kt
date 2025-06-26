package com.example.eclinic1.patient

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var doctors by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(true) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { userDocs ->
                val results = mutableListOf<Map<String, Any>>()

                userDocs.documents.forEach { userDoc ->
                    val userId = userDoc.id
                    val baseData = userDoc.data ?: return@forEach

                    db.collection("doctorProfiles").document(userId)
                        .get()
                        .addOnSuccessListener { profileDoc ->
                            val profileData = profileDoc.data ?: emptyMap()

                            db.collection("users").document(userId)
                                .collection("specializations")
                                .get()
                                .addOnSuccessListener { specDocs ->
                                    val specializations = specDocs.mapNotNull { it.getString("name") }
                                    val fullData = baseData + profileData + mapOf(
                                        "uid" to userId,
                                        "specializations" to specializations
                                    )
                                    results.add(fullData)
                                    doctors = results.sortedBy { it["surname"].toString() }
                                }
                        }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Doctors") })
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            items(doctors) { doctor ->
                DoctorCard(doctor)
            }
        }
    }
}

@Composable
fun DoctorCard(doctor: Map<String, Any>) {
    val name = "Dr. ${doctor["firstname"]} ${doctor["surname"]}"

    val specialization = when (val raw = doctor["Specialization"]) {
        is List<*> -> raw.filterIsInstance<String>().joinToString(", ")
        is String -> raw
        else -> "Unknown"
    }

    val note = doctor["note"]?.toString() ?: ""
    val avatarUrl = doctor["avatarUrl"]?.toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6EEFF)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!avatarUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )

            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Specializations: $specialization", fontSize = 14.sp)
                if (note.isNotEmpty()) {
                    Text("📝 $note", fontSize = 14.sp, color = Color.DarkGray)
                }
            }
        }
    }
}