package com.example.eclinic

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eclinic1.LoginScreen
import com.example.eclinic1.admin.AdminActivity
import com.example.eclinic1.doctor.DoctorHomeScreen
import com.example.eclinic1.patient.PatientHomeScreen
import com.example.eclinic1.patient.PatientMainScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("adminHome") { AdminActivity(navController) }
        composable("doctorHome") { DoctorHomeScreen(navController) }
        composable("patientHome") { PatientMainScreen(navController) }
    }
}