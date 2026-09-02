package com.mikhmon.native_app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityAddUserBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val profile = binding.etProfile.text.toString().trim()

        if (username.isEmpty()) {
            showError("اسم المستخدم مطلوب")
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
                    val words = mutableListOf("/ip/hotspot/user/add", "=name=$username")
                    if (password.isNotEmpty()) words.add("=password=$password")
                    if (profile.isNotEmpty()) words.add("=profile=$profile")
                    val resp = api.talk(words)
                    val trap = resp.firstOrNull { it.reply == "!trap" }
                    if (trap != null) {
                        throw Exception(trap.words["message"] ?: "تعذر إضافة المستخدم")
                    }
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
