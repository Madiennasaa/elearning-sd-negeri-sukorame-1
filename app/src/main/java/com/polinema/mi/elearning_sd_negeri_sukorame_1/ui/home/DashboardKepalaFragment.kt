package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentDashboardKepalaBinding

class DashboardKepalaFragment : Fragment() {

    private var _binding: FragmentDashboardKepalaBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardKepalaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        loadStats()
        setupGreeting()

        val homeActivity = activity as? HomeActivity

        binding.menuMonitorAbsen.setOnClickListener {
            homeActivity?.replaceFragment(KepalaMonitorAbsenFragment())
        }
        binding.menuMonitorNilai.setOnClickListener {
            homeActivity?.replaceFragment(KepalaMonitorNilaiFragment())
        }
        binding.menuLaporan.setOnClickListener {
            homeActivity?.replaceFragment(KepalaLaporanFragment())
        }
        binding.menuInfoSekolah.setOnClickListener {
            homeActivity?.replaceFragment(KepalaInfoSekolahFragment())
        }
    }

    private fun setupGreeting() {
        val user = sessionManager.getUser()
        binding.tvGreeting.text = "Halo, ${user?.name ?: "Kepala Sekolah"}!"
    }

    private fun loadStats() {
        db.collection("users").whereEqualTo("role", "guru").get().addOnSuccessListener { guruSnap ->
            if (!isAdded) return@addOnSuccessListener
            binding.tvTotalGuru.text = guruSnap.size().toString()
            
            db.collection("siswa").get().addOnSuccessListener { siswaSnap ->
                if (!isAdded) return@addOnSuccessListener
                binding.tvTotalSiswa.text = siswaSnap.size().toString()
                
                db.collection("kelas").get().addOnSuccessListener { kelasSnap ->
                    if (!isAdded) return@addOnSuccessListener
                    binding.tvTotalKelas.text = kelasSnap.size().toString()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memuat statistik", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}