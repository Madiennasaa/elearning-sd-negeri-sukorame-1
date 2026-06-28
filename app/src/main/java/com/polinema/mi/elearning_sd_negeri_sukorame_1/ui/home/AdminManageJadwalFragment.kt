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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
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
    
    private var selectedJadwalId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = JadwalAdapter(
            mutableListOf(),
            onEditClick = { jadwal -> editJadwal(jadwal) },
            onDeleteClick = { jadwal -> confirmDelete(jadwal) }
        )
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

    private fun editJadwal(jadwal: Jadwal) {
        selectedJadwalId = jadwal.id
        
        // Populate fields
        binding.etJamMulai.setText(jadwal.waktuMulai)
        binding.etJamSelesai.setText(jadwal.waktuSelesai)
        
        // Set spinner selections
        val hariPos = hariList.indexOf(jadwal.hari)
        if (hariPos >= 0) binding.spinnerHari.setSelection(hariPos)
        
        val kelasPos = kelasList.indexOfFirst { it.id == jadwal.kelasId }
        if (kelasPos >= 0) binding.spinnerKelas.setSelection(kelasPos)
        
        val mapelPos = mapelList.indexOfFirst { it.id == jadwal.mapelId }
        if (mapelPos >= 0) binding.spinnerMapel.setSelection(mapelPos)
        
        val guruPos = guruList.indexOfFirst { it.uid == jadwal.guruId }
        if (guruPos >= 0) binding.spinnerGuru.setSelection(guruPos)
        
        binding.btnSimpanJadwal.text = "UPDATE JADWAL"
        Toast.makeText(requireContext(), "Mode Edit: ${jadwal.namaMapel}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(jadwal: Jadwal) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Jadwal")
            .setMessage("Yakin ingin menghapus jadwal ${jadwal.namaMapel} di kelas ${jadwal.namaKelas}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("jadwal").document(jadwal.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadJadwal()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal menghapus jadwal", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanJadwal() {
        val jamMulai = binding.etJamMulai.text.toString().trim()
        val jamSelesai = binding.etJamSelesai.text.toString().trim()

        if (jamMulai.isEmpty() || jamSelesai.isEmpty()) {
            Toast.makeText(requireContext(), "Harap isi jam mulai dan selesai", Toast.LENGTH_SHORT).show()
            return
        }

        if (guruList.isEmpty() || kelasList.isEmpty() || mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Data master belum lengkap", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedGuru = guruList[binding.spinnerGuru.selectedItemPosition]
        val selectedKelas = kelasList[binding.spinnerKelas.selectedItemPosition]
        val selectedMapel = mapelList[binding.spinnerMapel.selectedItemPosition]
        val selectedHari = hariList[binding.spinnerHari.selectedItemPosition]

        // Bug A Fix: Validasi bentrok jadwal (Overlap)
        // Logika: (start1 < end2) AND (start2 < end1)
        db.collection("jadwal")
            .whereEqualTo("hari", selectedHari)
            .whereEqualTo("kelasId", selectedKelas.id)
            .get()
            .addOnSuccessListener { snapshot ->
                var isBentrok = false
                for (doc in snapshot.documents) {
                    // Kecualikan diri sendiri jika sedang edit
                    if (selectedJadwalId != null && doc.id == selectedJadwalId) continue
                    
                    val existingMulai = doc.getString("waktuMulai") ?: ""
                    val existingSelesai = doc.getString("waktuSelesai") ?: ""
                    
                    if (jamMulai < existingSelesai && existingMulai < jamSelesai) {
                        isBentrok = true
                        break
                    }
                }

                if (isBentrok) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Jadwal Bentrok")
                        .setMessage("Sudah ada mata pelajaran lain di kelas ini pada jam tersebut ($selectedHari).")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    executeSaveJadwal(jamMulai, jamSelesai, selectedGuru, selectedKelas, selectedMapel, selectedHari)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal validasi bentrok", Toast.LENGTH_SHORT).show()
            }
    }

    private fun executeSaveJadwal(
        jamMulai: String,
        jamSelesai: String,
        selectedGuru: User,
        selectedKelas: Kelas,
        selectedMapel: MataPelajaranData,
        selectedHari: String
    ) {
        val docRef = if (selectedJadwalId == null) {
            db.collection("jadwal").document()
        } else {
            db.collection("jadwal").document(selectedJadwalId!!)
        }

        val newJadwal = Jadwal(
            id = docRef.id,
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

        docRef.set(newJadwal)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                val msg = if (selectedJadwalId == null) "Jadwal ditambahkan" else "Jadwal diperbarui"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                resetForm()
                loadJadwal()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal menyimpan jadwal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetForm() {
        selectedJadwalId = null
        binding.etJamMulai.text.clear()
        binding.etJamSelesai.text.clear()
        binding.btnSimpanJadwal.text = "SIMPAN JADWAL"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
