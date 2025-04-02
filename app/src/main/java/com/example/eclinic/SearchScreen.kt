package com.example.eclinic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchScreen() {
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    val allItems = listOf("Doctor A", "Doctor B", "Doctor C", "Nurse X", "Clinic Y") // Sample Data
    val searchResults = remember(searchText.text) {
        allItems.filter { it.contains(searchText.text, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search", fontSize = 16.sp) }, // Hint Text
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,  // Keeps background clear
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = Color(0xFF00A6FB), // Bluish-cyan
                focusedIndicatorColor = Color(0xFF00A6FB), // Border color when focused
                unfocusedIndicatorColor = Color.Gray
            )
        )


        // Search Results List (LazyColumn is like RecyclerView)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(searchResults) { result ->
                SearchResultItem(result)
            }
        }
    }
}

// Individual Search Result Item
@Composable
fun SearchResultItem(result: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = result,
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp),
            color = Color.Black
        )
    }
}
