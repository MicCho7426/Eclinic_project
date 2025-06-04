package com.example.eclinic1.doctor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DoctorScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var selectedDate by remember { mutableStateOf(Date()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var hasSchedule by remember { mutableStateOf(false) }

    fun loadSchedule(date: String) {
        db.collection("schedules").document(userId)
            .collection(date)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    hasSchedule = false
                    startTime = ""
                    endTime = ""
                } else {
                    val sorted = result.sortedBy { it["startTime"] as String }
                    startTime = sorted.first()["startTime"] as String
                    endTime = sorted.last()["endTime"] as String
                    hasSchedule = true
                }
            }
    }

    LaunchedEffect(selectedDate) {
        loadSchedule(formatter.format(selectedDate))
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Doctor Schedule", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(context, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = cal.time
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }) {
            Text("Date: ${formatter.format(selectedDate)}")
        }

        Spacer(Modifier.height(12.dp))

        TimeInput("Start time", startTime) { startTime = it }
        Spacer(Modifier.height(8.dp))
        TimeInput("End time", endTime) { endTime = it }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val dateKey = formatter.format(selectedDate)
            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                val slots = generateTimeSlots(startTime, endTime)
                val collectionRef = db.collection("schedules").document(userId).collection(dateKey)

                // Delete existing slots first
                collectionRef.get().addOnSuccessListener { existing ->
                    val batch = db.batch()
                    existing.forEach { batch.delete(it.reference) }

                    // Add new slots
                    slots.forEach { (start, end) ->
                        val doc = collectionRef.document()
                        val data = hashMapOf(
                            "startTime" to start,
                            "endTime" to end,
                            "isBooked" to false
                        )
                        batch.set(doc, data)
                    }

                    batch.commit().addOnSuccessListener {
                        Toast.makeText(context, "Schedule saved", Toast.LENGTH_SHORT).show()
                        hasSchedule = true
                    }
                }
            }
        }) {
            Text("Save Schedule")
        }

        Spacer(Modifier.height(24.dp))

        if (hasSchedule) {
            Text(
                text = "Current schedule for ${formatter.format(selectedDate)}:",
                style = MaterialTheme.typography.titleMedium
            )
            Text("From $startTime to $endTime")
        } else {
            Text("No schedule for this date.")
        }
    }
}

@Composable
fun TimeInput(label: String, value: String, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    val cal = Calendar.getInstance()

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimeSelected(String.format("%02d:%02d", hour, minute))
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
            }
        }
    )
}

fun generateTimeSlots(start: String, end: String): List<Pair<String, String>> {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTime = fmt.parse(start)!!
    val endTime = fmt.parse(end)!!
    val slots = mutableListOf<Pair<String, String>>()
    var current = startTime

    while (current.before(endTime)) {
        val next = Date(current.time + 15 * 60 * 1000)
        if (next <= endTime) {
            slots.add(Pair(fmt.format(current), fmt.format(next)))
        }
        current = next
    }

    return slots
}
