package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentDashboardGuruBinding

class DashboardGuruFragment : Fragment() {
    private var _binding: FragmentDashboardGuruBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        val userId = user?.uid ?: 0
        val guruId = user?.idGuru ?: 0

        binding.tvGreeting.text = "Halo, ${user?.name ?: "Pak Guru"}!"

        val homeActivity = activity as? HomeActivity

        // Helper untuk membuat Bundle berisi USER_ID + GURU_ID sekaligus
        fun makeArgs() = Bundle().apply {
            putString("USER_ID", userId.toString())
            putString("GURU_ID", guruId?.toString())
        }

        // Lihat Jadwal
        binding.btnJadwalGuru.setOnClickListener {
            homeActivity?.navigateToList("JADWAL")
        }

        // Input Nilai
        binding.menuInputNilai.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruInputNilaiFragment().apply { arguments = makeArgs() }
            )
        }

        // Input Absensi
        binding.menuInputAbsen.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruInputAbsensiFragment().apply { arguments = makeArgs() }
            )
        }

        // Upload Materi (link video YouTube)
        binding.menuUploadMateri.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruInputMateriFragment().apply { arguments = makeArgs() }
            )
        }

        // Input Tugas + Soal
        binding.menuInputTugas.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruInputTugasFragment().apply { arguments = makeArgs() }
            )
        }

        // Rapor Siswa
        binding.menuRapor.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruRaporFragment().apply { arguments = makeArgs() }
            )
        }

        // Daftar Siswa di kelas
        binding.menuDaftarSiswa.setOnClickListener {
            homeActivity?.replaceFragment(
                GuruDaftarSiswaFragment().apply { arguments = makeArgs() }
            )
        }
    }

    private fun fetchGuruKelasAndNavigate(home: HomeActivity?, type: String) {
        val user = sessionManager.getUser()
        val gId = user?.idGuru ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val kId = doc.getString("kelasId") ?: ""
                if (kId.isNotEmpty()) {
                    home?.replaceFragment(ListFragment.newInstance(type, kId, ""))
                } else {
                    android.widget.Toast.makeText(requireContext(), "Anda tidak terdaftar di kelas manapun", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
