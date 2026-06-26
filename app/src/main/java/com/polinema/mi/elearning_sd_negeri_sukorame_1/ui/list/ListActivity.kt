package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.*
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivityListBinding

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type    = intent.getStringExtra("TYPE") ?: "MATERI"
        val kelasId = intent.getStringExtra("KELAS_ID") ?: intent.getIntExtra("KELAS_ID", 0).takeIf { it > 0 }?.toString()
        val siswaId = intent.getStringExtra("SISWA_ID") ?: intent.getIntExtra("SISWA_ID", 0).takeIf { it > 0 }?.toString()

        binding.tvTitle.text = "Daftar $type"
        binding.rvList.layoutManager = LinearLayoutManager(this)

        loadData(type, kelasId, siswaId)
    }

    private fun loadData(type: String, kelasId: String?, siswaId: String?) {
        when (type) {
            "MATERI" -> {
                if (kelasId.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                db.collection("materi").whereEqualTo("kelasId", kelasId).get()
                    .addOnSuccessListener { snapshot ->
                        val items = snapshot.documents.mapNotNull { doc ->
                            val m = doc.toObject(Materi::class.java)
                            m?.let { (it.judul ?: "-") to (it.namaMapel ?: "-") }
                        }
                        displayList(items)
                    }
                    .addOnFailureListener { failure() }
            }
            "TUGAS" -> {
                if (kelasId.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                db.collection("tugas").whereEqualTo("kelasId", kelasId).get()
                    .addOnSuccessListener { snapshot ->
                        val items = snapshot.documents.mapNotNull { doc ->
                            val t = doc.toObject(Tugas::class.java)
                            t?.let { (it.judul ?: "-") to "Deadline: ${it.deadline ?: "-"}" }
                        }
                        displayList(items)
                    }
                    .addOnFailureListener { failure() }
            }
            "HADIR" -> {
                if (siswaId.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                db.collection("absensi").whereEqualTo("siswaId", siswaId).get()
                    .addOnSuccessListener { snapshot ->
                        val items = snapshot.documents.mapNotNull { doc ->
                            val a = doc.toObject(Absensi::class.java)
                            a?.let { (it.tanggal ?: "-") to (it.status ?: "-") }
                        }
                        displayList(items)
                    }
                    .addOnFailureListener { failure() }
            }
            "NILAI" -> {
                if (siswaId.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                db.collection("nilai").whereEqualTo("siswaId", siswaId).get()
                    .addOnSuccessListener { snapshot ->
                        val items = snapshot.documents.mapNotNull { doc ->
                            val n = doc.toObject(Nilai::class.java)
                            n?.let { "${it.namaMapel ?: "-"}: ${it.nilai}" to (it.jenisNilai ?: "-") }
                        }
                        displayList(items)
                    }
                    .addOnFailureListener { failure() }
            }
            "RAPORT" -> {
                if (siswaId.isNullOrEmpty()) {
                    showEmpty()
                    return
                }
                db.collection("rapor").whereEqualTo("siswaId", siswaId).get()
                    .addOnSuccessListener { snapshot ->
                        val items = snapshot.documents.mapNotNull { doc ->
                            val r = doc.toObject(Rapor::class.java)
                            r?.let { "Semester ${it.semester ?: "-"}" to "Status: ${it.statusNaik ?: "-"}" }
                        }
                        displayList(items)
                    }
                    .addOnFailureListener { failure() }
            }
            else -> showEmpty()
        }
    }

    private fun displayList(items: List<Pair<String, String>>) {
        if (items.isEmpty()) {
            showEmpty()
        } else {
            binding.rvList.adapter = ListAdapter(items) {}
        }
    }

    private fun showEmpty() {
        binding.rvList.adapter = ListAdapter(listOf("Data Kosong" to "")) {}
    }

    private fun failure() {
        Toast.makeText(this, "Koneksi gagal", Toast.LENGTH_SHORT).show()
        showEmpty()
    }
}