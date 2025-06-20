package com.example.eclinic1.firebase

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.eclinic1.admin.Schedule
import com.example.eclinic1.admin.SimpleUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FetchAllUsers : FetchFirestoreName() {

    private val _allUsers = MutableStateFlow<List<SimpleUser>>(emptyList())
    val allUsers: StateFlow<List<SimpleUser>> = _allUsers

    private val _userRoles = mutableMapOf<String, String>()
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val schedules: StateFlow<List<Schedule>> = _schedules

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

    fun getSchedules(uid: String): Flow<List<Schedule>> {
        return db.collection("schedules")
            .document(uid)
            .collection("appointments")
            .whereEqualTo("isBooked", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    Schedule(
                        id = doc.id,
                        isBooked = doc.getBoolean("isBooked") ?: false,
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        date = doc.getDate("date")
                    )
                }
            }
    }
    fun deleteSchedule(userId: String, scheduleId: String) {
        db.collection("schedules")
            .document(userId)
            .collection("appointments")
            .document(scheduleId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Schedule deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting schedule", e)
            }
    }
}



