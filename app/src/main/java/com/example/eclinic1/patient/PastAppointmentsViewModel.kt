package com.example.eclinic1.patient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PastAppointmentsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPastAppointments() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val now = Timestamp.now()

                val snapshot = db.collection("meetings")
                    .whereEqualTo("patientId", userId)
                    .whereLessThan("date", now)  // Only past appointments
                    .orderBy("date", Query.Direction.DESCENDING)  // Newest first
                    .get()
                    .await()

                _appointments.value = snapshot.documents.mapNotNull { doc ->
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
                            status = data["status"] as? String ?: "completed"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("PastAppointments", "Error loading appointments", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}