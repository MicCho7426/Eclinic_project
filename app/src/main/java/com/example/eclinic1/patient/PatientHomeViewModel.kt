package com.example.eclinic1.patient

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PatientHomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _patientData = MutableStateFlow<PatientData?>(null)
    val patientData: StateFlow<PatientData?> = _patientData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    fun loadData(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Load user data
                val userDoc = db.collection("users").document(userId).get().await()
                _userData.value = UserData(
                    firstName = userDoc.getString("firstName") ?: userDoc.getString("firstname") ?: "",
                    surname = userDoc.getString("surname") ?: ""
                )

                // Load patient data
                val patientDoc = db.collection("patients").document(userId).get().await()
                _patientData.value = if (patientDoc.exists()) {
                    PatientData(
                        dob = patientDoc.getString("dob"),
                        height = patientDoc.getString("height"),
                        weight = patientDoc.getString("weight"),
                        medicalHistory = patientDoc.getString("medicalHistory"),
                        uploadedFiles = patientDoc.get("uploadedFiles") as? List<String> ?: emptyList()
                    )
                } else {
                    null
                }

                // Load appointments
                loadAppointments(userId)
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAppointments(userId: String) {
        viewModelScope.launch {
            try {
                val now = Timestamp.now()
                val snapshot = db.collection("meetings")
                    .whereEqualTo("patientId", userId)
                    .whereGreaterThanOrEqualTo("date", now)
                    .orderBy("date")
                    .get()
                    .await()
                // Debug logging
                Log.d("Appointments", "Found ${snapshot.size()} appointments")
                snapshot.documents.forEach { doc ->
                    Log.d("Appointment", "ID: ${doc.id}, Date: ${doc.getTimestamp("date")}")
                }

                val appointmentsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Appointment(
                            id = doc.id,
                            patientId = data["patientId"] as? String ?: "",
                            doctorId = data["doctorId"] as? String ?: "",
                            date = data["date"] as? Timestamp ?: Timestamp.now(),
                            patientName = data["patientName"] as? String ?: "",
                            doctorName = data["doctorName"] as? String ?: "",
                            startTime = data["startTime"] as? String ?: "",
                            endTime = data["endTime"] as? String ?: "",
                            status = data["status"] as? String ?: "scheduled"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _appointments.value = appointmentsList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class UserData(
    val firstName: String = "",
    val surname: String = ""
)