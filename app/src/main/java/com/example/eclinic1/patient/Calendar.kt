package com.example.eclinic1.patient

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun Calendar(
    viewModel: CalendarViewModel = viewModel()
) {
    val state = viewModel.state.value

    Scaffold(
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Weekday selector
                DateSelector(
                    dates = state.dates,
                    selectedDate = state.selectedDate,
                    onDateSelected = viewModel::selectDate
                )

                // Time grid with appointments
                TimeGridWithAppointments(
                    appointments = state.appointments,
                    timeSlots = generateTimeSlots(),
                    isDoctor = state.userRole == "doctor",
                    selectedDate = state.selectedDate
                )
            }
        }
    )
}

@Composable
private fun TimeGridWithAppointments(
    appointments: List<Appointment>,
    timeSlots: List<LocalTime>,
    isDoctor: Boolean,
    selectedDate: LocalDate  // Add selectedDate parameter
) {
    // Filter appointments for the selected date
    val filteredAppointments = remember(appointments, selectedDate) {
        appointments.filter { appt ->
            val appointmentDate = appt.date.toDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            appointmentDate == selectedDate
        }
    }

    // Group appointments by their start time hour
    val appointmentsByHour = remember(filteredAppointments) {
        filteredAppointments.groupBy { appt ->
            LocalTime.parse(appt.startTime).hour  // Group by hour
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        timeSlots.forEach { timeSlot ->
            // Get appointments for this hour
            val hourAppointments = appointmentsByHour[timeSlot.hour] ?: emptyList()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time label
                Text(
                    text = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm")),
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Divider line
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                // Appointments for this time slot
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hourAppointments.forEach { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            isDoctor = isDoctor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    isDoctor: Boolean
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (appointment.status.lowercase()) {
                "scheduled" -> MaterialTheme.colorScheme.primaryContainer
                "completed" -> MaterialTheme.colorScheme.secondaryContainer
                "cancelled" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = if (isDoctor) appointment.patientName else appointment.doctorName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${appointment.startTime} - ${appointment.endTime}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun generateTimeSlots(): List<LocalTime> {
    return (7..19).map { hour -> LocalTime.of(hour, 0) }
}

@Composable
private fun ErrorState(
    error: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error: ${error ?: "Unknown error"}",
            color = MaterialTheme.colorScheme.error
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No appointments found")
    }
}


@Composable
private fun LoadingState() {
    Log.d("CalendarDebug", "Loading state")
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@Composable
private fun AppointmentList(
    appointments: List<Appointment>,
    isDoctor: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count = appointments.size) { index ->
            val appointment = appointments[index]
            AppointmentCard(
                appointment = appointment,
                isDoctor = isDoctor
            )
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun DateSelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            DateItem(
                date = date,
                isSelected = date == selectedDate,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DateItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }
    val dayNumberFormatter = remember { DateTimeFormatter.ofPattern("d") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM") }

    Card(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(dayFormatter).uppercase(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = date.format(dayNumberFormatter),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = date.format(monthFormatter).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview
@Composable
fun CalendarPreview() {
    MaterialTheme {
        Calendar()
    }
}