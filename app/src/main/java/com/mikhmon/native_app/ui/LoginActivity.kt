package com.mikhmon.native_app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.RouterOsApi
import com.mikhmon.native_app.api.ServerConfig
import com.mikhmon.native_app.api.ServerStore
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshSavedList()

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun refreshSavedList() {
        val servers = ServerStore.loadAll(this)
        binding.rvSavedServers.layoutManager = LinearLayoutManager(this)
        binding.rvSavedServers.adapter = ServerAdapter(servers) { server ->
            binding.etName.setText(server.name)
            binding.etHost.setText(server.host)
            binding.etPort.setText(server.port.toString())
            binding.switchSsl.isChecked = server.useSsl
            binding.etUsername.setText(server.username)
            binding.etPassword.setText(server.password)
        }
    }

    private fun attemptLogin() {
        val name = binding.etName.text.toString().ifBlank { binding.etHost.text.toString() }
        val host = binding.etHost.text.toString().trim()
        val portText = binding.etPort.text.toString().trim()
        val useSsl = binding.switchSsl.isChecked
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (host.isEmpty() || portText.isEmpty() || username.isEmpty()) {
            showError("الرجاء تعبئة عنوان الجهاز والمنفذ واسم المستخدم")
            return
        }
        val port = portText.toIntOrNull()
        if (port == null) {
            showError("رقم المنفذ غير صحيح")
            return
        }

        setLoading(true)
        val config = ServerConfig(name, host, port, useSsl, username, password)

        lifecycleScope.launch {
            try {
                val api = RouterOsApi()
                withContext(Dispatchers.IO) {
                    api.connect(config.host, config.port, config.useSsl)
                    api.login(config.username, config.password)
                }
                Session.api = api
                Session.config = config
                ServerStore.upsert(this@LoginActivity, config)

                setLoading(false)
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
            } catch (e: Exception) {
                setLoading(false)
                showError("فشل الاتصال: ${e.message ?: "خطأ غير معروف"}")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !loading
        if (loading) binding.tvError.visibility = android.view.View.GONE
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        refreshSavedList()
    }
}
