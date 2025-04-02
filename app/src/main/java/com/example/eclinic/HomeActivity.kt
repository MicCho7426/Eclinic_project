package com.example.eclinic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.eclinic.ui.theme.EclinicTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EclinicTheme { // Wrap with theme
                val navController = rememberNavController() // ✅ Correctly initialize NavController

                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) } // ✅ Ensure bottom navigation is shown
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = "main" // ✅ Use a STRING instead of passing MainScreen()
                    )
                }
            }
        }
    }
}
