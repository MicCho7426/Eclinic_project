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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.tasks.await
import android.util.Log


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

    val zoneId = ZoneId.of("Europe/Warsaw")
    var currentServerTime by remember { mutableStateOf<ZonedDateTime?>(null) }

    var selectedDate by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var availableSlots by remember { mutableStateOf(listOf<AppointmentSlot>()) }
    var confirmationSlot by remember { mutableStateOf<AppointmentSlot?>(null) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // fetch current time once
    LaunchedEffect(Unit) {
        try {
            val docRef = db.collection("serverTime").document("now")
            docRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
            val snapshot = docRef.get().await()
            val timestamp = snapshot.getTimestamp("timestamp")?.toDate()
            if (timestamp != null) {
                val serverNow = timestamp.toInstant().atZone(zoneId)
                currentServerTime = serverNow
                selectedDate = serverNow.toLocalDate().format(formatter)
            }
        } catch (e: Exception) {
            Log.e("SERVER_TIME", "Failed to get server time", e)
        }
    }

    fun fetch() {
        if (specialization.isNotBlank() && currentServerTime != null) {
            fetchAvailableAppointments(
                specialization,
                selectedDate,
                currentServerTime!!,
                onResult = { availableSlots = it }
            )
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
                .clickable {
                    val current = LocalDate.parse(selectedDate, formatter)
                    val previous = current.minusDays(1)
                    if (!previous.isBefore(currentServerTime?.toLocalDate())) {
                        selectedDate = previous.format(formatter)
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
                        val now = currentServerTime ?: return@clickable
                        val today = now.toLocalDate()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = LocalDate.of(y, m + 1, d)
                                if (!picked.isBefore(today)) {
                                    selectedDate = picked.format(formatter)
                                    fetch()
                                }
                            },
                            now.year,
                            now.monthValue - 1,
                            now.dayOfMonth
                        ).apply {
                            datePicker.minDate = Date.from(today.atStartOfDay(zoneId).toInstant()).time
                        }.show()
                    }
            )

            Text(">", modifier = Modifier
                .clickable {
                    val current = LocalDate.parse(selectedDate, formatter)
                    selectedDate = current.plusDays(1).format(formatter)
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
    currentServerTime: ZonedDateTime,
    onResult: (List<AppointmentSlot>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val zoneId = ZoneId.of("Europe/Warsaw")

    db.collection("users")
        .whereEqualTo("role", "doctor")
        .get()
        .addOnSuccessListener { doctorDocs ->
            val matchingDoctors = doctorDocs.filter {
                val specs = it["Specialization"] as? List<*>
                specs?.contains(specialization) == true
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
                            val slotDateTime = LocalDateTime.parse("$date $startTime", formatter)
                            val zonedSlot = slotDateTime.atZone(zoneId)

                            if (zonedSlot.isAfter(currentServerTime)) {
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
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val dateTime = LocalDateTime.parse("${slot.date} ${slot.startTime}", formatter)
    val timestamp = Timestamp(dateTime.toEpochSecond(ZoneOffset.UTC), 0)

    db.collection("users").document(patientId).get()
        .addOnSuccessListener { patientDoc ->
            val patientName = "${patientDoc["firstname"]} ${patientDoc["surname"]}"

            val meetingData = hashMapOf(
                "doctorId" to slot.doctorId,
                "doctorName" to slot.doctorName,
                "patientId" to patientId,
                "patientName" to patientName,
                "date" to timestamp,
                "startTime" to slot.startTime,
                "endTime" to slot.endTime,
                "status" to "scheduled",
                "note" to "",
                "createdAt" to FieldValue.serverTimestamp()
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
}