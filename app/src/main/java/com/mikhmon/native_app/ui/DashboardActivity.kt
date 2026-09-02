package com.mikhmon.native_app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Session.api == null || Session.api?.isConnected() != true) {
            goToLogin()
            return
        }

        binding.tvServerAddr.text = "${Session.config?.host}:${Session.config?.port}"
        binding.tvDrawerServerName.text = Session.config?.name ?: Session.config?.host ?: ""

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        // شبكة إحصائيات الواجهة الرئيسية
        binding.cardTotalUsers.setOnClickListener { startActivity(Intent(this, UsersActivity::class.java)) }
        binding.cardAddUser.setOnClickListener { startActivity(Intent(this, AddUserActivity::class.java)) }
        binding.cardGenerate.setOnClickListener { startActivity(Intent(this, VouchersActivity::class.java)) }

        // القائمة الجانبية
        binding.navDashboard.setOnClickListener { closeDrawer() }
        binding.navUsers.setOnClickListener { closeDrawer(); startActivity(Intent(this, UsersActivity::class.java)) }
        binding.navProfiles.setOnClickListener { closeDrawer(); startActivity(Intent(this, ProfilesActivity::class.java)) }
        binding.navVouchers.setOnClickListener { closeDrawer(); startActivity(Intent(this, VouchersActivity::class.java)) }
        binding.navReports.setOnClickListener { closeDrawer(); startActivity(Intent(this, ReportsActivity::class.java)) }
        binding.navBackup.setOnClickListener { closeDrawer(); startActivity(Intent(this, BackupActivity::class.java)) }
        binding.navLogout.setOnClickListener { doLogout() }
        binding.btnLogout.setOnClickListener { doLogout() }

        loadData()
    }

    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun doLogout() {
        Session.disconnect()
        goToLogin()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        binding.tvError.visibility = android.view.View.GONE
        val api = Session.api ?: return

        lifecycleScope.launch {
            try {
                val (identity, resource, activeCount, totalUsers) = withContext(Dispatchers.IO) {
                    val identityResp = api.talk(listOf("/system/identity/print"))
                    val resourceResp = api.talk(listOf("/system/resource/print"))
                    val activeResp = api.talk(listOf("/ip/hotspot/active/print", "=count-only="))
                    val usersResp = api.talk(listOf("/ip/hotspot/user/print", "=count-only="))

                    val identityName = identityResp.firstOrNull { it.words.containsKey("name") }
                        ?.words?.get("name") ?: "MikroTik"
                    val resWords = resourceResp.firstOrNull { it.words.isNotEmpty() }?.words ?: emptyMap()
                    val count = activeResp.firstOrNull { it.reply == "!done" }?.words?.get("ret")
                        ?: activeResp.count { it.reply == "!re" }.toString()
                    val usersCount = usersResp.firstOrNull { it.reply == "!done" }?.words?.get("ret")
                        ?: usersResp.count { it.reply == "!re" }.toString()

                    listOf(identityName, resWords, count, usersCount)
                }

                @Suppress("UNCHECKED_CAST")
                val resWords = resource as Map<String, String>

                binding.tvIdentity.text = identity as String
                binding.tvVersion.text = "الإصدار: ${resWords["version"] ?: "-"}"
                binding.tvUptime.text = "مدة التشغيل: ${resWords["uptime"] ?: "-"}"
                binding.tvCpuLoad.text = "حمل المعالج: ${resWords["cpu-load"] ?: "-"}%"
                binding.tvFreeMem.text = "الذاكرة الحرة: ${formatBytes(resWords["free-memory"])}"
                binding.tvActiveCount.text = activeCount as String
                binding.tvTotalUsers.text = totalUsers as String

            } catch (e: Exception) {
                binding.tvError.text = "تعذر تحديث البيانات: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun formatBytes(value: String?): String {
        val bytes = value?.toLongOrNull() ?: return "-"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}
