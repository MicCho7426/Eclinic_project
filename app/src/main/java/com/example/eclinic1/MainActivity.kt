package com.example.eclinic1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.eclinic.AppNavHost
import com.example.eclinic1.ui.theme.EclinicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EclinicTheme {
                val navController = rememberNavController()
                val startDestination = intent.getStringExtra("startDestination") ?: "login"

                AppNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}