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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageGuruBinding

class AdminManageGuruFragment : Fragment() {

    private var _binding: FragmentAdminManageGuruBinding? = null
    private val binding get() = _binding!!
    private var listGuru = mutableListOf<User>()
    private lateinit var adapter: GuruAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // FUNGSI TOMBOL KEMBALI
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = GuruAdapter(listGuru, { u -> showGuruDialog(u) }, { u -> confirmDelete(u) })
        binding.rvGuru.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGuru.adapter = adapter
        loadData()
        binding.btnAddGuru.setOnClickListener { showGuruDialog(null) }
    }

    private fun loadData() {
        db.collection("users")
            .whereEqualTo("role", "guru")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                listGuru.clear()
                listGuru.addAll(snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showGuruDialog(guru: User?) {
        val isEdit = guru != null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_guru_form, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNamaGuruForm)
        val etNip  = dialogView.findViewById<EditText>(R.id.etNipGuruForm)
        val etNoHp = dialogView.findViewById<EditText>(R.id.etNoHpGuruForm)

        if (isEdit) {
            etNama.setText(guru!!.name)
            etNoHp.setText(guru.noHp)
            etNip.setText(guru.email?.substringBefore("@") ?: "")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Guru" else "Tambah Guru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val nip  = etNip.text.toString().trim()
                val noHp = etNoHp.text.toString().trim()
                if (nama.isEmpty()) { Toast.makeText(context, "Nama wajib diisi", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                
                val data = hashMapOf(
                    "name" to nama,
                    "noHp" to noHp,
                    "role" to "guru",
                    "email" to "guru_${nip.ifEmpty { System.currentTimeMillis().toString() }}@sukorame.sch.id",
                    "idGuru" to nip.ifEmpty { System.currentTimeMillis().toString() }
                )

                val task = if (isEdit) {
                    db.collection("users").document(guru!!.uid).update(data as Map<String, Any>)
                } else {
                    db.collection("users").add(data)
                }

                task.addOnSuccessListener {
                    Toast.makeText(context, "Tersimpan", Toast.LENGTH_SHORT).show()
                    loadData()
                }.addOnFailureListener {
                    Toast.makeText(context, "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(guru: User) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus guru ${guru.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(guru.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal hapus", Toast.LENGTH_SHORT).show()
                    }
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class GuruAdapter(var list: List<User>, val onEdit: (User)->Unit, val onDelete: (User)->Unit) : RecyclerView.Adapter<GuruAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama: TextView   = v.findViewById(R.id.tvNamaGuru)
            val tvNip: TextView    = v.findViewById(R.id.tvNipGuru)
            val tvStatus: TextView = v.findViewById(R.id.tvStatusGuru)
            val tvKelas: TextView  = v.findViewById(R.id.tvKelasGuru)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEditGuru)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteGuru)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_guru, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val g = list[pos]
            h.tvNama.text   = g.name
            h.tvNip.text    = "Email: ${g.email ?: "-"}"
            h.tvStatus.text = g.tipeGuru ?: "umum"
            h.tvKelas.text  = "ID Guru: ${g.idGuru ?: "-"}"
            h.btnEdit.setOnClickListener { onEdit(g) }
            h.btnDelete.setOnClickListener { onDelete(g) }
        }
    }
}
