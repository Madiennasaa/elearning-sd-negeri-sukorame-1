package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Sekolah
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminSchoolInfoBinding

class AdminSchoolInfoFragment : Fragment() {
    private var _binding: FragmentAdminSchoolInfoBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSchoolInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
        binding.btnSimpanSekolah.setOnClickListener { saveData() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun loadData() {
        db.collection("sekolah").document("info").get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val s = doc.toObject(Sekolah::class.java)
                if (s != null) {
                    binding.etNpsn.setText(s.npsn)
                    binding.etNamaSekolah.setText(s.nama)
                    binding.etAlamat.setText(s.alamat)
                    binding.etKepala.setText(s.kepalaSekolah)
                    binding.etAkreditasi.setText(s.akreditasi)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveData() {
        val nama = binding.etNamaSekolah.text.toString().trim()
        if (nama.isEmpty()) {
            Toast.makeText(requireContext(), "Nama sekolah wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        
        val data = hashMapOf(
            "npsn"          to binding.etNpsn.text.toString().trim(),
            "nama"          to nama,
            "alamat"        to binding.etAlamat.text.toString().trim(),
            "kepalaSekolah" to binding.etKepala.text.toString().trim(),
            "akreditasi"    to binding.etAkreditasi.text.toString().trim()
        )

        db.collection("sekolah").document("info").set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data sekolah berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}