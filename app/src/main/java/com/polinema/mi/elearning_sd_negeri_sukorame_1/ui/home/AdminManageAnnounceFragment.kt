package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Pengumuman
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageAnnounceBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminManageAnnounceFragment : Fragment() {

    private var _binding: FragmentAdminManageAnnounceBinding? = null
    private val binding get() = _binding!!
    private val list = mutableListOf<Pengumuman>()
    private lateinit var adapter: AnnounceAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageAnnounceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AnnounceAdapter(list, { p -> showDialog(p) }, { p -> confirmDelete(p) })
        binding.rvAnnounce.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnnounce.adapter = adapter
        loadData()
        binding.btnAddAnnounce.setOnClickListener { showDialog(null) }
    }

    private fun loadData() {
        db.collection("pengumuman")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val data = snapshot.toObjects(Pengumuman::class.java)
                list.clear()
                list.addAll(data)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat pengumuman", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialog(p: Pengumuman?) {
        val isEdit = p != null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_announce_form, null)
        val etJudul   = dialogView.findViewById<EditText>(R.id.etJudul)
        val etIsi     = dialogView.findViewById<EditText>(R.id.etIsi)
        val spKategori = dialogView.findViewById<Spinner>(R.id.spinnerKategori)
        val spUntuk   = dialogView.findViewById<Spinner>(R.id.spinnerUntuk)

        val kategoriList = listOf("Umum", "Akademik", "Keuangan", "Kegiatan")
        val untukList    = listOf("semua", "siswa", "guru", "wali_murid")
        spKategori.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategoriList)
        spUntuk.adapter    = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, untukList)

        if (isEdit) {
            etJudul.setText(p!!.judul)
            etIsi.setText(p.isi)
            val ki = kategoriList.indexOf(p.kategori)
            if (ki >= 0) spKategori.setSelection(ki)
            // Note: 'untuk' field is not in model but we'll try to find it in Firestore doc if needed
            // For now, keep it simple.
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Pengumuman" else "Tambah Pengumuman")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Simpan" else "Tambah") { _, _ ->
                val judul = etJudul.text.toString().trim()
                val isi   = etIsi.text.toString().trim()
                if (judul.isEmpty() || isi.isEmpty()) {
                    Toast.makeText(requireContext(), "Judul dan isi wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                
                val data = mutableMapOf(
                    "judul"    to judul,
                    "isi"      to isi,
                    "kategori" to spKategori.selectedItem.toString(),
                    "untuk"    to spUntuk.selectedItem.toString(),
                    "status"   to "Aktif",
                    "tanggal"  to now
                )

                val task = if (isEdit) {
                    db.collection("pengumuman").document(p!!.id).update(data as Map<String, Any>)
                } else {
                    val newDoc = db.collection("pengumuman").document()
                    data["id"] = newDoc.id
                    newDoc.set(data)
                }

                task.addOnSuccessListener {
                    if (!isAdded) return@addOnSuccessListener
                    Toast.makeText(requireContext(), if (isEdit) "Pengumuman diperbarui" else "Pengumuman ditambahkan", Toast.LENGTH_SHORT).show()
                    loadData()
                }.addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(p: Pengumuman) {
        AlertDialog.Builder(requireContext())
            .setMessage("Hapus \"${p.judul}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("pengumuman").document(p.id).delete()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "Pengumuman dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class AnnounceAdapter(
        private val list: List<Pengumuman>,
        private val onEdit: (Pengumuman) -> Unit,
        private val onDelete: (Pengumuman) -> Unit
    ) : RecyclerView.Adapter<AnnounceAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvJudul: TextView  = v.findViewById(R.id.tvJudul)
            val tvKategori: TextView = v.findViewById(R.id.tvKategori)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
            val btnToggle: Button = v.findViewById(R.id.btnToggle)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_announce, parent, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = list[pos]
            h.tvJudul.text    = p.judul
            h.tvKategori.text = p.kategori
            h.tvStatus.text   = p.tanggal?.take(10) ?: "-"
            h.btnToggle.text  = "Aktif"
            h.btnEdit.setOnClickListener { onEdit(p) }
            h.btnDelete.setOnClickListener { onDelete(p) }
            h.btnToggle.setOnClickListener { /* toggle via API jika ada */ }
        }
    }
}