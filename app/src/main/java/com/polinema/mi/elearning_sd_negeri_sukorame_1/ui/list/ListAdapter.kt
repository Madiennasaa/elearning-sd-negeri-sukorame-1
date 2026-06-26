package com.polinema.mi.elearning_sd_negeri_sukorame_1.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polinema.mi.elearning_sd_negeri_sukorame_1.databinding.ItemListBinding

class ListAdapter(
    private val items: List<Pair<String, String>>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (title, subtitle) = items[position]
        holder.binding.tvItemTitle.text = title
        holder.binding.tvItemSubtitle.text = subtitle
        holder.binding.root.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount() = items.size
}
