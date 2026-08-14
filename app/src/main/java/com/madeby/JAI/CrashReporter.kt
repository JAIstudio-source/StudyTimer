package com.madeby.JAI

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production Crash Reporting Engine.
 * - Captures unhandled thread crashes and exception stack traces.
 * - Scrubs sensitive tokens, auth keys, and emails before transmitting or storing.
 * - Stores crashes locally in offline journal if network is unavailable.
 * - Reports stack trace, app version, SDK, device model, and timestamp.
 */
object CrashReporter {

    private const val PREFS_NAME = "studytimer_crash_prefs"
    private const val KEY_CRASH_QUEUE = "offline_crashes_queue"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isFlushing = AtomicBoolean(false)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrashSynchronous(context, thread, throwable)
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        flushCrashes(context)
    }

    fun logHandledException(context: Context, throwable: Throwable, contextTag: String? = null) {
        scope.launch {
            try {
                val crashJson = buildCrashJson(context, "Handled Exception [${contextTag ?: "Unknown"}]", throwable)
                enqueueCrash(context, crashJson)
                flushCrashes(context)
            } catch (_: Exception) {}
        }
    }

    private fun recordCrashSynchronous(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val crashJson = buildCrashJson(context, "Thread: ${thread.name}", throwable)
            enqueueCrash(context, crashJson)
        } catch (_: Exception) {}
    }

    private fun buildCrashJson(context: Context, threadInfo: String, throwable: Throwable): JSONObject {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val rawTrace = sw.toString()
        val sanitizedTrace = sanitizeStackTrace(rawTrace)

        val anonId = AppAnalytics.getAnonymousId(context)
        val userId = AuthManager.getUserId(context)
        val isAuth = !AuthManager.isGuest(context) && !userId.isNullOrBlank()

        return JSONObject().apply {
            put("anonymous_id", anonId)
            put("user_id", if (isAuth) userId else JSONObject.NULL)
            put("is_authenticated", isAuth)
            put("exception_type", throwable.javaClass.name)
            put("message", sanitizeMessage(throwable.message ?: "No message"))
            put("stack_trace", sanitizedTrace)
            put("thread_info", threadInfo)
            put("app_version", BuildConfig.VERSION_NAME)
            put("version_code", BuildConfig.VERSION_CODE)
            put("android_sdk", Build.VERSION.SDK_INT)
            put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            put("timestamp", System.currentTimeMillis())
        }
    }

    private fun sanitizeStackTrace(trace: String): String {
        return trace
            .replace(Regex("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}"), "[EMAIL_REDACTED]")
            .replace(Regex("(?i)bearer\\s+[a-z0-9._-]+"), "Bearer [TOKEN_REDACTED]")
            .replace(Regex("(?i)apikey=[a-z0-9._-]+"), "apikey=[REDACTED]")
    }

    private fun sanitizeMessage(msg: String): String {
        return msg
            .replace(Regex("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}"), "[EMAIL_REDACTED]")
            .replace(Regex("(?i)bearer\\s+[a-z0-9._-]+"), "Bearer [TOKEN_REDACTED]")
    }

    private fun enqueueCrash(context: Context, crash: JSONObject) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingStr = prefs.getString(KEY_CRASH_QUEUE, "[]") ?: "[]"
            val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
            if (array.length() >= 50) {
                array.remove(0)
            }
            array.put(crash)
            prefs.edit().putString(KEY_CRASH_QUEUE, array.toString()).commit()
        }
    }

    fun flushCrashes(context: Context) {
        if (!isFlushing.compareAndSet(false, true)) return
        scope.launch {
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY
                if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                    isFlushing.set(false)
                    return@launch
                }

                val crashesToSend = ArrayList<JSONObject>()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                synchronized(CrashReporter) {
                    val existingStr = prefs.getString(KEY_CRASH_QUEUE, "[]") ?: "[]"
                    val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
                    for (i in 0 until array.length()) {
                        crashesToSend.add(array.getJSONObject(i))
                    }
                }

                if (crashesToSend.isEmpty()) {
                    isFlushing.set(false)
                    return@launch
                }

                val payload = JSONArray()
                for (item in crashesToSend) {
                    payload.put(item)
                }

                val url = URL("$supabaseUrl/rest/v1/app_crash_reports")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=ignore-duplicates")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    synchronized(CrashReporter) {
                        prefs.edit().putString(KEY_CRASH_QUEUE, "[]").apply()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isFlushing.set(false)
            }
        }
    }
}