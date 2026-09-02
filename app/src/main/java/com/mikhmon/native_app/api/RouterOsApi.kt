package com.mikhmon.native_app.api

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocketFactory

/**
 * تطبيق كامل لبروتوكول MikroTik RouterOS API (النسخة الحديثة plain-login، RouterOS >= 6.43).
 *
 * مرجع البروتوكول الرسمي:
 * https://help.mikrotik.com/docs/display/ROS/API
 *
 * الاستخدام:
 *   val api = RouterOsApi()
 *   api.connect(host, port, useSsl = true)
 *   val ok = api.login(username, password)
 *   val users = api.talk(listOf("/ip/hotspot/active/print"))
 *   api.close()
 *
 * ملاحظة: يجب استدعاء هذه الدوال من Thread/Coroutine خارج الـ Main Thread فقط.
 */
class RouterOsApi {

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    var connectTimeoutMs: Int = 8000
    var readTimeoutMs: Int = 15000

    /** يفتح اتصال TCP (أو TLS إن كان useSsl=true، المنفذ المعتاد لها 8729). */
    @Throws(Exception::class)
    fun connect(host: String, port: Int, useSsl: Boolean) {
        val rawSocket = if (useSsl) {
            val factory = SSLSocketFactory.getDefault()
            factory.createSocket()
        } else {
            Socket()
        }
        rawSocket.connect(InetSocketAddress(host, port), connectTimeoutMs)
        rawSocket.soTimeout = readTimeoutMs
        socket = rawSocket
        input = DataInputStream(rawSocket.getInputStream())
        output = DataOutputStream(rawSocket.getOutputStream())
    }

    fun isConnected(): Boolean = socket?.isConnected == true && socket?.isClosed == false

    /**
     * تسجيل الدخول بالطريقة الحديثة (plain). يعيد true عند النجاح.
     * في حال الفشل يرمي استثناء برسالة الخطأ القادمة من الراوتر (!trap).
     */
    @Throws(Exception::class)
    fun login(username: String, password: String): Boolean {
        val response = talk(
            listOf(
                "/login",
                "=name=$username",
                "=password=$password"
            )
        )
        val hasDone = response.any { it.reply == "!done" }
        val trap = response.firstOrNull { it.reply == "!trap" }
        if (trap != null) {
            val msg = trap.words["message"] ?: "فشل تسجيل الدخول"
            throw RouterOsException(msg)
        }
        return hasDone
    }

    /**
     * يرسل جملة أوامر (سطر أوامر RouterOS API) ويستقبل كل الجمل حتى !done.
     * مثال: talk(listOf("/ip/hotspot/user/print"))
     */
    @Throws(Exception::class)
    @Synchronized
    fun talk(words: List<String>): List<Sentence> {
        writeSentence(words)
        val results = mutableListOf<Sentence>()
        while (true) {
            val sentence = readSentence()
            if (sentence.words.isEmpty() && sentence.reply.isEmpty()) break
            results.add(sentence)
            if (sentence.reply == "!done") break
        }
        return results
    }

    fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
    }

    // ---------- الطبقة الداخلية لترميز/فك ترميز البروتوكول ----------

    private fun writeLength(len: Int) {
        val out = output ?: throw IllegalStateException("Not connected")
        when {
            len < 0x80 -> out.writeByte(len)
            len < 0x4000 -> {
                out.writeByte((len shr 8) or 0x80)
                out.writeByte(len and 0xFF)
            }
            len < 0x200000 -> {
                out.writeByte((len shr 16) or 0xC0)
                out.writeByte((len shr 8) and 0xFF)
                out.writeByte(len and 0xFF)
            }
            len < 0x10000000 -> {
                out.writeByte((len shr 24) or 0xE0)
                out.writeByte((len shr 16) and 0xFF)
                out.writeByte((len shr 8) and 0xFF)
                out.writeByte(len and 0xFF)
            }
            else -> {
                out.writeByte(0xF0)
                out.writeInt(len)
            }
        }
    }

    private fun writeWord(word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        writeLength(bytes.size)
        output?.write(bytes)
    }

    private fun writeSentence(words: List<String>) {
        for (w in words) writeWord(w)
        writeLength(0) // نهاية الجملة
        output?.flush()
    }

    private fun readLength(): Int {
        val inp = input ?: throw IllegalStateException("Not connected")
        val c = inp.readUnsignedByte()
        return when {
            c and 0x80 == 0x00 -> c
            c and 0xC0 == 0x80 -> ((c and 0x7F) shl 8) or inp.readUnsignedByte()
            c and 0xE0 == 0xC0 -> ((c and 0x3F) shl 16) or (inp.readUnsignedByte() shl 8) or inp.readUnsignedByte()
            c and 0xF0 == 0xE0 -> ((c and 0x1F) shl 24) or (inp.readUnsignedByte() shl 16) or
                    (inp.readUnsignedByte() shl 8) or inp.readUnsignedByte()
            else -> inp.readInt()
        }
    }

    private fun readWord(): String {
        val len = readLength()
        if (len == 0) return ""
        val buf = ByteArray(len)
        input?.readFully(buf)
        return String(buf, Charsets.UTF_8)
    }

    /** يقرأ جملة كاملة (سلسلة كلمات حتى كلمة فارغة) ويحوّلها إلى Sentence مبنية. */
    private fun readSentence(): Sentence {
        val words = mutableListOf<String>()
        while (true) {
            val w = try {
                readWord()
            } catch (e: SocketTimeoutException) {
                throw RouterOsException("انتهت مهلة الاتصال بالراوتر (تحقق من الشبكة/المنفذ)")
            }
            if (w.isEmpty()) break
            words.add(w)
        }
        return Sentence.from(words)
    }
}

/** تمثيل جملة رد واحدة قادمة من الراوتر: نوعها (!re, !done, !trap) + خصائصها كخريطة. */
data class Sentence(
    val reply: String,
    val words: Map<String, String>,
    val raw: List<String>
) {
    companion object {
        fun from(words: List<String>): Sentence {
            val reply = words.firstOrNull { it.startsWith("!") } ?: ""
            val map = mutableMapOf<String, String>()
            for (w in words) {
                if (w.startsWith("=")) {
                    val body = w.substring(1)
                    val idx = body.indexOf('=')
                    if (idx > 0) {
                        map[body.substring(0, idx)] = body.substring(idx + 1)
                    }
                }
            }
            return Sentence(reply, map, words)
        }
    }
}

class RouterOsException(message: String) : Exception(message)
