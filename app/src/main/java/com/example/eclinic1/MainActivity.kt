package com.example.eclinic1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.eclinic1.ui.theme.EclinicTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

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
