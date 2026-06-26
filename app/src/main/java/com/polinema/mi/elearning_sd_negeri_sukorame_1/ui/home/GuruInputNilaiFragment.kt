package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentGuruInputNilaiBinding

class GuruInputNilaiFragment : Fragment() {
    private var _binding: FragmentGuruInputNilaiBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager

    private val nilaiMap = mutableMapOf<String, Double>()
    private var mapelList = mutableListOf<MataPelajaranData>()
    private var kelasList = mutableListOf<Kelas>()
    private var guruId = ""
    private var kelasId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuruInputNilaiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        guruId = user?.idGuru ?: ""
        
        if (guruId.isEmpty()) {
            Toast.makeText(requireContext(), "Data guru tidak ditemukan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        fetchKelasInfo()
        binding.btnSimpanNilai.setOnClickListener { simpanNilai() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun fetchKelasInfo() {
        db.collection("kelas")
            .whereEqualTo("guruId", guruId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                }
                if (list.isNotEmpty()) {
                    kelasList.clear()
                    kelasList.addAll(list)
                    
                    val classNames = kelasList.map { it.namaKelas ?: "Kelas ${it.id}" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerKelas.adapter = adapter
                    
                    binding.spinnerKelas.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            kelasId = kelasList[position].id
                            setupStudentList()
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    
                    setupSpinners()
                } else {
                    Toast.makeText(requireContext(), "Guru belum memiliki kelas atau jadwal.", Toast.LENGTH_LONG).show()
                    binding.btnSimpanNilai.isEnabled = false
                    binding.labelKelas.visibility = View.GONE
                    binding.spinnerKelas.visibility = View.GONE
                    binding.dividerKelas.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat data kelas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSpinners() {
        db.collection("mata_pelajaran")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MataPelajaranData::class.java)?.copy(id = doc.id)
                }
                mapelList.clear()
                mapelList.addAll(list)
                
                val mapelNames = mapelList.map { it.nama }
                binding.spinnerMapel.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    mapelNames
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat mata pelajaran: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        val jenisNilai = listOf("Tugas", "UH", "UTS", "UAS")
        binding.spinnerJenisNilai.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            jenisNilai
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupStudentList() {
        db.collection("siswa")
            .whereEqualTo("kelasId", kelasId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val students = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Siswa::class.java)?.copy(id = doc.id)
                }
                if (students.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada siswa di kelas ini", Toast.LENGTH_SHORT).show()
                    binding.btnSimpanNilai.isEnabled = false
                } else {
                    binding.btnSimpanNilai.isEnabled = true
                    binding.rvInputNilai.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvInputNilai.adapter = InputNilaiAdapter(students) { sId, nilai ->
                        nilaiMap[sId] = nilai
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanNilai() {
        if (mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada mata pelajaran tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        if (nilaiMap.isEmpty()) {
            Toast.makeText(requireContext(), "Belum ada nilai yang diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val mapelId = mapelList[binding.spinnerMapel.selectedItemPosition].id
        val mapelNama = mapelList[binding.spinnerMapel.selectedItemPosition].nama
        val jenisNilai = binding.spinnerJenisNilai.selectedItem?.toString() ?: return

        val batch = db.batch()
        
        nilaiMap.forEach { (siswaId, nilaiVal) ->
            val docRef = db.collection("nilai").document()
            val data = hashMapOf(
                "siswaId" to siswaId,
                "namaMapel" to mapelNama,
                "mataPelajaranId" to mapelId,
                "guruId" to guruId,
                "jenisNilai" to jenisNilai,
                "nilai" to nilaiVal,
                "semester" to "1"
            )
            batch.set(docRef, data)
        }

        batch.commit()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Semua nilai berhasil disimpan!", Toast.LENGTH_SHORT).show()
                nilaiMap.clear()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menyimpan nilai: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
