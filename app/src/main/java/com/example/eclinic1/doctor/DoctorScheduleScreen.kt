package com.example.eclinic1.doctor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var weekStart by remember { mutableStateOf(getStartOfWeek(Date())) }
    val weekDates = remember(weekStart) {
        List(7) { offset ->
            Calendar.getInstance().apply {
                time = weekStart
                add(Calendar.DAY_OF_MONTH, offset)
            }.time
        }
    }

    val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val times = remember { mutableStateMapOf<String, Pair<String, String>>() }

    LaunchedEffect(weekStart) {
        times.clear()
        weekDates.forEach { date ->
            val dateKey = formatter.format(date)
            db.collection("schedules").document(userId).collection(dateKey)
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val sorted = docs.sortedBy { it["startTime"] as String }
                        val start = sorted.first()["startTime"] as String
                        val end = sorted.last()["endTime"] as String
                        times[dateKey] = start to end
                    }
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Weekly Schedule") }) }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.padding(16.dp).fillMaxSize()
        ) {
            item {
                Button(onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d)
                        weekStart = getStartOfWeek(cal.time)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) {
                    Text("Select week: ${formatter.format(weekStart)}")
                }
                Spacer(Modifier.height(12.dp))
            }

            itemsIndexed(weekDates) { i, date ->
                val key = formatter.format(date)
                val label = dayLabels.getOrElse(i) { "Day $i" }
                val start = times[key]?.first ?: ""
                val end = times[key]?.second ?: ""

                Text(label)
                TimeInput("Start time", start) { selected ->
                    times[key] = selected to (times[key]?.second ?: "")
                }
                TimeInput("End time", end) { selected ->
                    times[key] = (times[key]?.first ?: "") to selected
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                Button(onClick = {
                    times.forEach { (dateKey, pair) ->
                        val (start, end) = pair
                        if (start.isNotBlank() && end.isNotBlank()) {
                            val slots = generateTimeSlots(start, end)
                            val collectionRef = db.collection("schedules").document(userId).collection(dateKey)

                            collectionRef.get().addOnSuccessListener { existing ->
                                val batch = db.batch() // osobny batch w każdej iteracji
                                existing.forEach { batch.delete(it.reference) }

                                slots.forEach { (slotStart, slotEnd) ->
                                    val doc = collectionRef.document()
                                    batch.set(doc, mapOf(
                                        "startTime" to slotStart,
                                        "endTime" to slotEnd,
                                        "isBooked" to false
                                    ))
                                }

                                batch.commit().addOnSuccessListener {
                                    Toast.makeText(context, "Saved schedule for $dateKey", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Error for $dateKey: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }) {
                    Text("Save Weekly Schedule")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = {
                    val sourceStart = getStartOfWeek(weekStart)
                    val targetStart = Calendar.getInstance().apply {
                        time = sourceStart
                        add(Calendar.DAY_OF_MONTH, 7)
                    }.time
                    val sourceDates = List(7) { offset ->
                        Calendar.getInstance().apply {
                            time = sourceStart
                            add(Calendar.DAY_OF_MONTH, offset)
                        }.time
                    }
                    val targetDates = List(7) { offset ->
                        Calendar.getInstance().apply {
                            time = targetStart
                            add(Calendar.DAY_OF_MONTH, offset)
                        }.time
                    }

                    sourceDates.zip(targetDates).forEach { (sourceDate, targetDate) ->
                        val sourceKey = formatter.format(sourceDate)
                        val targetKey = formatter.format(targetDate)
                        val sourceRef = db.collection("schedules").document(userId).collection(sourceKey)
                        val targetRef = db.collection("schedules").document(userId).collection(targetKey)

                        sourceRef.get().addOnSuccessListener { documents ->
                            val batch = db.batch()
                            documents.forEach { doc ->
                                val data = doc.data.toMutableMap()
                                data["isBooked"] = false
                                val newDoc = targetRef.document()
                                batch.set(newDoc, data)
                            }

                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Copied $sourceKey → $targetKey", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error copying $sourceKey: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                }) {
                    Text("Copy to Next Week")
                }
            }
        }
    }
}

fun getStartOfWeek(date: Date): Date {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return cal.time
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
            slots.add(fmt.format(current) to fmt.format(next))
        }
        current = next
    }

    return slots
}