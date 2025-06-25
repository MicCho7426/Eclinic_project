package com.example.eclinic1.admin;

import java.util.Date

data class Schedule(
        val id: String = "",
        val isBooked: Boolean = false,
        val startTime: String = "",
        val endTime: String = "",
        val date: Date? = null,
        val dateString: String = ""
)
