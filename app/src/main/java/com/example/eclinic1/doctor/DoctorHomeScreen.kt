package com.example.eclinic1.doctor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.R
import com.example.eclinic1.chat.ChatDetailScreen
import com.example.eclinic1.chat.ChatScreen
import com.example.eclinic1.chat.CreateChatDoctorScreen
import com.example.eclinic1.chat.CreateChatPatientScreen
import com.example.eclinic1.patient.Calendar
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DoctorHomeScreen(navController: NavController) {
    val doctorNavController = rememberNavController()
    val items = listOf(
        DoctorNavItem.Home,
        DoctorNavItem.Search,
        DoctorNavItem.Profile,
        DoctorNavItem.Chat,
        DoctorNavItem.Calendar
    )

    Scaffold(
        bottomBar = {
            Box(modifier = Modifier) {
                DoctorBottomNavigation(doctorNavController, items)
            }
        }
    ) { padding ->
        Box(modifier = Modifier) {
            DoctorNavHost(
                navController = doctorNavController,
                parentNavController = navController,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// Navigation Host
@Composable
fun DoctorNavHost(
    navController: NavHostController,
    parentNavController: NavController, // Add parent nav controller
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = DoctorNavItem.Home.route
    ) {
        composable(DoctorNavItem.Home.route) { DoctorHomeContent(navController) }
        composable(DoctorNavItem.Search.route) { DoctorSearchScreen() }
        composable(DoctorNavItem.Chat.route){ ChatScreen(navController) }
        composable(DoctorNavItem.Calendar.route){ Calendar() }
        composable("doctorSchedule") {DoctorScheduleScreen(navController)}
        composable(DoctorNavItem.Profile.route) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DoctorProfileScreen(
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
        composable("chatDetail/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(chatId = chatId, navController = navController)
        }
        composable("createChatPatient") { CreateChatPatientScreen(navController) }
        composable("createChatDoctor") { CreateChatDoctorScreen(navController) }
    }
}

// Bottom Navigation Bar
@Composable
fun DoctorBottomNavigation(
    navController: NavHostController,
    items: List<DoctorNavItem>
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
sealed class DoctorNavItem(
    val route: String,
    val label: String,
    val icon: Int
) {
    object Home : DoctorNavItem("doctor_home", "Home", R.drawable.ic_home)
    object Search : DoctorNavItem("doctor_search", "Search", R.drawable.ic_search)
    object Profile : DoctorNavItem("doctor_profile", "Profile", R.drawable.ic_profile)
    object Chat : DoctorNavItem("chat", "Chat", R.drawable.ic_chat)
    object Calendar : DoctorNavItem("calendar", "Calendar", R.drawable.calendar)
}