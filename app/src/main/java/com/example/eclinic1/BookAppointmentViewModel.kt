package com.example.eclinic1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadDoctors() // Auto-load doctors when VM created
    }


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _doctorList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val doctorList: StateFlow<List<Pair<String, String>>> = _doctorList

    private val _selectedDoctor = MutableStateFlow<Pair<String, String>?>(null)
    val selectedDoctor: StateFlow<Pair<String, String>?> = _selectedDoctor

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate

    // Firestore field value capitalization
    fun createAppointment(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        _isLoading.value = true

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _isLoading.value = false
            onFailure(Exception("User not authenticated"))
            return
        }

        val doctor = _selectedDoctor.value
        val date = _selectedDate.value

        if (doctor == null || date == null) {
            _isLoading.value = false
            onFailure(Exception("Please select doctor and date"))
            return
        }

        //FieldValue capitalization
        val appointment = hashMapOf(
            "patientId" to currentUser.uid,
            "doctorId" to doctor.first,
            "doctorName" to doctor.second,
            "date" to date.formatDate(),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(), // Fixed capitalization
            "updatedAt" to FieldValue.serverTimestamp()  // Fixed capitalization
        )

        viewModelScope.launch {
            try {
                db.collection("appointments")
                    .add(appointment)
                    .addOnSuccessListener {
                        _isLoading.value = false
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        onFailure(e)
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                onFailure(e)
            }
        }
        onSuccess()
    }

    fun selectDoctor(doctor: Pair<String, String>) {
        _selectedDoctor.value = doctor
    }

    //Proper doctor list loading
    fun loadDoctors() {
        viewModelScope.launch {
            db.collection("users")
                .whereEqualTo("role", "doctor")
                .get()
                .addOnSuccessListener { snapshot ->
                    val doctors = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = "${doc.getString("firstName") ?: ""} ${doc.getString("lastName") ?: ""}".trim()
                        if (name.isNotEmpty()) id to name else null
                    }
                    _doctorList.value = doctors
                }
                .addOnFailureListener { e ->
                    // Handle error
                }
        }
    }

    //Date formatting extension
    private fun Date.formatDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)
    }
}