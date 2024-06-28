package com.example.hospitalbedmanager.ui.nurse

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalbedmanager.MainActivity
import com.example.hospitalbedmanager.R
import com.example.hospitalbedmanager.databinding.FragmentNurseBinding
import com.example.hospitalbedmanager.dataclasses.Bed
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Locale

class NurseFragment : Fragment() {

    private var _binding: FragmentNurseBinding? = null
    private val binding get() = _binding!!

    private lateinit var freeBedAdapter: FreeBedAdapter
    private val bedList = mutableListOf<Bed>()
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fetchBedsRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNurseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val activity = requireActivity() as MainActivity

        activity.hideFab()

        setupRecyclerView()
        startFetchingBeds()

        return root
    }

    private fun setupRecyclerView() {
        freeBedAdapter = FreeBedAdapter(requireContext(), bedList) { bed ->
            showAssignDialog(bed)
        }
        binding.recyclerViewCamas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = freeBedAdapter
        }
    }

    private fun fetchBeds() {
        bedList.clear()
        db.collection("beds")
            .whereEqualTo("occupied", false)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val bed = document.toObject(Bed::class.java)
                    bedList.add(bed)
                }
                bedList.sortBy { it.number }
                freeBedAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al obtener camas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startFetchingBeds() {
        fetchBedsRunnable = object : Runnable {
            override fun run() {
                fetchBeds()
                handler.postDelayed(this, 2000) // 2000 ms = 2 segundos
            }
        }
        handler.post(fetchBedsRunnable)
    }

    private fun stopFetchingBeds() {
        handler.removeCallbacks(fetchBedsRunnable)
    }

    private fun showAssignDialog(bed: Bed) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_assign_bed, null)
        val editTextPatientId = dialogView.findViewById<EditText>(R.id.editTextPatientId)
        val editTextConsultationNumber = dialogView.findViewById<EditText>(R.id.editTextConsultationNumber)

        builder.setView(dialogView)
            .setTitle("Asignar Cama número ${bed.number}")
            .setPositiveButton("Asignar") { _, _ ->
                val patientName = editTextPatientId.text.toString()
                val consultationNumber = editTextConsultationNumber.text.toString().toInt()

                if (patientName.isNotEmpty() && consultationNumber != -1) {
                    assignBed(bed.number, patientName, consultationNumber)
                } else {
                    Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    private fun assignBed(bedNumber: Int, patientName: String, consultationNumber: Int) {
        val bedQuery = db.collection("beds")
            .whereEqualTo("number", bedNumber)
            .limit(1)

        bedQuery.get().addOnSuccessListener { bedSnapshot ->
            if (bedSnapshot.isEmpty) {
                Toast.makeText(context, "Cama no encontrada", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val bedDoc = bedSnapshot.documents[0]
            val bedRef = bedDoc.reference

            db.runTransaction { transaction ->
                val snapshot = transaction.get(bedRef)

                // Verificar si la cama ya está ocupada
                if (snapshot.getBoolean("occupied") == true) {
                    throw FirebaseFirestoreException("Cama ya está ocupada", FirebaseFirestoreException.Code.ABORTED)
                }

                // Actualizar la cama
                transaction.update(bedRef, mapOf(
                    "patientAssociated" to patientName,
                    "consultationAssociated" to consultationNumber,
                    "occupied" to true,
                    "assignmentDate" to Timestamp.now()
                ))

                null
            }.addOnSuccessListener {
                Toast.makeText(context, "Cama asignada exitosamente", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.ABORTED) {
                    Toast.makeText(context, exception.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al asignar cama: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "Error al buscar cama: ${exception.message}", Toast.LENGTH_SHORT).show()
        }

        //fetchBeds()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopFetchingBeds()
        _binding = null
    }

    class FreeBedAdapter(
        private val context: Context,
        private val bedList: List<Bed>,
        private val onAssignClickListener: (Bed) -> Unit
    ) : RecyclerView.Adapter<FreeBedAdapter.FreeBedViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreeBedViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_free_bed, parent, false)
            return FreeBedViewHolder(view)
        }

        override fun onBindViewHolder(holder: FreeBedViewHolder, position: Int) {
            val bed = bedList[position]
            holder.bedNumber.text = "Cama número ${bed.number}"
            // Formatear la fecha de asignación
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val assignmentDate = bed.assignmentDate.toDate()
            val formattedDate = sdf.format(assignmentDate)
            holder.lastAssignment.text = "Última Asignación: $formattedDate"

            if (bed.consultationAssociated != -1) {
                holder.consultationNumber.text =
                    "Número de última consulta asociada: ${bed.consultationAssociated}"
            } else {
                holder.consultationNumber.text =
                    "Número de última consulta asociada: No hay registro"
            }

            holder.assignButton.setOnClickListener {
                onAssignClickListener(bed)
            }
        }

        override fun getItemCount(): Int {
            return bedList.size
        }

        class FreeBedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bedNumber: TextView = itemView.findViewById(R.id.text_bed_number)
            val lastAssignment: TextView = itemView.findViewById(R.id.text_last_assignment)
            val consultationNumber: TextView = itemView.findViewById(R.id.text_consultation_number)
            val assignButton: Button = itemView.findViewById(R.id.button_assign)
        }
    }
}
