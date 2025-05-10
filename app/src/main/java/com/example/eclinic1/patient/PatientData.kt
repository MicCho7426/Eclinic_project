package com.example.eclinic1.patient

data class PatientData(
    // These fields match your Firestore document structure
    var userId: String = "", // Optional: only if you want to store it in the object
    val dob: String? = null,
    val medicalHistory: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val uploadedFiles: List<String> = emptyList(),
    // Add any other fields that exist in your Firestore document
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
) {
    // Optional: Helper property to display full name
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")
}