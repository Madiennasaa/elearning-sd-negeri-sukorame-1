package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentDashboardWaliBinding

class DashboardWaliFragment : Fragment() {
    private var _binding: FragmentDashboardWaliBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var resolvedSiswaId: String = ""
    private var resolvedKelasId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardWaliBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMenuEnabled(false)
        loadChildData()
    }

    private fun loadChildData() {
        val user    = SessionManager(requireContext()).getUser()
        val siswaId = user?.idSiswa ?: ""

        if (siswaId.isEmpty()) {
            binding.tvChildName.text  = "Data anak tidak ditemukan"
            binding.tvChildKelas.text = "Kelas -"
            return
        }

        db.collection("siswa").document(siswaId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val siswa = doc.toObject(Siswa::class.java)?.copy(id = doc.id)
                if (siswa != null) {
                    resolvedSiswaId = siswa.id
                    resolvedKelasId = siswa.kelasId ?: ""
                    binding.tvChildName.text = siswa.namaLengkap ?: "Ananda"

                    if (resolvedKelasId.isNotEmpty()) {
                        fetchKelasName(resolvedKelasId)
                    } else {
                        binding.tvChildKelas.text = "Kelas -"
                    }

                    setupMenuListeners()
                    setMenuEnabled(true)
                } else {
                    binding.tvChildName.text  = "Data anak tidak ditemukan"
                    binding.tvChildKelas.text = "Kelas -"
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.tvChildName.text  = "Gagal memuat"
                binding.tvChildKelas.text = "Kelas -"
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchKelasName(kelasId: String) {
        db.collection("kelas").document(kelasId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val namaKelas = doc.toObject(Kelas::class.java)?.namaKelas
                binding.tvChildKelas.text = "Kelas ${namaKelas ?: kelasId}"
            }
            .addOnFailureListener {
                binding.tvChildKelas.text = "Kelas $kelasId"
            }
    }

    private fun setupMenuListeners() {
        val home = activity as? HomeActivity

        binding.menuNilaiAnak.setOnClickListener {
            home?.replaceFragment(NilaiFragment().apply {
                arguments = Bundle().apply {
                    putString("SISWA_ID", resolvedSiswaId)
                    putString("USER_ROLE", "wali_murid")
                }
            })
        }

        binding.menuAbsenAnak.setOnClickListener {
            home?.replaceFragment(AbsensiFragment().apply {
                arguments = Bundle().apply {
                    putString("SISWA_ID", resolvedSiswaId)
                    putString("USER_ROLE", "wali_murid")
                }
            })
        }

        binding.menuRaporAnak.setOnClickListener {
            home?.replaceFragment(RaporFragment().apply {
                arguments = Bundle().apply {
                    putString("SISWA_ID", resolvedSiswaId)
                    putString("USER_ROLE", "wali_murid")
                }
            })
        }

        binding.menuChatGuru.setOnClickListener {
            home?.replaceFragment(TanyaFragment().apply {
                arguments = Bundle().apply {
                    putString("SISWA_ID", resolvedSiswaId)
                    putString("USER_ROLE", "wali_murid")
                }
            })
        }
    }

    private fun setMenuEnabled(enabled: Boolean) {
        binding.menuNilaiAnak.isEnabled  = enabled
        binding.menuAbsenAnak.isEnabled  = enabled
        binding.menuRaporAnak.isEnabled  = enabled
        binding.menuChatGuru.isEnabled   = enabled
        binding.menuNilaiAnak.alpha  = if (enabled) 1f else 0.4f
        binding.menuAbsenAnak.alpha  = if (enabled) 1f else 0.4f
        binding.menuRaporAnak.alpha  = if (enabled) 1f else 0.4f
        binding.menuChatGuru.alpha   = if (enabled) 1f else 0.4f
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
