package com.example.eclinic1.admin

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eclinic1.R
import com.example.eclinic1.SplashScreen
import com.example.eclinic1.firebase.FetchAllUsers
import com.example.eclinic1.firebase.FetchFirestoreName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    navController: NavController,
    userId: String,
    date:String?=null,
    fetchAllUsers: FetchAllUsers = viewModel(),
    fetchFirestoreName: FetchFirestoreName= viewModel()
) {
    val context = LocalContext.current
    var doctorName by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(date ?: "") }
    var workdayExists by remember { mutableStateOf(false) }
    var workingHours by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var latestEnd by remember { mutableStateOf("") }
    var earliestEnd by remember { mutableStateOf("") }
    Scaffold (
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("E-Clinic", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xff2373c8),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LaunchedEffect(userId, date) {
                isLoading = true
                findDoctorWorkingHours(userId, date) { earliest, latest ->
                    latestEnd = latest
                    earliestEnd = earliest
                    workingHours = if (earliest.isNotEmpty() && latest.isNotEmpty()) {
                        "Working Hours: $earliest - $latest"
                    } else {
                        "No data"
                    }
                    isLoading = false
                }
                fetchAllUsers.getUserFullName(userId) { name ->
                    doctorName = name
                }

            }
            workdayExists = earliestEnd.isNotEmpty() && latestEnd.isNotEmpty()

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(workingHours, style = MaterialTheme.typography.bodyLarge)
            }
            Text("Set $doctorName Workday", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(16.dp))

            if (selectedDate.isNotEmpty()) {
                Text(
                    text = "Data: $selectedDate",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = earliestEnd,
                onValueChange = { earliestEnd = it },
                label = { Text("Start Time") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = latestEnd,
                onValueChange = { latestEnd = it },
                label = { Text("End Time") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedDate.isBlank() || earliestEnd.isBlank() || latestEnd.isBlank()) {
                        Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        fetchAllUsers.saveDoctorWorkday(userId, selectedDate, earliestEnd, latestEnd) {
                            isLoading = false
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            workdayExists = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Workday")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    fetchAllUsers.deleteDoctorWorkday(userId, selectedDate) {
                        isLoading = false
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        workdayExists = false
                    }
                },
                enabled = workdayExists,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Workday")
            }

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
    }

