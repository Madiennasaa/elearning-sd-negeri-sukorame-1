package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
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
    private var guruUid = ""
    private var kelasId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGuruInputNilaiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        guruUid = sessionManager.getUser()?.uid ?: ""
        
        if (guruUid.isEmpty()) {
            Toast.makeText(requireContext(), "Sesi tidak valid", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        fetchKelasInfo()
        binding.btnSimpanNilai.setOnClickListener { simpanNilai() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun fetchKelasInfo() {
        db.collection("kelas").whereEqualTo("guruId", guruUid).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }
                if (list.isNotEmpty()) {
                    kelasList.clear()
                    kelasList.addAll(list)
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kelasList.map { it.namaKelas ?: it.id })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerKelas.adapter = adapter
                    binding.spinnerKelas.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            kelasId = kelasList[pos].id
                            loadMapelByJadwal()
                            setupStudentList()
                        }
                        override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                    }
                }
            }
    }

    private fun loadMapelByJadwal() {
        db.collection("jadwal").whereEqualTo("guruId", guruUid).whereEqualTo("kelasId", kelasId).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val j = doc.toObject(Jadwal::class.java)
                    if (j != null) MataPelajaranData(id = j.mapelId ?: "", nama = j.namaMapel ?: "") else null
                }.distinctBy { it.id }
                mapelList.clear()
                mapelList.addAll(list)
                binding.spinnerMapel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mapelList.map { it.nama })
                    .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                
                binding.spinnerJenisNilai.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Tugas", "UH", "UTS", "UAS"))
                    .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
    }

    private fun setupStudentList() {
        db.collection("users").whereEqualTo("role", "siswa").whereEqualTo("kelasId", kelasId).get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val students = snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
                students.forEach { if (!nilaiMap.containsKey(it.uid)) nilaiMap[it.uid] = 0.0 }
                binding.rvInputNilai.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = InputNilaiAdapter(students) { sId, nilai -> nilaiMap[sId] = nilai }
                }
            }
    }

    private fun simpanNilai() {
        val mapelPos = binding.spinnerMapel.selectedItemPosition
        if (mapelPos < 0 || mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Pilih mata pelajaran", Toast.LENGTH_SHORT).show()
            return
        }

        val mapel = mapelList[mapelPos]
        val jenis = binding.spinnerJenisNilai.selectedItem?.toString() ?: "Tugas"
        
        if (nilaiMap.isEmpty()) {
            Toast.makeText(requireContext(), "Isi minimal satu nilai", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpanNilai.isEnabled = false
        val batch = db.batch()

        try {
            nilaiMap.forEach { (siswaId, nilaiVal) ->
                val docRef = db.collection("nilai").document()
                val payload = hashMapOf(
                    "siswaId" to siswaId,
                    "guruId" to guruUid,
                    "kelasId" to kelasId,
                    "mapelId" to mapel.id,
                    "namaMapel" to mapel.nama,
                    "jenisNilai" to jenis,
                    "nilai" to (nilaiVal ?: 0.0),
                    "semester" to "1",
                    "tahunAjaran" to "2024/2025",
                    "createdAt" to Timestamp.now()
                )
                batch.set(docRef, payload)
            }

            batch.commit()
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Nilai berhasil disimpan", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        binding.btnSimpanNilai.isEnabled = true
                        Log.e("SAVE_ERROR", "Error: ${e.message}")
                        Toast.makeText(requireContext(), "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            binding.btnSimpanNilai.isEnabled = true
            Toast.makeText(requireContext(), "Kesalahan sistem: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
