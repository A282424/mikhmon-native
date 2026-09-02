package com.mikhmon.native_app.api

/** يحمل اتصال RouterOS API الحالي طوال مدة الجلسة (بين الشاشات المختلفة). */
object Session {
    var api: RouterOsApi? = null
    var config: ServerConfig? = null

    fun disconnect() {
        api?.close()
        api = null
        config = null
    }
}
