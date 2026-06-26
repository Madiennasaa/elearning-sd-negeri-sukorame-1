package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Sekolah
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentKepalaInfoSekolahBinding

class KepalaInfoSekolahFragment : Fragment() {
    private var _binding: FragmentKepalaInfoSekolahBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKepalaInfoSekolahBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db.collection("sekolah").document("info").get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val s = doc.toObject(Sekolah::class.java)
                binding.tvNpsn.text         = "NPSN: ${s?.npsn ?: "-"}"
                binding.tvNamaSekolah.text  = s?.nama ?: "-"
                binding.tvAlamat.text        = s?.alamat ?: "-"
                binding.tvKepala.text        = "Kepala Sekolah: ${s?.kepalaSekolah ?: "-"}"
                binding.tvAkreditasi.text    = "Akreditasi: ${s?.akreditasi ?: "-"}"
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                binding.tvNamaSekolah.text = "Gagal memuat data sekolah"
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}