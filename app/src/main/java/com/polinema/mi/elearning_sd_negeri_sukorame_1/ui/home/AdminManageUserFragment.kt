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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminManageUserBinding
import java.util.*

class AdminManageUserFragment : Fragment() {

    private var _binding: FragmentAdminManageUserBinding? = null
    private val binding get() = _binding!!
    private var allUsers = mutableListOf<User>()
    private var allKelas = mutableListOf<Kelas>()
    private lateinit var adapter: UserAdapter
    private val db = FirebaseFirestore.getInstance()

    private var isKepsekRegistered = false

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
        
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        adapter = UserAdapter(allUsers, { u -> showUserDialog(u) }, { u -> confirmDelete(u) })
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        
        loadUsers()
        loadKelas()
        checkKepsekAvailability()

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
                isKepsekRegistered = allUsers.any { it.role == "kepala_sekolah" }
                setupAddButton()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Gagal memuat user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadKelas() {
        db.collection("kelas").get().addOnSuccessListener { snapshot ->
            allKelas = snapshot.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }.toMutableList()
        }
    }

    private fun checkKepsekAvailability() {
        db.collection("users")
            .whereEqualTo("role", "kepala_sekolah")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                isKepsekRegistered = !snapshot.isEmpty
                setupAddButton()
            }
    }

    private fun setupAddButton() {
        val rolesDisplay = mutableListOf("Guru", "Murid", "Wali Murid")
        val rolesValues = mutableListOf("guru", "siswa", "wali_murid")

        if (!isKepsekRegistered) {
            rolesDisplay.add(0, "Kepala Sekolah")
            rolesValues.add(0, "kepala_sekolah")
        }

        binding.btnAddUser.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Pilih Role Pengguna Baru")
                .setItems(rolesDisplay.toTypedArray()) { _, which ->
                    showUserDialog(null, rolesValues[which])
                }
                .show()
        }
    }

    private fun showUserDialog(user: User?, forcedRole: String? = null) {
        val isEdit = user != null
        val roleToUse = (forcedRole ?: user?.role ?: "siswa").lowercase()
        
        if (!isEdit && roleToUse == "admin") {
            Toast.makeText(requireContext(), "Anda tidak diizinkan membuat Admin baru!", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        val etName     = dialogView.findViewById<EditText>(R.id.etNama)
        val etEmail    = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNoHp     = dialogView.findViewById<EditText>(R.id.etNoHp)
        
        val spinnerRole  = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val spinnerKelas = dialogView.findViewById<Spinner>(R.id.spinnerKelas)
        val tvLabelKelas = dialogView.findViewById<TextView>(R.id.tvLabelKelas)
        
        val layoutGuru  = dialogView.findViewById<LinearLayout>(R.id.layoutGuruFields)
        val layoutSiswa = dialogView.findViewById<LinearLayout>(R.id.layoutSiswaFields)
        val layoutSelectPersonil = dialogView.findViewById<LinearLayout>(R.id.layoutSelectPersonil)
        val spinnerPersonil = dialogView.findViewById<Spinner>(R.id.spinnerPersonil)
        
        val etNip       = dialogView.findViewById<EditText>(R.id.etNip)
        val spTipeGuru  = dialogView.findViewById<Spinner>(R.id.spinnerTipeGuru)
        val etNisn      = dialogView.findViewById<EditText>(R.id.etNisn)
        val spGender    = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val etTglLahir  = dialogView.findViewById<EditText>(R.id.etTglLahir)

        val dialogRolesDisplay = mutableListOf("Guru", "Murid", "Wali Murid")
        val dialogRolesValues = mutableListOf("guru", "siswa", "wali_murid")

        if (isEdit && user?.role == "admin") {
            dialogRolesDisplay.add(0, "Admin")
            dialogRolesValues.add(0, "admin")
        }
        if (roleToUse == "kepala_sekolah" || !isKepsekRegistered) {
            dialogRolesDisplay.add("Kepala Sekolah")
            dialogRolesValues.add("kepala_sekolah")
        }

        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dialogRolesDisplay)
        spTipeGuru.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Umum", "Mulok"))
        spGender.adapter   = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Laki-laki", "Perempuan"))
        
        val kelasNames = mutableListOf("Tanpa Kelas")
        kelasNames.addAll(allKelas.map { it.namaKelas ?: it.id })
        spinnerKelas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kelasNames)

        val studentsList = allUsers.filter { it.role == "siswa" }
        val studentNames = studentsList.map { "${it.name} (${it.email})" }
        spinnerPersonil.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, studentNames)

        etTglLahir.setOnClickListener {
            val c = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                etTglLahir.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        fun applyRoleVisibility(r: String) {
            layoutGuru.visibility  = if (r == "guru") View.VISIBLE else View.GONE
            layoutSiswa.visibility = if (r == "siswa") View.VISIBLE else View.GONE
            layoutSelectPersonil.visibility = if (r == "wali_murid") View.VISIBLE else View.GONE
            spinnerKelas.visibility = if (r == "guru" || r == "siswa") View.VISIBLE else View.GONE
            tvLabelKelas.visibility = if (r == "guru" || r == "siswa") View.VISIBLE else View.GONE
            
            if (r == "wali_murid") {
                (layoutSelectPersonil.getChildAt(0) as? TextView)?.text = "Pilih Anak (Siswa)"
            }
        }

        applyRoleVisibility(roleToUse)
        val rPos = dialogRolesValues.indexOf(roleToUse)
        if (rPos >= 0) spinnerRole.setSelection(rPos)
        spinnerRole.isEnabled = isEdit

        if (isEdit && user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etNoHp.setText(user.noHp)
            etEmail.isEnabled = false
            etPassword.hint = "Isi hanya jika ingin GANTI password"
            
            val userKelasId = user.kelasId
            if (!userKelasId.isNullOrEmpty()) {
                val kPos = allKelas.indexOfFirst { it.id == userKelasId }
                if (kPos >= 0) spinnerKelas.setSelection(kPos + 1)
            }

            if (roleToUse == "wali_murid" && !user.idSiswa.isNullOrEmpty()) {
                val sPos = studentsList.indexOfFirst { it.uid == user.idSiswa }
                if (sPos >= 0) spinnerPersonil.setSelection(sPos)
            }
            
            if (roleToUse == "guru") {
                etNip.setText(user.nip)
                user.tipeGuru?.let { t ->
                    val pos = listOf("umum", "mulok").indexOf(t.lowercase())
                    if (pos >= 0) spTipeGuru.setSelection(pos)
                }
            } else if (roleToUse == "siswa") {
                etNisn.setText(user.nisn)
                etTglLahir.setText(user.tanggalLahir)
                user.jenisKelamin?.let { g ->
                    val pos = listOf("Laki-laki", "Perempuan").indexOf(g)
                    if (pos >= 0) spGender.setSelection(pos)
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
                val nisn  = etNisn.text.toString().trim()
                val tipeGuruVal = spTipeGuru.selectedItem.toString().lowercase()
                val kIdx  = spinnerKelas.selectedItemPosition
                val kId   = if (kIdx > 0) allKelas[kIdx - 1].id else null
                val sIdx  = spinnerPersonil.selectedItemPosition
                
                val selectedRoleValue = dialogRolesValues[spinnerRole.selectedItemPosition]

                if (name.isEmpty() || email.isEmpty() || (!isEdit && pass.isEmpty())) {
                    Toast.makeText(context, "Data wajib diisi lengkap", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // MAPPING DATA KE OBJEK USER SESUAI ATURAN BISNIS
                val userModel = User(
                    uid = user?.uid ?: "",
                    name = name,
                    email = email,
                    role = selectedRoleValue,
                    noHp = noHp,
                    tipeGuru = if (selectedRoleValue == "guru") tipeGuruVal else null,
                    idSiswa = when (selectedRoleValue) {
                        "siswa" -> user?.uid 
                        "wali_murid" -> if (sIdx >= 0) studentsList[sIdx].uid else null
                        else -> null
                    },
                    idGuru = null, // Sesuai aturan: diisi null
                    kelasId = if (selectedRoleValue == "guru" || selectedRoleValue == "siswa") kId else null,
                    nip = if (selectedRoleValue == "guru") nip else null,
                    nisn = if (selectedRoleValue == "siswa") nisn else null,
                    jenisKelamin = if (selectedRoleValue == "siswa") spGender.selectedItem.toString() else null,
                    tanggalLahir = if (selectedRoleValue == "siswa") etTglLahir.text.toString() else null
                )

                if (!isEdit) {
                    if (selectedRoleValue == "admin") {
                        Toast.makeText(context, "Dilarang membuat Admin baru!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (selectedRoleValue == "kepala_sekolah" && isKepsekRegistered) {
                        Toast.makeText(requireContext(), "Kepala Sekolah sudah ada!", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                }
                
                executeSaveUser(user, userModel, isEdit, pass)
            }
            .setNeutralButton(if(isEdit) "Reset Password" else null) { _, _ ->
                if (isEdit && user?.email != null) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(user.email)
                        .addOnSuccessListener { Toast.makeText(context, "Link reset dikirim", Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun executeSaveUser(
        user: User?,
        userData: User,
        isEdit: Boolean,
        pass: String
    ) {
        if (isEdit && user != null) {
            // Cara bersih menggunakan set() dengan objek User
            db.collection("users").document(user.uid).set(userData)
                .addOnSuccessListener {
                    handleRoleSync(user.uid, userData)
                    Toast.makeText(context, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    loadUsers()
                }
                .addOnFailureListener { e -> Toast.makeText(context, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show() }
        } else {
            secondaryAuth.createUserWithEmailAndPassword(userData.email!!, pass)
                .addOnSuccessListener { authResult ->
                    val newUid = authResult.user?.uid ?: ""
                    // Aturan: idSiswa diisi UID miliknya sendiri jika role adalah siswa
                    val finalUser = userData.copy(
                        uid = newUid,
                        idSiswa = if (userData.role == "siswa") newUid else userData.idSiswa
                    )
                    
                    db.collection("users").document(newUid).set(finalUser)
                        .addOnSuccessListener {
                            handleRoleSync(newUid, finalUser)
                            Toast.makeText(context, "User berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            secondaryAuth.signOut()
                            loadUsers()
                        }
                        .addOnFailureListener { e -> Toast.makeText(context, "Gagal simpan DB: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
                .addOnFailureListener { e -> Toast.makeText(context, "Error Auth: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun handleRoleSync(uid: String, u: User) {
        // Sync ke koleksi legacy untuk mendukung fitur lain (Jadwal, Absensi, dll)
        when (u.role) {
            "guru" -> {
                val guruMap = hashMapOf("id" to uid, "userId" to uid, "nip" to u.nip, "tipeGuru" to u.tipeGuru)
                db.collection("guru").document(uid).set(guruMap)
                u.kelasId?.let { kId -> db.collection("kelas").document(kId).update("guruId", uid) }
            }
            "siswa" -> {
                val siswaMap = hashMapOf(
                    "id" to uid, "userId" to uid, "namaLengkap" to u.name, "nisn" to u.nisn,
                    "kelasId" to u.kelasId, "jenisKelamin" to u.jenisKelamin, "tanggalLahir" to u.tanggalLahir
                )
                db.collection("siswa").document(uid).set(siswaMap)
            }
        }
    }

    private fun confirmDelete(user: User) {
        if (user.uid == FirebaseAuth.getInstance().currentUser?.uid) {
            AlertDialog.Builder(requireContext()).setTitle("Ditolak").setMessage("Tidak bisa hapus diri sendiri.").show()
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
            h.tvRole.text  = u.role?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
            h.tvEmail.text = u.email
            h.btnEdit.setOnClickListener { onEdit(u) }
            h.btnDelete.setOnClickListener { onDelete(u) }
        }
    }
}
