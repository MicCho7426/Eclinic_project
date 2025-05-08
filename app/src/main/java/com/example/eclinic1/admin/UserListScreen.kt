package com.example.eclinic1.admin

import com.example.eclinic1.firebase.FetchAllUsers
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavHostController,
    viewModel: FetchAllUsers = viewModel()
) {
    val users by viewModel.allUsers.collectAsState(initial = emptyList()) // Zbieramy dane użytkowników

    // Ładujemy dane użytkowników
    LaunchedEffect(Unit) {
        viewModel.fetchUserData()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Users List") })
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                var role by remember { mutableStateOf(user.role) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = user.firstname + user.secondname,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = role,
                            onValueChange = {
                                role = it
                                viewModel.updateUserRole(user.uid, it)
                            },
                            label = { Text("Role") },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}