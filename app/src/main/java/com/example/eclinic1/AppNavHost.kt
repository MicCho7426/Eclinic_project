package com.example.eclinic1

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eclinic1.admin.UserListScreen
import com.example.eclinic1.home.MainScreen


@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") {
            RegisterScreen(navController) { email, password, firstname, surname ->
                registerUser(email, password, firstname, surname,navController)
            }
        }
        composable("main") { MainScreen(navController) }
        composable("admin") { UserListScreen(navController) }
    }
}
