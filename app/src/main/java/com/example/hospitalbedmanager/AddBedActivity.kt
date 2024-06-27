package com.example.hospitalbedmanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hospitalbedmanager.dataclasses.Bed
import com.google.firebase.firestore.FirebaseFirestore

class AddBedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_bed)
        supportActionBar?.title = "Crear nueva cama"

        db = FirebaseFirestore.getInstance()

        val numberEditText: EditText = findViewById(R.id.edit_text_bed_number)
        val addButton: Button = findViewById(R.id.button_add_bed)
        val cancelButton: Button = findViewById(R.id.button_cancel)

        addButton.setOnClickListener {
            val bedNumber = numberEditText.text.toString().toIntOrNull()

            if (bedNumber != null) {
                db.collection("beds")
                    .whereEqualTo("number", bedNumber)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            val newBed = Bed(number = bedNumber)
                            db.collection("beds")
                                .add(newBed)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Cama añadida con éxito", Toast.LENGTH_SHORT).show()
                                    finish() // Volver a la actividad anterior
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al añadir cama: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Ya existe una cama con ese número", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al verificar número de cama: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish() // Volver a la actividad anterior
        }
    }
}
