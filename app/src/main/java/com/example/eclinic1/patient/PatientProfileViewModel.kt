package com.example.eclinic1.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PatientProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<PatientProfileUiState>(PatientProfileUiState.Loading)
    val uiState: StateFlow<PatientProfileUiState> = _uiState.asStateFlow()

    init {
        loadPatientData()
    }

    fun loadPatientData() {
        viewModelScope.launch {
            try {
                _uiState.value = PatientProfileUiState.Loading
                val userId = auth.currentUser?.uid ?: throw Exception("Not authenticated")

                // Load both user and patient data in parallel
                val userDeferred = async { db.collection("users").document(userId).get().await() }
                val patientDeferred = async { db.collection("patients").document(userId).get().await() }

                val userDoc = userDeferred.await()
                val patientDoc = patientDeferred.await()

                if (!patientDoc.exists()) {
                    throw Exception("Patient record not found")
                }

                _uiState.value = PatientProfileUiState.Success(
                    PatientProfileData(
                        fullName = "${userDoc.getString("firstname")} ${userDoc.getString("surname")}",
                        email = userDoc.getString("email"),
                        dob = patientDoc.getString("dob"),
                        height = patientDoc.getString("height"),
                        weight = patientDoc.getString("weight"),
                        medicalHistory = patientDoc.getString("medicalHistory"),
                        documents = patientDoc.get("documents") as? List<String> ?: emptyList(),
                        profileImageUrl = userDoc.getString("profileImageUrl")
                    )
                )
            } catch (e: Exception) {
                _uiState.value = PatientProfileUiState.Error(
                    message = e.message ?: "Failed to load profile",
                    retryable = true
                )
            }
        }
    }
}

sealed class PatientProfileUiState {
    object Loading : PatientProfileUiState()
    data class Success(val data: PatientProfileData) : PatientProfileUiState()
    data class Error(val message: String, val retryable: Boolean) : PatientProfileUiState()
}

data class PatientProfileData(
    val fullName: String,
    val email: String?,
    val dob: String?,
    val height: String?,
    val weight: String?,
    val medicalHistory: String?,
    val documents: List<String>,
    val profileImageUrl: String?
)