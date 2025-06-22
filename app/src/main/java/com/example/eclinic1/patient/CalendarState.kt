package com.example.eclinic1.patient

import java.time.LocalDate

data class CalendarState(
    val dates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val appointments: List<Appointment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userRole: String? = null
)