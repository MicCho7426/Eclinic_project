package com.example.eclinic1.patient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.BookAppointmentScreen
import com.example.eclinic1.R
import com.example.eclinic1.SearchScreen
import com.example.eclinic1.chat.ChatScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PatientMainScreen(navController: NavController) {
    val patientNavController = rememberNavController()
    val items = listOf(
        PatientNavItem.Home,
        PatientNavItem.Search,
        PatientNavItem.Profile,
        PatientNavItem.Chat
    )

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier) {
                PatientBottomNavigation(patientNavController, items)
            }
        }
    ) { padding ->
        Box(modifier = Modifier) {
            PatientNavHost(
                navController = patientNavController,
                parentNavController = navController,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// Navigation Host
@Composable
fun PatientNavHost(
    navController: NavHostController,
    parentNavController: NavController, // Add parent nav controller
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = PatientNavItem.Home.route
    ) {
        composable(
            PatientNavItem.Home.route
        ) { backStackEntry ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                PatientHomeScreen(
                    patientNavController = navController,
                    parentNavController = parentNavController,
                    navController = navController,
                    viewModel = viewModel()
                )
            } else {
                // Handle not logged in case
            }
        }
        composable("bookAppointment") {
            BookAppointmentScreen(navController = navController)}
        composable(PatientNavItem.Search.route) { SearchScreen() }
        composable(PatientNavItem.Chat.route) { ChatScreen(navController) }
        composable(PatientNavItem.Profile.route) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                PatientProfileScreen(
                    navController = navController,
                    onLogout = {
                        // Handle logout
                        FirebaseAuth.getInstance().signOut()
                        parentNavController.navigate("login") {
                            popUpTo(parentNavController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}

// Bottom Navigation Bar
@Composable
fun PatientBottomNavigation(
    navController: NavHostController,
    items: List<PatientNavItem>
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(ImageVector.vectorResource(item.icon), contentDescription = null) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// Navigation Items
sealed class PatientNavItem(
    val route: String,
    val label: String,
    val icon: Int
) {
    object Home : PatientNavItem("patient_home", "Home", R.drawable.ic_home)
    object Search : PatientNavItem("patient_search", "Search", R.drawable.ic_search)
    object Profile : PatientNavItem("patient_profile", "Profile", R.drawable.ic_profile)
    object Chat : PatientNavItem("chat", "Chat", R.drawable.ic_chat)
}