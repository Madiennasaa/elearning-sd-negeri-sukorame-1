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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageSiswaBinding
import java.util.*

class AdminManageSiswaFragment : Fragment() {

    private var _binding: FragmentAdminManageSiswaBinding? = null
    private val binding get() = _binding!!
    private var listSiswa = mutableListOf<User>()
    private var listKelas = mutableListOf<Kelas>()
    private var listWali = mutableListOf<User>()
    private lateinit var adapter: SiswaAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageSiswaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        // Load Kelas
        db.collection("kelas").get().addOnSuccessListener { kSnap ->
            if (!isAdded) return@addOnSuccessListener
            listKelas.clear()
            listKelas.addAll(kSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) })
            
            // Load Wali Murid (untuk dropdown)
            db.collection("users").whereEqualTo("role", "wali_murid").get().addOnSuccessListener { wSnap ->
                if (!isAdded) return@addOnSuccessListener
                listWali.clear()
                listWali.addAll(wSnap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) })
                
                // Load Siswa
                db.collection("users").whereEqualTo("role", "siswa").get().addOnSuccessListener { sSnap ->
                    if (!isAdded) return@addOnSuccessListener
                    listSiswa.clear()
                    listSiswa.addAll(sSnap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) })
                    adapter.notifyDataSetChanged()
                }
            }
        }.addOnFailureListener {
            if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSiswaDialog(siswa: User?) {
        val isEdit = siswa != null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        
        val etNama   = dialogView.findViewById<EditText>(R.id.etNama)
        val etEmail  = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPass   = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNoHp   = dialogView.findViewById<EditText>(R.id.etNoHp)
        val etNisn   = dialogView.findViewById<EditText>(R.id.etNisn)
        val etTgl    = dialogView.findViewById<EditText>(R.id.etTglLahir)
        val spKelas  = dialogView.findViewById<Spinner>(R.id.spinnerKelas)
        val spGender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val spWali   = dialogView.findViewById<Spinner>(R.id.spinnerPersonil)
        
        // Sembunyikan field yang tidak relevan untuk admin khusus siswa
        dialogView.findViewById<View>(R.id.spinnerRole)?.visibility = View.GONE
        dialogView.findViewById<View>(R.id.layoutGuruFields)?.visibility = View.GONE
        dialogView.findViewById<View>(R.id.layoutSiswaFields)?.visibility = View.VISIBLE
        dialogView.findViewById<View>(R.id.layoutSelectPersonil)?.visibility = View.VISIBLE
        (dialogView.findViewById<TextView>(R.id.tvLabelKelas))?.text = "Pilih Kelas"
        (dialogView.findViewById<View>(R.id.layoutSelectPersonil).findViewById<TextView>(android.R.id.text1) as? TextView)?.text = "Pilih Wali Murid"

        // Setup Spinner Kelas
        val kelasDisplay = mutableListOf("Tanpa Kelas")
        kelasDisplay.addAll(listKelas.map { it.namaKelas ?: "-" })
        spKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasDisplay)

        // Setup Spinner Gender
        val genders = listOf("Laki-laki", "Perempuan")
        spGender.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)

        // Setup Spinner Wali
        val waliDisplay = mutableListOf("Tanpa Wali")
        waliDisplay.addAll(listWali.map { "${it.name} (${it.email})" })
        spWali.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, waliDisplay)

        etTgl.setOnClickListener {
            val c = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                etTgl.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        if (isEdit && siswa != null) {
            etNama.setText(siswa.name)
            etEmail.setText(siswa.email)
            etEmail.isEnabled = false
            etPass.hint = "Kosongkan jika tidak ganti"
            etNoHp.setText(siswa.noHp)
            etNisn.setText(siswa.nisn)
            etTgl.setText(siswa.tanggalLahir)
            
            val kPos = listKelas.indexOfFirst { it.id == siswa.kelasId }
            if (kPos >= 0) spKelas.setSelection(kPos + 1)
            
            val gPos = genders.indexOf(siswa.jenisKelamin)
            if (gPos >= 0) spGender.setSelection(gPos)
            
            val wPos = listWali.indexOfFirst { it.uid == siswa.waliMuridId }
            if (wPos >= 0) spWali.setSelection(wPos + 1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Siswa" else "Tambah Siswa")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString().trim()
                
                if (nama.isEmpty() || email.isEmpty() || (!isEdit && pass.isEmpty())) {
                    Toast.makeText(context, "Data wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val kIdx = spKelas.selectedItemPosition
                val wIdx = spWali.selectedItemPosition
                
                val userObj = User(
                    uid = siswa?.uid ?: "",
                    name = nama,
                    email = email,
                    role = "siswa",
                    noHp = etNoHp.text.toString().trim(),
                    nisn = etNisn.text.toString().trim(),
                    tanggalLahir = etTgl.text.toString().trim(),
                    jenisKelamin = spGender.selectedItem.toString(),
                    kelasId = if (kIdx > 0) listKelas[kIdx - 1].id else null,
                    waliMuridId = if (wIdx > 0) listWali[wIdx - 1].uid else null,
                    idSiswa = siswa?.uid ?: "" // Akan dioverwrite saat pendaftaran baru
                )

                if (isEdit) {
                    db.collection("users").document(siswa!!.uid).set(userObj)
                        .addOnSuccessListener { loadData() }
                } else {
                    // Catatan: Idealnya pendaftaran via Auth, namun di sini diasumsikan 
                    // penambahan data user langsung (ID auto-generate oleh Firestore)
                    db.collection("users").add(userObj).addOnSuccessListener { doc ->
                        doc.update("uid", doc.id, "idSiswa", doc.id).addOnSuccessListener { loadData() }
                    }
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(siswa: User) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus siswa ${siswa.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(siswa.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class SiswaAdapter(val list: List<User>, val onEdit: (User)->Unit, val onDelete: (User)->Unit) : RecyclerView.Adapter<SiswaAdapter.VH>() {
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
            h.tvName.text  = s.name
            val kelas = listKelas.find { it.id == s.kelasId }
            h.tvSub.text   = "Kelas: ${kelas?.namaKelas ?: "Tanpa Kelas"}"
            h.tvIden.text  = "NISN: ${s.nisn ?: "-"}"
            h.btnEdit.setOnClickListener { onEdit(s) }
            h.btnDelete.setOnClickListener { onDelete(s) }
        }
    }
}
