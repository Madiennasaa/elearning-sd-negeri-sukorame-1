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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageGuruBinding

class AdminManageGuruFragment : Fragment() {

    private var _binding: FragmentAdminManageGuruBinding? = null
    private val binding get() = _binding!!
    private var listGuru = mutableListOf<User>()
    private var allKelas = mutableListOf<Kelas>()
    private lateinit var adapter: GuruAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        adapter = GuruAdapter(listGuru, { u -> showGuruDialog(u) }, { u -> confirmDelete(u) })
        binding.rvGuru.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGuru.adapter = adapter
        
        loadKelas()
        loadData()
        
        binding.btnAddGuru.setOnClickListener { showGuruDialog(null) }
    }

    private fun loadKelas() {
        db.collection("kelas").get().addOnSuccessListener { snapshot ->
            allKelas = snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }.toMutableList()
            adapter.notifyDataSetChanged()
        }
    }

    private fun loadData() {
        // Poin 1: Ambil data dari collection 'users' dengan filter role 'guru'
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
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showGuruDialog(guru: User?) {
        val isEdit = guru != null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_guru_form, null)
        
        val etNama = dialogView.findViewById<EditText>(R.id.etNamaGuruForm)
        val etNip  = dialogView.findViewById<EditText>(R.id.etNipGuruForm)
        val etNoHp = dialogView.findViewById<EditText>(R.id.etNoHpGuruForm)
        val spTipe = dialogView.findViewById<Spinner>(R.id.spJenisGuru)
        val spKelas = dialogView.findViewById<Spinner>(R.id.spKelasGuru)

        // Setup Spinners
        val tipeList = listOf("Umum", "Mulok")
        spTipe.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipeList)
        
        val kelasNames = mutableListOf("Tanpa Kelas")
        kelasNames.addAll(allKelas.map { it.namaKelas ?: it.id })
        spKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasNames)

        if (isEdit && guru != null) {
            etNama.setText(guru.name)
            etNip.setText(guru.nip)
            etNoHp.setText(guru.noHp)
            
            val tPos = tipeList.indexOf(guru.tipeGuru?.replaceFirstChar { it.uppercase() })
            if (tPos >= 0) spTipe.setSelection(tPos)
            
            guru.kelasId?.let { kId ->
                val kPos = allKelas.indexOfFirst { it.id == kId }
                if (kPos >= 0) spKelas.setSelection(kPos + 1)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit Guru" else "Tambah Guru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val nip  = etNip.text.toString().trim()
                val noHp = etNoHp.text.toString().trim()
                val tipe = spTipe.selectedItem.toString().lowercase()
                val kIdx = spKelas.selectedItemPosition
                val kId  = if (kIdx > 0) allKelas[kIdx - 1].id else null

                if (nama.isEmpty()) { 
                    Toast.makeText(context, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton 
                }
                
                // Poin 2: Membuat objek User dengan role guru dan field pendukung
                val userObj = User(
                    uid = guru?.uid ?: "",
                    name = nama,
                    nip = nip,
                    noHp = noHp,
                    role = "guru",
                    tipeGuru = tipe,
                    kelasId = kId,
                    idSiswa = null, // Dikosongkan sesuai aturan
                    email = guru?.email ?: "guru_${nip.ifEmpty { System.currentTimeMillis() }}@sukorame.sch.id"
                )

                val docRef = if (isEdit) {
                    db.collection("users").document(guru!!.uid)
                } else {
                    db.collection("users").document() // Atau menggunakan add() jika ingin ID auto
                }

                val finalUser = if (isEdit) userObj else userObj.copy(uid = docRef.id)

                docRef.set(finalUser)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data guru berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
            }.setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(guru: User) {
        AlertDialog.Builder(requireContext()).setMessage("Hapus guru ${guru.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(guru.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data guru dihapus", Toast.LENGTH_SHORT).show()
                        loadData()
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
        
        // Poin 3: Sesuaikan ViewHolder untuk membaca langsung dari properti User
        override fun onBindViewHolder(h: VH, pos: Int) {
            val g = list[pos]
            h.tvNama.text   = g.name
            h.tvNip.text    = "NIP: ${g.nip ?: "-"}"
            h.tvStatus.text = "Tipe: ${g.tipeGuru ?: "umum"}"
            
            val kelas = allKelas.find { it.id == g.kelasId }
            h.tvKelas.text  = "Kelas: ${kelas?.namaKelas ?: "Bukan Wali Kelas"}"

            h.btnEdit.setOnClickListener { onEdit(g) }
            h.btnDelete.setOnClickListener { onDelete(g) }
        }
    }
}
