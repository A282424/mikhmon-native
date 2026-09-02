package com.mikhmon.native_app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityUsersBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var adapter: HotspotUserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = HotspotUserAdapter(mutableListOf()) { user -> confirmDelete(user) }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadUsers() }
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }

        loadUsers()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun loadUsers() {
        val api = Session.api ?: return
        binding.swipeRefresh.isRefreshing = true
        binding.tvError.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    val resp = api.talk(listOf("/ip/hotspot/user/print"))
                    resp.filter { it.reply == "!re" }.map {
                        HotspotUserRow(
                            id = it.words[".id"] ?: "",
                            name = it.words["name"] ?: "-",
                            profile = it.words["profile"] ?: ""
                        )
                    }
                }
                adapter.replaceAll(rows)
            } catch (e: Exception) {
                binding.tvError.text = "تعذر تحميل المستخدمين: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(user: HotspotUserRow) {
        AlertDialog.Builder(this)
            .setTitle("حذف المستخدم")
            .setMessage("هل تريد حذف \"${user.name}\"؟")
            .setPositiveButton("حذف") { _, _ -> deleteUser(user) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteUser(user: HotspotUserRow) {
        val api = Session.api ?: return
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.talk(listOf("/ip/hotspot/user/remove", "=.id=${user.id}"))
                }
                adapter.removeById(user.id)
            } catch (e: Exception) {
                binding.tvError.text = "تعذر حذف المستخدم: ${e.message}"
                binding.tvError.visibility = android.view.View.VISIBLE
            }
        }
    }
}
