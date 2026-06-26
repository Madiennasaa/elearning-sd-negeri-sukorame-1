package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.SystemLog
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentAdminSystemLogBinding

class AdminSystemLogFragment : Fragment() {

    private var _binding: FragmentAdminSystemLogBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    data class LogItem(val label: String, val value: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSystemLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvLog.layoutManager = LinearLayoutManager(requireContext())

        val items = mutableListOf<LogItem>()

        // Fetch counts for summary
        db.collection("users").whereEqualTo("role", "guru").get().addOnSuccessListener { guruSnap ->
            items.add(LogItem("Total Guru", guruSnap.size().toString()))
            
            db.collection("siswa").get().addOnSuccessListener { siswaSnap ->
                items.add(LogItem("Total Siswa", siswaSnap.size().toString()))
                
                db.collection("kelas").get().addOnSuccessListener { kelasSnap ->
                    items.add(LogItem("Total Kelas", kelasSnap.size().toString()))
                    
                    // Fetch activity logs
                    db.collection("system_log").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get().addOnSuccessListener { logSnap ->
                        logSnap.documents.forEach { doc ->
                            val log = doc.toObject(SystemLog::class.java)
                            if (log != null) {
                                items.add(LogItem(
                                    "${log.user} — ${log.activity}",
                                    log.createdAt.take(16)
                                ))
                            }
                        }
                        
                        if (items.isEmpty()) items.add(LogItem("Tidak ada log", "-"))
                        binding.rvLog.adapter = LogAdapter(items)
                    }.addOnFailureListener {
                        if (items.isEmpty()) items.add(LogItem("Tidak ada log", "-"))
                        binding.rvLog.adapter = LogAdapter(items)
                    }
                }
            }
        }.addOnFailureListener {
            binding.rvLog.adapter = LogAdapter(listOf(LogItem("Koneksi gagal", it.message ?: "-")))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    inner class LogAdapter(private val list: List<LogItem>) : RecyclerView.Adapter<LogAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvLabel: TextView = v.findViewById(R.id.tvLogLabel)
            val tvValue: TextView = v.findViewById(R.id.tvLogValue)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_log, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) { h.tvLabel.text = list[pos].label; h.tvValue.text = list[pos].value }
    }
}