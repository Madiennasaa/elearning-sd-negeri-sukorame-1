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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Materi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager

class GuruInputMateriFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager
    private var guruId = ""
    private var kelasId = ""
    private var mapelList = mutableListOf<MataPelajaranData>()
    private val listMateri = mutableListOf<Materi>()
    private lateinit var adapter: MateriAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_guru_input_materi, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val user = sessionManager.getUser()
        guruId = user?.uid ?: ""
        kelasId = user?.kelasId ?: ""

        if (guruId.isEmpty()) {
            Toast.makeText(requireContext(), "Data guru tidak ditemukan", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        if (kelasId.isEmpty()) {
            fetchKelasInfo()
        } else {
            setupUI(view)
            loadMateri()
            loadMapel()
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
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Kelas::class.java)?.copy(id = doc.id)
                }
                if (list.isNotEmpty()) {
                    kelasId = list[0].id
                    setupUI(requireView())
                    loadMateri()
                    loadMapel()
                } else {
                    Toast.makeText(requireContext(), "Guru belum memiliki kelas.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setupUI(view: View) {
        adapter = MateriAdapter(listMateri) { id -> confirmDeleteMateri(id) }
        view.findViewById<RecyclerView>(R.id.rvMateri).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GuruInputMateriFragment.adapter
        }

        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fabTambahMateri
        ).setOnClickListener {
            showMateriDialog()
        }
    }

    private fun loadMateri() {
        db.collection("materi")
            .whereEqualTo("guruId", guruId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Materi::class.java)?.copy(id = doc.id)
                }
                listMateri.clear()
                listMateri.addAll(list)
                adapter.notifyDataSetChanged()
            }
    }

    private fun loadMapel() {
        db.collection("jadwal")
            .whereEqualTo("guruId", guruId)
            .whereEqualTo("kelasId", kelasId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val j = doc.toObject(Jadwal::class.java)
                    if (j != null) MataPelajaranData(id = j.mapelId ?: "", nama = j.namaMapel ?: "") else null
                }.distinctBy { it.id }
                mapelList.clear()
                mapelList.addAll(list)
            }
    }

    private fun showMateriDialog() {
        if (kelasId.isEmpty()) {
            Toast.makeText(requireContext(), "Guru belum memiliki kelas terdaftar.", Toast.LENGTH_LONG).show()
            return
        }
        if (mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada mata pelajaran di jadwal Anda.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_materi_form, null)
        val etJudul = dialogView.findViewById<EditText>(R.id.etJudulMateri)
        val etDeskripsi = dialogView.findViewById<EditText>(R.id.etDeskripsiMateri)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrlMateri)
        val spinnerMapel = dialogView.findViewById<Spinner>(R.id.spinnerMapelMateri)

        spinnerMapel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            mapelList.map { it.nama }
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Materi")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val judul = etJudul.text.toString().trim()
                val deskripsi = etDeskripsi.text.toString().trim()
                val url = etUrl.text.toString().trim()
                val mapel = mapelList[spinnerMapel.selectedItemPosition]
                val mapelId = mapel.id
                val mapelNama = mapel.nama

                if (judul.isEmpty()) {
                    Toast.makeText(requireContext(), "Judul wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tipeMateri = if (url.isNotEmpty()) "video" else "materi"

                val data = hashMapOf(
                    "judul" to judul,
                    "deskripsi" to deskripsi,
                    "urlVideo" to url,
                    "tipe" to tipeMateri,
                    "mapelId" to mapelId,
                    "namaMapel" to mapelNama,
                    "guruId" to guruId,
                    "namaGuru" to (sessionManager.getUser()?.name ?: "Guru"),
                    "kelasId" to kelasId
                )

                db.collection("materi")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Materi berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        loadMateri()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDeleteMateri(id: String) {
        AlertDialog.Builder(requireContext())
            .setMessage("Hapus materi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("materi").document(id).delete().addOnSuccessListener { loadMateri() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    inner class MateriAdapter(val list: List<Materi>, val onDelete: (String) -> Unit) : RecyclerView.Adapter<MateriAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvJudul: TextView = v.findViewById(R.id.tvJudulMateriItem)
            val tvMapel: TextView = v.findViewById(R.id.tvMapelMateriItem)
            val tvUrl: TextView = v.findViewById(R.id.tvUrlMateriItem)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteMateri)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_materi_guru, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvJudul.text = item.judul
            holder.tvMapel.text = item.namaMapel
            holder.tvUrl.text = item.urlVideo ?: "-"
            holder.btnDelete.setOnClickListener { onDelete(item.id) }
        }
        override fun getItemCount() = list.size
    }
}
