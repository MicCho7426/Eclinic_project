package com.example.eclinic

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEditText = findViewById<EditText>(R.id.editTextName)
        val surnameEditText = findViewById<EditText>(R.id.editTextSurname)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
        val addressEditText = findViewById<EditText>(R.id.editTextAddress)
        val dobTextView = findViewById<TextView>(R.id.textViewDOB)
        val ageTextView = findViewById<TextView>(R.id.textViewAge)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        dobTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val dob = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dobTextView.text = dob

                val age = year - selectedYear
                ageTextView.text = "Age: $age"
            }, year, month, day)

            datePickerDialog.show()
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            val dob = dobTextView.text.toString().trim()
            val age = ageTextView.text.toString().replace("Age: ", "").trim()

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userMap = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "email" to email,
                            "phone" to phone,
                            "address" to address,
                            "dob" to dob,
                            "age" to age
                        )
                        userId?.let {
                            db.collection("users").document(it).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}