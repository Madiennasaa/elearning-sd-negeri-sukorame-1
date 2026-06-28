package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageJadwalBinding

class AdminManageJadwalFragment : Fragment() {

    private var _binding: FragmentAdminManageJadwalBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JadwalAdapter
    private val db = FirebaseFirestore.getInstance()

    private var guruList  = listOf<User>()
    private var kelasList = listOf<Kelas>()
    private var mapelList = listOf<MataPelajaranData>()
    private val hariList  = listOf("Senin","Selasa","Rabu","Kamis","Jumat","Sabtu")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = JadwalAdapter(mutableListOf()) { jadwal ->
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Jadwal")
                .setMessage("Yakin hapus jadwal ini?")
                .setPositiveButton("Hapus") { _, _ ->
                    db.collection("jadwal").document(jadwal.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                            loadJadwal()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal hapus", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal", null).show()
        }
        binding.rvManageJadwal.layoutManager = LinearLayoutManager(requireContext())
        binding.rvManageJadwal.adapter = adapter

        binding.btnSimpanJadwal.setOnClickListener { simpanJadwal() }

        loadMasterData()
    }

    private fun loadMasterData() {
        db.collection("users").whereEqualTo("role", "guru").get().addOnSuccessListener { gSnap ->
            if (!isAdded) return@addOnSuccessListener
            guruList = gSnap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }

            db.collection("kelas").get().addOnSuccessListener { kSnap ->
                if (!isAdded) return@addOnSuccessListener
                kelasList = kSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }

                db.collection("mapel").get().addOnSuccessListener { mSnap ->
                    if (!isAdded) return@addOnSuccessListener
                    mapelList = mSnap.documents.mapNotNull { it.toObject(MataPelajaranData::class.java)?.copy(id = it.id) }

                    setupSpinners()
                    loadJadwal()
                }
            }
        }.addOnFailureListener {
            if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data master", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        binding.spinnerGuru.adapter  = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, guruList.map { it.name ?: "-" }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, kelasList.map { it.namaKelas ?: "-" }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerMapel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mapelList.map { it.nama }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerHari.adapter  = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, hariList).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun loadJadwal() {
        db.collection("jadwal").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val data = snapshot.documents.mapNotNull { it.toObject(Jadwal::class.java)?.copy(id = it.id) }
                    .filter { !it.namaMapel.isNullOrEmpty() && !it.namaKelas.isNullOrEmpty() }
                adapter.updateData(data)
            }
    }

    private fun simpanJadwal() {
        val jamMulai = binding.etJamMulai.text.toString().trim()
        val jamSelesai = binding.etJamSelesai.text.toString().trim()

        if (jamMulai.isEmpty() || jamSelesai.isEmpty()) {
            Toast.makeText(requireContext(), "Jam harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (guruList.isEmpty() || kelasList.isEmpty() || mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Data master belum dimuat", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedGuru = guruList[binding.spinnerGuru.selectedItemPosition]
        val selectedKelas = kelasList[binding.spinnerKelas.selectedItemPosition]
        val selectedMapel = mapelList[binding.spinnerMapel.selectedItemPosition]
        val selectedHari = hariList[binding.spinnerHari.selectedItemPosition]

        // Validasi bentrok
        db.collection("jadwal")
            .whereEqualTo("hari", selectedHari)
            .whereEqualTo("guruId", selectedGuru.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                var isBentrok = false
                for (doc in snapshot.documents) {
                    val existingMulai = doc.getString("waktuMulai") ?: ""
                    val existingSelesai = doc.getString("waktuSelesai") ?: ""
                    if (jamMulai < existingSelesai && jamSelesai > existingMulai) {
                        isBentrok = true
                        break
                    }
                }

                if (isBentrok) {
                    Toast.makeText(requireContext(), "Guru bentrok jadwal!", Toast.LENGTH_LONG).show()
                } else {
                    executeInsertJadwal(jamMulai, jamSelesai, selectedGuru, selectedKelas, selectedMapel, selectedHari)
                }
            }
    }

    private fun executeInsertJadwal(
        jamMulai: String,
        jamSelesai: String,
        selectedGuru: User,
        selectedKelas: Kelas,
        selectedMapel: MataPelajaranData,
        selectedHari: String
    ) {
        // 1. Dapatkan referensi dokumen kosong untuk Auto-ID
        val docRef = db.collection("jadwal").document()

        // 2. Susun objek Jadwal (Denormalisasi: ID + Nama)
        val newJadwal = Jadwal(
            id = docRef.id, // Simpan Auto-ID ke field id
            kelasId = selectedKelas.id,
            mapelId = selectedMapel.id,
            guruId = selectedGuru.uid,
            hari = selectedHari,
            waktuMulai = jamMulai,
            waktuSelesai = jamSelesai,
            namaMapel = selectedMapel.nama,
            namaGuru = selectedGuru.name,
            namaKelas = selectedKelas.namaKelas
        )

        // 3. Simpan ke Firestore
        docRef.set(newJadwal)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Jadwal berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                binding.etJamMulai.text.clear()
                binding.etJamSelesai.text.clear()
                loadJadwal()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal menyimpan jadwal", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}