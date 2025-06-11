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

data class AppointmentSlot(
    val doctorId: String,
    val doctorName: String,
    val specialization: List<String>,
    val date: String,
    val startTime: String,
    val endTime: String,
    val scheduleDocId: String
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
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(formatter.format(calendar.time)) }
    var availableSlots by remember { mutableStateOf(listOf<AppointmentSlot>()) }
    var confirmationSlot by remember { mutableStateOf<AppointmentSlot?>(null) }

    val today = formatter.format(Date())

    fun fetch() {
        if (specialization.isNotBlank()) {
            fetchAvailableAppointments(specialization, selectedDate) {
                availableSlots = it
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = specialization,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select specialization") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SPECIALIZATIONS.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = {
                        specialization = it
                        expanded = false
                        fetch()
                    })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("<", modifier = Modifier
                .clickable(enabled = selectedDate > today) {
                    calendar.time = formatter.parse(selectedDate)!!
                    calendar.add(Calendar.DATE, -1)
                    val newDate = formatter.format(calendar.time)
                    if (newDate >= today) {
                        selectedDate = newDate
                        fetch()
                    }
                }
                .padding(8.dp))

            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val todayDate = Calendar.getInstance()
                        val picker = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = Calendar.getInstance()
                                picked.set(y, m, d)
                                val newDate = formatter.format(picked.time)
                                if (newDate >= today) {
                                    selectedDate = newDate
                                    fetch()
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        picker.datePicker.minDate = todayDate.timeInMillis
                        picker.show()
                    }
            )

            Text(">", modifier = Modifier
                .clickable {
                    calendar.time = formatter.parse(selectedDate)!!
                    calendar.add(Calendar.DATE, 1)
                    selectedDate = formatter.format(calendar.time)
                    fetch()
                }
                .padding(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            if (availableSlots.isEmpty()) {
                item { Text("No available appointments.") }
            } else {
                items(availableSlots) { slot ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmationSlot = slot }
                            .padding(12.dp)
                    ) {
                        Text("Date: ${slot.date}")
                        Text("Doctor: ${slot.doctorName}")
                        Text("Time: ${slot.startTime} - ${slot.endTime}")
                        Divider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }

    confirmationSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { confirmationSlot = null },
            title = { Text("Confirm Appointment") },
            text = {
                Column {
                    Text("Date: ${slot.date}")
                    Text("Doctor: ${slot.doctorName}")
                    Text("Time: ${slot.startTime} - ${slot.endTime}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    bookAppointment(slot, userId, context)
                    confirmationSlot = null
                    availableSlots = emptyList()
                    specialization = ""
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationSlot = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun fetchAvailableAppointments(
    specialization: String,
    date: String,
    onResult: (List<AppointmentSlot>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val now = Calendar.getInstance().time
    val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)

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
                            val startTime = slot["startTime"] as String
                            val slotTime = formatter.parse("$date $startTime")
                            if (slotTime != null && (date > currentDateString || slotTime.after(now))) {
                                result.add(
                                    AppointmentSlot(
                                        doctorId = doctorId,
                                        doctorName = doctorName,
                                        specialization = specList,
                                        date = date,
                                        startTime = startTime,
                                        endTime = slot["endTime"] as String,
                                        scheduleDocId = slot.id
                                    )
                                )
                            }
                        }
                        completed++
                        if (completed == matchingDoctors.size) {
                            result.sortWith(compareBy({ it.date }, { it.startTime }))
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
