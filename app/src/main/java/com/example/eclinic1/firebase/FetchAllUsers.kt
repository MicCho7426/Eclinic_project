package com.example.eclinic1.firebase

import android.util.Log
import com.example.eclinic1.admin.SimpleUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FetchAllUsers : FetchFirestoreName() {

    private val _allUsers = MutableStateFlow<List<SimpleUser>>(emptyList())
    val allUsers: StateFlow<List<SimpleUser>> = _allUsers

    override fun fetchUserData() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val usersNameList = mutableListOf<SimpleUser>()
                for (document in result) {
                    val id = document.id
                    val firstname = document.getString("firstname") ?: "Unknown"
                    val surname = document.getString("surname") ?: "Unknown"
                    val role = document.getString("role") ?: ""
                    // Add more fields if needed later (e.g., DoctorId, Specialization)
                    usersNameList.add(SimpleUser(id, firstname, surname, role))
                }
                _allUsers.value = usersNameList
            }
            .addOnFailureListener {e->
                Log.e("FirestoreError", "Failed to fetch users: ${e.message}")
                _allUsers.value = emptyList()
            }
    }
    fun updateUserRole(uid:String,newRole:String){
        db.collection("users").document(uid)
            .update("role", newRole)
            .addOnSuccessListener {
                Log.d("FirestoreUpdate", "User $uid role successfully updated")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUpdateError", "Failed to update role: ${e.message}")
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
}
