package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val user    = session.getUser()
        val kelasId = user?.kelasId

        binding.tvGreeting.text = "Halo, ${user?.name ?: "Adik"}! 👋"
        binding.tvKelas.text    = "Memuat kelas... • SD Negeri Sukorame 1"

        setupMenuListeners()
        loadKelasName(kelasId)
    }

    private fun setupMenuListeners() {
        binding.menuMateri.setOnClickListener {
            (activity as? HomeActivity)?.navigateToList("MATERI")
        }
        binding.menuTugas.setOnClickListener {
            (activity as? HomeActivity)?.navigateToList("TUGAS")
        }
    }

    private fun loadKelasName(kelasId: String?) {
        if (kelasId.isNullOrEmpty()) {
            binding.tvKelas.text = "Kelas - • SD Negeri Sukorame 1"
            return
        }

        db.collection("kelas").document(kelasId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val kelas = doc.toObject(Kelas::class.java)
                val namaKelas = kelas?.namaKelas
                if (namaKelas != null) {
                    binding.tvKelas.text = "Kelas $namaKelas • SD Negeri Sukorame 1"
                } else {
                    binding.tvKelas.text = "Kelas $kelasId • SD Negeri Sukorame 1"
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                binding.tvKelas.text = "Kelas $kelasId • SD Negeri Sukorame 1"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
