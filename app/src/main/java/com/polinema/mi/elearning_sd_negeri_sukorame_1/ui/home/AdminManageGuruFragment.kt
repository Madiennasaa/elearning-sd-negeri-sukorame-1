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
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageGuruBinding

class AdminManageGuruFragment : Fragment() {

    private var _binding: FragmentAdminManageGuruBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listGuru = mutableListOf<User>()
    private var listKelas = mutableListOf<Kelas>()
    private lateinit var guruAdapter: GuruAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageGuruBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadData()
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnAddGuru.setOnClickListener { showGuruDialog(null) }
    }

    private fun setupRecyclerView() {
        guruAdapter = GuruAdapter(listGuru, { showGuruDialog(it) }, { confirmDelete(it) })
        binding.rvGuru.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guruAdapter
        }
    }

    private fun loadData() {
        db.collection("kelas").get().addOnSuccessListener { snapshot ->
            listKelas = snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }.toMutableList()
            loadGuru()
        }
    }

    private fun loadGuru() {
        db.collection("users")
            .whereEqualTo("role", "guru")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                listGuru.clear()
                listGuru.addAll(snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) })
                guruAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data guru", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showGuruDialog(guru: User?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_guru_form, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNamaGuruForm)
        val etNip = dialogView.findViewById<EditText>(R.id.etNipGuruForm)
        val etNoHp = dialogView.findViewById<EditText>(R.id.etNoHpGuruForm)
        val spTipe = dialogView.findViewById<Spinner>(R.id.spJenisGuru)
        val spKelas = dialogView.findViewById<Spinner>(R.id.spKelasGuru)

        val tipeOptions = listOf("umum", "mulok")
        spTipe.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipeOptions.map { it.uppercase() })

        val kelasNames = mutableListOf("Bukan Wali Kelas")
        kelasNames.addAll(listKelas.map { it.namaKelas ?: it.id })
        spKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasNames)

        guru?.let {
            etNama.setText(it.name)
            etNip.setText(it.nip)
            etNoHp.setText(it.noHp)
            spTipe.setSelection(tipeOptions.indexOf(it.tipeGuru?.lowercase() ?: "umum").coerceAtLeast(0))
            val kIdx = listKelas.indexOfFirst { k -> k.id == it.kelasId }
            if (kIdx >= 0) spKelas.setSelection(kIdx + 1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (guru == null) "Tambah Guru" else "Edit Guru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString().trim()
                val nip = etNip.text.toString().trim()
                val noHp = etNoHp.text.toString().trim()
                val tipe = tipeOptions[spTipe.selectedItemPosition]
                val kPos = spKelas.selectedItemPosition
                val kId = if (kPos > 0) listKelas[kPos - 1].id else null

                if (nama.isEmpty()) return@setPositiveButton

                val userObj = (guru ?: User()).copy(
                    name = nama,
                    nip = nip,
                    noHp = noHp,
                    role = "guru",
                    tipeGuru = tipe,
                    kelasId = kId,
                    email = guru?.email ?: "guru_${nip.ifEmpty { System.currentTimeMillis() }}@sukorame.sch.id"
                )

                val docRef = if (guru == null) db.collection("users").document() else db.collection("users").document(guru.uid)
                docRef.set(if (guru == null) userObj.copy(uid = docRef.id) else userObj)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDelete(guru: User) {
        AlertDialog.Builder(requireContext())
            .setMessage("Hapus data ${guru.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(guru.uid).delete().addOnSuccessListener { loadGuru() }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class GuruAdapter(
        private val list: List<User>,
        private val onEdit: (User) -> Unit,
        private val onDelete: (User) -> Unit
    ) : RecyclerView.Adapter<GuruAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivFoto: ImageView = v.findViewById(R.id.ivFotoGuru)
            val tvNama: TextView = v.findViewById(R.id.tvNamaGuru)
            val tvNip: TextView = v.findViewById(R.id.tvNipGuru)
            val tvStatus: TextView = v.findViewById(R.id.tvStatusGuru)
            val tvKelas: TextView = v.findViewById(R.id.tvKelasGuru)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEditGuru)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteGuru)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guru, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val g = list[position]
            holder.tvNama.text = g.name ?: "-"
            holder.tvNip.text = "NIP: ${g.nip ?: "-"}"
            holder.tvStatus.text = "Tipe: ${g.tipeGuru ?: "umum"}"
            
            val kelasData = listKelas.find { it.id == g.kelasId }
            holder.tvKelas.text = "Wali Kelas: ${kelasData?.namaKelas ?: "-"}"
            
            Glide.with(holder.itemView.context)
                .load(g.foto)
                .placeholder(R.drawable.ic_user_solid)
                .error(R.drawable.ic_user_solid)
                .into(holder.ivFoto)
            
            holder.btnEdit.setOnClickListener { onEdit(g) }
            holder.btnDelete.setOnClickListener { onDelete(g) }
        }

        override fun getItemCount(): Int = list.size
    }
}
