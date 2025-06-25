package com.example.eclinic1.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eclinic1.firebase.FetchAllUsers


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    navController: NavController,
    userId: String,
    date:String,
    fetchAllUsers: FetchAllUsers = viewModel()
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var localSchedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            val schedules = fetchAllUsers.getSchedules(userId, date = date)
            localSchedules = schedules
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Schedules") }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $error")
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(localSchedules) { schedule ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Date: ${schedule.dateString}")
                                Text("Time: ${schedule.startTime} - ${schedule.endTime}")
                                Text("Booked: ${schedule.isBooked}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        fetchAllUsers.deleteSchedule(schedule.id)
                                        localSchedules = localSchedules.filter { it.id != schedule.id }
                                        Toast.makeText(
                                            context,
                                            "Deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
