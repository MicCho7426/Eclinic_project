package com.example.eclinic1.patient

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class Appointment(
    @DocumentId val id: String = "",  // Auto-ID z Firestore
    val uid: String = "",             // ID użytkownika
    val date: Timestamp = Timestamp.now(), // Data i godzina
    val patientName: String = "",
    val doctorName: String = "",
)
