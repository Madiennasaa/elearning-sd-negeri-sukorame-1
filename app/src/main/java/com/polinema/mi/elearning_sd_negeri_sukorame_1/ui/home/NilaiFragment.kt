package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Nilai
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentNilaiBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list.ListAdapter

class NilaiFragment : Fragment() {
    private var _binding: FragmentNilaiBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNilaiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // BUG FIX: NilaiFragment sebelumnya selalu pakai idSiswa dari session,
        // sehingga wali_murid yang membuka fragment ini tidak melihat nilai anaknya.
        // Sekarang argumen SISWA_ID dari caller (wali) diprioritaskan.
        val siswaId = arguments?.getString("SISWA_ID")
            ?: sessionManager.getUser()?.idSiswa

        if (siswaId == null) {
            Toast.makeText(requireContext(), "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        loadNilai(siswaId)
    }

    private fun loadNilai(siswaId: String) {
        db.collection("nilai")
            .whereEqualTo("siswaId", siswaId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                // FIX: Gunakan mapNotNull + copy(id = doc.id) agar field id terisi dari document ID Firestore
                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Nilai::class.java)?.copy(id = doc.id)
                }
                if (data.isNotEmpty()) {
                    binding.rvNilai.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvNilai.adapter = ListAdapter(
                        data.map { "${it.namaMapel}: ${it.nilai}" to (it.jenisNilai ?: "-") }
                    ) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("⭐ Capaian Belajar")
                            .setMessage("Keren! Ini hasil belajarmu:\n\n" +
                                     "Mata Pelajaran: ${item.namaMapel}\n" +
                                     "Jenis: ${item.jenisNilai ?: "-"}\n" +
                                     "Nilai: ${item.nilai}\n\n" +
                                     "Terus tingkatkan prestasimu ya! 🚀")
                            .setPositiveButton("Hebat!", null)
                            .show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Belum ada data nilai", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat nilai: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}