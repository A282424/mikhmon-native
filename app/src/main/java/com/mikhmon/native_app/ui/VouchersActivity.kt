package com.mikhmon.native_app.ui

import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikhmon.native_app.api.Session
import com.mikhmon.native_app.databinding.ActivityVouchersBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * يولّد "كروتاً" (Vouchers) بأسلوب Mikhmon الأصلي: كل كرت هو مستخدم Hotspot
 * حيث اسم المستخدم وكلمة المرور متطابقان (رمز عشوائي)، مرتبط بباقة (Profile) محددة.
 */
class VouchersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVouchersBinding
    private lateinit var adapter: VoucherAdapter
    private val generated = mutableListOf<Voucher>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVouchersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = VoucherAdapter(generated)
        binding.rvVouchers.layoutManager = LinearLayoutManager(this)
        binding.rvVouchers.adapter = adapter

        binding.btnGenerate.setOnClickListener { generate() }
        binding.btnPrint.setOnClickListener { printVouchers() }
    }

    private fun generate() {
        val countText = binding.etCount.text.toString().trim()
        val profile = binding.etProfile.text.toString().trim()
        val prefix = binding.etPrefix.text.toString().trim()
        val lengthText = binding.etLength.text.toString().trim()

        val count = countText.toIntOrNull()
        val length = lengthText.toIntOrNull()

        if (count == null || count <= 0 || count > 500) {
            showError("عدد الكروت يجب أن يكون بين 1 و 500")
            return
        }
        if (profile.isEmpty()) {
            showError("اسم الباقة (Profile) مطلوب")
            return
        }
        if (length == null || length !in 3..12) {
            showError("طول الرمز يجب أن يكون بين 3 و 12")
            return
        }
        val api = Session.api ?: run {
            showError("انتهت الجلسة، الرجاء تسجيل الدخول مجدداً")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val vouchers = mutableListOf<Voucher>()
                val errors = mutableListOf<String>()

                withContext(Dispatchers.IO) {
                    repeat(count) {
                        val code = prefix + randomDigits(length)
                        val resp = api.talk(
                            listOf(
                                "/ip/hotspot/user/add",
                                "=name=$code",
                                "=password=$code",
                                "=profile=$profile",
                                "=comment=voucher"
                            )
                        )
                        val trap = resp.firstOrNull { it.reply == "!trap" }
                        if (trap != null) {
                            errors.add("$code: ${trap.words["message"] ?: "فشل"}")
                        } else {
                            vouchers.add(Voucher(code, code))
                        }
                    }
                }

                generated.addAll(0, vouchers)
                adapter.replaceAll(generated)
                setLoading(false)

                if (vouchers.isNotEmpty()) {
                    binding.btnPrint.visibility = android.view.View.VISIBLE
                }
                if (errors.isNotEmpty()) {
                    showError("تم توليد ${vouchers.size} كرت بنجاح، وفشل ${errors.size}:\n${errors.take(5).joinToString("\n")}")
                } else {
                    binding.tvError.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                setLoading(false)
                showError("خطأ: ${e.message}")
            }
        }
    }

    private fun randomDigits(length: Int): String {
        val sb = StringBuilder()
        repeat(length) { sb.append(Random.nextInt(0, 10)) }
        return sb.toString()
    }

    /** يبني صفحة HTML بسيطة تعرض الكروت كبطاقات، ثم يفتح نظام الطباعة في أندرويد. */
    private fun printVouchers() {
        if (generated.isEmpty()) return

        val cardsHtml = generated.joinToString("") { v ->
            """
            <div class="card">
                <div class="label">Mikhmon Voucher</div>
                <div class="row"><span>User:</span> <b>${v.username}</b></div>
                <div class="row"><span>Pass:</span> <b>${v.password}</b></div>
            </div>
            """.trimIndent()
        }

        val html = """
            <html>
            <head>
            <meta charset="utf-8">
            <style>
                body { font-family: monospace; }
                .card {
                    display: inline-block;
                    width: 45%;
                    border: 1px dashed #333;
                    border-radius: 8px;
                    padding: 10px;
                    margin: 6px;
                    box-sizing: border-box;
                }
                .label { font-weight: bold; font-size: 12px; margin-bottom: 6px; }
                .row { font-size: 14px; }
                .row span { color: #555; }
            </style>
            </head>
            <body>$cardsHtml</body>
            </html>
        """.trimIndent()

        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(PRINT_SERVICE) as PrintManager
                val jobName = "Mikhmon Vouchers"
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnGenerate.isEnabled = !loading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}
