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
        // Load semua Guru untuk pilihan Wali Kelas agar data kelasId guru terbaru didapat
        db.collection("users").whereEqualTo("role", "guru").get()
            .addOnSuccessListener { gSnap ->
                if (!isAdded) return@addOnSuccessListener
                guruList.clear()
                guruList.addAll(gSnap.documents.mapNotNull {
                    it.toObject(User::class.java)?.copy(uid = it.id)
                })
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
            setPadding(60, 40, 60, 20)
        }

        val etNamaKelas = EditText(requireContext()).apply {
            hint = "Nama Kelas (contoh: 1-A)"
            kelas?.let { setText(it.namaKelas) }
        }
        dialogLayout.addView(etNamaKelas)

        val tvPilihGuru = TextView(requireContext()).apply {
            text = "Pilih Wali Kelas:"
            setPadding(0, 30, 0, 10)
        }
        dialogLayout.addView(tvPilihGuru)

        val spinnerGuru = Spinner(requireContext())
        val guruOptions = mutableListOf("(Tanpa Wali Kelas)")
        guruOptions.addAll(guruList.map { it.name ?: it.email ?: "-" })
        spinnerGuru.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            guruOptions
        )

        if (kelas != null) {
            val currentGuruIndex = guruList.indexOfFirst { it.uid == kelas.guruId }
            if (currentGuruIndex >= 0) spinnerGuru.setSelection(currentGuruIndex + 1)
        }
        dialogLayout.addView(spinnerGuru)

        AlertDialog.Builder(requireContext())
            .setTitle(if (kelas == null) "Tambah Kelas" else "Edit Kelas")
            .setView(dialogLayout)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNamaKelas.text.toString().trim()
                if (nama.isEmpty()) {
                    Toast.makeText(context, "Nama kelas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedGuruPos = spinnerGuru.selectedItemPosition
                val newGuru = if (selectedGuruPos > 0) guruList[selectedGuruPos - 1] else null
                
                // Gunakan ID Hash otomatis jika kelas baru
                val docRef = if (kelas == null) db.collection("kelas").document() else db.collection("kelas").document(kelas.id)
                val kelasId = docRef.id

                val kelasData = Kelas(
                    id = kelasId,
                    namaKelas = nama,
                    tingkat = nama.take(1),
                    guruId = newGuru?.uid
                )

                docRef.set(kelasData)
                    .addOnSuccessListener {
                        // Sinkronisasi dua arah yang robust
                        syncGuruAndKelas(kelas?.guruId, newGuru, kelasId)
                        if (isAdded) Toast.makeText(requireContext(), "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Menangani sinkronisasi dua arah antara Kelas dan Guru Wali Kelas.
     * 1. Menghapus referensi kelasId pada guru lama (jika ada).
     * 2. Menghapus referensi guruId pada kelas lama si guru baru (jika guru tersebut pindah kelas).
     * 3. Menambahkan referensi kelasId pada guru baru.
     */
    private fun syncGuruAndKelas(oldGuruId: String?, newGuru: User?, currentKelasId: String) {
        // 1. Jika guru di kelas ini berubah, bersihkan kelasId dari guru lama
        if (oldGuruId != null && oldGuruId != newGuru?.uid) {
            db.collection("users").document(oldGuruId).update("kelasId", null)
        }

        if (newGuru != null) {
            // 2. Jika guru baru sebelumnya wali di kelas lain, bersihkan guruId di kelas lama tersebut
            if (!newGuru.kelasId.isNullOrEmpty() && newGuru.kelasId != currentKelasId) {
                db.collection("kelas").document(newGuru.kelasId!!).update("guruId", null)
            }

            // 3. Update guru baru dengan kelasId saat ini
            db.collection("users").document(newGuru.uid).update("kelasId", currentKelasId)
        }
    }

    private fun confirmDelete(kelas: Kelas) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Kelas")
            .setMessage("Apakah Anda yakin ingin menghapus kelas ${kelas.namaKelas}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("kelas").document(kelas.id).delete()
                    .addOnSuccessListener {
                        // Bersihkan referensi kelasId pada guru saat kelas dihapus
                        if (!kelas.guruId.isNullOrEmpty()) {
                            db.collection("users").document(kelas.guruId!!).update("kelasId", null)
                        }
                        if (isAdded) Toast.makeText(requireContext(), "Kelas berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal menghapus kelas", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class KelasAdapter(private var list: List<Kelas>, private val onEdit: (Kelas)->Unit, private val onDelete: (Kelas)->Unit) : RecyclerView.Adapter<KelasAdapter.VH>() {
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
            h.tvName.text = String.format("Kelas %s", k.namaKelas)
            
            val waliKelas = guruList.find { it.uid == k.guruId }
            h.tvSub.text  = if (waliKelas != null) "Wali Kelas: ${waliKelas.name}" else "Tanpa Wali Kelas"

            h.btnEdit.setOnClickListener { onEdit(k) }
            h.btnDelete.setOnClickListener { onDelete(k) }
        }
    }
}
