package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Absensi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Nilai
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Rapor
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager

class GuruRaporFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager
    private var guruUid = ""
    private var kelasId = ""
    private val listSiswa = mutableListOf<User>()
    private val mapRaporStatus = mutableMapOf<String, String>()
    private lateinit var adapter: SiswaRaporAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_guru_rapor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        progressBar = view.findViewById(R.id.pbGuruRapor) ?: ProgressBar(requireContext())

        val user = sessionManager.getUser()
        guruUid = user?.uid ?: ""

        if (guruUid.isEmpty()) {
            Toast.makeText(requireContext(), "Sesi guru berakhir", Toast.LENGTH_SHORT).show()
            view.post { parentFragmentManager.popBackStack() }
            return
        }

        adapter = SiswaRaporAdapter(listSiswa, mapRaporStatus) { siswa -> showRaporDialog(siswa) }
        view.findViewById<RecyclerView>(R.id.rvSiswaRapor).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GuruRaporFragment.adapter
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadData()
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE

        // Get Kelas for this Guru
        db.collection("kelas")
            .whereEqualTo("guruId", guruUid)
            .get()
            .addOnSuccessListener { kelasSnapshot ->
                if (!isAdded) return@addOnSuccessListener
                val kelasList = kelasSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                }
                val kelas = kelasList.firstOrNull()
                if (kelas != null) {
                    kelasId = kelas.id
                    view?.findViewById<TextView>(R.id.tvNamaKelasRapor)?.text = "Kelas ${kelas.namaKelas}"

                    // Load Siswa in this Kelas (Query ke koleksi users dengan role siswa)
                    db.collection("users")
                        .whereEqualTo("role", "siswa")
                        .whereEqualTo("kelasId", kelasId)
                        .get()
                        .addOnSuccessListener { siswaSnapshot ->
                            if (!isAdded) return@addOnSuccessListener
                            listSiswa.clear()
                            listSiswa.addAll(siswaSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(User::class.java)?.copy(uid = doc.id)
                            })

                            // Load Rapor statuses
                            loadRaporStatuses()
                        }
                        .addOnFailureListener { e ->
                            if (!isAdded) return@addOnFailureListener
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Gagal memuat data siswa", Toast.LENGTH_SHORT).show()
                            updateEmptyView()
                        }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Anda belum memiliki kelas wali", Toast.LENGTH_SHORT).show()
                    updateEmptyView()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                updateEmptyView()
            }
    }

    private fun loadRaporStatuses() {
        val siswaIds = listSiswa.map { it.uid }
        if (siswaIds.isEmpty()) {
            adapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE
            updateEmptyView()
            return
        }

        val batches = siswaIds.chunked(10)
        var completedBatches = 0
        mapRaporStatus.clear()

        for (batch in batches) {
            db.collection("rapor")
                .whereIn("siswaId", batch)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!isAdded) return@addOnSuccessListener
                    snapshot.documents.forEach { doc ->
                        val siswaId = doc.getString("siswaId") ?: return@forEach
                        val statusNaik = doc.getString("statusNaik") ?: "Sudah diisi"
                        mapRaporStatus[siswaId] = statusNaik
                    }
                    completedBatches++
                    if (completedBatches == batches.size) {
                        adapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                        updateEmptyView()
                    }
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    completedBatches++
                    if (completedBatches == batches.size) {
                        adapter.notifyDataSetChanged()
                        progressBar.visibility = View.GONE
                        updateEmptyView()
                    }
                }
        }
    }

    private fun updateEmptyView() {
        view?.findViewById<TextView>(R.id.tvEmptySiswaRapor)?.visibility =
            if (listSiswa.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showRaporDialog(siswa: User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rapor_form, null)
        val etCatatan = dialogView.findViewById<EditText>(R.id.etCatatanRapor)
        val spinnerSemester = dialogView.findViewById<Spinner>(R.id.spinnerSemesterRapor)
        val spinnerStatusNaik = dialogView.findViewById<Spinner>(R.id.spinnerStatusNaik)
        val tvNilaiRingkasan = dialogView.findViewById<TextView>(R.id.tvNilaiRingkasan)
        val tvAbsensiRingkasan = dialogView.findViewById<TextView>(R.id.tvAbsensiRingkasan)

        val semesterOptions = listOf("1", "2")
        spinnerSemester.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, semesterOptions
        )

        val statusOptions = listOf("Naik Kelas", "Tidak Naik Kelas")
        spinnerStatusNaik.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions
        )

        progressBar.visibility = View.VISIBLE

        var totalHadir = 0
        var totalSakit = 0
        var totalIzin = 0
        var totalAlpha = 0

        // Load nilai ringkasan menggunakan UID siswa
        db.collection("nilai")
            .whereEqualTo("siswaId", siswa.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val nilaiList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Nilai::class.java)?.copy(id = doc.id)
                }
                tvNilaiRingkasan.text = if (nilaiList.isNotEmpty()) {
                    nilaiList.joinToString("\n") { "${it.namaMapel}: ${it.nilai} (${it.jenisNilai})" }
                } else "Belum ada nilai"
            }

        // Load absensi ringkasan menggunakan UID siswa
        db.collection("absensi")
            .whereEqualTo("siswaId", siswa.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val absensiList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Absensi::class.java)?.copy(id = doc.id)
                }
                totalHadir = absensiList.count { it.status == "Hadir" }
                totalSakit = absensiList.count { it.status == "Sakit" }
                totalIzin = absensiList.count { it.status == "Izin" }
                totalAlpha = absensiList.count { it.status == "Alpha" }
                tvAbsensiRingkasan.text = "Hadir: $totalHadir | Sakit: $totalSakit | Izin: $totalIzin | Alpha: $totalAlpha"
            }

        db.collection("rapor")
            .whereEqualTo("siswaId", siswa.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val existing = snapshot.documents.firstOrNull()
                val existingRapor = existing?.toObject(Rapor::class.java)?.copy(id = existing.id)

                if (existingRapor != null) {
                    etCatatan.setText(existingRapor.catatanWali)
                    spinnerSemester.setSelection(semesterOptions.indexOf(existingRapor.semester).coerceAtLeast(0))
                    spinnerStatusNaik.setSelection(statusOptions.indexOf(existingRapor.statusNaik).coerceAtLeast(0))
                }

                progressBar.visibility = View.GONE

                AlertDialog.Builder(requireContext())
                    .setTitle("Rapor: ${siswa.name}")
                    .setView(dialogView)
                    .setPositiveButton("Simpan Rapor") { _, _ ->
                        saveRapor(
                            siswa,
                            spinnerSemester.selectedItem.toString(),
                            spinnerStatusNaik.selectedItem.toString(),
                            etCatatan.text.toString(),
                            totalHadir, totalSakit, totalIzin, totalAlpha,
                            existingRapor?.id
                        )
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat data rapor", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRapor(
        siswa: User,
        semester: String,
        statusNaik: String,
        catatan: String,
        hadir: Int,
        sakit: Int,
        izin: Int,
        alpha: Int,
        existingRaporId: String? = null
    ) {
        val data = hashMapOf(
            "siswaId" to siswa.uid,
            "kelasId" to kelasId,
            "semester" to semester,
            "tahunAjaran" to "2024/2025",
            "totalHadir" to hadir,
            "totalSakit" to sakit,
            "totalIzin" to izin,
            "totalAlpha" to alpha,
            "catatanWali" to catatan,
            "statusNaik" to statusNaik
        )

        val task = if (existingRaporId != null) {
            db.collection("rapor").document(existingRaporId).set(data)
        } else {
            db.collection("rapor").add(data)
        }

        task.addOnSuccessListener {
            if (!isAdded) return@addOnSuccessListener
            Toast.makeText(requireContext(), "Rapor berhasil disimpan!", Toast.LENGTH_SHORT).show()
            loadData()
        }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal menyimpan rapor", Toast.LENGTH_SHORT).show()
            }
    }

    inner class SiswaRaporAdapter(
        val list: List<User>,
        val statusMap: Map<String, String>,
        val onInput: (User) -> Unit
    ) : RecyclerView.Adapter<SiswaRaporAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama: TextView = v.findViewById(R.id.tvNamaSiswaRapor)
            val tvNisn: TextView = v.findViewById(R.id.tvNisnSiswaRapor)
            val tvStatusRapor: TextView = v.findViewById(R.id.tvStatusRapor)
            val btnInputRapor: Button = v.findViewById(R.id.btnInputRapor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_siswa_rapor, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa = list[position]
            holder.tvNama.text = siswa.name
            holder.tvNisn.text = "NISN: ${siswa.nisn ?: "-"}"

            val status = statusMap[siswa.uid]
            if (status != null) {
                holder.tvStatusRapor.text = "✓ $status"
                holder.tvStatusRapor.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                holder.tvStatusRapor.text = "Belum diisi"
                holder.tvStatusRapor.setTextColor(requireContext().getColor(android.R.color.darker_gray))
            }

            holder.btnInputRapor.setOnClickListener { onInput(siswa) }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
