package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.databinding.ItemReportRowBinding

data class ReportRow(val name: String, val totalBytes: Long, val label: String)

class ReportAdapter(private val items: MutableList<ReportRow>) :
    RecyclerView.Adapter<ReportAdapter.VH>() {

    inner class VH(val binding: ItemReportRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReportRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val max = items.maxOfOrNull { it.totalBytes }?.takeIf { it > 0 } ?: 1L
        holder.binding.tvReportName.text = item.name
        holder.binding.tvReportTotal.text = item.label
        holder.binding.pbReportUsage.progress = ((item.totalBytes.toDouble() / max) * 100).toInt()
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<ReportRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
