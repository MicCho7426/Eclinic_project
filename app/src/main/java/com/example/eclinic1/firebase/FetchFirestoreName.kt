package com.example.eclinic1.firebase

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class FetchFirestoreName : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    init {
        fetchUserData()
    }

    open fun fetchUserData() {
        val userId = auth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            _userName.value = "Not Logged In"
            return
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstname = document.getString("firstname") ?: ""
                    val surname = document.getString("surname") ?: "" // Changed from "secondname" to "surname"

                    if (firstname.isNotBlank() || surname.isNotBlank()) {
                        _userName.value = "$firstname $surname".trim()
                    } else {
                        _userName.value = "No Name Found"
                    }
                } else {
                    _userName.value = "User Not Found"
                }
            }
            .addOnFailureListener { e ->
                _userName.value = "Error: ${e.localizedMessage}"
            }

    }
}
