package com.example.eclinic1.firebase

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class FetchFcmToken : ViewModel() {
    private val db = FirebaseFirestore.getInstance()


    fun getToken(uid: String, onResult: (String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val token = document.getString("fcm")
                    onResult(token)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FetchFcmToken", "Failed to get token: ${e.message}")
                onResult(null)
            }
    }
}
