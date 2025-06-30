package com.example.eclinic1

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eclinic1.admin.AdminActivity
import com.example.eclinic1.admin.Create
import com.example.eclinic1.admin.SchedulesScreen
import com.example.eclinic1.doctor.DoctorHomeScreen
import com.example.eclinic1.patient.PatientMainScreen
import com.example.eclinic1.admin.UserListScreen
import com.example.eclinic1.chat.ChatDetailScreen
import com.example.eclinic1.chat.ChatScreen
import com.example.eclinic1.chat.CreateChatDoctorScreen
import com.example.eclinic1.chat.CreateChatPatientScreen
import com.example.eclinic1.doctor.DoctorScheduleScreen


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
                registerUser(email, password, firstname, surname, navController)
            }
        }
        composable("bookAppointment") {
            BookAppointmentScreen(navController)
        }
        composable("admin") { UserListScreen(navController) }
        composable(
            route = "schedules/{userId}?date={date}",
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val date = backStackEntry.arguments?.getString("date")

            SchedulesScreen(
                userId = userId,
                date = date, navController = navController
            )
        }

        composable("create"){Create(navController)}
    }
}
