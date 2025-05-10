package com.example.eclinic1.firebase

import com.example.eclinic1.firebase.FetchFirestoreName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FetchAllUsers : FetchFirestoreName() {

    private val _allUsers = MutableStateFlow<List<String>>(emptyList())
    val allUsers: StateFlow<List<String>> = _allUsers

    override fun fetchUserData() {
        // Zmiana metody fetchującej dla wszystkich użytkowników
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val usersList = mutableListOf<String>()
                for (document in result) {
                    val firstname = document.getString("firstname") ?: "Unknown"
                    val secondname = document.getString("secondname") ?: "Unknown"
                    val login=document.getString("login")?:""
                    val type=document.getString("type")?:""
                }
                _allUsers.value = usersList
            }
            .addOnFailureListener {
                _allUsers.value = listOf("Error")
            }
    }
}