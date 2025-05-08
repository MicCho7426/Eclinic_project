package com.example.eclinic1

enum class Types(val type: String) {
    ADMIN("admin"),
    PATIENT("patient"),
    DOCTOR("doctor");

    companion object {
        fun fromString(type: String?): Types? {
            return values().find { it.type == type }
        }
    }
}