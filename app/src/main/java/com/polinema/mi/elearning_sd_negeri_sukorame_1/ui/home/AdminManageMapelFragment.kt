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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageMapelBinding

class AdminManageMapelFragment : Fragment() {

    private var _binding: FragmentAdminManageMapelBinding? = null
    private val binding get() = _binding!!
    private var allMapel = mutableListOf<MataPelajaranData>()
    private lateinit var adapter: MapelAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageMapelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = MapelAdapter(allMapel, { m -> showMapelDialog(m) }, { m -> confirmDelete(m) })
        binding.rvMapel.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMapel.adapter = adapter
        loadData()
        binding.btnAddMapel.setOnClickListener { showMapelDialog(null) }
    }

    private fun loadData() {
        // Poin 1: Menggunakan nama koleksi "mapel"
        db.collection("mapel").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allMapel.clear()
                allMapel.addAll(snapshot.documents.mapNotNull { it.toObject(MataPelajaranData::class.java)?.copy(id = it.id) })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat mapel", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMapelDialog(mapel: MataPelajaranData?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mapel_form, null)
        val etKode  = dialogView.findViewById<EditText>(R.id.etKodeMapel)
        val etNama  = dialogView.findViewById<EditText>(R.id.etNamaMapel)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerJenisMapel)
        
        val options = listOf("wajib", "mulok")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        
        mapel?.let {
            etNama.setText(it.nama)
            etKode.setText(it.kode)
            val pos = options.indexOf(it.jenis.lowercase())
            if (pos >= 0) spinner.setSelection(pos)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (mapel == null) "Tambah Mapel" else "Edit Mapel")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val kode = etKode.text.toString().trim()
                val jenis = spinner.selectedItem.toString()

                if (nama.isEmpty() || kode.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama dan Kode wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Poin 2: Menggunakan Hash ID otomatis dan menyimpannya ke field 'id'
                val docRef = if (mapel == null) {
                    db.collection("mapel").document() // Reference kosong untuk Auto-ID
                } else {
                    db.collection("mapel").document(mapel.id)
                }

                val dataMapel = MataPelajaranData(
                    id = docRef.id, // Simpan Hash ID ke field property id
                    nama = nama,
                    kode = kode,
                    jenis = jenis
                )

                docRef.set(dataMapel)
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Mapel berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDelete(mapel: MataPelajaranData) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus mapel ${mapel.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("mapel").document(mapel.id).delete()
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Mapel dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal hapus", Toast.LENGTH_SHORT).show()
                    }
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class MapelAdapter(val list: List<MataPelajaranData>, val onEdit: (MataPelajaranData)->Unit, val onDelete: (MataPelajaranData)->Unit) : RecyclerView.Adapter<MapelAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvUserName)
            val tvSub: TextView  = v.findViewById(R.id.tvUserRole)
            val btnEdit: ImageButton   = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_user, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val m = list[pos]
            h.tvName.text = m.nama
            h.tvSub.text  = "Jenis: ${m.jenis.uppercase()} | Kode: ${m.kode}"
            h.btnEdit.setOnClickListener { onEdit(m) }
            h.btnDelete.setOnClickListener { onDelete(m) }
        }
    }
}
