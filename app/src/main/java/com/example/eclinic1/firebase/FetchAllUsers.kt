package com.example.eclinic1.firebase

import android.util.Log
import androidx.lifecycle.viewModelScope
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

    fun getUserRole(uid: String): Flow<String> {
        return db.collection("users")
            .document(uid)
            .snapshots()
            .map { it.getString("role") ?: "patient" }
    }

    suspend fun getSchedules(uid: String,date:String): List<Schedule> {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(uid)
                .collection("2025-06-09")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val isBooked=doc.getBoolean("isBooked")?:false
                if(isBooked) {


                    Schedule(
                        id = doc.id,
                        isBooked = doc.getBoolean("isBooked") ?: false,
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        date = doc.getDate("date"),
                        dateString = doc.getString("dateString") ?: ""
                    )
                }else{
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


    fun deleteSchedule(scheduleId: String) {
        FirebaseFirestore.getInstance()
            .collection("schedules")
            .document(scheduleId)
            .delete()
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
}




