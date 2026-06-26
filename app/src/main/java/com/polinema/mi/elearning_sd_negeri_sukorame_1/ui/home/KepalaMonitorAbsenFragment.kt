package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Absensi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentKepalaMonitorAbsenBinding

class KepalaMonitorAbsenFragment : Fragment() {

    private var _binding: FragmentKepalaMonitorAbsenBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    data class AbsenSummary(val namaSiswa: String, val namaKelas: String, val hadir: Int, val sakit: Int, val izin: Int, val alpha: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKepalaMonitorAbsenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAbsenMonitor.layoutManager = LinearLayoutManager(requireContext())

        db.collection("absensi").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val raw = snapshot.documents.mapNotNull { it.toObject(Absensi::class.java) }
                .filter { !it.siswaId.isNullOrEmpty() && !it.namaSiswa.isNullOrEmpty() }
            
            val summaryMap = mutableMapOf<String, AbsenSummary>()
            raw.forEach { a ->
                val key = a.siswaId ?: ""
                val nama = a.namaSiswa ?: "Siswa"
                val kelas = a.namaKelas ?: "-"
                val cur = summaryMap[key] ?: AbsenSummary(nama, kelas, 0, 0, 0, 0)
                summaryMap[key] = when (a.status?.lowercase()) {
                    "hadir"  -> cur.copy(hadir = cur.hadir + 1)
                    "sakit"  -> cur.copy(sakit = cur.sakit + 1)
                    "izin"   -> cur.copy(izin  = cur.izin  + 1)
                    "alpha"  -> cur.copy(alpha = cur.alpha + 1)
                    else     -> cur
                }
            }
            val data = summaryMap.values.toList()
            binding.rvAbsenMonitor.adapter = AbsenAdapter(data)
            binding.tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        }.addOnFailureListener {
            binding.tvEmpty.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class AbsenAdapter(private val list: List<AbsenSummary>) : RecyclerView.Adapter<AbsenAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama: TextView = v.findViewById(R.id.tvNamaSiswa)
            val tvKelas: TextView = v.findViewById(R.id.tvKelas)
            val tvHadir: TextView = v.findViewById(R.id.tvHadir)
            val tvSakit: TextView = v.findViewById(R.id.tvSakit)
            val tvIzin: TextView = v.findViewById(R.id.tvIzin)
            val tvAlpha: TextView = v.findViewById(R.id.tvAlpha)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_absen_monitor, parent, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val d = list[pos]
            h.tvNama.text  = d.namaSiswa
            h.tvKelas.text = d.namaKelas
            h.tvHadir.text = "H: ${d.hadir}"
            h.tvSakit.text = "S: ${d.sakit}"
            h.tvIzin.text  = "I: ${d.izin}"
            h.tvAlpha.text = "A: ${d.alpha}"
        }
    }
}