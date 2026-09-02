package com.mikhmon.native_app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityReportsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * يعرض استهلاك البيانات التراكمي لكل مستخدم Hotspot، بالاعتماد على الحقول
 * bytes-in / bytes-out التي يحتفظ بها الراوتر لكل مستخدم (تصفّر عند
 * /ip/hotspot/user/reset-counters).
 */
class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ReportAdapter(mutableListOf())
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    private fun load() {
        val api = Session.api ?: return
        binding.swipeRefresh.isRefreshing = true
        binding.tvError.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val (rows, totalBytes) = withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/ip/hotspot/user/print"))
                    val users = resp.filter { it.reply == "!re" }.map {
                        val name = it.words["name"] ?: "-"
                        val bytesIn = it.words["bytes-in"]?.toLongOrNull() ?: 0L
                        val bytesOut = it.words["bytes-out"]?.toLongOrNull() ?: 0L
                        val total = bytesIn + bytesOut
                        ReportRow(name, total, formatBytes(total))
                    }
                    val sorted = users.filter { it.totalBytes > 0 }.sortedByDescending { it.totalBytes }
                    val sum = users.sumOf { it.totalBytes }
                    Pair(sorted.take(20), sum)
                }

                binding.tvTotalUsage.text = formatBytes(totalBytes)
                binding.tvUserCount.text = "بين ${rows.size} مستخدم لديهم استهلاك مسجّل"
                adapter.replaceAll(rows)

                if (rows.isEmpty()) {
                    binding.tvError.text = "لا يوجد استهلاك مسجّل بعد لأي مستخدم"
                    binding.tvError.visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                binding.tvError.text = "تعذر تحميل التقارير: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return String.format("%.2f GB", gb)
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }
}
