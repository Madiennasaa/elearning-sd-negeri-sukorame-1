package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentJadwalBinding

class JadwalFragment : Fragment() {

    private var _binding: FragmentJadwalBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JadwalAdapter
    private var currentDay = "Senin"
    private val db = FirebaseFirestore.getInstance()

    private var guruList = listOf<User>()
    private var kelasList = listOf<Kelas>()
    private var mapelList = listOf<MataPelajaranData>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJadwalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener { (activity as? HomeActivity)?.backToHome() }

        adapter = JadwalAdapter(
            mutableListOf(),
            { jadwal -> handleEditAction(jadwal) },
            { jadwal -> handleDeleteAction(jadwal) }
        )

        binding.rvJadwal.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJadwal.adapter = adapter
        
        setupTabLayout()
        loadMasterData()
        loadJadwal()
    }

    private fun loadMasterData() {
        db.collection("users").whereEqualTo("role", "guru").get().addOnSuccessListener { gSnap ->
            guruList = gSnap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
        }
        db.collection("kelas").get().addOnSuccessListener { kSnap ->
            kelasList = kSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }
        }
        db.collection("mapel").get().addOnSuccessListener { mSnap ->
            mapelList = mSnap.documents.mapNotNull { it.toObject(MataPelajaranData::class.java)?.copy(id = it.id) }
        }
    }

    private fun handleEditAction(jadwal: Jadwal) {
        val role = arguments?.getString("USER_ROLE") ?: "siswa"
        if (role == "admin" || role == "guru") {
            showEditDialog(jadwal)
        } else {
            Toast.makeText(requireContext(), "Hanya Admin/Guru yang dapat mengedit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDeleteAction(jadwal: Jadwal) {
        val role = arguments?.getString("USER_ROLE") ?: "siswa"
        if (role == "admin") {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Jadwal")
                .setMessage("Hapus jadwal ${jadwal.namaMapel.orEmpty()}?")
                .setPositiveButton("Hapus") { _, _ ->
                    db.collection("jadwal").document(jadwal.id).delete().addOnSuccessListener {
                        Toast.makeText(requireContext(), "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadJadwal()
                    }
                }
                .setNegativeButton("Batal", null).show()
        }
    }

    private fun showEditDialog(jadwal: Jadwal) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_admin_manage_jadwal, null)
        view.findViewById<View>(R.id.rvManageJadwal)?.visibility = View.GONE
        view.findViewById<View>(R.id.btnSimpanJadwal)?.visibility = View.GONE
        
        val etMulai = view.findViewById<EditText>(R.id.etJamMulai)
        val etSelesai = view.findViewById<EditText>(R.id.etJamSelesai)
        val spMapel = view.findViewById<Spinner>(R.id.spinnerMapel)
        val spGuru = view.findViewById<Spinner>(R.id.spinnerGuru)
        val spKelas = view.findViewById<Spinner>(R.id.spinnerKelas)
        val spHari = view.findViewById<Spinner>(R.id.spinnerHari)

        val hariOptions = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        spHari.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, hariOptions)
        spMapel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, mapelList.map { it.nama })
        spGuru.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, guruList.map { it.name.orEmpty() })
        spKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasList.map { it.namaKelas.orEmpty() })

        etMulai.setText(jadwal.waktuMulai.orEmpty())
        etSelesai.setText(jadwal.waktuSelesai.orEmpty())
        spHari.setSelection(hariOptions.indexOf(jadwal.hari).coerceAtLeast(0))
        spMapel.setSelection(mapelList.indexOfFirst { it.id == jadwal.mapelId }.coerceAtLeast(0))
        spGuru.setSelection(guruList.indexOfFirst { it.uid == jadwal.guruId }.coerceAtLeast(0))
        spKelas.setSelection(kelasList.indexOfFirst { it.id == jadwal.kelasId }.coerceAtLeast(0))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Jadwal")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val mulai = etMulai.text.toString().trim()
                val selesai = etSelesai.text.toString().trim()
                val hari = spHari.selectedItem.toString()
                val kIdx = spKelas.selectedItemPosition
                val mIdx = spMapel.selectedItemPosition
                val gIdx = spGuru.selectedItemPosition
                
                if (mulai.isNotEmpty() && selesai.isNotEmpty() && kIdx >= 0 && mIdx >= 0 && gIdx >= 0) {
                    val kelas = kelasList[kIdx]
                    val mapel = mapelList[mIdx]
                    val guru = guruList[gIdx]
                    checkOverlapThenSave(jadwal.id, mulai, selesai, hari, kelas, mapel, guru)
                } else {
                    Toast.makeText(requireContext(), "Data belum lengkap", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun checkOverlapThenSave(id: String, mulai: String, selesai: String, hari: String, kelas: Kelas, mapel: MataPelajaranData, guru: User) {
        db.collection("jadwal")
            .whereEqualTo("hari", hari)
            .whereEqualTo("kelasId", kelas.id)
            .get()
            .addOnSuccessListener { snap ->
                var bentrok = false
                for (doc in snap.documents) {
                    if (doc.id == id) continue
                    val exMulai = doc.getString("waktuMulai").orEmpty()
                    val exSelesai = doc.getString("waktuSelesai").orEmpty()
                    if (mulai < exSelesai && exMulai < selesai) {
                        bentrok = true
                        break
                    }
                }

                if (bentrok) {
                    Toast.makeText(requireContext(), "Jadwal bentrok!", Toast.LENGTH_LONG).show()
                } else {
                    val updated = Jadwal(
                        id = id,
                        kelasId = kelas.id,
                        mapelId = mapel.id,
                        guruId = guru.uid,
                        hari = hari,
                        waktuMulai = mulai,
                        waktuSelesai = selesai,
                        namaMapel = mapel.nama,
                        namaGuru = guru.name,
                        namaKelas = kelas.namaKelas
                    )
                    db.collection("jadwal").document(id).set(updated).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        loadJadwal()
                    }
                }
            }
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
        val role = arguments?.getString("USER_ROLE") ?: "siswa"
        val kelasId = arguments?.getString("KELAS_ID")
        val guruId = arguments?.getString("GURU_ID")

        binding.progressBar.visibility = View.VISIBLE

        val query = if (role == "guru") {
            db.collection("jadwal").whereEqualTo("guruId", guruId).whereEqualTo("hari", currentDay)
        } else {
            db.collection("jadwal").whereEqualTo("kelasId", kelasId).whereEqualTo("hari", currentDay)
        }

        query.get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val list = snapshot.documents.mapNotNull { it.toObject(Jadwal::class.java)?.copy(id = it.id) }
            adapter.isGuruView = (role == "guru")
            adapter.updateData(list.sortedBy { it.waktuMulai })
            binding.progressBar.visibility = View.GONE
            binding.layoutEmptyJadwal.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }.addOnFailureListener {
            if (isAdded) binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
