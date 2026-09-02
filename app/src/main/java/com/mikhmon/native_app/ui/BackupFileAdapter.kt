package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.databinding.ItemBackupFileBinding

data class BackupFileRow(val name: String, val size: String, val created: String)

class BackupFileAdapter(private val items: MutableList<BackupFileRow>) :
    RecyclerView.Adapter<BackupFileAdapter.VH>() {

    inner class VH(val binding: ItemBackupFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBackupFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvBackupName.text = item.name
        holder.binding.tvBackupMeta.text = "${item.size}  •  ${item.created}"
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<BackupFileRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
