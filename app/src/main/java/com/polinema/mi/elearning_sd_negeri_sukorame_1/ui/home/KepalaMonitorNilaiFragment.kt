package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Nilai
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentKepalaMonitorNilaiBinding

class KepalaMonitorNilaiFragment : Fragment() {

    private var _binding: FragmentKepalaMonitorNilaiBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    data class NilaiSummary(val namaKelas: String, val namaMapel: String, val rataRata: Double)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKepalaMonitorNilaiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvNilaiMonitor.layoutManager = LinearLayoutManager(requireContext())

        db.collection("nilai").get().addOnSuccessListener { nilaiSnap ->
            db.collection("siswa").get().addOnSuccessListener { siswaSnap ->
                db.collection("kelas").get().addOnSuccessListener { kelasSnap ->
                    if (!isAdded) return@addOnSuccessListener
                    
                    val nilaiList = nilaiSnap.documents.mapNotNull { it.toObject(Nilai::class.java) }
                    val siswaList = siswaSnap.documents.mapNotNull { it.toObject(Siswa::class.java)?.copy(id = it.id) }
                    val kelasList = kelasSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }
                    
                    val siswaToKelas = siswaList.associate { it.id to (it.kelasId ?: "") }
                    val kelasNames   = kelasList.associate { it.id to (it.namaKelas ?: "-") }
                    
                    // Group by (kelasId, namaMapel)
                    val grouped = nilaiList.groupBy { 
                        val kId = siswaToKelas[it.siswaId] ?: ""
                        kId to (it.namaMapel ?: "-")
                    }
                    
                    val data = grouped.mapNotNull { (key, list) ->
                        val (kId, mapel) = key
                        val namaKelas = kelasNames[kId]
                        if (namaKelas != null && kId.isNotEmpty()) {
                            NilaiSummary(namaKelas, mapel, list.map { it.nilai }.average())
                        } else null
                    }.sortedBy { it.namaKelas }
                    
                    binding.rvNilaiMonitor.adapter = NilaiAdapter(data)
                    binding.tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }.addOnFailureListener {
            if (isAdded) binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class NilaiAdapter(private val list: List<NilaiSummary>) : RecyclerView.Adapter<NilaiAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama: TextView  = v.findViewById(R.id.tvNamaSiswa)
            val tvKelas: TextView = v.findViewById(R.id.tvKelas)
            val tvMapel: TextView = v.findViewById(R.id.tvMapel)
            val tvNilai: TextView = v.findViewById(R.id.tvNilaiRata)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_nilai_monitor, parent, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val d = list[pos]
            h.tvNama.text  = "Rata-rata Kelas"
            h.tvKelas.text = "Kelas ${d.namaKelas}"
            h.tvMapel.text = d.namaMapel
            h.tvNilai.text = String.format("%.1f", d.rataRata)
        }
    }
}