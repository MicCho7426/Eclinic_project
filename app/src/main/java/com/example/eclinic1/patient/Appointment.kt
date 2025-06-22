package com.example.eclinic1.patient

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.ZoneId

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val date: Timestamp = Timestamp.now(),
    val patientName: String = "",
    val doctorName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "" // scheduled, completed, cancelled
) {
    fun toLocalDate(): LocalDate {
        return date.toDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}