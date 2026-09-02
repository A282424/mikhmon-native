package com.mikhmon.native_app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityAddProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val rateLimit = binding.etRateLimit.text.toString().trim()
        val sessionTimeout = binding.etSessionTimeout.text.toString().trim()
        val sharedUsers = binding.etSharedUsers.text.toString().trim()

        if (name.isEmpty()) {
            showError("اسم الباقة مطلوب")
            return
        }
        val api = Session.api ?: run {
            showError("انتهت الجلسة، الرجاء تسجيل الدخول مجدداً")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val words = mutableListOf("/ip/hotspot/user/profile/add", "=name=$name")
                    if (rateLimit.isNotEmpty()) words.add("=rate-limit=$rateLimit")
                    if (sessionTimeout.isNotEmpty()) words.add("=session-timeout=$sessionTimeout")
                    if (sharedUsers.isNotEmpty()) words.add("=shared-users=$sharedUsers")
                    val resp = api.talk(words)
                    val trap = resp.firstOrNull { it.reply == "!trap" }
                    if (trap != null) throw Exception(trap.words["message"] ?: "تعذر إضافة الباقة")
                }
                setLoading(false)
                finish()
            } catch (e: Exception) {
                setLoading(false)
                showError("خطأ: ${e.message}")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSave.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}
