package com.example.eclinic1.patient

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

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
) {
    val fullName: String
        get() = listOfNotNull(firstname, surname).joinToString(" ")

    val bmi: Double?
        get() = calculateBMI()

    private fun calculateBMI(): Double? {
        return try {
            val heightM = height?.toDouble()?.div(100) ?: return null
            val weightKg = weight?.toDouble() ?: return null
            weightKg / (heightM * heightM)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidDOB(): Boolean {
        return dob?.let {
            try {
                val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT)
                LocalDate.parse(it, formatter)
                true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }
}