package com.example.eclinic1.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.AppNavHost
import com.example.eclinic1.ui.theme.EclinicTheme

class StartAdmin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            EclinicTheme {
                AppNavHost(
                    navController = navController,
                    startDestination = "admin" // startujemy od listy użytkowników
                )
            }
        }
    }
}