package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentJadwalBinding

class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JadwalAdapter
    private var currentDay = "Senin"
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener { (activity as? HomeActivity)?.backToHome() }
        adapter = JadwalAdapter(mutableListOf()) {}
        binding.rvJadwal.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJadwal.adapter = adapter
        setupTabLayout()
        loadJadwal()
    }

    private fun setupTabLayout() {
        binding.tabDays.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentDay = tab?.text.toString()
                binding.tvHariAktif.text = "📅  $currentDay"
                loadJadwal()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadJadwal() {
        val role    = arguments?.getString("USER_ROLE") ?: "siswa"
        val kelasId = arguments?.getString("KELAS_ID")
        val guruId  = arguments?.getString("GURU_ID")

        binding.progressBar.visibility = View.VISIBLE
        adapter.updateData(emptyList())

        val query = if (role == "guru") {
            db.collection("jadwal")
                .whereEqualTo("guruId", guruId)
                .whereEqualTo("hari", currentDay)
        } else {
            db.collection("jadwal")
                .whereEqualTo("kelasId", kelasId)
                .whereEqualTo("hari", currentDay)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { it.toObject(Jadwal::class.java)?.copy(id = it.id) }
                adapter.isGuruView = (role == "guru")
                adapter.updateData(list.distinctBy { "${it.namaMapel}-${it.waktuMulai}" })
                
                binding.progressBar.visibility = View.GONE
                if (list.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada jadwal hari $currentDay", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Koneksi gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}