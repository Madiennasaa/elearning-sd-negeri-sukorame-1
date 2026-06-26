package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Materi
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ItemMateriSiswaBinding

class MateriAdapter(
    private val items: List<Materi>,
    private val onClick: (Materi) -> Unit
) : RecyclerView.Adapter<MateriAdapter.VH>() {

    // Warna strip per mata pelajaran biar beda-beda, lebih menarik
    private val mapelColors = listOf(
        "#4A90D9", // biru  — default
        "#FF6B35", // oranye
        "#43A047", // hijau
        "#AB47BC", // ungu
        "#E53935", // merah
        "#FB8C00", // amber
        "#00ACC1", // teal
    )

    inner class VH(val b: ItemMateriSiswaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMateriSiswaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val materi = items[position]
        val b = holder.b

        b.tvJudulMateri.text = materi.judul ?: "Materi"
        b.tvMapelMateri.text = materi.namaMapel ?: "-"

        // Warna strip berganti-ganti per item
        val color = Color.parseColor(mapelColors[position % mapelColors.size])
        b.stripMateri.setBackgroundColor(color)
        b.ivIconMateri.setColorFilter(color)

        b.rowMateri.setOnClickListener { onClick(materi) }
    }
}