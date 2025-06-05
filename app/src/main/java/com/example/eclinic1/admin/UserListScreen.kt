package com.example.eclinic1.admin
import androidx.compose.foundation.Image
import com.example.eclinic1.firebase.FetchAllUsers
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eclinic1.R
import com.example.eclinic1.Types
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
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
        "admin" -> users.filter { it.role == "admin" }
        "doctor" -> users.filter { it.role == "doctor" }
        "patient" -> users.filter { it.role == "patient" }
        else -> users
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
                    IconButton(onClick = {
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { fabExpanded = true }) {
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
                // Filtrowanie
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Sort by: ", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Button(onClick = { dropdownExpanded = true }) {
                            Text(selectedRole)
                        }
                        DropdownMenu(

                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("All", "admin", "doctor", "patient").forEach { role ->
                                DropdownMenuItem(text= { Text(role.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedRole = role
                                        dropdownExpanded = false
                                    }
                            )
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        var role by remember { mutableStateOf(user.role) }
                        var expanded by remember { mutableStateOf(false) }
                        var showDialog by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${user.firstname} ${user.secondname}",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box {
                                        Button(onClick = { expanded = true }) {
                                            Text(role)
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            Types.values().forEach { userType ->
                                                DropdownMenuItem(
                                                    text = { Text(userType.type) },
                                                    onClick = {
                                                        role = userType.name
                                                        expanded = false
                                                        viewModel.updateUserRole(
                                                            user.uid,
                                                            userType.name
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        showDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Usuń użytkownika",
                                            tint = Color.Red
                                        )
                                    }
                                }

                                if (showDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDialog = false },
                                        title = { Text("Alert") },
                                        text = { Text("Are you sure you want to delete this account?") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                FirebaseAuth.getInstance().currentUser?.delete()
                                                showDialog = false
                                            }) {
                                                Text("Yes")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDialog = false }) {
                                                Text("No")
                                            }
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
