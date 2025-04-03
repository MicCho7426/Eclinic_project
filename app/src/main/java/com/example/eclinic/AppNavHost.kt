package com.example.eclinic

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = "main") {
        composable("splash") { SplashScreen() } // Splash screen before navigation
        composable("login") { LoginScreen(navController) }
        composable("register") {
            RegisterScreen(navController) { email, password, firstname, surname ->
                registerUser(email, password, firstname, surname, navController)
            }
        }

        composable("main") { MainScreen(navController) }
        composable("admin") { AdminScreen(navController) }
        composable("patientData") { PatientDataScreen(navController) }

    }
}
