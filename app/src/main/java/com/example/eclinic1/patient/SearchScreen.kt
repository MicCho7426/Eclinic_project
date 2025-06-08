package com.example.eclinic1.patient

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

val SPECIALIZATIONS = listOf(
    "Cardiology", "Dermatology", "Endocrinology", "Gastroenterology",
    "Neurology", "Oncology", "Orthopedics", "Pediatrics",
    "Psychiatry", "Urology"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    var specialization by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf("") }
    var availableSlots by remember { mutableStateOf(listOf<AppointmentSlot>()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()

    Column(modifier = Modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = specialization,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select specialization") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SPECIALIZATIONS.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            specialization = it
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            label = { Text("Select date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            selectedDate = formatter.format(calendar.time)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
            readOnly = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            if (specialization.isNotBlank() && selectedDate.isNotBlank()) {
                fetchAvailableAppointments(specialization, selectedDate) {
                    availableSlots = it
                }
            }
        }) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            if (availableSlots.isEmpty()) {
                item { Text("No available appointments.") }
            } else {
                items(availableSlots) { slot ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                bookAppointment(slot, userId, context)
                            }
                            .padding(12.dp)
                    ) {
                        Text("Doctor: ${slot.doctorName}")
                        Text("Time: ${slot.startTime} - ${slot.endTime}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                    }
                }
            }
        }
    }
}

data class AppointmentSlot(
    val doctorId: String,
    val doctorName: String,
    val specialization: List<String>,
    val date: String,
    val startTime: String,
    val endTime: String,
    val scheduleDocId: String
)

fun fetchAvailableAppointments(
    specialization: String,
    date: String,
    onResult: (List<AppointmentSlot>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("users")
        .whereEqualTo("role", "doctor")
        .get()
        .addOnSuccessListener { doctorDocs ->
            val matchingDoctors = doctorDocs.filter {
                val specializations = it["Specialization"] as? List<*>
                specializations?.contains(specialization) == true
            }

            if (matchingDoctors.isEmpty()) {
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val result = mutableListOf<AppointmentSlot>()
            var completed = 0

            for (doc in matchingDoctors) {
                val doctorId = doc.id
                val doctorName = "${doc["firstname"]} ${doc["surname"]}"
                val specList = doc["Specialization"] as List<String>

                db.collection("schedules").document(doctorId).collection(date)
                    .whereEqualTo("isBooked", false)
                    .get()
                    .addOnSuccessListener { slots ->
                        for (slot in slots) {
                            result.add(
                                AppointmentSlot(
                                    doctorId = doctorId,
                                    doctorName = doctorName,
                                    specialization = specList,
                                    date = date,
                                    startTime = slot["startTime"] as String,
                                    endTime = slot["endTime"] as String,
                                    scheduleDocId = slot.id
                                )
                            )
                        }
                        completed++
                        if (completed == matchingDoctors.size) {
                            onResult(result)
                        }
                    }
            }
        }
}

fun bookAppointment(slot: AppointmentSlot, patientId: String, context: android.content.Context) {
    val db = FirebaseFirestore.getInstance()
    val meetingData = mapOf(
        "doctorId" to slot.doctorId,
        "doctorName" to slot.doctorName,
        "patientId" to patientId,
        "date" to slot.date,
        "startTime" to slot.startTime,
        "endTime" to slot.endTime,
        "status" to "scheduled",
        "note" to ""
    )

    db.collection("meetings")
        .add(meetingData)
        .addOnSuccessListener {
            db.collection("schedules")
                .document(slot.doctorId)
                .collection(slot.date)
                .document(slot.scheduleDocId)
                .update("isBooked", true)

            Toast.makeText(context, "Appointment booked!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
}

