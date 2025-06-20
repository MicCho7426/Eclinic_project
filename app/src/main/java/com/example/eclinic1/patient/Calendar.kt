package com.example.eclinic1.patient

import CalendarState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Timestamp
import java.util.Date

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
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Tutaj dodamy poszczególne komponenty
                DateSelector(
                    dates = state.dates, selectedDate = state.selectedDate,
                    onDateSelected = viewModel::selectDate,
                )
                TimeGrid(appointments = state.appointments)
            }
        }

    )


}

@Composable
fun DateSelector(
    dates: List<LocalDate>, selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()//skad ta funkcja
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            DateItem(date = date, isSelected = date == selectedDate,
                onClick={ onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DateItem(
    date: LocalDate, isSelected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }//co to za pattern
    val dayNumberFormatter = remember { DateTimeFormatter.ofPattern("d") }

    Card(//co sie tutaj dzieje co to card defaults
        onClick = onClick,
        modifier = modifier.size(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = date.format(dayFormatter).uppercase(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = date.format(dayNumberFormatter),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun TimeGrid(appointments: List<Appointment>, modifier: Modifier = Modifier) {//baza danych
    val hours = (7..19).toList() // Godziny przyjęć np. 7:00-19:00
    val appointmentsByHour = appointments
        .filter { it.date.toDate().hours in hours }
        .groupBy {
        it.date.toDate().hours }

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        items(hours.size) { hour ->
            val hour = hours[hour]
            TimeSlot(
                hour = hour,
                appointments = appointmentsByHour[hour]?: emptyList(),
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
        }
    }
}
@Composable
fun TimeSlot(hour: Int, appointments: List<Appointment>, modifier: Modifier) {
    val hours = (7..19).toList()
    val hour = hours
    Row(
        modifier = modifier
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${hour.forEach { hour-> println(hour) }}:00",
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            appointments.forEach { appointment ->
                AppointmentCard(appointment = appointment)
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = appointment.patientName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                text = timeFormatter.format(appointment.date.toDate()),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// Przykładowe dane do preview
private val sampleAppointments = listOf(
    Appointment(
        patientName = "Jan Kowalski",
        date = Timestamp(Date().apply { hours = 9 })
    ),
    Appointment(
        patientName = "Anna Nowak",
        date = Timestamp(Date().apply { hours = 14 })
    )
)

private val sampleCalendarState = CalendarState(
    dates = (-15..15).map { LocalDate.now().plusDays(it.toLong()) },
    selectedDate = LocalDate.now(),
    appointments = sampleAppointments
)

// Mock ViewModel dla preview
class PreviewCalendarViewModel : CalendarViewModel() {
    init {
        _state.value = sampleCalendarState
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {
    MaterialTheme {
        Calendar(viewModel = PreviewCalendarViewModel())
    }
}

@Preview
@Composable
fun DateSelectorPreview() {
    MaterialTheme {
        DateSelector(
            dates = sampleCalendarState.dates,
            selectedDate = sampleCalendarState.selectedDate,
            onDateSelected = {}
        )
    }
}

@Preview
@Composable
fun DateItemPreview() {
    MaterialTheme {
        DateItem(
            date = LocalDate.now(),
            isSelected = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun TimeGridPreview() {
    MaterialTheme {
        TimeGrid(appointments = sampleAppointments)
    }
}

@Preview
@Composable
fun TimeSlotPreview() {
    MaterialTheme {
        TimeSlot(
            hour = 10,
            appointments = sampleAppointments,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
fun AppointmentCardPreview() {
    MaterialTheme {
        AppointmentCard(appointment = sampleAppointments[0])
    }
}
