package com.mikhmon.native_app.api

import org.json.JSONArray
import org.json.JSONObject

/** إعدادات الاتصال بجهاز MikroTik واحد (يمكن حفظ أكثر من جهاز، كما في Mikhmon الأصلي). */
data class ServerConfig(
    var name: String,
    var host: String,
    var port: Int,
    var useSsl: Boolean,
    var username: String,
    var password: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("host", host)
        put("port", port)
        put("useSsl", useSsl)
        put("username", username)
        put("password", password)
    }

    companion object {
        fun fromJson(o: JSONObject): ServerConfig = ServerConfig(
            name = o.getString("name"),
            host = o.getString("host"),
            port = o.getInt("port"),
            useSsl = o.getBoolean("useSsl"),
            username = o.getString("username"),
            password = o.getString("password")
        )
    }
}

/**
 * تخزين بسيط لقائمة الأجهزة المحفوظة داخل SharedPreferences (مشفّرة على مستوى نظام أندرويد فقط).
 * ملاحظة: لتخزين أكثر أماناً لكلمات المرور يُنصح لاحقاً باستخدام EncryptedSharedPreferences.
 */
object ServerStore {
    private const val PREFS = "mikhmon_servers"
    private const val KEY_LIST = "servers_json"
    private const val KEY_LAST = "last_server_name"

    fun loadAll(ctx: android.content.Context): List<ServerConfig> {
        val prefs = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { ServerConfig.fromJson(arr.getJSONObject(it)) }
    }

    fun saveAll(ctx: android.content.Context, servers: List<ServerConfig>) {
        val prefs = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val arr = JSONArray()
        servers.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun upsert(ctx: android.content.Context, server: ServerConfig) {
        val all = loadAll(ctx).toMutableList()
        val idx = all.indexOfFirst { it.name == server.name }
        if (idx >= 0) all[idx] = server else all.add(server)
        saveAll(ctx, all)
        setLastUsed(ctx, server.name)
    }

    fun setLastUsed(ctx: android.content.Context, name: String) {
        ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST, name).apply()
    }

    fun lastUsed(ctx: android.content.Context): String? =
        ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(KEY_LAST, null)
}
