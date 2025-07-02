package com.example.eclinic1.admin

import com.google.firebase.firestore.FirebaseFirestore

fun findDoctorWorkingHours(uid: String, date: String?=null,
                           onSuccess: (earliest: String, latest: String) -> Unit) {
    if (date==null){
        onSuccess("","")
        return
    }
    FirebaseFirestore.getInstance()
        .collection("schedules")
        .document(uid)
        .collection(date)
        .get()
        .addOnSuccessListener { result ->
            val slots = result.documents.mapNotNull { doc ->
                doc.getString("startTime")?.let { startTime ->
                    doc.getString("endTime")?.let { endTime ->
                        Pair(startTime, endTime)
                    }
                }
            }

            if (slots.isNotEmpty()) {
                val earliestStart = slots.minByOrNull { it.first }?.first ?: ""
                val latestEnd = slots.maxByOrNull { it.second }?.second ?: ""
                onSuccess(earliestStart, latestEnd)
            } else {
                onSuccess("", "")
            }
        }
        .addOnFailureListener {
            onSuccess("", "")
        }
}