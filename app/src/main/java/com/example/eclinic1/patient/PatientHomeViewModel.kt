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
import java.text.SimpleDateFormat
import java.util.Locale

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

                // Debug: Print current time and user ID
                Log.d("AppointmentsDebug", "Current time: ${now.toDate()}")
                Log.d("AppointmentsDebug", "User ID: $userId")

                // First, get all appointments for this patient without date filter
                val snapshot = db.collection("meetings")
                    .whereEqualTo("patientId", userId)
                    .get()
                    .await()

                Log.d("AppointmentsDebug", "Found ${snapshot.size()} appointments for patient")

                val appointmentsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null

                        // Safely handle the date field which might not be a Timestamp
                        val date = when (val dateField = data["date"]) {
                            is Timestamp -> dateField
                            is String -> try {
                                // Parse from string if stored as ISO format
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dateObj = dateFormat.parse(dateField)
                                Timestamp(dateObj)
                            } catch (e: Exception) {
                                Log.e("DateParse", "Failed to parse date string", e)
                                Timestamp.now() // Fallback
                            }
                            is com.google.firebase.Timestamp -> dateField // Alternative Timestamp class
                            else -> {
                                Log.w("DateWarning", "Unknown date format in doc ${doc.id}")
                                Timestamp.now() // Fallback
                            }
                        }

                        // Only include future appointments
                        if (date.toDate().before(now.toDate())) {
                            Log.d("AppointmentFilter", "Skipping past appointment: ${doc.id}")
                            return@mapNotNull null
                        }

                        Appointment(
                            id = doc.id,
                            patientId = data["patientId"] as? String ?: "",
                            doctorId = data["doctorId"] as? String ?: "",
                            date = date,
                            patientName = data["patientName"] as? String ?: "",
                            doctorName = data["doctorName"] as? String ?: "",
                            startTime = data["startTime"] as? String ?: "",
                            endTime = data["endTime"] as? String ?: "",
                            status = data["status"] as? String ?: "scheduled"
                        )
                    } catch (e: Exception) {
                        Log.e("AppointmentError", "Error parsing doc ${doc.id}", e)
                        null
                    }
                }

                Log.d("AppointmentsDebug", "Filtered to ${appointmentsList.size} future appointments")
                _appointments.value = appointmentsList.sortedBy { it.date }

            } catch (e: Exception) {
                Log.e("AppointmentsError", "Failed to load appointments", e)
                _appointments.value = emptyList() // Ensure empty state rather than crashing
            }
        }
    }
}

data class UserData(
    val firstName: String = "",
    val surname: String = ""
)