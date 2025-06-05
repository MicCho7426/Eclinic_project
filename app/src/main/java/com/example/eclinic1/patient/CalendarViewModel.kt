package com.example.eclinic1.patient

import CalendarState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.State
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

open class CalendarViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Prywatny stan, aktualizowany przez ViewModel
    val _state = mutableStateOf(CalendarState())

    // Publiczny stan, który UI może tylko czytać
    val state: State<CalendarState> = _state

    init {
        loadInitialDates()
        loadAppointmentsForDate(_state.value.selectedDate)
    }

    private fun loadInitialDates() {
        val dates = (-15..15).map { offset ->
            LocalDate.now().plusDays(offset.toLong())
        }
        _state.value = _state.value.copy(dates = dates)
    }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(
            selectedDate = date,
            isLoading = true
        )
    }

    private fun loadAppointmentsForDate(date: LocalDate) {

        viewModelScope.launch {
            try {
                val userid = auth.currentUser?.uid
                val startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

                db.collection("appointments")
                    .whereEqualTo("uid",userid)
                    .whereGreaterThanOrEqualTo("date", startOfDay)
                    .whereLessThan("date", endOfDay)
                    .get()
                    .addOnSuccessListener { result ->
                        val appointments = result.documents.mapNotNull { doc ->
                            doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                        }
                        _state.value = _state.value.copy(
                            appointments = appointments,
                            isLoading = false
                        )
                    }
                    .addOnFailureListener { exception ->
                        _state.value= _state.value.copy(
                            error = exception.message ?: "Unknown error",
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error",
                    isLoading = false
                )
            }
        }
    }
}
