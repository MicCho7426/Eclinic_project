package com.example.eclinic1.patient

data class PatientData(
    val userId: String? = null,
    val dob: String? = null,
    val medicalHistory: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val uploadedFiles: List<String> = emptyList(),
    val firstname: String? = null,
    val surname: String? = null,
    val email: String? = null
)