package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Materi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentMateriBinding

class MateriFragment : Fragment() {
    private var _binding: FragmentMateriBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMateriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.ivBack.setOnClickListener {
            (activity as? HomeActivity)?.backToHome()
        }

        binding.swipeRefresh.setOnRefreshListener {
            val user = sessionManager.getUser()
            loadMateri(user?.kelasId ?: "")
        }

        val user = sessionManager.getUser()
        val kelasId = user?.kelasId ?: ""

        loadMateri(kelasId)
    }

    private fun loadMateri(kelasId: String) {
        if (kelasId.isEmpty()) {
            binding.swipeRefresh.isRefreshing = false
            showEmpty(true)
            return
        }
        binding.swipeRefresh.isRefreshing = true

        db.collection("materi")
            .whereEqualTo("kelasId", kelasId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.swipeRefresh.isRefreshing = false
                if (!isAdded) return@addOnSuccessListener

                val data = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Materi::class.java)?.copy(id = doc.id)
                }

                if (data.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                    binding.rvMateri.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvMateri.adapter = MateriAdapter(data) { materi ->
                        val url = materi.urlVideo?.trim()
                        val tipe = materi.tipe?.trim()
                        val currentCtx = requireContext()
                        when {
                            tipe == "video" && !url.isNullOrEmpty() -> {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    Toast.makeText(currentCtx, "Tidak dapat membuka link video", Toast.LENGTH_SHORT).show()
                                }
                            }
                            !materi.deskripsi.isNullOrEmpty() -> {
                                android.app.AlertDialog.Builder(currentCtx)
                                    .setTitle(materi.judul ?: "Materi")
                                    .setMessage(materi.deskripsi)
                                    .setPositiveButton("Tutup", null)
                                    .show()
                            }
                            !url.isNullOrEmpty() -> {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    Toast.makeText(currentCtx, "Tidak dapat membuka link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                Toast.makeText(currentCtx, "Materi \"${materi.judul}\" belum memiliki konten", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.swipeRefresh.isRefreshing = false
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat materi: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            }
    }

    private fun showEmpty(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvMateri.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}