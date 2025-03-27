package com.example.eclinic

import FetchFirestoreName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.eclinic.ui.theme.EclinicTheme
import com.example.eclinic.ui.theme.Green80

class Start : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            EclinicTheme {
                val fetchFirestore: FetchFirestoreName=viewModel()
                val username by fetchFirestore.userName.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = username,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }


        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            color = Green80,
            modifier = modifier
        )
    }
    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        EclinicTheme {
            Greeting("Dla Jego")
        }
    }
}