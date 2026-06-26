package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Absensi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAbsensiBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list.ListAdapter

class AbsensiFragment : Fragment() {
    private var _binding: FragmentAbsensiBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAbsensiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // BUG FIX: AbsensiFragment sebelumnya selalu pakai idSiswa dari session,
        // sehingga wali_murid tidak bisa melihat kehadiran anaknya.
        // Sekarang argumen SISWA_ID dari caller diprioritaskan.
        val siswaId = arguments?.getString("SISWA_ID")
            ?: sessionManager.getUser()?.idSiswa

        if (siswaId == null) {
            Toast.makeText(requireContext(), "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        loadAbsensi(siswaId)
    }

    private fun loadAbsensi(siswaId: String) {
        db.collection("absensi")
            .whereEqualTo("siswaId", siswaId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                // FIX: Gunakan mapNotNull + copy(id = doc.id) agar field id terisi dari document ID Firestore
                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Absensi::class.java)?.copy(id = doc.id)
                }
                if (data.isNotEmpty()) {
                    binding.rvAbsensi.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvAbsensi.adapter = ListAdapter(
                        data.map { (it.tanggal ?: "-") to "Status: ${it.status} (${it.namaKelas ?: "-"})" }
                    ) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("📅 Catatan Kehadiran")
                            .setMessage("Halo, Ananda!\n\nTanggal: ${item.tanggal}\n" +
                                     "Status: ${item.status}\n\n" +
                                     "Keterangan:\n${item.keterangan ?: "-"}\n\nTetap rajin sekolah ya! ✨")
                            .setPositiveButton("Siap!", null)
                            .show()
                    }

                    // BUG FIX: status comparison case-sensitive — normalise ke lowercase
                    binding.tvHadirCount.text = data.count { it.status?.equals("Hadir", ignoreCase = true) == true }.toString()
                    binding.tvSakitCount.text = data.count { it.status?.equals("Sakit", ignoreCase = true) == true }.toString()
                    binding.tvIzinCount.text  = data.count { it.status?.equals("Izin",  ignoreCase = true) == true }.toString()
                    binding.tvAlphaCount.text = data.count { it.status?.equals("Alpha", ignoreCase = true) == true }.toString()
                } else {
                    Toast.makeText(requireContext(), "Belum ada data absensi", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat absensi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}