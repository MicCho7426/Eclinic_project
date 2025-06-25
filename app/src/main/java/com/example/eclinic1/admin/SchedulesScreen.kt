//package com.example.eclinic1.admin
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import com.example.eclinic1.R
//import com.example.eclinic1.Types
//import com.example.eclinic1.firebase.FetchAllUsers
//import com.google.firebase.auth.FirebaseAuth
//import androidx.compose.foundation.layout.FlowRow
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import java.text.SimpleDateFormat
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//suspend fun SchedulesScreen(
//    userId: String,
//    onBack: () -> Unit,
//    viewModel: FetchAllUsers = viewModel()
//) {
//    val schedules by viewModel.getSchedules(userId).collectAsState(initial = emptyList())
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Image(
//                            painter = painterResource(id = R.drawable.logo),
//                            contentDescription = "Logo",
//                            modifier = Modifier.size(50.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("E-Clinic", style = MaterialTheme.typography.titleLarge)
//                    }
//                })
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = {
//                // Dodaj logikę do otwarcia ekranu dodawania harmonogramu
//            }) {
//                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
//            }
//        }
//    ) { padding ->
//        if (schedules.isEmpty()) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("No schedules found for this doctor.")
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//            ) {
//                items(schedules) { schedule ->
//                    ScheduleItem(schedule = schedule) {
//                        viewModel.deleteSchedule(userId, schedule.id)
//                    }
//                }
//            }
//        }
//    }
//}
//    @Composable fun ScheduleItem(
//        schedule: Schedule,
//        onDelete: () -> Unit
//    ) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp),
//            elevation = CardDefaults.cardElevation()
//        ) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                schedule.date?.let {
//                    Text(
//                        "Date: ${SimpleDateFormat("yyyy-MM-dd").format(it)}",
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//                Text("From: ${schedule.startTime}", style = MaterialTheme.typography.bodyMedium)
//                Text("To: ${schedule.endTime}", style = MaterialTheme.typography.bodyMedium)
//
//                Spacer(modifier = Modifier.height(8.dp))
//                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
//                    IconButton(onClick = onDelete) {
//                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
//                    }
//                }
//            }
//        }
//    }
