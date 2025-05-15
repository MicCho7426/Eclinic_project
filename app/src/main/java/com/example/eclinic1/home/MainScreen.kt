package com.example.eclinic1.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.eclinic1.ProfileScreen
import com.example.eclinic1.SearchScreen
import com.example.eclinic1.chat.ChatScreen // ⬅️ dodany import

@Composable
fun MainScreen(navController: NavHostController) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(bottomNavController) }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("search") { SearchScreen() }
            composable("chat") { ChatScreen(navController) } // ⬅️ Zamiana placeholdera na faktyczny ekran
            composable("profile") { ProfileScreen(navController) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = Color.Black) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White) },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") navController.navigate("home")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
            selected = currentRoute == "search",
            onClick = {
                if (currentRoute != "search") navController.navigate("search")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Message, contentDescription = "Chat", tint = Color.White) },
            selected = currentRoute == "chat",
            onClick = {
                if (currentRoute != "chat") navController.navigate("chat")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White) },
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") navController.navigate("profile")
            }
        )
    }
}
