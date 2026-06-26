package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Tugas
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ItemTugasSiswaBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TugasSiswaAdapter(
    private val items: List<Tugas>,
    private val onKerjakan: (Tugas) -> Unit
) : RecyclerView.Adapter<TugasSiswaAdapter.VH>() {

    private val expandedSet = mutableSetOf<Int>()
    private val nowMs = System.currentTimeMillis()

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    )

    inner class VH(val b: ItemTugasSiswaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTugasSiswaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tugas = items[position]
        val b = holder.b

        // ── Judul ──
        b.tvJudul.text = tugas.judul ?: "Tugas"

        // ── Parse deadline ──
        val deadlineDate = parseDate(tugas.deadline)
        val sisaHari = deadlineDate?.let { d ->
            TimeUnit.MILLISECONDS.toDays(d.time - nowMs).toInt()
        }

        // ── Urgency style ──
        val style = buildUrgencyStyle(sisaHari)
        b.stripUrgency.setBackgroundColor(Color.parseColor(style.stripColor))
        b.tvEmoji.text = style.emoji
        b.tvDeadlinePill.text = style.pillText
        b.tvDeadlinePill.setTextColor(Color.parseColor(style.pillColor))

        // ── Badge BARU: tidak ada createdAt di model, sembunyikan ──
        b.tvBadgeBaru.visibility = View.GONE

        // ── Expanded detail ──
        // Tugas tidak punya deskripsi, pakai namaMapel + durasi sebagai info
        val mapelInfo  = tugas.namaMapel?.let { "Mata Pelajaran: $it" }
            ?: "Kerjakan sesuai petunjuk guru ya!"
        val durasiInfo = tugas.durasi?.let { "\n⏱ Durasi: $it menit" } ?: ""
        b.tvDeskripsi.text = mapelInfo + durasiInfo

        b.tvDeadlineFull.text = "Deadline: ${formatTanggalIndo(deadlineDate)}"

        val sisaText = when {
            sisaHari == null -> "—"
            sisaHari < 0    -> "${abs(sisaHari)} hari telat!"
            sisaHari == 0   -> "Hari ini!"
            else            -> "$sisaHari hari lagi"
        }
        b.tvSisaHari.text = sisaText
        b.tvSisaHari.setBackgroundColor(Color.parseColor(style.badgeDaysBg))

        // ── Expand / collapse ──
        val isExpanded = position in expandedSet
        b.layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
        b.ivArrow.rotation = if (isExpanded) 180f else 0f

        b.rowCollapsed.setOnClickListener {
            val willExpand = position !in expandedSet
            if (willExpand) expandedSet.add(position) else expandedSet.remove(position)
            notifyItemChanged(position)

            ObjectAnimator.ofFloat(b.ivArrow, "rotation", b.ivArrow.rotation,
                if (willExpand) 180f else 0f).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        b.btnKerjakan.setOnClickListener { onKerjakan(tugas) }
    }

    private fun buildUrgencyStyle(sisaHari: Int?): UrgencyStyle = when {
        sisaHari == null -> UrgencyStyle("#9E9E9E", "Tidak ada deadline", "#757575", "#9E9E9E", "📝")
        sisaHari < 0    -> UrgencyStyle("#B71C1C", "⚠️ Sudah lewat!", "#B71C1C", "#C62828", "⚠️")
        sisaHari == 0   -> UrgencyStyle("#E53935", "🔥 Hari ini!", "#E53935", "#C62828", "🔥")
        sisaHari == 1   -> UrgencyStyle("#F4511E", "⏰ Besok!", "#F4511E", "#D84315", "⏰")
        sisaHari <= 3   -> UrgencyStyle("#FB8C00", "📋 $sisaHari hari lagi", "#E65100", "#E65100", "📋")
        sisaHari <= 7   -> UrgencyStyle("#FDD835", "📄 $sisaHari hari lagi", "#F57F17", "#F9A825", "📄")
        else            -> UrgencyStyle("#43A047", "📝 $sisaHari hari lagi", "#2E7D32", "#388E3C", "📝")
    }

    private fun parseDate(str: String?): Date? {
        if (str.isNullOrBlank()) return null
        for (fmt in dateFormats) {
            try { return fmt.parse(str) } catch (_: Exception) {}
        }
        return null
    }

    private fun formatTanggalIndo(date: Date?): String {
        if (date == null) return "Tidak ada"
        val hari  = arrayOf("Minggu","Senin","Selasa","Rabu","Kamis","Jumat","Sabtu")
        val bulan = arrayOf("","Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
        val cal   = Calendar.getInstance().apply { time = date }
        return "${hari[cal.get(Calendar.DAY_OF_WEEK) - 1]}, " +
                "${cal.get(Calendar.DAY_OF_MONTH)} " +
                "${bulan[cal.get(Calendar.MONTH) + 1]} " +
                "${cal.get(Calendar.YEAR)}"
    }

    private data class UrgencyStyle(
        val stripColor: String,
        val pillText: String,
        val pillColor: String,
        val badgeDaysBg: String,
        val emoji: String
    )
}