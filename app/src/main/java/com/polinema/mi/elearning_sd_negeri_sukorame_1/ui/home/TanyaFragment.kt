package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.polinema.mi.elearning_sd_negeri_sukorame_1.R
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.network.SessionManager
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.FragmentTanyaBinding

class TanyaFragment : Fragment() {
    private var _binding: FragmentTanyaBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private var senderId = ""
    private var senderName = ""

    data class ChatMessage(val senderId: String = "", val senderName: String = "", val message: String = "", val timestamp: Long = 0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTanyaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = SessionManager(requireContext()).getUser()
        senderId   = user?.uid ?: ""
        senderName = user?.name ?: "Orang Tua"

        adapter = ChatAdapter(messages, senderId)
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter

        listenToMessages()

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun listenToMessages() {
        db.collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val newMessages = snapshot.toObjects(ChatMessage::class.java)
                messages.clear()
                messages.addAll(newMessages)
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) binding.rvChat.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val msg = ChatMessage(senderId, senderName, text, System.currentTimeMillis())
        db.collection("chat").add(msg)
            .addOnSuccessListener {
                binding.etMessage.setText("")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ── CHAT ADAPTER ──────────────────────────────────────
    inner class ChatAdapter(private val list: List<ChatMessage>, private val myId: String) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        override fun getItemViewType(pos: Int) = if (list[pos].senderId == myId) 1 else 0

        // Bikin kelas ViewHolder eksplisit biar Kotlin nggak bingung nebak tipe data view
        inner class ChatViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvMessage: TextView = v.findViewById(R.id.tvMessage)
            val tvSender: TextView? = v.findViewById(R.id.tvSender) // Pake nullable karena di chat kanan mungkin ga ada tvSender
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val layout = if (viewType == 1) R.layout.item_chat_right else R.layout.item_chat_left
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ChatViewHolder(view)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(h: ChatViewHolder, pos: Int) {
            val m = list[pos]
            h.tvMessage.text = m.message

            // Mengisi nama pengirim jika komponen tvSender ditemukan (biasanya di item_chat_left)
            h.tvSender?.text = m.senderName
        }
    }
}