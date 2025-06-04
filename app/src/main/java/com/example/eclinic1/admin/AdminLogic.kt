package com.example.eclinic1.admin
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun checkAndUpdateUserData(uid: String) {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(uid)

    userRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val userType = document.getString("type")

            // Sprawdzamy, czy użytkownik jest "Patient"
            if (userType == "Patient") {
                val weightExists = document.contains("weight")
                val heightExists = document.contains("height")

                // Jeśli pola już istnieją, nie dodajemy ich ponownie
                if (weightExists && heightExists) {
                    Log.d("Firestore", "Pola weight i height już istnieją. Pomijam aktualizację.")
                    return@addOnSuccessListener
                }

                // Tworzymy mapę z danymi do dodania
                val updatedFields = mutableMapOf<String, Any>()
                if (!weightExists) updatedFields["weight"] = 0.0
                if (!heightExists) updatedFields["height"] = 0.0

                // Aktualizujemy dokument w Firestore
                userRef.update(updatedFields)
                    .addOnSuccessListener { Log.d("Firestore", "Dodatkowe pola dodane.") }
                    .addOnFailureListener { e -> Log.e("Firestore", "Błąd: ", e) }
            } else if (userType == "Doctor") {
                val doctoridexists = document.contains("Doctorid")
                val specializationexists = document.contains("Specialization")
                // Jeśli pola już istnieją, nie dodajemy ich ponownie
                if (doctoridexists && specializationexists) {
                    Log.d(
                        "Firestore",
                        "Pola doctorID i Specialization już istnieją. Pomijam aktualizację."
                    )
                    return@addOnSuccessListener
                }
                val updatedFields = mutableMapOf<String, Any>()
                if (!doctoridexists) updatedFields["Doctorid"] = ""
                if (!specializationexists) updatedFields["Specialization"] = ""
                userRef.update(updatedFields)
                    .addOnSuccessListener { Log.d("Firestore", "Dodatkowe pola dodane.") }
                    .addOnFailureListener { e -> Log.e("Firestore", "Błąd: ", e) }

            } else {
                Log.d("Firestore", "Dokument użytkownika nie istnieje.")
            }
    }
}
}
fun updateAllPatients() {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").get().addOnSuccessListener { result ->
        for (document in result) {
            val uid = document.id
            checkAndUpdateUserData(uid)
        }
    }.addOnFailureListener { e ->
        Log.e("Firestore", "Błąd pobierania użytkowników: ", e)
    }
}



