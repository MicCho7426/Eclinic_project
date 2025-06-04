package com.example.eclinic1

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    navController: NavHostController,
    viewModel: BookAppointmentViewModel = viewModel(),
) {
    val context = LocalContext.current
    val doctors by viewModel.doctorList.collectAsState()
    val selectedDoctor by viewModel.selectedDoctor.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadDoctors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Doctor Selection Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            // Clickable field to show dropdown
            OutlinedTextField(
                value = selectedDoctor?.second ?: "",
                onValueChange = { searchQuery = it },
                label = { Text("Select Doctor") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Show doctors"
                    )
                }
            )

            // Dropdown Menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (doctors.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No doctors available") },
                        onClick = { expanded = false }
                    )
                } else {
                    doctors.filter { doctor ->
                        doctor.second.contains(searchQuery, ignoreCase = true)
                    }.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text(doctor.second) },
                            onClick = {
                                viewModel.selectDoctor(doctor)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.formatDate() ?: "Select Date")
        }

        // Material3 Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dateState.selectedDateMillis?.let { millis ->
                                viewModel.updateSelectedDate(Date(millis))  // Use ViewModel method
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        // Book Button
        Button(
            onClick = {
                if (selectedDoctor == null || selectedDate == null) {
                    Toast.makeText(context, "Please select doctor and date", Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                viewModel.createAppointment(
                    onSuccess = {
                        navController.popBackStack()
                        Toast.makeText(context, "Appointment booked!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White)
            else Text("Book Appointment")
        }
    }
}


    // Extension function for date formatting
    private fun Date.formatDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
    }