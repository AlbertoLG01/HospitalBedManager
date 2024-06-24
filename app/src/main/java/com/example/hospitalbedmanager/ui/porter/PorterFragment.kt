package com.example.hospitalbedmanager.ui.porter

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalbedmanager.R
import com.example.hospitalbedmanager.databinding.FragmentPorterBinding
import com.example.hospitalbedmanager.dataclasses.Bed
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PorterFragment : Fragment() {

    private var _binding: FragmentPorterBinding? = null
    private val binding get() = _binding!!

    private lateinit var occupiedBedAdapter: OccupiedBedAdapter
    private val bedList = mutableListOf<Bed>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPorterBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        fetchBeds()

        return root
    }

    private fun setupRecyclerView() {
        occupiedBedAdapter = OccupiedBedAdapter(requireContext(), bedList) { bed ->
            showUnassignDialog(bed)
        }
        binding.recyclerViewCamas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = occupiedBedAdapter
        }
    }

    private fun fetchBeds() {
        bedList.clear()
        db.collection("beds")
            .whereEqualTo("isOccupied", true)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val bed = document.toObject(Bed::class.java)
                    bedList.add(bed)
                }
                occupiedBedAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al obtener camas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUnassignDialog(bed: Bed) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_unassign_bed, null)

        builder.setView(dialogView)
            .setTitle("Desasignar Cama num. ${bed.number}")
            .setPositiveButton("Liberar") { _, _ ->
                unassignBed(bed.number)
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    private fun unassignBed(bedNumber: Int) {
        db.collection("beds")
            .whereEqualTo("number", bedNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "Cama no encontrada", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Asumiendo que solo hay un documento con este número de cama
                val bedDoc = documents.documents[0]
                val bedRef = bedDoc.reference

                bedRef.update(
                    mapOf(
                        "isOccupied" to false,
                    )
                ).addOnSuccessListener {
                    Toast.makeText(context, "Cama liberada exitosamente", Toast.LENGTH_SHORT).show()
                    // Elimina la cama de la lista local
                    bedList.removeIf { it.number == bedNumber }
                    occupiedBedAdapter.notifyDataSetChanged()
                }.addOnFailureListener { exception ->
                    Toast.makeText(context, "Error al desasignar cama: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al buscar cama: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class OccupiedBedAdapter(
        private val context: Context,
        private val bedList: List<Bed>,
        private val onAssignClickListener: (Bed) -> Unit
    ) : RecyclerView.Adapter<OccupiedBedAdapter.OccupiedBedViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccupiedBedViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_occupied_bed, parent, false)
            return OccupiedBedViewHolder(view)
        }

        override fun onBindViewHolder(holder: OccupiedBedViewHolder, position: Int) {
            val bed = bedList[position]
            holder.bedNumber.text = "Cama número ${bed.number}"
            // Formatear la fecha de asignación
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val assignmentDate = bed.assignmentDate.toDate()
            val formattedDate = sdf.format(assignmentDate)
            holder.lastAssignment.text = "Fecha de Asignación: $formattedDate"

            if(bed.consultationAssociated != -1) {
                holder.consultationNumber.text =
                    "Número de consulta asociada: ${bed.consultationAssociated}"
            }
            else{
                holder.consultationNumber.text =
                    "Número de última consulta asociada: No hay registro"
            }

            if(bed.patientAssociated != "") {
                holder.patientAssociated.text =
                    "Paciente asignado: ${bed.patientAssociated}"
            }
            else{
                holder.patientAssociated.text =
                    "Paciente asignado: No hay registro"
            }

            holder.unassignButton.setOnClickListener {
                onAssignClickListener(bed)
            }
        }

        override fun getItemCount(): Int {
            return bedList.size
        }

        class OccupiedBedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bedNumber: TextView = itemView.findViewById(R.id.text_bed_number)
            val lastAssignment: TextView = itemView.findViewById(R.id.text_last_assignment)
            val consultationNumber: TextView = itemView.findViewById(R.id.text_consultation_number)
            val unassignButton: Button = itemView.findViewById(R.id.button_unassign)
            val patientAssociated: TextView = itemView.findViewById(R.id.text_patient_associated)
        }
    }

}