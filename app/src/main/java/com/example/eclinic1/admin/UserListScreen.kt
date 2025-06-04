package com.example.eclinic1.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.eclinic1.firebase.FetchAllUsers

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
                var selectedSpecialization by remember { mutableStateOf("") }
                var specExpanded by remember { mutableStateOf(false) }
                var selectedSpecs by remember { mutableStateOf(listOf<String>()) }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "${user.firstname} ${user.secondname}")
                        Spacer(modifier = Modifier.height(8.dp))

                        var roleExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = roleExpanded,
                            onExpandedChange = { roleExpanded = !roleExpanded }
                        ) {
                            TextField(
                                value = selectedRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role") },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false }
                            ) {
                                listOf("patient", "admin", "doctor").forEach { roleOption ->
                                    DropdownMenuItem(
                                        text = { Text(roleOption) },
                                        onClick = {
                                            selectedRole = roleOption
                                            viewModel.updateUserRole(user.uid, roleOption)
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedRole == "doctor") {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = doctorId,
                                onValueChange = { doctorId = it },
                                label = { Text("Doctor ID") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Specialization selector
                            ExposedDropdownMenuBox(
                                expanded = specExpanded,
                                onExpandedChange = { specExpanded = !specExpanded }
                            ) {
                                TextField(
                                    value = selectedSpecialization,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Specialization") },
                                    modifier = Modifier.menuAnchor(),
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = specExpanded,
                                    onDismissRequest = { specExpanded = false }
                                ) {
                                    SPECIALIZATIONS.forEach { spec ->
                                        DropdownMenuItem(
                                            text = { Text(spec) },
                                            onClick = {
                                                if (!selectedSpecs.contains(spec)) {
                                                    selectedSpecs = selectedSpecs + spec
                                                }
                                                selectedSpecialization = ""
                                                specExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Show selected specializations
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedSpecs.forEach { spec ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(spec) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                selectedSpecs = selectedSpecs - spec
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove")
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(onClick = {
                                viewModel.updateDoctorDetails(
                                    user.uid,
                                    doctorId,
                                    selectedSpecs
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
