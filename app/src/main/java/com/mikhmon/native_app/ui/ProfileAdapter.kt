package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.databinding.ItemProfileBinding

data class ProfileRow(
    val id: String,
    val name: String,
    val rateLimit: String,
    val sessionTimeout: String,
    val sharedUsers: String
)

class ProfileAdapter(
    private val items: MutableList<ProfileRow>,
    private val onDelete: (ProfileRow) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.VH>() {

    inner class VH(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvProfileName.text = item.name
        val details = buildString {
            if (item.rateLimit.isNotBlank()) append("السرعة: ${item.rateLimit}  ")
            if (item.sessionTimeout.isNotBlank()) append("المدة: ${item.sessionTimeout}  ")
            if (item.sharedUsers.isNotBlank()) append("المستخدمون المشتركون: ${item.sharedUsers}")
        }
        holder.binding.tvProfileDetails.text = details.ifBlank { "بدون قيود" }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<ProfileRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeById(id: String) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
