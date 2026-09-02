package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.databinding.ItemHotspotUserBinding

data class HotspotUserRow(val id: String, val name: String, val profile: String)

class HotspotUserAdapter(
    private val items: MutableList<HotspotUserRow>,
    private val onDelete: (HotspotUserRow) -> Unit
) : RecyclerView.Adapter<HotspotUserAdapter.VH>() {

    inner class VH(val binding: ItemHotspotUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHotspotUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvUserName.text = item.name
        holder.binding.tvUserProfile.text = item.profile.ifBlank { "بدون باقة (default)" }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<HotspotUserRow>) {
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
