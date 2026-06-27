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

        // FUNGSI TOMBOL KEMBALI
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
        db.collection("users").whereEqualTo("role", "guru").get()
            .addOnSuccessListener { gSnap ->
                if (!isAdded) return@addOnSuccessListener
                guruList.clear()
                guruList.addAll(gSnap.documents.mapNotNull {
                    it.toObject(User::class.java)?.copy(uid = it.id)
                })
            }

        db.collection("kelas").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allKelas.clear()
                allKelas.addAll(snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat kelas", Toast.LENGTH_SHORT).show()
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

        if (kelas != null) {
            val existingGuru = guruList.indexOfFirst { user ->
                user.kelasId == kelas.id
            }
            if (existingGuru >= 0) {
                spinnerGuru.setSelection(existingGuru + 1)
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
                val guruId = selectedGuru?.idGuru

                val data = hashMapOf(
                    "namaKelas" to nama,
                    "tingkat"   to nama.take(1),
                    "guruId"    to guruId
                )

                if (kelas != null) {
                    db.collection("kelas").document(kelas.id).update(data as Map<String, Any>)
                        .addOnSuccessListener {
                            updateGuruKelasId(selectedGuru, kelas.id)
                            Toast.makeText(requireContext(), "Tersimpan", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("kelas").add(data)
                        .addOnSuccessListener { docRef ->
                            updateGuruKelasId(selectedGuru, docRef.id)
                            Toast.makeText(requireContext(), "Tersimpan", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateGuruKelasId(guru: User?, kelasId: String) {
        if (guru == null) return
        val guruLama = guruList.firstOrNull { it.kelasId == kelasId && it.uid != guru.uid }
        guruLama?.let {
            db.collection("users").document(it.uid).update("kelasId", null)
        }
        db.collection("users").document(guru.uid).update("kelasId", kelasId)
    }

    private fun confirmDelete(kelas: Kelas) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus kelas ${kelas.namaKelas}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("kelas").document(kelas.id).delete()
                    .addOnSuccessListener {
                        val guruYangPegang = guruList.firstOrNull { it.kelasId == kelas.id }
                        guruYangPegang?.let {
                            db.collection("users").document(it.uid).update("kelasId", null)
                        }
                        Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal hapus", Toast.LENGTH_SHORT).show()
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
            h.tvSub.text  = "Tingkat ${k.tingkat ?: k.namaKelas?.take(1) ?: "-"}"
            h.btnEdit.setOnClickListener { onEdit(k) }
            h.btnDelete.setOnClickListener { onDelete(k) }
        }
    }
}
