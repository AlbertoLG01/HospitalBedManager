package com.example.hospitalbedmanager.ui.doctor

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalbedmanager.MainActivity
import com.example.hospitalbedmanager.R
import com.example.hospitalbedmanager.databinding.FragmentDoctorBinding
import com.example.hospitalbedmanager.dataclasses.Bed
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorFragment : Fragment() {

    private var _binding: FragmentDoctorBinding? = null
    private val binding get() = _binding!!

    private lateinit var totalBedAdapter: TotalBedAdapter
    private val bedList = mutableListOf<Bed>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val activity = requireActivity() as MainActivity

        activity.showFab()

        setupRecyclerView()
        fetchBeds()

        return root
    }

    private fun setupRecyclerView() {
        totalBedAdapter = TotalBedAdapter(requireContext(), bedList) { bed ->
            showDeleteDialog(bed)
        }
        binding.recyclerViewCamas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = totalBedAdapter
        }
    }

    private fun fetchBeds() {
        bedList.clear()
        db.collection("beds")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    var bed = document.toObject(Bed::class.java)
                    bedList.add(bed)
                }
                bedList.sortBy { it.number }
                totalBedAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error al obtener camas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteDialog(bed: Bed) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_bed, null)

        builder.setView(dialogView)
            .setTitle("Eliminar Cama Num ${bed.number}")
            .setPositiveButton("Eliminar") { _, _ ->

                deleteBed(bed.number)
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    private fun deleteBed(bedNumber: Int) {
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

                bedRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Cama eliminada exitosamente", Toast.LENGTH_SHORT).show()
                    // Elimina la cama de la lista local
                    bedList.removeIf { it.number == bedNumber }
                    totalBedAdapter.notifyDataSetChanged()
                }.addOnFailureListener { exception ->
                    Toast.makeText(context, "Error al eliminar cama: ${exception.message}", Toast.LENGTH_SHORT).show()
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

    class TotalBedAdapter(
        private val context: Context,
        private val bedList: List<Bed>,
        private val onDeleteClickListener: (Bed) -> Unit
    ) : RecyclerView.Adapter<TotalBedAdapter.TotalBedViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TotalBedViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_bed, parent, false)
            return TotalBedViewHolder(view)
        }

        override fun onBindViewHolder(holder: TotalBedViewHolder, position: Int) {
            val bed = bedList[position]
            holder.bedNumber.text = "Cama número ${bed.number}"
            // Formatear la fecha de asignación
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val assignmentDate = bed.assignmentDate.toDate()
            val formattedDate = sdf.format(assignmentDate)
            holder.lastAssignment.text = "Última Asignación: $formattedDate"

            if(bed.occupied) {
                holder.isBedOccupied.text =
                    "Cama Ocupada"
                holder.isBedOccupied.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
            else{
                holder.isBedOccupied.text =
                    "Cama Libre"
                holder.isBedOccupied.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            }

            holder.deleteButton.setOnClickListener {
                onDeleteClickListener(bed)
            }
        }

        override fun getItemCount(): Int {
            return bedList.size
        }

        class TotalBedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bedNumber: TextView = itemView.findViewById(R.id.text_bed_number)
            val lastAssignment: TextView = itemView.findViewById(R.id.text_last_assignment)
            val isBedOccupied: TextView = itemView.findViewById(R.id.text_bed_occupied)
            val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        }
    }
}