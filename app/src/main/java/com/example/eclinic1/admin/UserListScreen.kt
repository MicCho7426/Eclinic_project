package com.example.eclinic1.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.eclinic1.firebase.FetchAllUsers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavHostController,
    viewModel: FetchAllUsers = viewModel()
) {
    val users by viewModel.allUsers.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.fetchUserData()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Users List") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                var selectedRole by remember { mutableStateOf(user.role) }
                var doctorId by remember { mutableStateOf("") }
                var specializations by remember { mutableStateOf(listOf<String>()) }
                var newSpecialization by remember { mutableStateOf("") }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "${user.firstname} ${user.secondname}")

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role dropdown
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            TextField(
                                value = selectedRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role") },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("patient", "admin", "doctor").forEach { roleOption ->
                                    DropdownMenuItem(
                                        text = { Text(roleOption) },
                                        onClick = {
                                            selectedRole = roleOption
                                            viewModel.updateUserRole(user.uid, roleOption)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedRole == "doctor") {
                            Spacer(modifier = Modifier.height(12.dp))

                            // DoctorId input
                            OutlinedTextField(
                                value = doctorId,
                                onValueChange = { doctorId = it },
                                label = { Text("Doctor ID") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add specialization field
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSpecialization,
                                    onValueChange = { newSpecialization = it },
                                    label = { Text("Add Specialization") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    if (newSpecialization.isNotBlank()) {
                                        specializations = specializations + newSpecialization
                                        newSpecialization = ""
                                    }
                                }) {
                                    Text("+")
                                }
                            }

                            // Display added specializations
                            Column {
                                specializations.forEach { spec ->
                                    Text("- $spec", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                viewModel.updateDoctorDetails(
                                    user.uid,
                                    doctorId,
                                    specializations
                                )
                            }) {
                                Text("Save Doctor Details")
                            }
                        }
                    }
                }
            }
        }
    }
}
