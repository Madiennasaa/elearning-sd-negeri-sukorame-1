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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageKelasBinding

class AdminManageKelasFragment : Fragment() {

    private var _binding: FragmentAdminManageKelasBinding? = null
    private val binding get() = _binding!!
    private var allKelas = mutableListOf<Kelas>()
    private var guruList = mutableListOf<User>()
    private lateinit var adapter: KelasAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageKelasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = KelasAdapter(allKelas, { k -> showKelasDialog(k) }, { k -> confirmDelete(k) })
        binding.rvKelas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKelas.adapter = adapter
        loadData()
        binding.btnAddKelas.setOnClickListener { showKelasDialog(null) }
    }

    private fun loadData() {
        // Load semua Guru untuk pilihan Wali Kelas
        db.collection("users").whereEqualTo("role", "guru").get()
            .addOnSuccessListener { gSnap ->
                if (!isAdded) return@addOnSuccessListener
                guruList.clear()
                guruList.addAll(gSnap.documents.mapNotNull {
                    it.toObject(User::class.java)?.copy(uid = it.id)
                })
                // Refresh data kelas setelah guru dimuat agar Wali Kelas tampil benar
                loadKelasOnly()
            }
    }

    private fun loadKelasOnly() {
        db.collection("kelas").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allKelas.clear()
                allKelas.addAll(snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat kelas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showKelasDialog(kelas: Kelas?) {
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etNamaKelas = EditText(requireContext()).apply {
            hint = "Nama Kelas (contoh: 1-A)"
            kelas?.let { setText(it.namaKelas) }
        }
        dialogLayout.addView(etNamaKelas)

        val tvPilihGuru = TextView(requireContext()).apply {
            text = "Guru Wali Kelas:"
            setPadding(0, 24, 0, 4)
        }
        dialogLayout.addView(tvPilihGuru)

        val spinnerGuru = Spinner(requireContext())
        val guruOptions = mutableListOf("(Tidak ada wali kelas)")
        guruOptions.addAll(guruList.map { it.name ?: "-" })
        spinnerGuru.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            guruOptions
        )

        // Set seleksi awal jika sedang mengedit
        if (kelas != null) {
            val currentGuruIndex = guruList.indexOfFirst { it.uid == kelas.guruId }
            if (currentGuruIndex >= 0) {
                spinnerGuru.setSelection(currentGuruIndex + 1)
            }
        }
        dialogLayout.addView(spinnerGuru)

        AlertDialog.Builder(requireContext())
            .setTitle(if (kelas == null) "Tambah Kelas" else "Edit Kelas")
            .setView(dialogLayout)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNamaKelas.text.toString().trim()
                if (nama.isEmpty()) return@setPositiveButton

                val selectedGuruPos = spinnerGuru.selectedItemPosition
                val selectedGuru = if (selectedGuruPos > 0) guruList[selectedGuruPos - 1] else null
                
                // Poin 2: Gunakan uid dari akun guru
                val guruUid = selectedGuru?.uid

                // Poin 1: Gunakan document() kosong untuk ID Hash otomatis
                val docRef = if (kelas == null) db.collection("kelas").document() else db.collection("kelas").document(kelas.id)
                val kelasId = docRef.id

                val kelasData = Kelas(
                    id = kelasId, // Simpan ID Hash ke dalam field 'id'
                    namaKelas = nama,
                    tingkat = nama.take(1),
                    guruId = guruUid
                )

                docRef.set(kelasData)
                    .addOnSuccessListener {
                        // Poin 3: Sinkronisasi balik ke data Guru (kelasId)
                        updateGuruKelasId(selectedGuru, kelasId)
                        if (isAdded) Toast.makeText(requireContext(), "Tersimpan", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateGuruKelasId(newGuru: User?, kelasId: String) {
        // 1. Bersihkan kelasId dari Guru lama yang sebelumnya memegang kelas ini
        val previousGuru = guruList.firstOrNull { it.kelasId == kelasId && it.uid != newGuru?.uid }
        previousGuru?.let {
            db.collection("users").document(it.uid).update("kelasId", null)
        }

        // 2. Jika ada guru baru yang dipilih, update kelasId miliknya
        newGuru?.let {
            db.collection("users").document(it.uid).update("kelasId", kelasId)
        }
    }

    private fun confirmDelete(kelas: Kelas) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus kelas ${kelas.namaKelas}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("kelas").document(kelas.id).delete()
                    .addOnSuccessListener {
                        // Bersihkan referensi kelasId di guru saat kelas dihapus
                        val currentGuru = guruList.firstOrNull { it.kelasId == kelas.id }
                        currentGuru?.let {
                            db.collection("users").document(it.uid).update("kelasId", null)
                        }
                        if (isAdded) Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal hapus", Toast.LENGTH_SHORT).show()
                    }
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class KelasAdapter(val list: List<Kelas>, val onEdit: (Kelas)->Unit, val onDelete: (Kelas)->Unit) : RecyclerView.Adapter<KelasAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvUserName)
            val tvSub: TextView  = v.findViewById(R.id.tvUserRole)
            val btnEdit: ImageButton   = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_user, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val k = list[pos]
            h.tvName.text = "Kelas ${k.namaKelas}"
            
            // Cari nama Wali Kelas untuk ditampilkan di sub-label
            val waliKelas = guruList.find { it.uid == k.guruId }
            h.tvSub.text  = if (waliKelas != null) "Wali Kelas: ${waliKelas.name}" else "Belum ada Wali Kelas"

            h.btnEdit.setOnClickListener { onEdit(k) }
            h.btnDelete.setOnClickListener { onDelete(k) }
        }
    }
}
