package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Tugas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentTugasBinding
import com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.cbt.CbtActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TugasFragment : Fragment() {

    private var _binding: FragmentTugasBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTugasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.ivBack.setOnClickListener {
            (activity as? HomeActivity)?.backToHome()
        }

        val user = sessionManager.getUser()
        val kelasId = user?.kelasId ?: ""
        val siswaId = user?.idSiswa ?: ""

        loadTugas(kelasId, siswaId)
    }

    private fun loadTugas(kelasId: String, siswaId: String) {
        db.collection("hasil_cbt")
            .whereEqualTo("siswaId", siswaId)
            .get()
            .addOnSuccessListener { hasilSnapshot ->
                if (!isAdded) return@addOnSuccessListener
                val completedTugasIds = hasilSnapshot.documents.mapNotNull { it.getString("tugasId") }.toSet()
                
                db.collection("tugas")
                    .whereEqualTo("kelasId", kelasId)
                    .get()
                    .addOnSuccessListener { tugasSnapshot ->
                        if (!isAdded) return@addOnSuccessListener
                        
                        val rawData = tugasSnapshot.documents.mapNotNull { doc ->
                            val t = doc.toObject(Tugas::class.java)?.copy(id = doc.id)
                            t?.copy(sudahDikerjakan = completedTugasIds.contains(doc.id))
                        }
                        
                        if (rawData.isEmpty()) {
                            showEmpty(true)
                        } else {
                            showEmpty(false)
                            processTugas(rawData, siswaId)
                        }
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(requireContext(), "Gagal memuat tugas: ${e.message}", Toast.LENGTH_SHORT).show()
                        showEmpty(true)
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Gagal memuat status pengerjaan: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmpty(true)
            }
    }

    private fun processTugas(rawData: List<Tugas>, siswaId: String) {
        val currentCtx = context ?: return
        
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
        fun parseDeadline(str: String?): Date? {
            if (str.isNullOrBlank()) return null
            for (fmt in dateFormats) {
                try { return fmt.parse(str) } catch (_: Exception) {}
            }
            return null
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val activeTugas = rawData.filter { t ->
            if (t.sudahDikerjakan) return@filter false
            val d = parseDeadline(t.deadline)
            d == null || d.time >= todayStart
        }

        if (activeTugas.isEmpty()) {
            showEmpty(true)
            return
        } else {
            showEmpty(false)
        }

        val sorted = activeTugas.sortedWith(Comparator { a, b ->
            val da = parseDeadline(a.deadline)
            val db = parseDeadline(b.deadline)
            when {
                da == null && db == null -> 0
                da == null -> 1
                db == null -> -1
                else -> da.compareTo(db)
            }
        })

        binding.tvChipAktif.text = "${sorted.size} Aktif"

        binding.rvTugas.layoutManager = LinearLayoutManager(currentCtx)
        binding.rvTugas.adapter = TugasSiswaAdapter(sorted) { tugas ->
            val intent = Intent(currentCtx, CbtActivity::class.java).apply {
                putExtra("TUGAS_ID", tugas.id)
                putExtra("SISWA_ID", siswaId)
            }
            startActivity(intent)
        }
    }

    private fun showEmpty(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmpty.visibility  = View.VISIBLE
            binding.rvTugas.visibility      = View.GONE
            binding.tvChipAktif.text        = "0 Aktif"
        } else {
            binding.layoutEmpty.visibility  = View.GONE
            binding.rvTugas.visibility      = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}