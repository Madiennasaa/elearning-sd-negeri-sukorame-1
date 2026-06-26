package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.cbt

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.SoalRaw
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.SoalData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.PilihanData
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.HasilCbt
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ActivityCbtBinding

class CbtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCbtBinding
    private lateinit var sessionManager: SessionManager
    private val db = FirebaseFirestore.getInstance()
    private var soalList: List<SoalData> = listOf()
    private var currentSoalIndex = 0
    private var siswaId = ""
    private var tugasId = ""
    private var jawabanBenar = 0
    private val jawabanDipilih = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCbtBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        tugasId = intent.getStringExtra("TUGAS_ID") ?: ""
        siswaId = intent.getStringExtra("SISWA_ID") ?: ""

        binding.btnBack.setOnClickListener { finish() }

        cekSudahDikerjakan()
    }

    private fun cekSudahDikerjakan() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnNext.isEnabled = false

        db.collection("hasil_cbt")
            .whereEqualTo("tugasId", tugasId)
            .whereEqualTo("siswaId", siswaId)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val hasil = doc?.toObject(HasilCbt::class.java)

                if (hasil != null && hasil.totalSoal > 0 && hasil.jumlahDijawab >= hasil.totalSoal) {
                    binding.progressBar.visibility = View.GONE
                    showHasilSudahSelesai(hasil.jawabanBenar, hasil.totalSoal, hasil.nilai.toInt())
                } else {
                    loadSoalFromApi(tugasId)
                }
            }
            .addOnFailureListener {
                loadSoalFromApi(tugasId)
            }
    }

    private fun showHasilSudahSelesai(benar: Int, total: Int, skor: Int) {
        AlertDialog.Builder(this)
            .setTitle("Sudah Dikerjakan")
            .setMessage(
                "Kamu sudah mengerjakan tugas ini.\n\n" +
                        "Jawaban benar : $benar dari $total soal\n" +
                        "Skor kamu     : $skor / 100"
            )
            .setPositiveButton("Kembali") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadSoalFromApi(tugasId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnNext.isEnabled = false

        db.collection("tugas").document(tugasId).collection("soal")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                binding.btnNext.isEnabled = true

                val rawSoalList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SoalRaw::class.java)?.copy(id = doc.id)
                }

                soalList = rawSoalList.map { raw ->
                    val pilihan = mutableListOf<PilihanData>()
                    if (!raw.pilihanA.isNullOrEmpty())
                        pilihan.add(PilihanData(raw.id + "_A", "A", raw.pilihanA, false))
                    if (!raw.pilihanB.isNullOrEmpty())
                        pilihan.add(PilihanData(raw.id + "_B", "B", raw.pilihanB, false))
                    if (!raw.pilihanC.isNullOrEmpty())
                        pilihan.add(PilihanData(raw.id + "_C", "C", raw.pilihanC, false))
                    if (!raw.pilihanD.isNullOrEmpty())
                        pilihan.add(PilihanData(raw.id + "_D", "D", raw.pilihanD, false))

                    SoalData(raw.id, raw.soal ?: "", raw.gambarSoal, pilihan)
                }

                if (soalList.isNotEmpty()) {
                    binding.progressBar.max = soalList.size
                    displaySoal()
                } else {
                    Toast.makeText(this@CbtActivity, "Tidak ada soal untuk tugas ini", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CbtActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupBtnNext() {
        binding.btnNext.setOnClickListener {
            val selectedOptionId = binding.rgOptions.checkedRadioButtonId
            if (selectedOptionId == -1) {
                Toast.makeText(this, "Pilih jawaban terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadioButton = findViewById<RadioButton>(selectedOptionId)
            val hurufDipilih = selectedRadioButton.tag as? String ?: return@setOnClickListener
            val soal = soalList[currentSoalIndex]

            jawabanDipilih[currentSoalIndex] = hurufDipilih

            submitJawabanToApi(siswaId, soal.id, hurufDipilih)

            if (currentSoalIndex < soalList.size - 1) {
                currentSoalIndex++
                displaySoal()
            } else {
                loadHasilDariServer()
            }
        }
    }

    private fun loadHasilDariServer() {
        binding.btnNext.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        db.collection("tugas").document(tugasId).collection("soal")
            .get()
            .addOnSuccessListener { soalSnapshot ->
                val correctAnswers = soalSnapshot.documents.associate { doc ->
                    doc.id to (doc.getString("jawabanBenar") ?: "")
                }

                db.collection("jawabanCbt")
                    .whereEqualTo("tugasId", tugasId)
                    .whereEqualTo("siswaId", siswaId)
                    .get()
                    .addOnSuccessListener { jawabanSnapshot ->
                        binding.progressBar.visibility = View.GONE

                        var benar = 0
                        var salah = 0

                        jawabanSnapshot.documents.forEach { doc ->
                            val soalId = doc.getString("soalId") ?: ""
                            val jawaban = doc.getString("jawaban") ?: ""
                            val correct = correctAnswers[soalId]
                            if (correct != null) {
                                if (jawaban == correct) {
                                    benar++
                                } else {
                                    salah++
                                }
                            }
                        }

                        val total = correctAnswers.size
                        val totalDijawab = jawabanSnapshot.documents.size
                        val score = if (total > 0) (benar.toDouble() / total * 100) else 0.0

                        val hasilData = hashMapOf(
                            "tugasId" to tugasId,
                            "siswaId" to siswaId,
                            "totalSoal" to total,
                            "jawabanBenar" to benar,
                            "jawabanSalah" to salah,
                            "jumlahDijawab" to totalDijawab,
                            "nilai" to score
                        )

                        db.collection("hasil_cbt")
                            .add(hasilData)
                            .addOnSuccessListener {
                                showHasilDialog(benar, total, score.toInt())
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@CbtActivity, "Gagal menyimpan hasil: ${e.message}", Toast.LENGTH_SHORT).show()
                                showHasilDialog(benar, total, score.toInt())
                            }
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@CbtActivity, "Gagal memproses hasil", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CbtActivity, "Gagal memproses hasil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitJawabanToApi(siswaId: String, soalId: String, hurufJawaban: String) {
        val data = hashMapOf(
            "siswaId" to siswaId,
            "tugasId" to tugasId,
            "soalId" to soalId,
            "jawaban" to hurufJawaban
        )
        db.collection("jawabanCbt")
            .whereEqualTo("siswaId", siswaId)
            .whereEqualTo("tugasId", tugasId)
            .whereEqualTo("soalId", soalId)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    db.collection("jawabanCbt").document(doc.id).set(data)
                } else {
                    db.collection("jawabanCbt").add(data)
                }
            }
    }

    private fun displaySoal() {
        if (currentSoalIndex == 0) setupBtnNext()

        val soal = soalList[currentSoalIndex]
        val total = soalList.size
        val nomor = currentSoalIndex + 1

        binding.tvQuestionNumber.text = "Soal No. $nomor"
        binding.tvQuestionText.text = soal.pertanyaan
        binding.tvSkorSementara.text = "Dijawab: ${jawabanDipilih.size} / $total"
        binding.progressBar.max = total
        binding.progressBar.progress = nomor
        binding.btnNext.text = if (currentSoalIndex == total - 1) "Selesai" else "Selanjutnya"

        binding.rgOptions.clearCheck()
        binding.rgOptions.removeAllViews()

        soal.pilihan.forEach { pilihan ->
            val rb = RadioButton(this)
            rb.text = "${pilihan.pilihan}. ${pilihan.isiPilihan}"
            rb.tag = pilihan.pilihan
            rb.textSize = 16f
            rb.buttonDrawable = null
            rb.setBackgroundResource(com.polinema.mi.elearning_sd_negeri_sukorame_1.R.drawable.bg_option_item)
            rb.setTextColor(ContextCompat.getColor(this, com.polinema.mi.elearning_sd_negeri_sukorame_1.R.color.text_dark))

            val params = android.widget.RadioGroup.LayoutParams(
                android.widget.RadioGroup.LayoutParams.MATCH_PARENT,
                android.widget.RadioGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            rb.layoutParams = params
            rb.setPadding(48, 48, 48, 48)

            val sudahDijawab = jawabanDipilih[currentSoalIndex]
            if (sudahDijawab != null && sudahDijawab == pilihan.pilihan) {
                rb.isChecked = true
                rb.isEnabled = false
            }

            binding.rgOptions.addView(rb)
        }

        if (jawabanDipilih.containsKey(currentSoalIndex)) {
            for (i in 0 until binding.rgOptions.childCount) {
                binding.rgOptions.getChildAt(i).isEnabled = false
            }
        }
    }

    private fun showHasilDialog(benar: Int, total: Int, skor: Int) {
        AlertDialog.Builder(this)
            .setTitle("Tugas Selesai!")
            .setMessage("Jawaban benar: $benar dari $total soal\nSkor kamu: $skor / 100")
            .setPositiveButton("Kembali") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}