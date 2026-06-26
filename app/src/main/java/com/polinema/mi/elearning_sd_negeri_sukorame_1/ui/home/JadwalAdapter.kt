package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Jadwal

class JadwalAdapter(
    private val items: MutableList<Jadwal> = mutableListOf(),
    private val onDeleteClick: (Jadwal) -> Unit
) : RecyclerView.Adapter<JadwalAdapter.VH>() {

    var isGuruView: Boolean = false

    fun updateData(newItems: List<Jadwal>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private val stripColors = listOf(
        "#7C4DFF", "#4A90D9", "#43A047", "#FB8C00",
        "#E53935", "#00ACC1", "#AB47BC"
    )

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val stripJadwal: View    = v.findViewById(R.id.stripJadwal)
        val tvJamMulai: TextView = v.findViewById(R.id.tvJamMulai)
        val tvJamSelesai: TextView = v.findViewById(R.id.tvJamSelesai)
        val tvNamaMapel: TextView = v.findViewById(R.id.tvNamaMapel)
        val tvNamaGuru: TextView  = v.findViewById(R.id.tvNamaGuru)
        val tvDurasi: TextView    = v.findViewById(R.id.tvDurasi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_jadwal, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val jadwal = items[position]
        val color  = Color.parseColor(stripColors[position % stripColors.size])

        holder.stripJadwal.setBackgroundColor(color)

        holder.tvJamMulai.text   = jadwal.waktuMulai ?: "-"
        holder.tvJamSelesai.text = jadwal.waktuSelesai ?: "-"
        holder.tvJamMulai.setTextColor(color)

        holder.tvNamaMapel.text = jadwal.namaMapel ?: "-"
        holder.tvNamaGuru.text  = if (isGuruView) {
            "Kelas: " + (jadwal.namaKelas ?: "-")
        } else {
            jadwal.namaGuru?.takeIf { it != "-" } ?: "—"
        }

        // Hitung durasi menit dari jam
        holder.tvDurasi.text = hitungDurasi(
            jadwal.waktuMulai ?: "",
            jadwal.waktuSelesai ?: ""
        )

        holder.itemView.setOnLongClickListener {
            onDeleteClick(jadwal)
            true
        }
    }

    private fun hitungDurasi(mulai: String, selesai: String): String {
        return try {
            val separator = if (mulai.contains(":")) ":" else "."
            val (hM, mM) = mulai.split(separator).map { it.toInt() }
            val (hS, mS) = selesai.split(separator).map { it.toInt() }
            val durasi = (hS * 60 + mS) - (hM * 60 + mM)
            if (durasi > 0) "$durasi mnt" else "-"
        } catch (_: Exception) { "-" }
    }
}