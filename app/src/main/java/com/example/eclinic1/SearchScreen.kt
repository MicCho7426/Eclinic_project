package com.example.eclinic1

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
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class AppointmentSlot(
    val doctorId: String = "",
    val doctorName: String = "",
    val specialization: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val docRefPath: String = ""
)

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

    var selectedSpecialization by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var availableSlots by remember { mutableStateOf(listOf<AppointmentSlot>()) }
    var showDialogForSlot by remember { mutableStateOf<AppointmentSlot?>(null) }

    val specializationExpanded = remember { mutableStateOf(false) }
    val cal = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(Modifier.padding(16.dp)) {
        // Specialization Dropdown
        ExposedDropdownMenuBox(
            expanded = specializationExpanded.value,
            onExpandedChange = { specializationExpanded.value = !specializationExpanded.value }
        ) {
            OutlinedTextField(
                value = selectedSpecialization,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select specialization") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .clickable { specializationExpanded.value = true }
            )
            ExposedDropdownMenu(
                expanded = specializationExpanded.value,
                onDismissRequest = { specializationExpanded.value = false }
            ) {
                SPECIALIZATIONS.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec) },
                        onClick = {
                            selectedSpecialization = spec
                            specializationExpanded.value = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Picker
        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            cal.set(year, month, day)
                            selectedDate = formatter.format(cal.time)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Button
        Button(onClick = {
            val today = formatter.parse(formatter.format(Date()))!!
            val chosen = formatter.parse(selectedDate)!!
            if (chosen.before(today)) {
                Toast.makeText(context, "Date must be today or later", Toast.LENGTH_SHORT).show()
                return@Button
            }

            db.collection("users")
                .whereEqualTo("role", "doctor")
                .whereEqualTo("specialization", selectedSpecialization)
                .get()
                .addOnSuccessListener { doctors ->
                    val allSlots = mutableListOf<AppointmentSlot>()
                    for (doc in doctors) {
                        val docId = doc.id
                        val docName = doc.getString("name") ?: ""
                        val spec = doc.getString("specialization") ?: ""

                        db.collection("schedules").document(docId).collection(selectedDate)
                            .whereEqualTo("isBooked", false)
                            .get()
                            .addOnSuccessListener { slots ->
                                for (slot in slots) {
                                    allSlots.add(
                                        AppointmentSlot(
                                            doctorId = docId,
                                            doctorName = docName,
                                            specialization = spec,
                                            date = selectedDate,
                                            startTime = slot.getString("startTime") ?: "",
                                            endTime = slot.getString("endTime") ?: "",
                                            docRefPath = slot.reference.path
                                        )
                                    )
                                }
                                availableSlots = allSlots
                            }
                    }
                }
        }) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        if (availableSlots.isEmpty()) {
            Text("No available appointments.")
        } else {
            LazyColumn {
                items(availableSlots) { slot ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { showDialogForSlot = slot }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Time: ${slot.startTime} - ${slot.endTime}")
                            Text("Doctor: ${slot.doctorName}")
                            Text("Specialization: ${slot.specialization}")
                        }
                    }
                }
            }
        }

        // Dialog
        showDialogForSlot?.let { slot ->
            AlertDialog(
                onDismissRequest = { showDialogForSlot = null },
                title = { Text("Confirm Appointment") },
                text = {
                    Text("Do you want to book an appointment with ${slot.doctorName} (${slot.specialization}) at ${slot.startTime}?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val meetingId = UUID.randomUUID().toString()
                        val meeting = mapOf(
                            "id" to meetingId,
                            "doctorId" to slot.doctorId,
                            "patientId" to userId,
                            "date" to slot.date,
                            "startTime" to slot.startTime,
                            "endTime" to slot.endTime,
                            "status" to "booked"
                        )
                        db.collection("meetings").document(meetingId).set(meeting)
                        db.document(slot.docRefPath).update("isBooked", true)
                        Toast.makeText(context, "Appointment confirmed", Toast.LENGTH_SHORT).show()
                        showDialogForSlot = null
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialogForSlot = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
