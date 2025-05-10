/*@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.eclinic1

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale


// For DatePicker

// For TimePicker


// For LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookAppointmentScreen(
    userId: String,
    onAppointmentCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val calendar = remember { Calendar.getInstance() }

    var patientName by remember { mutableStateOf("") }
    var doctorId by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Time picker state
    var showTimePicker by remember { mutableStateOf(false) }

    // Load patient name
    LaunchedEffect(Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                patientName = "${doc.getString("firstname")} ${doc.getString("surname")}"
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Book Appointment",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Doctor Selection
        DoctorDropdown(
            onDoctorSelected = { id, name ->
                doctorId = id
                doctorName = name
            }
        )

        // Date Selection
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.formatDate() ?: "Select Date")
        }

        // Time Selection
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedTime?.formatTime() ?: "Select Time")
        }

        // Book Button
        Button(
            onClick = {
                if (validateInputs(doctorId, selectedDate, selectedTime)) {
                    isLoading = true
                    createAppointment(
                        patientId = userId,
                        patientName = patientName,
                        doctorId = doctorId,
                        doctorName = doctorName,
                        date = selectedDate!!.formatDate(),
                        time = selectedTime!!.formatTime(),
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Appointment booked!", Toast.LENGTH_SHORT)
                                .show()
                            onAppointmentCreated()
                        },
                        onFailure = {
                            isLoading = false
                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG)
                                .show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Confirm Booking")
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    /* Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                showTimePicker = false
            },
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
    }*/
}

// Extension functions for date/time formatting
fun Date.formatDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
}

@RequiresApi(Build.VERSION_CODES.O)
fun LocalTime.formatTime(): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

fun validateInputs(
    doctorId: String,
    date: Date?,
    time: LocalTime?
): Boolean {
    return doctorId.isNotBlank() && date != null && time != null
}


@Composable
fun DoctorDropdown(
    onDoctorSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val db = FirebaseFirestore.getInstance()
    var doctorList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var filteredDoctors by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDoctor by remember { mutableStateOf<Pair<String, String>?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "doctor")
            .get()
            .addOnSuccessListener { snapshot ->
                doctorList = snapshot.documents.map {
                    it.id to "${it.getString("firstname") ?: ""} ${it.getString("surname") ?: ""}"
                }
                filteredDoctors = doctorList
            }
    }

    LaunchedEffect(searchQuery) {
        filteredDoctors = if (searchQuery.isBlank()) {
            doctorList
        } else {
            doctorList.filter { (_, name) ->
                name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Select Doctor",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Doctors") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedDoctor?.second ?: "Choose a Doctor",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (filteredDoctors.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No doctors found") },
                    onClick = { expanded = false }
                )
            } else {
                filteredDoctors.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedDoctor = id to name
                            expanded = false
                            onDoctorSelected(id, name)
                        }
                    )
                }
            }
        }
    }
}
fun createAppointment(
    patientId: String,
    patientName: String,
    doctorId: String,
    doctorName: String,
    date: String,
    time: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val appointment = hashMapOf(
        "patientId" to patientId,
        "doctorId" to doctorId,
        "patientName" to patientName,
        "doctorName" to doctorName,
        "date" to date,
        "time" to time,
        "status" to "pending", // or "confirmed" based on your workflow
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    db.collection("appointments")
        .add(appointment)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}
*/