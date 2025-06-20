package com.example.eclinic1.admin

import androidx.compose.foundation.Image

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.eclinic1.R
import com.example.eclinic1.Types
import com.example.eclinic1.firebase.FetchAllUsers
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.FlowRow
import java.text.SimpleDateFormat

val SPECIALIZATIONS = listOf(
    "Cardiology", "Dermatology", "Endocrinology", "Gastroenterology",
    "Neurology", "Oncology", "Orthopedics", "Pediatrics",
    "Psychiatry", "Urology"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserListScreen(
    navController: NavHostController,
    viewModel: FetchAllUsers = viewModel()
) {
    val users by viewModel.allUsers.collectAsState(initial = emptyList())
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var fabExpanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("All") }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val filteredUsers = when (selectedRole) {
        "All" -> users
        else -> users.filter { it.role.equals(selectedRole, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUserData()
    }

    Scaffold(
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
                        auth.signOut()
                        navController.navigate("login")
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log out")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                fabExpanded = true

            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.Gray)
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter by:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Button(onClick = { dropdownExpanded = true }) {
                            Text(
                                when (selectedRole.lowercase()) {
                                    "all" -> "All"
                                    "admin" -> "Admin"
                                    "doctor" -> "Doctor"
                                    "patient" -> "Patient"
                                    else -> selectedRole
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("All", "Admin", "Doctor", "Patient").forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        selectedRole =
                                            if (role == "All") "All" else role.lowercase()
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(filteredUsers) { user ->
                        var showDialog by remember { mutableStateOf(false) }
                        var doctorDetailsExpanded by remember { mutableStateOf(false) }

                        val currentRole by viewModel.getUserRole(user.uid)
                            .collectAsState(initial = user.role)

                        UserCard(
                            user = user,
                            currentRole = currentRole,
                            onRoleChange = { newRole ->
                                viewModel.updateUserRole(user.uid, newRole)
                            },
                            onDelete = { showDialog = true },
                            onViewSchedules = { navController.navigate("schedules/${user.uid}") },
                            navController = navController
                        )

                        if (showDialog) {
                            DeleteUserDialog(
                                onDismiss = { showDialog = false },
                                onConfirm = { viewModel.deleteUser(user.uid) }
                            )
                        }
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCard(
    user: SimpleUser,
    currentRole: String,
    onRoleChange: (String) -> Unit,
    onDelete: () -> Unit,
    onViewSchedules: () -> Unit,
    navController: NavHostController
) {
    var roleExpanded by remember { mutableStateOf(false) }
    var doctorDetailsExpanded by remember { mutableStateOf(false) }
    var doctorId by remember { mutableStateOf("") }
    var selectedSpecs by remember { mutableStateOf<List<String>>(emptyList()) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${user.firstname} ${user.secondname}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            // Wybór roli
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }) {
                TextField(
                    value = currentRole.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Role") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    Types.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.type.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onRoleChange(type.name)
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            // Szczegóły lekarza (jeśli rola to doctor)
            if (currentRole == "doctor") {
                Spacer(Modifier.height(12.dp))
                // W UserCard:
                Button(
                    onClick = { navController.navigate("schedules/${user.uid}") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Schedules")
                }


                Button(
                    onClick = { doctorDetailsExpanded = !doctorDetailsExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Doctor Details")
                }
            }
            if (doctorDetailsExpanded) {
                DoctorDetailsForm(
                    doctorId = doctorId,
                    onDoctorIdChange = { doctorId = it },
                    selectedSpecs = selectedSpecs,
                    onSpecsChange = { selectedSpecs = it }
                )
            }
        }

        // Przycisk usuwania
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorDetailsForm(
    doctorId: String,
    onDoctorIdChange: (String) -> Unit,
    selectedSpecs: List<String>,
    onSpecsChange: (List<String>) -> Unit
) {
    var specExpanded by remember { mutableStateOf(false) }
    var tempSpec by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = doctorId,
            onValueChange = onDoctorIdChange,
            label = { Text("Doctor ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = specExpanded,
            onExpandedChange = { specExpanded = it }
        ) {
            TextField(
                value = tempSpec,
                onValueChange = { tempSpec = it },
                label = { Text("Add specialization") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
            )
            ExposedDropdownMenu(
                expanded = specExpanded,
                onDismissRequest = { specExpanded = false }
            ) {
                SPECIALIZATIONS.filter { !selectedSpecs.contains(it) }.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec) },
                        onClick = {
                            onSpecsChange(selectedSpecs + spec)
                            tempSpec = ""
                            specExpanded = false
                        }
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            selectedSpecs.forEach { spec ->
                FilterChip(
                    selected = true,
                    onClick = { onSpecsChange(selectedSpecs - spec) },
                    label = { Text(spec) },
                    trailingIcon = { Icon(Icons.Default.Close, null) }
                )
            }
        }
    }
}

@Composable
private fun DeleteUserDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete User") },
        text = { Text("Are you sure you want to delete this user?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: FetchAllUsers = viewModel()
) {
    val schedules by viewModel.getSchedules(userId).collectAsState(initial = emptyList())

    Scaffold(
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
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No booked schedules")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(schedules) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        onDelete = { viewModel.deleteSchedule(userId, schedule.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    schedule.date?.let {
                        Text(
                            "Date: ${SimpleDateFormat("yyyy-MM-dd").format(it)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text("From: ${schedule.startTime}", style = MaterialTheme.typography.bodyMedium)
                    Text("To: ${schedule.endTime}", style = MaterialTheme.typography.bodyMedium)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}