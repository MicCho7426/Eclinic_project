import com.google.firebase.firestore.FirebaseFirestore

fun getAvailableSlots(doctorUid: String, date: String, onSuccess: (List<TimeSlot>) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("schedules")
        .document(doctorUid)
        .collection(date)
        .whereEqualTo("isBooked", false)
        .get()
        .addOnSuccessListener { result ->
            val slots = result.documents.map { doc ->
                TimeSlot(
                    id = doc.id,
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: "",
                    isBooked = doc.getBoolean("isBooked") ?: false
                )
            }
            onSuccess(slots)
        }
        .addOnFailureListener { exception ->
            // Handle error
        }
}

data class TimeSlot(
    val id: String,
    val startTime: String,
    val endTime: String,
    val isBooked: Boolean
)