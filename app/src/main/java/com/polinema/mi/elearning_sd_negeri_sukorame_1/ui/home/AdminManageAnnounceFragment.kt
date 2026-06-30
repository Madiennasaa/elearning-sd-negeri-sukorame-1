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
    private val listAnnounce = mutableListOf<Pengumuman>()
    private lateinit var adapter: AnnounceAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageAnnounceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        adapter = AnnounceAdapter(listAnnounce, { p -> showDialog(p) }, { p -> confirmDelete(p) })
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
                listAnnounce.clear()
                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Pengumuman::class.java)?.copy(id = doc.id)
                }
                listAnnounce.addAll(data)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat pengumuman", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialog(p: Pengumuman?) {
        val isEdit = p != null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_announce_form, null)
        val etJudul    = dialogView.findViewById<EditText>(R.id.etJudul)
        val etIsi      = dialogView.findViewById<EditText>(R.id.etIsi)
        val spKategori = dialogView.findViewById<Spinner>(R.id.spinnerKategori)
        val spUntuk    = dialogView.findViewById<Spinner>(R.id.spinnerUntuk)

        val kategoriList = listOf("Umum", "Akademik", "Keuangan", "Kegiatan")
        val untukList    = listOf("semua", "siswa", "guru", "wali_murid")
        spKategori.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategoriList)
        spUntuk.adapter    = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, untukList)

        if (isEdit && p != null) {
            etJudul.setText(p.judul)
            etIsi.setText(p.isi)
            spKategori.setSelection(kategoriList.indexOf(p.kategori).coerceAtLeast(0))
            spUntuk.setSelection(untukList.indexOf(p.untuk).coerceAtLeast(0))
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Pengumuman" else "Tambah Pengumuman")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val judul = etJudul.text.toString().trim()
                val isi   = etIsi.text.toString().trim()
                
                if (judul.isEmpty() || isi.isEmpty()) {
                    Toast.makeText(requireContext(), "Judul dan isi wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val docRef = if (isEdit) db.collection("pengumuman").document(p!!.id) 
                             else db.collection("pengumuman").document()

                val newPengumuman = Pengumuman(
                    id = docRef.id,
                    judul = judul,
                    isi = isi,
                    kategori = spKategori.selectedItem.toString(),
                    untuk = spUntuk.selectedItem.toString(),
                    tanggal = p?.tanggal ?: now,
                    status = p?.status ?: "Aktif"
                )

                docRef.set(newPengumuman)
                    .addOnSuccessListener {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(p: Pengumuman) {
        AlertDialog.Builder(requireContext())
            .setMessage("Hapus pengumuman \"${p.judul}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("pengumuman").document(p.id).delete()
                    .addOnSuccessListener {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
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
            val tvKat: TextView    = v.findViewById(R.id.tvKategori)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
            val tvIsi: TextView    = v.findViewById(R.id.tvIsi)
            val btnToggle: Button  = v.findViewById(R.id.btnToggle)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_announce, parent, false))
            
        override fun getItemCount() = list.size
        
        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = list[pos]
            h.tvJudul.text = p.judul
            h.tvKat.text   = p.kategori?.uppercase()
            h.tvStatus.text = "Untuk: ${p.untuk} • ${p.tanggal?.take(10)}"
            h.tvIsi.text   = p.isi
            
            // Logika sederhana expanded/collapsed (opsional, default tampil singkat)
            var isExpanded = false
            h.tvIsi.maxLines = 2
            h.btnToggle.setOnClickListener {
                isExpanded = !isExpanded
                h.tvIsi.maxLines = if (isExpanded) Int.MAX_VALUE else 2
                h.btnToggle.text = if (isExpanded) "Tutup" else "Lihat Selengkapnya"
            }

            h.btnEdit.setOnClickListener { onEdit(p) }
            h.btnDelete.setOnClickListener { onDelete(p) }
        }
    }
}
