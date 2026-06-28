package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.home

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polinema.mi.elearning_sd_negeri_sukorame_1.data.model.User
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ItemInputNilaiBinding

class InputNilaiAdapter(
    private val students: List<User>,
    private val onNilaiChanged: (String, Double) -> Unit
) : RecyclerView.Adapter<InputNilaiAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemInputNilaiBinding) : RecyclerView.ViewHolder(binding.root) {
        var currentWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInputNilaiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        holder.binding.tvStudentName.text = student.name
        
        // Remove old watcher if any
        holder.currentWatcher?.let { holder.binding.etNilai.removeTextChangedListener(it) }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toDoubleOrNull() ?: 0.0
                onNilaiChanged(student.uid, value)
            }
        }
        holder.binding.etNilai.addTextChangedListener(watcher)
        holder.currentWatcher = watcher
    }

    override fun getItemCount() = students.size
}
