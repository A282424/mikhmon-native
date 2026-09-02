package com.mikhmon.native_app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityBackupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding
    private lateinit var adapter: BackupFileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BackupFileAdapter(mutableListOf())
        binding.rvBackups.layoutManager = LinearLayoutManager(this)
        binding.rvBackups.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadBackups() }
        binding.btnCreateBackup.setOnClickListener { createBackup() }

        loadBackups()
    }

    private fun createBackup() {
        val name = binding.etBackupName.text.toString().trim().ifBlank { "mikhmon-native-backup" }
        val api = Session.api ?: run {
            showError("انتهت الجلسة، الرجاء تسجيل الدخول مجدداً")
            return
        }

        setLoading(true)
        binding.tvSuccess.visibility = android.view.View.GONE
        binding.tvError.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/system/backup/save", "=name=$name"))
                    val trap = resp.firstOrNull { it.reply == "!trap" }
                    if (trap != null) throw Exception(trap.words["message"] ?: "تعذر إنشاء النسخة الاحتياطية")
                    // إعطاء الراوتر لحظة لكتابة الملف قبل إعادة سرد الملفات
                    delay(800)
                }
                setLoading(false)
                binding.tvSuccess.text = "تم إنشاء النسخة الاحتياطية \"$name.backup\" على الراوتر بنجاح"
                binding.tvSuccess.visibility = android.view.View.VISIBLE
                loadBackups()
            } catch (e: Exception) {
                setLoading(false)
                showError("خطأ: ${e.message}")
            }
        }
    }

    private fun loadBackups() {
        val api = Session.api ?: return
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/file/print"))
                    resp.filter { it.reply == "!re" }
                        .filter { (it.words["name"] ?: "").endsWith(".backup") }
                        .map {
                            BackupFileRow(
                                name = it.words["name"] ?: "-",
                                size = formatSize(it.words["size"]),
                                created = it.words["creation-time"] ?: "-"
                            )
                        }
                        .sortedByDescending { it.created }
                }
                adapter.replaceAll(rows)
            } catch (e: Exception) {
                showError("تعذر تحميل قائمة الملفات: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun formatSize(value: String?): String {
        val bytes = value?.toLongOrNull() ?: return "-"
        val kb = bytes / 1024.0
        return if (kb > 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnCreateBackup.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}
