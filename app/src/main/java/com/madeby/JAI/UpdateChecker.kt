package com.madeby.JAI

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val apkUrl: String?,
    val releaseNotes: String?
) {
    val hasReleaseNotes: Boolean get() = !releaseNotes.isNullOrBlank()
}

object UpdateChecker {

    private const val MANIFEST_URL = "https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/version.json"
    private const val FALLBACK_URL = "https://get-studytimer.vercel.app/"

    fun check(context: Context, onResult: (UpdateInfo?) -> Unit) {
        Thread {
            val result = runCatching { fetch() }.getOrNull()
            context.applicationContext.mainExecutor.execute { onResult(result) }
        }.start()
    }

    private fun fetch(): UpdateInfo? {
        val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(body)
            val versionCode = json.optInt("versionCode", -1)
            val versionName = json.optString("versionName", "")
            val url = json.optString("url", "").ifBlank { FALLBACK_URL }
            val apkUrl = json.optString("apkUrl", "").ifBlank { null }
            val releaseNotes = if (json.has("releaseNotes") && !json.isNull("releaseNotes")) {
                json.optString("releaseNotes").ifBlank { null }
            } else null
            if (versionCode <= 0) return null
            UpdateInfo(versionCode, versionName, url, apkUrl, releaseNotes)
        } finally {
            connection.disconnect()
        }
    }
}
