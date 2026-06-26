package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentKepalaLaporanBinding
// PENTING: Import model data Firestore kamu yang kemarin kita rapikan
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Kelas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Siswa
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Absensi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Nilai

class KepalaLaporanFragment : Fragment() {

    private var _binding: FragmentKepalaLaporanBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    data class LaporanItem(val namaKelas: String, val totalSiswa: String, val rataAbsen: String, val rataMapel: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKepalaLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvLaporanKelas.layoutManager = LinearLayoutManager(requireContext())

        // Fetch semua koleksi master & transaksional untuk dihitung statistiknya secara lokal
        db.collection("kelas").get().addOnSuccessListener { kelasSnap ->
            db.collection("siswa").get().addOnSuccessListener { siswaSnap ->
                db.collection("absensi").get().addOnSuccessListener { absenSnap ->
                    db.collection("nilai").get().addOnSuccessListener { nilaiSnap ->
                        if (!isAdded) return@addOnSuccessListener

                        // Mapping data snapshot Firestore ke Model Object Kotlin secara aman
                        val kelasList = kelasSnap.documents.mapNotNull { it.toObject(Kelas::class.java)?.copy(id = it.id) }
                        val siswaList = siswaSnap.documents.mapNotNull { it.toObject(Siswa::class.java)?.copy(id = it.id) }
                        val absenList = absenSnap.documents.mapNotNull { it.toObject(Absensi::class.java)?.copy(id = it.id) }
                        val nilaiList = nilaiSnap.documents.mapNotNull { it.toObject(Nilai::class.java)?.copy(id = it.id) }

                        val data = kelasList.map { k ->
                            // Filter siswa yang masuk dalam kelas ini
                            val studentsInClass = siswaList.filter { it.kelasId == k.id }
                            val studentIds = studentsInClass.map { it.id }

                            val totalSiswa = studentsInClass.size

                            // Hitung Persentase Rata-rata Kehadiran: (Hadir / Total Absensi) * 100
                            val classAbsen = absenList.filter { it.siswaId in studentIds }
                            val rataAbsen = if (classAbsen.isNotEmpty()) {
                                val hadir = classAbsen.count { it.status?.equals("hadir", true) == true }
                                "${(hadir * 100) / classAbsen.size}%"
                            } else "0%"

                            // Hitung Rata-rata Nilai Kelas (Ujian/Tugas)
                            val classNilai = nilaiList.filter { it.siswaId in studentIds }
                            val rataMapel = if (classNilai.isNotEmpty()) {
                                String.format("%.1f", classNilai.map { it.nilai }.average())
                            } else "0.0"

                            LaporanItem(
                                namaKelas  = k.namaKelas ?: "-",
                                totalSiswa = "$totalSiswa Siswa",
                                rataAbsen  = rataAbsen,
                                rataMapel  = rataMapel
                            )
                        }

                        if (data.isEmpty()) {
                            db.collection("users").get().addOnSuccessListener { uSnap ->
                                val totalGuru = uSnap.documents.count { it.getString("role") == "guru" }
                                val items = listOf(LaporanItem("Ringkasan Sekolah", "-", "$totalGuru Guru", "-"))
                                binding.rvLaporanKelas.adapter = LaporanAdapter(items)
                            }
                        } else {
                            binding.rvLaporanKelas.adapter = LaporanAdapter(data)
                        }
                    }
                }
            }
        }.addOnFailureListener {
            if (isAdded) Toast.makeText(requireContext(), "Gagal memuat data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class LaporanAdapter(private val list: List<LaporanItem>) : RecyclerView.Adapter<LaporanAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvKelas: TextView = v.findViewById(R.id.tvNamaKelas)
            val tvSiswa: TextView = v.findViewById(R.id.tvTotalSiswa)
            val tvAbsen: TextView = v.findViewById(R.id.tvRataAbsen)
            val tvNilai: TextView = v.findViewById(R.id.tvRataNilai)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_laporan_kelas, parent, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val d = list[pos]
            h.tvKelas.text = "Kelas ${d.namaKelas}"
            h.tvSiswa.text = d.totalSiswa
            h.tvAbsen.text = "Kehadiran: ${d.rataAbsen}"
            h.tvNilai.text = "Rata Nilai: ${d.rataMapel}"
        }
    }
}