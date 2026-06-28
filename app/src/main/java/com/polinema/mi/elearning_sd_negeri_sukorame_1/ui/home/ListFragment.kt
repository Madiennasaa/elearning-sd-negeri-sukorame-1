package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.*
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivityListBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.cbt.CbtActivity
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list.ListAdapter

class ListFragment : Fragment() {

    private var _binding: ActivityListBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    companion object {
        fun newInstance(type: String, kelasId: String, siswaId: String) = ListFragment().apply {
            arguments = Bundle().apply {
                putString("TYPE", type); putString("KELAS_ID", kelasId); putString("SISWA_ID", siswaId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type    = arguments?.getString("TYPE") ?: "MATERI"
        val user    = SessionManager(requireContext()).getUser()
        val kelasId = arguments?.getString("KELAS_ID") ?: user?.kelasId ?: ""
        val siswaId = arguments?.getString("SISWA_ID") ?: user?.idSiswa ?: ""

        binding.tvTitle.text = when (type) {
            "MATERI" -> "Daftar Materi"
            "TUGAS"  -> "Daftar Tugas"
            "HADIR"  -> "Rekap Absensi"
            "NILAI"  -> "Daftar Nilai"
            "RAPORT" -> "Rapor"
            "SISWA"  -> "Daftar Siswa"
            else     -> "Daftar $type"
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())

        loadListData(type, kelasId, siswaId)
    }

    private fun loadListData(type: String, kelasId: String, siswaId: String) {
        when (type) {
            "MATERI" -> {
                db.collection("materi").whereEqualTo("kelasId", kelasId).get().addOnSuccessListener { snap ->
                    val data = snap.documents.mapNotNull { it.toObject(Materi::class.java) }
                    if (data.isEmpty()) { binding.tvTitle.text = "Belum ada materi"; return@addOnSuccessListener }
                    binding.rvList.adapter = ListAdapter(data.map { (it.judul ?: "-") to (it.namaMapel ?: "-") }) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("📖 Detail Materi")
                            .setMessage("Ayo belajar, Ananda!\n\nJudul: ${item.judul}\nMata Pelajaran: ${item.namaMapel}\n\nKlik OK untuk melihat materi ya! ✨")
                            .setPositiveButton("Lihat Materi") { _, _ ->
                                val url = item.urlVideo
                                if (!url.isNullOrEmpty()) startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                            .setNegativeButton("Tutup", null)
                            .show()
                    }
                }
            }
            "TUGAS" -> {
                db.collection("tugas").whereEqualTo("kelasId", kelasId).get().addOnSuccessListener { tugasSnap ->
                    db.collection("hasil_cbt").whereEqualTo("siswaId", siswaId).get().addOnSuccessListener { hasilSnap ->
                        if (!isAdded) return@addOnSuccessListener
                        
                        val completedTugasIds = hasilSnap.documents.mapNotNull { it.getString("tugasId") }.toSet()
                        val data = tugasSnap.documents.mapNotNull { it.toObject(Tugas::class.java)?.copy(id = it.id) }
                        
                        if (data.isEmpty()) { binding.tvTitle.text = "Belum ada tugas"; return@addOnSuccessListener }
                        
                        binding.rvList.adapter = ListAdapter(data.map {
                            val isDone = it.id in completedTugasIds
                            (it.judul ?: "-") to "Deadline: ${it.deadline} ${if (isDone) "✅ Sudah Selesai" else "📝 Belum Dikerjakan"}"
                        }) { pos ->
                            val item = data[pos]
                            val isDone = item.id in completedTugasIds
                            
                            if (isDone) {
                                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("🎉 Tugas Selesai!")
                                    .setMessage("Hebat! Kamu sudah mengerjakan tugas ini. 👍\nTetap semangat belajar ya!")
                                    .setPositiveButton("Siap!", null)
                                    .show()
                            } else {
                                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("📝 Kerjakan Tugas")
                                    .setMessage("Siap mengerjakan tugas ini, Ananda?\n\nJudul: ${item.judul}\nMapel: ${item.namaMapel}\n\nJangan lupa berdoa dulu ya! ✨")
                                    .setPositiveButton("Mulai!") { _, _ ->
                                        startActivity(Intent(requireContext(), CbtActivity::class.java).apply {
                                            putExtra("TUGAS_ID", item.id)
                                            putExtra("SISWA_ID", siswaId)
                                        })
                                    }
                                    .setNegativeButton("Nanti saja", null)
                                    .show()
                            }
                        }
                    }
                }
            }
            "HADIR" -> {
                db.collection("absensi").whereEqualTo("siswaId", siswaId).get().addOnSuccessListener { snap ->
                    val data = snap.documents.mapNotNull { it.toObject(Absensi::class.java) }
                    if (data.isEmpty()) { binding.tvTitle.text = "Belum ada data absensi"; return@addOnSuccessListener }
                    binding.rvList.adapter = ListAdapter(data.map { (it.tanggal ?: "-") to (it.status ?: "-") }) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Detail Absensi")
                            .setMessage("Tanggal: ${item.tanggal}\nStatus: ${item.status}\n\nKeterangan:\n${item.keterangan ?: "-"}")
                            .setPositiveButton("Tutup", null)
                            .show()
                    }
                }
            }
            "NILAI" -> {
                db.collection("nilai").whereEqualTo("siswaId", siswaId).get().addOnSuccessListener { snap ->
                    val data = snap.documents.mapNotNull { it.toObject(Nilai::class.java) }
                    if (data.isEmpty()) { binding.tvTitle.text = "Belum ada nilai"; return@addOnSuccessListener }
                    binding.rvList.adapter = ListAdapter(data.map { "${it.namaMapel}: ${it.nilai}" to (it.jenisNilai ?: "-") }) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Detail Nilai")
                            .setMessage("Mapel: ${item.namaMapel}\nJenis: ${item.jenisNilai}\nNilai: ${item.nilai}\nSemester: ${item.semester ?: "-"}")
                            .setPositiveButton("Tutup", null)
                            .show()
                    }
                }
            }
            "RAPORT" -> {
                db.collection("rapor").whereEqualTo("siswaId", siswaId).get().addOnSuccessListener { snap ->
                    val data = snap.documents.mapNotNull { it.toObject(Rapor::class.java) }
                    if (data.isEmpty()) { binding.tvTitle.text = "Belum ada rapor"; return@addOnSuccessListener }
                    binding.rvList.adapter = ListAdapter(data.map { "Semester ${it.semester} - ${it.tahunAjaran}" to "Status: ${it.statusNaik ?: "-"}" }) { pos ->
                        val item = data[pos]
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Detail Rapor")
                            .setMessage("Tahun: ${item.tahunAjaran}\nSemester: ${item.semester}\nStatus: ${item.statusNaik ?: "-"}\n\n" +
                                     "Hadir: ${item.totalHadir}\nSakit: ${item.totalSakit}\nIzin: ${item.totalIzin}\nAlpha: ${item.totalAlpha}\n\n" +
                                     "Catatan:\n${item.catatanWali ?: "-"}")
                            .setPositiveButton("Tutup", null)
                            .show()
                    }
                }
            }
            "SISWA" -> {
                // Poin 3: Query ke koleksi 'users' dengan filter role 'siswa' dan kelasId
                db.collection("users")
                    .whereEqualTo("role", "siswa")
                    .whereEqualTo("kelasId", kelasId)
                    .get()
                    .addOnSuccessListener { snap ->
                        val data = snap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }
                        if (data.isEmpty()) { binding.tvTitle.text = "Belum ada siswa"; return@addOnSuccessListener }
                        binding.rvList.adapter = ListAdapter(data.map { (it.name ?: "-") to "NISN: ${it.nisn}" }) { pos ->
                            val item = data[pos]
                            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Detail Siswa")
                                .setMessage("Nama: ${item.name}\nNISN: ${item.nisn}\nGender: ${item.jenisKelamin}\nLahir: ${item.tanggalLahir}")
                                .setPositiveButton("Tutup", null)
                                .show()
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
