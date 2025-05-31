package com.example.eclinic1

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.eclinic1.BookAppointmentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    navController: NavHostController,
    viewModel: BookAppointmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val doctors by viewModel.doctorList.collectAsState()
    val selectedDoctor by viewModel.selectedDoctor.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State for dropdown visibility
    var expanded by remember { mutableStateOf(false) }
    // State for search query
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
            // Input field for search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Doctors") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown menu - Fix 1: Added all required parameters
            DropdownMenu(
                expanded = expanded, // Fix: Added expanded parameter
                onDismissRequest = { expanded = false }, // Fix: Added dismiss handler
                modifier = Modifier.fillMaxWidth()
            ) {
                if (doctors.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No doctors available") }, // Fix: Added text parameter
                        onClick = { expanded = false }
                    )
                } else {
                    doctors.forEach { (id, name) -> // Fix: Explicit destructuring
                        DropdownMenuItem(
                            text = { Text(name) }, // Fix: Added text parameter
                            onClick = {
                                viewModel.selectDoctor(id to name) // Fix: Correct function call
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Date Picker Section
        OutlinedButton(
            onClick = { /* Show date picker */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.formatDate() ?: "Select Date")
        }

        // Book Button
        Button(
            onClick = {
                viewModel.createAppointment(
                    onSuccess = { navController.popBackStack() },
                    onFailure = { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = !isLoading && selectedDoctor != null && selectedDate != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Book Appointment")
            }
        }
    }
}

// Extension function for date formatting
private fun Date.formatDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
}