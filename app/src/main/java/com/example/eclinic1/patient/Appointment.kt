package com.example.eclinic1.patient

import android.util.Log
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
    init {
        require(patientId.isNotBlank()) { "patientId cannot be blank" }
        require(doctorId.isNotBlank()) { "doctorId cannot be blank" }
    }
    companion object {
        fun fromFirestore(
            id: String,
            data: Map<String, Any>
        ): Appointment? = try {
            Appointment(
                id = id,
                patientId = data["patientId"] as? String ?: "",
                doctorId = data["doctorId"] as? String ?: "",
                date = data["date"] as? Timestamp ?: Timestamp.now(),
                patientName = data["patientName"] as? String ?: "",
                doctorName = data["doctorName"] as? String ?: "",
                startTime = data["startTime"] as? String ?: "",
                endTime = data["endTime"] as? String ?: "",
                status = data["status"] as? String ?: "scheduled"
            )
        } catch (e: Exception) {
            Log.e("AppointmentParse", "Error creating Appointment from Firestore", e)
            null
        }
    }
}