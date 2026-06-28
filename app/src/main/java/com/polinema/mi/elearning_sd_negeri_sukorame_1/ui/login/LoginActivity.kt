package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivityLoginBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home.HomeActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            // Cek role user yang tersimpan di session untuk redirect otomatis
            val user = sessionManager.getUser()
            user?.role?.let { navigateByRole(it) } ?: goToHome() // fallback
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etNisn.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi Email dan Kata Sandi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        // 1. Firebase Auth Login
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // 2. Ambil data profil dari koleksi 'users' satu kali (get)
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val user = doc.toObject(User::class.java)?.copy(uid = uid)
                        if (user != null) {
                            // Simpan ke session lokal
                            sessionManager.saveUser(user)
                            Toast.makeText(this, "Selamat Datang, ${user.name}!", Toast.LENGTH_SHORT).show()

                            // 3. Logika navigasi berdasarkan ROLE
                            navigateByRole(user.role ?: "siswa")
                        } else {
                            showError("Data profil tidak ditemukan di database.")
                        }
                    }
                    .addOnFailureListener { showError("Gagal memuat profil: ${it.message}") }
            }
            .addOnFailureListener { showError("Email atau Password salah.") }
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
    }

    private fun navigateByRole(role: String) {
        // Gunakan HomeActivity karena saat ini pusat dashboard ada di sana (via Fragment)
        // Jika Anda membuat Activity baru (AdminActivity, dsb), ganti HomeActivity::class.java di bawah.
        val intent = when (role.lowercase()) {
            "admin"          -> Intent(this, HomeActivity::class.java)
            "guru"           -> Intent(this, HomeActivity::class.java)
            "siswa"          -> Intent(this, HomeActivity::class.java)
            "wali_murid"      -> Intent(this, HomeActivity::class.java)
            "kepala_sekolah" -> Intent(this, HomeActivity::class.java)
            else             -> Intent(this, HomeActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
