package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Rapor
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentRaporBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list.ListAdapter

class RaporFragment : Fragment() {
    private var _binding: FragmentRaporBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRaporBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val role    = arguments?.getString("USER_ROLE") ?: "siswa"
        val siswaId = arguments?.getString("SISWA_ID")
            ?: arguments?.getInt("SISWA_ID")?.takeIf { it > 0 }?.toString()
            ?: SessionManager(requireContext()).getUser()?.idSiswa

        binding.rvRapor.layoutManager = LinearLayoutManager(requireContext())

        if (siswaId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        loadRapor(siswaId)
    }

    private fun loadRapor(siswaId: String) {
        db.collection("rapor")
            .whereEqualTo("siswaId", siswaId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Rapor::class.java)?.copy(id = doc.id)
                }
                if (data.isEmpty()) {
                    Toast.makeText(requireContext(), "Rapor kosong", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                binding.rvRapor.adapter = ListAdapter(
                    data.map {
                        "Semester ${it.semester} - ${it.tahunAjaran}" to
                                "Hadir: ${it.totalHadir} | Status: ${it.statusNaik ?: "-"}\n${it.catatanWali ?: ""}"
                    }
                ) { pos ->
                    val item = data[pos]
                    
                    // Fetch grades for this student and semester to show in dialog
                    db.collection("nilai")
                        .whereEqualTo("siswaId", item.siswaId)
                        .whereEqualTo("semester", item.semester)
                        .get()
                        .addOnSuccessListener { nSnap ->
                            val grades = nSnap.documents.mapNotNull { it.toObject(com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Nilai::class.java) }
                            val gradeList = if (grades.isNotEmpty()) {
                                "\n\nDaftar Nilai:\n" + grades.joinToString("\n") { "• ${it.namaMapel}: ${it.nilai}" }
                            } else "\n\nBelum ada data nilai."

                            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("🏅 Rapor Hasil Belajar")
                                .setMessage("Halo, Ananda!\n\n" +
                                         "Semester: ${item.semester}\n" +
                                         "Tahun: ${item.tahunAjaran}\n\n" +
                                         "📊 Rekap Kehadiran:\n" +
                                         "• Hadir: ${item.totalHadir}\n" +
                                         "• Sakit: ${item.totalSakit}\n" +
                                         "• Izin: ${item.totalIzin}\n" +
                                         "• Alpha: ${item.totalAlpha}\n" +
                                         gradeList + "\n\n" +
                                         "📝 Catatan Guru:\n${item.catatanWali ?: "-"}\n\n" +
                                         "Rajin pangkal pandai, semangat terus! 🌟")
                                .setPositiveButton("Terima Kasih!", null)
                                .show()
                        }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat rapor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { 
        super.onDestroyView()
        _binding = null 
    }
}