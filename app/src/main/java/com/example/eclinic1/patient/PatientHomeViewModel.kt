package com.example.eclinic1.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eclinic1.Appointment
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

                // Load from users collection
                val userDoc = db.collection("users").document(userId).get().await()
                _userData.value = UserData(
                    firstName = userDoc.getString("firstname"),
                    surname = userDoc.getString("surname")
                )

                // Load from patients collection
                val patientDoc = db.collection("patients").document(userId).get().await()
                if (patientDoc.exists()) {
                    _patientData.value = PatientData(
                        dob = patientDoc.getString("dob"),
                        height = patientDoc.getString("height"),
                        weight = patientDoc.getString("weight"),
                        medicalHistory = patientDoc.getString("medicalHistory"),
                        uploadedFiles = patientDoc.get("uploadedFiles") as? List<String> ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

data class UserData(
    val firstName: String?,
    val surname: String?
)

    fun loadAppointments(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("appointments")
            .whereEqualTo("patientId", userId)
            .get()
            .addOnSuccessListener { result ->
                _appointments.value = result.toObjects(Appointment::class.java)
            }
    }
}
