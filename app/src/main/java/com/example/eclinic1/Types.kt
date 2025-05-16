package com.example.eclinic1

enum class Types(val type: String) {
    admin("admin"),
    patient("patient"),
    doctor("doctor");

    companion object {
        fun fromString(type: String?): Types? {
            return values().find { it.type == type }
        }
    }
}