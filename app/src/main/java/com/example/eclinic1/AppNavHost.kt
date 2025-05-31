package com.example.eclinic1

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eclinic1.admin.AdminActivity
import com.example.eclinic1.doctor.DoctorHomeScreen
import com.example.eclinic1.patient.PatientMainScreen
import com.example.eclinic1.admin.UserListScreen
import com.example.eclinic1.chat.ChatScreen
import com.google.firebase.auth.FirebaseAuth


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("adminHome") { AdminActivity(navController) }
        composable("doctorHome") { DoctorHomeScreen(navController) }
        composable("patientHome") { PatientMainScreen(navController) }
        composable("register") {
            RegisterScreen(navController) { email, password, firstname, surname ->
                registerUser(email, password, firstname, surname,navController)
            }
        }
        composable("bookAppointment") {
            // Get current user ID safely
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("bookAppointment") { inclusive = true }
                    }
                }
                return@composable
            }
            BookAppointmentScreen(
                navController = navController,
                viewModel = viewModel<BookAppointmentViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return BookAppointmentViewModel() as T
                        }
                    }
                )
            )
        }
        composable("admin") { UserListScreen(navController) }
    }
}
