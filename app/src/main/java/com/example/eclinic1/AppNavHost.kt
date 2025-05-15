package com.example.eclinic1

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eclinic1.admin.UserListScreen
import com.example.eclinic1.chat.ChatDetailScreen
import com.example.eclinic1.chat.ChatScreen
import com.example.eclinic1.chat.CreateChatDoctorScreen
import com.example.eclinic1.chat.CreateChatPatientScreen
import com.example.eclinic1.home.MainScreen
import com.example.eclinic1.patient.PatientDataScreen
import com.example.eclinic1.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") { SplashScreen() }
        composable("login") { LoginScreen(navController) }
        composable("register") {
            RegisterScreen(navController) { email, password, firstname, surname ->
                registerUser(email, password, firstname, surname, navController)
            }
        }
        composable("main") { MainScreen(navController) }
        composable("admin") { UserListScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("patientData") { PatientDataScreen(navController) }

        // Chat routes
        composable("chat") { ChatScreen(navController) }
        composable("createChatPatient") { CreateChatPatientScreen(navController) }
        composable("createChatDoctor") { CreateChatDoctorScreen(navController) }

        // Chat detail with chatId
        composable("chatDetail/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(chatId = chatId, navController = navController)
        }
    }
}
