package com.example.eclinic1

data class User(
    val uid: String = "",
    val login: String = "",
    val firstname:String = "",
    val secondname:String="",
    val dob:String="",
    val gender:String="",
    val userType: String = "",
    val weight: Double?= null,  // Pojawi się tylko dla pacjenta
    val height: Double?= null,  // Pojawi się tylko dla pacjenta
    val doctorId: String? = null,  // Pojawi się tylko dla lekarza
    val specialization: String? = null  // Pojawi się tylko dla lekarza
)



