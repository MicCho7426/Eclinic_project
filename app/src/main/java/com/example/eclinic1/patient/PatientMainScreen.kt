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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eclinic1.BookAppointmentScreen
import com.example.eclinic1.R
import com.example.eclinic1.SearchScreen
import com.example.eclinic1.chat.ChatDetailScreen
import com.example.eclinic1.chat.ChatScreen
import com.example.eclinic1.chat.CreateChatDoctorScreen
import com.example.eclinic1.chat.CreateChatPatientScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PatientMainScreen(navController: NavController) {
    val patientNavController = rememberNavController()
    val items = listOf(
        PatientNavItem.Home,
        PatientNavItem.Search,
        PatientNavItem.Profile,
        PatientNavItem.Chat,
        PatientNavItem.Calendar
    )
    val navBackStackEntry by patientNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "chatDetail/{chatId}") {
                Box(modifier = Modifier) {
                    PatientBottomNavigation(patientNavController, items)
                }
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
    parentNavController: NavController,
    patientNavController: NavHostController = rememberNavController(),
    modifier: Modifier,
    navigationState: NavigationState = rememberNavigationState()
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
        composable(PatientNavItem.Calendar.route) {Calendar()}
        composable("patientData") {PatientDataScreen(navController)}
        composable("appointmentDetails") {  }
        composable(PatientNavItem.Chat.route) {ChatScreen(navController)}
        composable(
            route = "chatDetail/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""

            // Hide bottom bar when entering chat detail
            LaunchedEffect(Unit) {
                navigationState.showBottomBar = false
            }
            // Restore bottom bar when leaving
            DisposableEffect(Unit) {
                onDispose {
                    navigationState.showBottomBar = true
                }
            }
            ChatDetailScreen(chatId = chatId, navController = patientNavController)
        }
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
        composable("createChatPatient") { CreateChatPatientScreen(navController) }
        composable("createChatDoctor") { CreateChatDoctorScreen(navController) }

        // Chat detail with chatId

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
    object Calendar: PatientNavItem("calender", "Calendar", R.drawable.calendar)
}
class NavigationState {
    var showBottomBar by mutableStateOf(true)
}

@Composable
fun rememberNavigationState() = remember { NavigationState() }