package com.mikhmon.native_app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityProfilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilesBinding
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ProfileAdapter(mutableListOf()) { profile -> confirmDelete(profile) }
        binding.rvProfiles.layoutManager = LinearLayoutManager(this)
        binding.rvProfiles.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadProfiles() }
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddProfileActivity::class.java))
        }

        loadProfiles()
    }

    override fun onResume() {
        super.onResume()
        loadProfiles()
    }

    private fun loadProfiles() {
        val api = Session.api ?: return
        binding.swipeRefresh.isRefreshing = true
        binding.tvError.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/ip/hotspot/user/profile/print"))
                    resp.filter { it.reply == "!re" }.map {
                        ProfileRow(
                            id = it.words[".id"] ?: "",
                            name = it.words["name"] ?: "-",
                            rateLimit = it.words["rate-limit"] ?: "",
                            sessionTimeout = it.words["session-timeout"] ?: "",
                            sharedUsers = it.words["shared-users"] ?: ""
                        )
                    }
                }
                adapter.replaceAll(rows)
            } catch (e: Exception) {
                binding.tvError.text = "تعذر تحميل الباقات: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(profile: ProfileRow) {
        AlertDialog.Builder(this)
            .setTitle("حذف الباقة")
            .setMessage("هل تريد حذف الباقة \"${profile.name}\"؟\nملاحظة: لا يمكن حذف باقة يستخدمها مستخدمون حالياً.")
            .setPositiveButton("حذف") { _, _ -> deleteProfile(profile) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteProfile(profile: ProfileRow) {
        val api = Session.api ?: return
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/ip/hotspot/user/profile/remove", "=.id=${profile.id}"))
                    val trap = resp.firstOrNull { it.reply == "!trap" }
                    if (trap != null) throw Exception(trap.words["message"] ?: "تعذر الحذف")
                }
                adapter.removeById(profile.id)
            } catch (e: Exception) {
                binding.tvError.text = "خطأ: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            }
        }
    }
}
