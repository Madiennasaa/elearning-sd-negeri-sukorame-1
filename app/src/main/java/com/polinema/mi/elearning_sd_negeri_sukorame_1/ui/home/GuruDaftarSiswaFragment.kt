package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.graphics.Color
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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager

class GuruDaftarSiswaFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager
    private var guruId = ""
    private var kelasId = ""
    private val listSiswa = mutableListOf<Siswa>()
    private val listSiswaFull = mutableListOf<Siswa>()
    private lateinit var adapter: DaftarSiswaAdapter

    private val stripColors = listOf(
        "#7C4DFF", "#4A90D9", "#43A047", "#FB8C00",
        "#E53935", "#00ACC1", "#AB47BC"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_guru_daftar_siswa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        guruId = user?.idGuru ?: ""
        kelasId = user?.kelasId ?: ""

        if (guruId.isEmpty()) {
            Toast.makeText(requireContext(), "Data guru tidak ditemukan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        if (kelasId.isEmpty()) {
            // Coba fetch kelas_id jika di user session kosong
            fetchKelasInfo()
        } else {
            setupUI(view)
            loadSiswa()
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun fetchKelasInfo() {
        db.collection("kelas")
            .whereEqualTo("guruId", guruId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val listKelas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                }
                if (listKelas.isNotEmpty()) {
                    kelasId = listKelas[0].id
                    setupUI(view!!)
                    loadSiswa()
                } else {
                    Toast.makeText(requireContext(), "Guru belum memiliki kelas terdaftar.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat data kelas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI(view: View) {
        db.collection("kelas").document(kelasId)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val kelas = doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                view.findViewById<TextView>(R.id.tvNamaKelasDaftarSiswa).text =
                    "Daftar Siswa — Kelas ${kelas?.namaKelas ?: ""}"
            }
            .addOnFailureListener { /* silent */ }

        adapter = DaftarSiswaAdapter(listSiswa) { siswa -> showDetailSiswa(siswa) }
        view.findViewById<RecyclerView>(R.id.rvDaftarSiswa).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GuruDaftarSiswaFragment.adapter
        }

        view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchSiswa)
            .setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    filterSiswa(newText ?: "")
                    return true
                }
            })
    }

    private fun loadSiswa() {
        db.collection("siswa")
            .whereEqualTo("kelasId", kelasId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Siswa::class.java)?.copy(id = doc.id)
                }
                listSiswaFull.clear()
                listSiswaFull.addAll(data)
                listSiswa.clear()
                listSiswa.addAll(data)
                adapter.notifyDataSetChanged()
                updateEmptyAndCounter()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat data siswa: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterSiswa(keyword: String) {
        listSiswa.clear()
        if (keyword.isEmpty()) {
            listSiswa.addAll(listSiswaFull)
        } else {
            listSiswa.addAll(listSiswaFull.filter {
                it.namaLengkap?.contains(keyword, ignoreCase = true) == true ||
                        it.nisn?.contains(keyword, ignoreCase = true) == true
            })
        }
        adapter.notifyDataSetChanged()
        updateEmptyAndCounter()
    }

    private fun updateEmptyAndCounter() {
        val v = view ?: return
        v.findViewById<LinearLayout>(R.id.layoutEmptyDaftarSiswa).visibility =
            if (listSiswa.isEmpty()) View.VISIBLE else View.GONE
        v.findViewById<RecyclerView>(R.id.rvDaftarSiswa).visibility =
            if (listSiswa.isEmpty()) View.GONE else View.VISIBLE
        v.findViewById<TextView>(R.id.tvJumlahSiswa).text = "${listSiswa.size} Siswa"
    }

    private fun showDetailSiswa(siswa: Siswa) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_detail_siswa, null)

        dialogView.findViewById<TextView>(R.id.tvDetailNamaSiswa).text = siswa.namaLengkap
        dialogView.findViewById<TextView>(R.id.tvDetailNisnSiswa).text = "NISN: ${siswa.nisn}"

        // Load nilai
        db.collection("nilai")
            .whereEqualTo("siswaId", siswa.id)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val nilaiList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Nilai::class.java)?.copy(id = doc.id)
                }
                dialogView.findViewById<TextView>(R.id.tvDetailNilaiSiswa).text =
                    if (nilaiList.isNotEmpty())
                        nilaiList.joinToString("\n") { "• ${it.namaMapel} (${it.jenisNilai}): ${it.nilai}" }
                    else "Belum ada nilai"
            }
            .addOnFailureListener { /* silent */ }

        // Load absensi
        db.collection("absensi")
            .whereEqualTo("siswaId", siswa.id)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val absensiList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Absensi::class.java)?.copy(id = doc.id)
                }
                val hadir = absensiList.count { it.status == "Hadir" }
                val sakit = absensiList.count { it.status == "Sakit" }
                val izin  = absensiList.count { it.status == "Izin" }
                val alpha = absensiList.count { it.status == "Alpha" }
                dialogView.findViewById<TextView>(R.id.tvDetailAbsensiSiswa).text =
                    "Hadir: $hadir hari\nSakit: $sakit hari\nIzin: $izin hari\nAlpha: $alpha hari"
            }
            .addOnFailureListener { /* silent */ }

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Siswa")
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    inner class DaftarSiswaAdapter(
        val list: List<Siswa>,
        val onClick: (Siswa) -> Unit
    ) : RecyclerView.Adapter<DaftarSiswaAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val stripSiswa: View    = v.findViewById(R.id.stripSiswa)
            val tvInisial: TextView = v.findViewById(R.id.tvInisial)
            val tvNama: TextView    = v.findViewById(R.id.tvNamaSiswa)
            val tvNisn: TextView    = v.findViewById(R.id.tvNisnSiswa)
            val tvNomor: TextView   = v.findViewById(R.id.tvNomor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_siswa_guru, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val siswa = list[position]
            val color = Color.parseColor(stripColors[position % stripColors.size])

            holder.stripSiswa.setBackgroundColor(color)
            holder.tvInisial.text = siswa.namaLengkap
                ?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.tvInisial.setTextColor(color)
            holder.tvNama.text  = siswa.namaLengkap
            holder.tvNisn.text  = "NISN: ${siswa.nisn ?: "-"}"
            holder.tvNomor.text = "${position + 1}"
            holder.itemView.setOnClickListener { onClick(siswa) }
        }

        override fun getItemCount() = list.size
    }
}
