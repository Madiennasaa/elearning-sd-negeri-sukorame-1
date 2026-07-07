package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.Calendar
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.MataPelajaranData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Tugas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager

class GuruInputTugasFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager
    private var guruId = ""
    private var kelasId = ""
    private var mapelList = mutableListOf<MataPelajaranData>()
    private val listTugas = mutableListOf<Tugas>()
    private lateinit var adapter: TugasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_guru_input_tugas, container, false)

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
            loadTugas()
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
                    loadTugas()
                    loadMapel()
                } else {
                    Toast.makeText(requireContext(), "Guru belum memiliki kelas terdaftar.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal mengambil info kelas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI(view: View) {
        adapter = TugasAdapter(listTugas,
            onManageSoal = { tugasId, judulTugas -> showManageSoalDialog(tugasId, judulTugas) },
            onDelete = { id -> confirmDeleteTugas(id) }
        )
        view.findViewById<RecyclerView>(R.id.rvTugas).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GuruInputTugasFragment.adapter
        }

        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.fabTambahTugas
        ).setOnClickListener { showTugasDialog() }
    }

    private fun loadTugas() {
        db.collection("tugas")
            .whereEqualTo("guruId", guruId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Tugas::class.java)?.copy(id = doc.id)
                }
                listTugas.clear()
                listTugas.addAll(list)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMapel() {
        db.collection("mata_pelajaran")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MataPelajaranData::class.java)?.copy(id = doc.id)
                }
                mapelList.clear()
                mapelList.addAll(list)
            }
    }

    private fun showTugasDialog() {
        if (kelasId.isEmpty()) {
            Toast.makeText(requireContext(), "Guru belum memiliki kelas terdaftar.", Toast.LENGTH_LONG).show()
            return
        }
        if (mapelList.isEmpty()) {
            Toast.makeText(requireContext(), "Tidak ada mata pelajaran tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tugas_form, null)
        val etJudul = dialogView.findViewById<EditText>(R.id.etJudulTugas)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadlineTugas)
        val spinnerMapel = dialogView.findViewById<Spinner>(R.id.spinnerMapelTugas)

        etDeadline.isFocusable = false
        etDeadline.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                etDeadline.setText(dateStr)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        spinnerMapel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            mapelList.map { it.nama }
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Buat Tugas / Ujian (CBT)")
            .setView(dialogView)
            .setPositiveButton("Lanjut ke Buat Soal") { _, _ ->
                val judul = etJudul.text.toString().trim()
                val deadline = etDeadline.text.toString().trim()
                val mapel = mapelList[spinnerMapel.selectedItemPosition]
                val mapelId = mapel.id
                val mapelNama = mapel.nama

                if (judul.isEmpty()) {
                    Toast.makeText(requireContext(), "Judul tugas wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (deadline.isEmpty()) {
                    Toast.makeText(requireContext(), "Deadline wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = hashMapOf(
                    "judul" to judul,
                    "mapelId" to mapelId,
                    "namaMapel" to mapelNama,
                    "guruId" to guruId,
                    "kelasId" to kelasId,
                    "deadline" to deadline,
                    "jumlahSoal" to 0,
                    "durasi" to 60
                )

                db.collection("tugas")
                    .add(data)
                    .addOnSuccessListener { docRef ->
                        if (!isAdded) return@addOnSuccessListener
                        val newId = docRef.id
                        Toast.makeText(requireContext(), "Tugas berhasil dibuat. Silakan tambah soal!", Toast.LENGTH_LONG).show()
                        loadTugas()
                        showManageSoalDialog(newId, judul)
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Gagal menyimpan tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showManageSoalDialog(tugasId: String, judulTugas: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_soal_form, null)
        val etPertanyaan = dialogView.findViewById<EditText>(R.id.etPertanyaan)
        val etPilA = dialogView.findViewById<EditText>(R.id.etPilihanA)
        val etPilB = dialogView.findViewById<EditText>(R.id.etPilihanB)
        val etPilC = dialogView.findViewById<EditText>(R.id.etPilihanC)
        val etPilD = dialogView.findViewById<EditText>(R.id.etPilihanD)
        val rgJawaban = dialogView.findViewById<RadioGroup>(R.id.rgJawabanBenar)

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Soal - $judulTugas")
            .setView(dialogView)
            .setPositiveButton("Simpan Soal") { _, _ ->
                val pertanyaan = etPertanyaan.text.toString().trim()
                val pilA = etPilA.text.toString().trim()
                val pilB = etPilB.text.toString().trim()
                val pilC = etPilC.text.toString().trim()
                val pilD = etPilD.text.toString().trim()

                val jawabanBenar = when (rgJawaban.checkedRadioButtonId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }

                if (pertanyaan.isEmpty() || pilA.isEmpty() || pilB.isEmpty()) {
                    Toast.makeText(requireContext(), "Pertanyaan dan minimal 2 pilihan wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (jawabanBenar.isEmpty()) {
                    Toast.makeText(requireContext(), "Pilih jawaban yang benar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = hashMapOf(
                    "soal" to pertanyaan,
                    "pilihanA" to pilA,
                    "pilihanB" to pilB,
                    "pilihanC" to pilC,
                    "pilihanD" to pilD,
                    "jawabanBenar" to jawabanBenar,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("tugas").document(tugasId).collection("soal")
                    .add(data)
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        
                        db.collection("tugas").document(tugasId)
                            .update("jumlahSoal", com.google.firebase.firestore.FieldValue.increment(1))
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Soal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                loadTugas()
                                if (isAdded) {
                                    AlertDialog.Builder(requireContext())
                                        .setMessage("Soal berhasil disimpan. Tambah soal lagi untuk tugas ini?")
                                        .setPositiveButton("Ya") { d, _ -> 
                                            d.dismiss()
                                            showManageSoalDialog(tugasId, judulTugas) 
                                        }
                                        .setNegativeButton("Selesai", null)
                                        .show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Gagal update jumlah soal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Error Simpan Soal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDeleteTugas(id: String) {
        AlertDialog.Builder(requireContext())
            .setMessage("Hapus tugas ini beserta semua soalnya?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("tugas").document(id)
                    .delete()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Tugas dihapus", Toast.LENGTH_SHORT).show()
                        loadTugas()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Gagal menghapus tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    inner class TugasAdapter(
        val list: List<Tugas>,
        val onManageSoal: (String, String) -> Unit,
        val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<TugasAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvJudul: TextView = v.findViewById(R.id.tvJudulTugasItem)
            val tvMapel: TextView = v.findViewById(R.id.tvMapelTugasItem)
            val tvDeadline: TextView = v.findViewById(R.id.tvDeadlineTugasItem)
            val tvJumlahSoal: TextView = v.findViewById(R.id.tvJumlahSoalItem)
            val btnTambahSoal: Button = v.findViewById(R.id.btnTambahSoal)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteTugas)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_tugas_guru, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvJudul.text = item.judul
            holder.tvMapel.text = item.namaMapel
            holder.tvDeadline.text = "Deadline: ${item.deadline ?: "-"}"
            holder.tvJumlahSoal.text = "${item.jumlahSoal ?: 0} soal"
            holder.btnTambahSoal.setOnClickListener { onManageSoal(item.id, item.judul ?: "") }
            holder.btnDelete.setOnClickListener { onDelete(item.id) }
        }

        override fun getItemCount() = list.size
    }
}
