package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentProfileBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.login.LoginActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.ivBack.setOnClickListener {
            (activity as? HomeActivity)?.backToHome()
        }
        
        // Show local data first
        val user = sessionManager.getUser()
        updateUI(user)
        
        // Refresh from Firestore
        refreshProfile()
        
        binding.btnLogout.setOnClickListener {
            (activity as? HomeActivity)?.performLogout()
        }

        binding.btnDeleteAccount.setOnClickListener {
            val currentUser = sessionManager.getUser()
            if (currentUser?.role == "admin") {
                Toast.makeText(requireContext(), "Admin tidak boleh menghapus akun sendiri!", Toast.LENGTH_SHORT).show()
            } else {
                deleteMyAccount()
            }
        }
    }

    private fun refreshProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val updatedUser = doc.toObject(User::class.java)?.copy(uid = uid)
                if (updatedUser != null) {
                    sessionManager.saveUser(updatedUser)
                    updateUI(updatedUser)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal refresh profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteMyAccount() {
        val user = auth.currentUser
        val uid = user?.uid ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun Permanen")
            .setMessage("Apakah Anda yakin? Semua data Anda akan dihapus secara permanen dan aplikasi akan otomatis logout.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                // 1. Hapus data di Firestore terlebih dahulu
                db.collection("users").document(uid).delete()
                    .addOnSuccessListener {
                        // 2. Jika Firestore berhasil, hapus user di Firebase Auth
                        user.delete()
                            .addOnSuccessListener {
                                // 3. Jika Auth berhasil, bersihkan session lokal dan redirect
                                handleCleanLogout("Akun Anda telah dihapus secara permanen.")
                            }
                            .addOnFailureListener { e ->
                                // Jika gagal hapus Auth (misal: perlu re-login), beri tahu user
                                Toast.makeText(requireContext(), "Gagal hapus autentikasi: ${e.message}. Silakan login ulang dan coba lagi.", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal hapus data Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleCleanLogout(message: String) {
        // Panggil SessionManager untuk hapus SharedPreferences & FirebaseAuth.signOut()
        sessionManager.logout()

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Redirect ke LoginActivity dan bersihkan Backstack
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun updateUI(user: User?) {
        binding.tvName.text = user?.name ?: "User"
        binding.tvRole.text = user?.role?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Role"
        binding.tvEmail.text = user?.email ?: "-"
        binding.tvPhone.text = user?.noHp ?: "-"

        // Restriction: Hide delete button if the user is an admin
        if (user?.role == "admin") {
            binding.btnDeleteAccount.visibility = View.GONE
        } else {
            binding.btnDeleteAccount.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
