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

    private val inputStates = mutableMapOf<String, String>()

    inner class ViewHolder(val binding: ItemInputNilaiBinding) : RecyclerView.ViewHolder(binding.root) {
        private var textWatcher: TextWatcher? = null

        fun bind(student: User) {
            binding.tvStudentName.text = student.name
            
            // Remove old watcher to prevent conflicts during recycling
            textWatcher?.let { binding.etNilai.removeTextChangedListener(it) }
            
            // Restore state
            binding.etNilai.setText(inputStates[student.uid] ?: "")

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val input = s.toString()
                    inputStates[student.uid] = input
                    val value = input.toDoubleOrNull() ?: 0.0
                    onNilaiChanged(student.uid, value)
                }
            }
            binding.etNilai.addTextChangedListener(textWatcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemInputNilaiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(students[position])
    override fun getItemCount() = students.size
}
