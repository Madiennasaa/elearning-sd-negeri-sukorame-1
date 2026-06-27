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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageSiswaBinding

class AdminManageSiswaFragment : Fragment() {

    private var _binding: FragmentAdminManageSiswaBinding? = null
    private val binding get() = _binding!!
    private var listSiswa = mutableListOf<Siswa>()
    private var listKelas = listOf<Kelas>()
    private lateinit var adapter: SiswaAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageSiswaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FUNGSI TOMBOL KEMBALI
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = SiswaAdapter(listSiswa, { s -> showSiswaDialog(s) }, { s -> confirmDelete(s) })
        binding.rvSiswa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSiswa.adapter = adapter
        loadData()
        binding.btnTambahSiswa.setOnClickListener { showSiswaDialog(null) }
    }

    private fun loadData() {
        db.collection("kelas").get().addOnSuccessListener { kSnap ->
            if (!isAdded) return@addOnSuccessListener
            listKelas = kSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }
            
            db.collection("siswa").get().addOnSuccessListener { sSnap ->
                if (!isAdded) return@addOnSuccessListener
                listSiswa.clear()
                listSiswa.addAll(sSnap.documents.mapNotNull { it.toObject(Siswa::class.java)?.copy(id = it.id) })
                adapter.notifyDataSetChanged()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSiswaDialog(siswa: Siswa?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        val etNama  = dialogView.findViewById<EditText>(R.id.etNama)
        val etNisn  = dialogView.findViewById<EditText>(R.id.etEmail)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        dialogView.findViewById<EditText>(R.id.etPassword)?.visibility = View.GONE
        dialogView.findViewById<View>(R.id.layoutSelectPersonil)?.visibility = View.GONE

        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listKelas.map { it.namaKelas ?: "-" })

        siswa?.let {
            etNama.setText(it.namaLengkap)
            etNisn.setText(it.nisn)
            val pos = listKelas.indexOfFirst { k -> k.id == it.kelasId }
            if (pos >= 0) spinner.setSelection(pos)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (siswa == null) "Tambah Siswa" else "Edit Siswa")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val selectedKelas = if (listKelas.isNotEmpty()) listKelas[spinner.selectedItemPosition] else null
                val data = hashMapOf(
                    "namaLengkap"  to etNama.text.toString().trim(),
                    "nisn"         to etNisn.text.toString().trim(),
                    "kelasId"      to (selectedKelas?.id ?: "")
                )

                val task = if (siswa != null) {
                    db.collection("siswa").document(siswa.id).update(data as Map<String, Any>)
                } else {
                    db.collection("siswa").add(data)
                }

                task.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Tersimpan", Toast.LENGTH_SHORT).show()
                    loadData()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(siswa: Siswa) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus siswa ${siswa.namaLengkap}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("siswa").document(siswa.id).delete()
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

    inner class SiswaAdapter(val list: List<Siswa>, val onEdit: (Siswa)->Unit, val onDelete: (Siswa)->Unit) : RecyclerView.Adapter<SiswaAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvUserName)
            val tvSub: TextView  = v.findViewById(R.id.tvUserRole)
            val tvIden: TextView = v.findViewById(R.id.tvUserEmail)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_user, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val s = list[pos]
            h.tvName.text  = s.namaLengkap
            val kelas = listKelas.find { it.id == s.kelasId }
            h.tvSub.text   = "Kelas: ${kelas?.namaKelas ?: s.kelasId ?: "-"}"
            h.tvIden.text  = "NISN: ${s.nisn}"
            h.btnEdit.setOnClickListener { onEdit(s) }
            h.btnDelete.setOnClickListener { onDelete(s) }
        }
    }
}
