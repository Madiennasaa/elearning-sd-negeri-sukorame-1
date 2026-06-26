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
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageUserBinding

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas

class AdminManageUserFragment : Fragment() {

    private var _binding: FragmentAdminManageUserBinding? = null
    private val binding get() = _binding!!
    private var allUsers = mutableListOf<User>()
    private var allKelas = mutableListOf<Kelas>()
    private lateinit var adapter: UserAdapter
    private val db = FirebaseFirestore.getInstance()

    // Secondary Auth to create users without logging out Admin
    private val secondaryAuth: FirebaseAuth by lazy {
        val options = FirebaseApp.getInstance().options
        val name = "SecondaryAuthApp"
        val secondaryApp = try {
            FirebaseApp.initializeApp(requireContext(), options, name)
        } catch (e: Exception) {
            FirebaseApp.getInstance(name)
        }
        FirebaseAuth.getInstance(secondaryApp)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminManageUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = UserAdapter(allUsers, { u -> showUserDialog(u) }, { u -> confirmDelete(u) })
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        loadUsers()
        loadKelas()
        binding.btnAddUser.setOnClickListener { showRoleSelectionDialog() }
        binding.searchUser.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(t: String?): Boolean {
                adapter.updateList(allUsers.filter {
                    it.name?.contains(t ?: "", ignoreCase = true) == true ||
                            it.email?.contains(t ?: "", ignoreCase = true) == true
                })
                return true
            }
        })
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                allUsers.clear()
                allUsers.addAll(snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) })
                adapter.updateList(allUsers)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadKelas() {
        db.collection("kelas").get().addOnSuccessListener { snapshot ->
            allKelas = snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }.toMutableList()
        }
    }

    private fun showRoleSelectionDialog() {
        val roles = listOf("Admin", "Guru", "Siswa", "Wali Murid", "Kepala Sekolah")
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Role Pengguna Baru")
            .setItems(roles.toTypedArray()) { _, which ->
                val selectedRole = roles[which].lowercase().replace(" ", "_")
                showUserDialog(null, selectedRole)
            }
            .show()
    }

    private fun showUserDialog(user: User?, forcedRole: String? = null) {
        val isEdit = user != null
        val roleToUse = (forcedRole ?: user?.role ?: "admin").lowercase()
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        val etName     = dialogView.findViewById<EditText>(R.id.etNama)
        val etEmail    = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNoHp     = dialogView.findViewById<EditText>(R.id.etNoHp)
        
        val spinnerRole  = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val spinnerKelas = dialogView.findViewById<Spinner>(R.id.spinnerKelas)
        val tvLabelKelas = dialogView.findViewById<TextView>(R.id.tvLabelKelas)
        
        // Role-Specific Containers
        val layoutGuru  = dialogView.findViewById<LinearLayout>(R.id.layoutGuruFields)
        val layoutSiswa = dialogView.findViewById<LinearLayout>(R.id.layoutSiswaFields)
        
        // Role-Specific Inputs
        val etNip       = dialogView.findViewById<EditText>(R.id.etNip)
        val spTipeGuru  = dialogView.findViewById<Spinner>(R.id.spinnerTipeGuru)
        val etNisn      = dialogView.findViewById<EditText>(R.id.etNisn)
        val spGender    = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val etTglLahir  = dialogView.findViewById<EditText>(R.id.etTglLahir)

        val roles = listOf("admin", "guru", "siswa", "wali_murid", "kepala_sekolah")
        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)
        
        spTipeGuru.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Umum", "Mulok"))
        spGender.adapter   = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Laki-laki", "Perempuan"))
        
        val kelasNames = mutableListOf("Tanpa Kelas")
        kelasNames.addAll(allKelas.map { it.namaKelas ?: it.id })
        spinnerKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasNames)

        etTglLahir.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                etTglLahir.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        fun applyRoleVisibility(r: String) {
            layoutGuru.visibility  = if (r == "guru") View.VISIBLE else View.GONE
            layoutSiswa.visibility = if (r == "siswa") View.VISIBLE else View.GONE
            spinnerKelas.visibility = if (r == "guru" || r == "siswa") View.VISIBLE else View.GONE
            tvLabelKelas.visibility = if (r == "guru" || r == "siswa") View.VISIBLE else View.GONE
        }

        applyRoleVisibility(roleToUse)
        val rPos = roles.indexOf(roleToUse)
        if (rPos >= 0) spinnerRole.setSelection(rPos)
        spinnerRole.isEnabled = false

        if (isEdit) {
            etName.setText(user?.name)
            etEmail.setText(user?.email)
            etNoHp.setText(user?.noHp)
            etEmail.isEnabled = false
            etPassword.hint = "Isi hanya jika ingin GANTI password"
            
            val userKelasId = user?.kelasId
            if (!userKelasId.isNullOrEmpty()) {
                val kPos = allKelas.indexOfFirst { it.id == userKelasId }
                if (kPos >= 0) spinnerKelas.setSelection(kPos + 1)
            }
            
            if (roleToUse == "guru") {
                db.collection("guru").document(user!!.idGuru ?: user.uid).get().addOnSuccessListener { d ->
                    etNip.setText(d.getString("nip"))
                    val t = d.getString("tipeGuru")
                    if (t != null) spTipeGuru.setSelection(listOf("Umum", "Mulok").indexOf(t).coerceAtLeast(0))
                }
            } else if (roleToUse == "siswa") {
                db.collection("siswa").document(user!!.idSiswa ?: user.uid).get().addOnSuccessListener { d ->
                    etNisn.setText(d.getString("nisn"))
                    etTglLahir.setText(d.getString("tanggalLahir"))
                    val g = d.getString("jenisKelamin")
                    if (g != null) spGender.setSelection(listOf("Laki-laki", "Perempuan").indexOf(g).coerceAtLeast(0))
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) "Edit $roleToUse" else "Tambah $roleToUse Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name  = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val pass  = etPassword.text.toString().trim()
                val noHp  = etNoHp.text.toString().trim()
                val nip   = etNip.text.toString().trim()
                val tipeGuru = spTipeGuru.selectedItem.toString()
                val kIdx  = spinnerKelas.selectedItemPosition
                val kId   = if (kIdx > 0) allKelas[kIdx - 1].id else null

                if (name.isEmpty() || email.isEmpty() || (!isEdit && pass.isEmpty())) {
                    Toast.makeText(context, "Data wajib diisi lengkap", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val userData = mutableMapOf<String, Any?>(
                    "name" to name,
                    "email" to email,
                    "role" to roleToUse,
                    "noHp" to noHp,
                    "kelasId" to kId,
                    "tipeGuru" to if (roleToUse == "guru") tipeGuru else null,
                    "idGuru" to if (roleToUse == "guru") (user?.idGuru ?: "") else null,
                    "idSiswa" to if (roleToUse == "siswa") (user?.idSiswa ?: "") else null
                )

                if (isEdit) {
                    db.collection("users").document(user!!.uid).update(userData)
                        .addOnSuccessListener {
                            if (roleToUse == "guru") {
                                val gId = user.idGuru ?: user.uid
                                db.collection("guru").document(gId).set(hashMapOf(
                                    "id" to gId, "userId" to user.uid, 
                                    "nip" to nip, "tipeGuru" to tipeGuru
                                ))
                                if (kId != null) db.collection("kelas").document(kId).update("guruId", gId)
                            } else if (roleToUse == "siswa") {
                                val sId = user.idSiswa ?: user.uid
                                db.collection("siswa").document(sId).set(hashMapOf(
                                    "id" to sId, "userId" to user.uid, "namaLengkap" to name,
                                    "nisn" to etNisn.text.toString(), "kelasId" to kId,
                                    "jenisKelamin" to spGender.selectedItem.toString(), "tanggalLahir" to etTglLahir.text.toString()
                                ))
                            }
                            Toast.makeText(context, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                } else {
                    secondaryAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { authResult ->
                            val newUid = authResult.user?.uid ?: ""
                            userData["uid"] = newUid
                            userData["idGuru"] = if (roleToUse == "guru") newUid else null
                            userData["idSiswa"] = if (roleToUse == "siswa") newUid else null
                            
                            db.collection("users").document(newUid).set(userData)
                                .addOnSuccessListener {
                                    if (roleToUse == "guru") {
                                        db.collection("guru").document(newUid).set(hashMapOf(
                                            "id" to newUid, "userId" to newUid, 
                                            "nip" to nip, "tipeGuru" to tipeGuru
                                        ))
                                        if (kId != null) db.collection("kelas").document(kId).update("guruId", newUid)
                                    } else if (roleToUse == "siswa") {
                                        db.collection("siswa").document(newUid).set(hashMapOf(
                                            "id" to newUid, "userId" to newUid, "namaLengkap" to name,
                                            "nisn" to etNisn.text.toString(), "kelasId" to kId,
                                            "jenisKelamin" to spGender.selectedItem.toString(), "tanggalLahir" to etTglLahir.text.toString()
                                        ))
                                    }
                                    Toast.makeText(context, "User $roleToUse berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                    secondaryAuth.signOut()
                                    loadUsers()
                                }
                        }
                        .addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            }
            .setNeutralButton(if(isEdit) "Reset Password" else null) { _, _ ->
                if (isEdit && !user?.email.isNullOrEmpty()) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(user!!.email!!)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Link reset password telah dikirim ke email user", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal mengirim link: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun confirmDelete(user: User) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (user.uid == currentUid) {
            AlertDialog.Builder(requireContext())
                .setTitle("Tindakan Ditolak")
                .setMessage("Anda tidak dapat menghapus akun Anda sendiri dari halaman manajemen ini. Gunakan menu 'Hapus Akun' di profil jika ingin menghapus akun sendiri.")
                .setPositiveButton("Mengerti", null)
                .show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setMessage("Hapus akun ${user.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("users").document(user.uid).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "User berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal hapus: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class UserAdapter(private var list: List<User>, private val onEdit: (User)->Unit, private val onDelete: (User)->Unit) : RecyclerView.Adapter<UserAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvUserName)
            val tvRole: TextView = v.findViewById(R.id.tvUserRole)
            val tvEmail: TextView = v.findViewById(R.id.tvUserEmail)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }
        fun updateList(l: List<User>) { list = l; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val u = list[pos]
            h.tvName.text  = u.name
            h.tvRole.text  = u.role?.replaceFirstChar { it.uppercase() }
            h.tvEmail.text = u.email
            h.btnEdit.setOnClickListener { onEdit(u) }
            h.btnDelete.setOnClickListener { onDelete(u) }
        }
    }
}
