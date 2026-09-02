package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.api.ServerConfig
import com.mikhmon.native_app.databinding.ItemServerBinding

class ServerAdapter(
    private val items: List<ServerConfig>,
    private val onClick: (ServerConfig) -> Unit
) : RecyclerView.Adapter<ServerAdapter.VH>() {

    inner class VH(val binding: ItemServerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvServerName.text = item.name
        holder.binding.tvServerHost.text = "${item.host}:${item.port}" + if (item.useSsl) "  🔒" else ""
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
