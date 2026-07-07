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
        val userId = user?.uid ?: ""
        val guruId = user?.uid ?: ""

        binding.tvGreeting.text = "Halo, ${user?.name ?: "Bapak/Ibu Guru"}!"

        val homeActivity = activity as? HomeActivity

        fun makeArgs() = Bundle().apply {
            putString("USER_ID", userId)
            putString("GURU_ID", guruId)
        }

        binding.btnJadwalGuru.setOnClickListener {
            homeActivity?.navigateToList("JADWAL")
        }

        binding.menuInputNilai.setOnClickListener {
            homeActivity?.replaceFragment(GuruInputNilaiFragment().apply { arguments = makeArgs() })
        }

        binding.menuInputAbsen.setOnClickListener {
            homeActivity?.replaceFragment(GuruInputAbsensiFragment().apply { arguments = makeArgs() })
        }

        binding.menuUploadMateri.setOnClickListener {
            homeActivity?.replaceFragment(GuruInputMateriFragment().apply { arguments = makeArgs() })
        }

        binding.menuInputTugas.setOnClickListener {
            homeActivity?.replaceFragment(GuruInputTugasFragment().apply { arguments = makeArgs() })
        }

        binding.menuRapor.setOnClickListener {
            homeActivity?.replaceFragment(GuruRaporFragment().apply { arguments = makeArgs() })
        }

        binding.menuDaftarSiswa.setOnClickListener {
            homeActivity?.replaceFragment(GuruDaftarSiswaFragment().apply { arguments = makeArgs() })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
