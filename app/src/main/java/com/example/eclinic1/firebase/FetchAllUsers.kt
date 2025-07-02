package com.example.eclinic1.firebase

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.eclinic1.admin.DoctorWorkday
import com.example.eclinic1.admin.Schedule
import com.example.eclinic1.admin.SimpleUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class FetchAllUsers : FetchFirestoreName() {

    private val _allUsers = MutableStateFlow<List<SimpleUser>>(emptyList())
    val allUsers: StateFlow<List<SimpleUser>> = _allUsers

    private val _userRoles = mutableMapOf<String, String>()
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules
    val functions = Firebase.functions

    override fun fetchUserData() {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Failed to fetch users", error)
                    return@addSnapshotListener
                }

                val usersList = mutableListOf<SimpleUser>()
                snapshot?.documents?.forEach { document ->
                    val user = SimpleUser(
                        uid = document.id,
                        firstname = document.getString("firstname") ?: "",
                        secondname = document.getString("surname") ?: "",
                        role = document.getString("role") ?: "patient"
                    )
                    usersList.add(user)
                    _userRoles[document.id] = user.role
                }
                _allUsers.value = usersList
            }
    }

    fun updateUserRole(uid: String, newRole: String) {
        db.collection("users").document(uid)
            .update("role", newRole)
            .addOnSuccessListener {
                _userRoles[uid] = newRole
                Log.d("Firestore", "Role updated for $uid")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating role", e)
            }
    }

    fun updateDoctorDetails(uid: String, doctorId: String, specializations: List<String>) {
        val updateMap = mapOf(
            "DoctorId" to doctorId,
            "Specialization" to specializations
        )

        db.collection("users").document(uid)
            .update(updateMap)
            .addOnSuccessListener {
                Log.d("FirestoreUpdate", "Doctor fields updated for $uid")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUpdateError", "Failed to update doctor fields: ${e.message}")
            }
    }
    fun getDoctorDetails(uid: String, onResult: (String, List<String>) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val doctorId = document.getString("DoctorId") ?: ""
                val specialization = document.get("Specialization") as? List<String> ?: emptyList()
                onResult(doctorId, specialization)
            }
    }


    fun deleteUser(uid: String) {
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            if (currentUser.uid == uid) {
                currentUser.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                        }
                    }
            }
        }


        FirebaseFirestore.getInstance().collection("users").document(uid).delete()
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }
    fun saveDoctorWorkday(
        uid: String,
        date: String,
        startTime: String,
        endTime: String,
        onResult: () -> Unit
    ) {
        val data = mapOf(
            "uid" to uid,
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime
        )

        FirebaseFirestore.getInstance()
            .collection("doctorworkdays")
            .document("${uid}_$date")
            .set(data)
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onResult() }
    }

    fun deleteDoctorWorkday(
        uid: String,
        date: String,
        onResult: () -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("doctorworkdays")
            .document("${uid}_$date")
            .delete()
            .addOnSuccessListener { onResult() }
            .addOnFailureListener { onResult() }
    }

    fun getUserRole(uid: String): Flow<String> {
        return db.collection("users")
            .document(uid)
            .snapshots()
            .map { it.getString("role") ?: "patient" }
    }
    fun getUserFullName(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstname = document.getString("firstname") ?: ""
                    val surname = document.getString("surname") ?: ""
                    val fullName = "$firstname $surname".trim()

                    if (fullName.isNotBlank()) {
                        onResult(fullName)
                    } else {
                        onResult("No Name Found")
                    }
                } else {
                    onResult("User Not Found")
                }
            }
            .addOnFailureListener { e ->
                onResult("Error: ${e.localizedMessage}")
            }
    }


    fun sendPushNotification(token: String?, message: String?, onComplete: (Boolean) -> Unit ){
        val data = mapOf(
            "token" to token,
            "message" to message
        )
        functions.getHttpsCallable("sendPushNotification").call(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                onComplete(false)
            }
    }
    fun sendPushToAll(tokens: List<String>?,message: String?,onComplete: (Boolean) -> Unit){
        val data= mapOf(
            "tokens" to tokens,
            "message" to message
        )
        functions.getHttpsCallable("sendPushToAll").call(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}




