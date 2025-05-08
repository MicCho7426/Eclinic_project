package com.example.eclinic1.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.AppNavHost
import com.example.eclinic1.ui.theme.EclinicTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EclinicTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = "main"
                    )
                }
            }
        }
    }
}
