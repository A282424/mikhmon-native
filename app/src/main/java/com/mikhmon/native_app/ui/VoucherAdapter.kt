package com.mikhmon.native_app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mikhmon.native_app.databinding.ItemVoucherBinding

data class Voucher(val username: String, val password: String)

class VoucherAdapter(private val items: MutableList<Voucher>) :
    RecyclerView.Adapter<VoucherAdapter.VH>() {

    inner class VH(val binding: ItemVoucherBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVoucherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvVoucherUser.text = item.username
        holder.binding.tvVoucherPass.text = item.password
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<Voucher>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
