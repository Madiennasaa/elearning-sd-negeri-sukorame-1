package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.Pengumuman
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private var userRole: String = "siswa"
    private val listNotif = mutableListOf<Pengumuman>()
    private lateinit var adapter: NotificationAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userRole = arguments?.getString("USER_ROLE") ?: "siswa"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_notification, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivBack       = view.findViewById<ImageView>(R.id.ivBack)
        val rvNotif      = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val layoutEmpty  = view.findViewById<LinearLayout>(R.id.layoutEmpty)
        // ProgressBar sudah ada di layout, atau kita pakai layoutEmpty sementara loading
        // Jika belum ada ProgressBar di XML, kita sembunyikan list dan tampilkan empty dulu

        ivBack.setOnClickListener {
            (activity as? HomeActivity)?.backToHome()
        }

        adapter = NotificationAdapter(listNotif)
        rvNotif.layoutManager = LinearLayoutManager(requireContext())
        rvNotif.adapter = adapter

        // Sembunyikan keduanya saat mulai loading
        layoutEmpty.visibility = View.GONE
        rvNotif.visibility = View.GONE

        loadPengumuman(layoutEmpty, rvNotif)
    }

    private fun loadPengumuman(layoutEmpty: LinearLayout, rvNotif: RecyclerView) {
        val targetRole = userRole.lowercase()

        db.collection("pengumuman")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                
                val allData = snapshot.toObjects(Pengumuman::class.java)
                // Filter locally: show if 'untuk' is 'semua' or matches userRole, or if user is admin
                val filtered = if (targetRole == "admin") {
                    allData
                } else {
                    allData.filter { it.untuk == "semua" || it.untuk?.lowercase() == targetRole }
                }
                
                listNotif.clear()
                listNotif.addAll(filtered)
                adapter.notifyDataSetChanged()

                if (listNotif.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvNotif.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvNotif.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                layoutEmpty.visibility = View.VISIBLE
                rvNotif.visibility = View.GONE
            }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    inner class NotificationAdapter(private val data: List<Pengumuman>) :
        RecyclerView.Adapter<NotificationAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle:   TextView = v.findViewById(R.id.tvNotifTitle)
            val tvMessage: TextView = v.findViewById(R.id.tvNotifMessage)
            val tvDate:    TextView = v.findViewById(R.id.tvNotifDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = data[pos]
            h.tvTitle.text   = item.judul ?: "-"
            h.tvMessage.text = item.isi   ?: "-"
            // Format tanggal: ambil 10 karakter pertama dari "2024-06-12 10:30:00"
            h.tvDate.text    = item.tanggal?.take(10) ?: "-"
        }

        override fun getItemCount(): Int = data.size
    }
}