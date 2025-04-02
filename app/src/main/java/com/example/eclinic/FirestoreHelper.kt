package com.example.eclinic.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreHelper {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun saveUserToFirestore(
        userId: String,
        email: String,
        firstname: String,
        surname: String,
        role: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userData = hashMapOf<String, Any>(
            "userId" to userId,
            "email" to email,
            "firstname" to firstname,
            "surname" to surname,
            "role" to role
        )

        // Add role-specific fields
        when (role.lowercase()) {
            "doctor" -> {
                userData.putAll(
                    hashMapOf(
                        "specialization" to "",
                        "experience" to "",
                        "availability" to listOf<Map<String, Any>>() // Empty list for availability
                    )
                )
            }
            "patient" -> {
                userData.putAll(
                    hashMapOf(
                        "medicalHistory" to emptyList<Map<String, String>>(),
                        "documents" to emptyList<String>() // List for storing document URLs
                    )
                )
            }
            "admin" -> {
                userData.putAll(
                    hashMapOf(
                        "permissions" to listOf("manage_users", "manage_doctors", "view_reports") // Example permissions
                    )
                )
            }
        }

        // Save to Firestore
        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
    fun userHasRole(requiredRole: String, onResult: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return onResult(false)
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val userRole = document.getString("role") ?: ""
                onResult(userRole == requiredRole)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

}
