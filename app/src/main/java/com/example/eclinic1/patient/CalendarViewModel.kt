package com.example.eclinic1.patient

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class CalendarViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _state = mutableStateOf(CalendarState())
    val state = _state

    init {
        loadInitialDates()
        loadUserRole()
        loadAppointmentsForDate(_state.value.selectedDate)
    }

    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val doc = db.collection("users").document(userId).get().await()
                _state.value = _state.value.copy(
                    userRole = doc.getString("role") ?: "patient"
                )
            } catch (e: Exception) {
                Log.e("CalendarVM", "Error loading user role", e)
                _state.value = _state.value.copy(
                    error = "Failed to load user profile"
                )
            }
        }
    }

    private fun loadInitialDates() {
        _state.value = _state.value.copy(
            dates = (-15..15).map { offset ->
                LocalDate.now().plusDays(offset.toLong())
            }
        )
    }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(
            selectedDate = date,
            isLoading = true,
            appointments = emptyList()
        )
        loadAppointmentsForDate(date)
    }

    private fun loadAppointmentsForDate(date: LocalDate) {
        val userId = auth.currentUser?.uid ?: run {
            _state.value = _state.value.copy(error = "Not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                val start = date.atStartOfDay(ZoneId.systemDefault())
                val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())

                val query = db.collection("meetings")
                    .whereGreaterThanOrEqualTo("date", Timestamp(Date.from(start.toInstant())))
                    .whereLessThan("date", Timestamp(Date.from(end.toInstant())))
                    .orderBy("date")

                val finalQuery = when (_state.value.userRole) {
                    "doctor" -> query.whereEqualTo("doctorId", userId)
                    else -> query.whereEqualTo("patientId", userId)
                }

                val snapshot = finalQuery.get().await()
                val appointments = snapshot.documents.mapNotNull { doc ->
                    try {
                        Appointment(
                            id = doc.id,
                            patientId = doc.getString("patientId") ?: "",
                            doctorId = doc.getString("doctorId") ?: "",
                            date = doc.getTimestamp("date") ?: Timestamp.now(),
                            patientName = doc.getString("patientName") ?: "Patient",
                            doctorName = doc.getString("doctorName") ?: "Doctor",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            status = doc.getString("status") ?: "scheduled"
                        )
                    } catch (e: Exception) {
                        Log.e("CalendarVM", "Error parsing doc ${doc.id}", e)
                        null
                    }
                }

                _state.value = _state.value.copy(
                    appointments = appointments,
                    isLoading = false,
                    error = if (appointments.isEmpty()) "No appointments" else null
                )
            } catch (e: Exception) {
                Log.e("CalendarVM", "Appointments load failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Load failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun retry() {
        loadAppointmentsForDate(_state.value.selectedDate)
    }
}