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
        
        setupThemeToggle()
        updateUI(sessionManager.getUser())
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

    private fun setupThemeToggle() {
        binding.switchDarkMode.isChecked = sessionManager.isDarkMode()
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.setDarkMode(isChecked)
            activity?.recreate()
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
    }

    private fun deleteMyAccount() {
        val user = auth.currentUser
        val uid = user?.uid ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Akun")
            .setMessage("Hapus akun permanen?")
            .setPositiveButton("Ya") { _, _ ->
                db.collection("users").document(uid).delete().addOnSuccessListener {
                    user.delete().addOnSuccessListener { handleCleanLogout("Akun dihapus.") }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleCleanLogout(message: String) {
        sessionManager.logout()
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun updateUI(user: User?) {
        binding.tvName.text = user?.name ?: "User"
        binding.tvRole.text = user?.role ?: "Role"
        binding.tvEmail.text = user?.email ?: "-"
        binding.tvPhone.text = user?.noHp ?: "-"
        binding.btnDeleteAccount.visibility = if (user?.role == "admin") View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
