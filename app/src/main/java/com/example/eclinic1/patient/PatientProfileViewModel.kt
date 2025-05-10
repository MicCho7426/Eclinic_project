package com.example.eclinic1.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PatientProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _patientData = MutableStateFlow<PatientData?>(null)
    val patientData: StateFlow<PatientData?> = _patientData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val currentUserId = auth.currentUser?.uid

    init {
        loadPatientData()
    }

    fun loadPatientData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = currentUserId ?: run {
                    _errorMessage.value = "No authenticated user found"
                    return@launch
                }

                val document = db.collection("patients").document(userId).get().await()
                if (document.exists()) {
                    _patientData.value = document.toObject(PatientData::class.java)?.apply {
                        // If you need to store userId separately in the data class
                        this.userId = userId
                    }
                } else {
                    _errorMessage.value = "No patient data found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}