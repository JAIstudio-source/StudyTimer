package com.madeby.JAI

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Privacy-first, lightweight analytics engine with instant 1st launch telemetry
 * and throttled background telemetry for minimal Supabase resource footprint.
 */
object AppAnalytics {

    private const val PREFS_NAME = "studytimer_analytics_prefs"
    private const val KEY_ANON_ID = "anonymous_id"
    private const val KEY_FIRST_SEEN = "first_seen_timestamp"
    private const val KEY_FIRST_LAUNCH_REPORTED = "first_launch_reported_v1"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    private const val KEY_OFFLINE_EVENTS = "offline_events_queue"
    private const val KEY_TOTAL_SESSIONS = "stats_total_sessions"
    private const val KEY_TOTAL_STUDY_SECS = "stats_total_study_secs"
    private const val KEY_LAST_FLUSH_TIME = "last_flush_time"
    private const val KEY_LAST_COHORT_SYNC_TIME = "last_cohort_sync_time"

    private const val MIN_FLUSH_INTERVAL_MS = 180000L // 3 minutes throttling for routine events
    private const val MIN_COHORT_INTERVAL_MS = 300000L // 5 minutes throttling for background syncs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isFlushing = AtomicBoolean(false)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.getBoolean(KEY_FIRST_LAUNCH_REPORTED, false)
        ensureAnonymousId(context)

        if (isFirstLaunch) {
            // Priority Instant 1st launch report
            trackEvent(context, "first_install_open", mapOf("is_new_install" to true))
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH_REPORTED, true).apply()
            syncUserCohort(context, force = true)
            flushEvents(context, force = true)
        } else {
            recordAppOpen(context)
            flushEvents(context, force = false)
        }
    }

    fun getAnonymousId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ANON_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_ANON_ID, id).apply()
        }
        return id
    }

    private fun ensureAnonymousId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var id = prefs.getString(KEY_ANON_ID, null)
        val editor = prefs.edit()
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            editor.putString(KEY_ANON_ID, id)
            editor.putLong(KEY_FIRST_SEEN, now)
        }
        editor.apply()
        return id
    }

    fun associateUser(context: Context, userId: String?) {
        scope.launch {
            if (userId.isNullOrBlank()) return@launch
            trackEvent(context, "user_identified", mapOf("user_id" to userId))
            syncUserCohort(context, force = true)
            flushEvents(context, force = true)
        }
    }

    fun onLogout(context: Context) {
        scope.launch {
            trackEvent(context, "user_logged_out", emptyMap())
            syncUserCohort(context, force = true)
            flushEvents(context, force = true)
        }
    }

    fun trackSessionStart(context: Context, mode: String) {
        scope.launch {
            trackEvent(context, "timer_started", mapOf("timer_mode" to mode))
        }
    }

    fun trackSessionEnd(context: Context, mode: String, durationSecs: Long, completed: Boolean) {
        if (durationSecs <= 0L) return
        scope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentTotal = prefs.getLong(KEY_TOTAL_STUDY_SECS, 0L) + durationSecs
            val currentSessions = prefs.getLong(KEY_TOTAL_SESSIONS, 0L) + 1L
            prefs.edit()
                .putLong(KEY_TOTAL_STUDY_SECS, currentTotal)
                .putLong(KEY_TOTAL_SESSIONS, currentSessions)
                .apply()

            trackEvent(
                context,
                if (completed) "timer_completed" else "timer_stopped",
                mapOf(
                    "timer_mode" to mode,
                    "duration_secs" to durationSecs,
                    "completed" to completed
                )
            )
            syncUserCohort(context, force = false)
            flushEvents(context, force = true)
        }
    }

    fun trackSessionPause(context: Context) {
        scope.launch {
            trackEvent(context, "timer_paused", emptyMap())
        }
    }

    fun trackSessionResume(context: Context) {
        scope.launch {
            trackEvent(context, "timer_resumed", emptyMap())
        }
    }

    fun trackFeatureUsage(context: Context, featureName: String) {
        scope.launch {
            trackEvent(context, "feature_used", mapOf("feature" to featureName))
        }
    }

    private fun recordAppOpen(context: Context) {
        scope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)
            val isNewDay = lastDate != todayStr

            if (isNewDay) {
                prefs.edit().putString(KEY_LAST_ACTIVE_DATE, todayStr).apply()
                trackEvent(context, "app_open", mapOf("first_open_today" to true))
                syncUserCohort(context, force = true)
                flushEvents(context, force = true)
            } else {
                trackEvent(context, "app_open", mapOf("first_open_today" to false))
            }
        }
    }

    fun trackEvent(context: Context, eventName: String, properties: Map<String, Any>) {
        try {
            val anonId = getAnonymousId(context)
            val userId = AuthManager.getUserId(context)
            val isAuth = !AuthManager.isGuest(context) && !userId.isNullOrBlank()

            val eventJson = JSONObject().apply {
                put("anonymous_id", anonId)
                put("user_id", if (isAuth) userId else JSONObject.NULL)
                put("is_authenticated", isAuth)
                put("event_name", eventName)
                put("app_version", BuildConfig.VERSION_NAME)
                put("version_code", BuildConfig.VERSION_CODE)
                put("android_sdk", Build.VERSION.SDK_INT)
                put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("timestamp", System.currentTimeMillis())
                put("properties", JSONObject(properties))
            }

            enqueueEvent(context, eventJson)
        } catch (_: Exception) {}
    }

    private fun enqueueEvent(context: Context, event: JSONObject) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingStr = prefs.getString(KEY_OFFLINE_EVENTS, "[]") ?: "[]"
            val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
            if (array.length() >= 150) {
                array.remove(0)
            }
            array.put(event)
            prefs.edit().putString(KEY_OFFLINE_EVENTS, array.toString()).apply()

            if (array.length() >= 10) {
                flushEvents(context, force = true)
            }
        }
    }

    fun flushEvents(context: Context, force: Boolean = false) {
        if (!isFlushing.compareAndSet(false, true)) return
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastFlush = prefs.getLong(KEY_LAST_FLUSH_TIME, 0L)
                val now = System.currentTimeMillis()

                if (!force && (now - lastFlush) < MIN_FLUSH_INTERVAL_MS) {
                    isFlushing.set(false)
                    return@launch
                }

                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY
                if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                    isFlushing.set(false)
                    return@launch
                }

                val eventsToSend = ArrayList<JSONObject>()
                synchronized(AppAnalytics) {
                    val existingStr = prefs.getString(KEY_OFFLINE_EVENTS, "[]") ?: "[]"
                    val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
                    for (i in 0 until array.length()) {
                        eventsToSend.add(array.getJSONObject(i))
                    }
                }

                if (eventsToSend.isEmpty()) {
                    isFlushing.set(false)
                    return@launch
                }

                val payload = JSONArray()
                for (evt in eventsToSend) {
                    payload.put(evt)
                }

                val url = URL("$supabaseUrl/rest/v1/app_analytics_events")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=ignore-duplicates")
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    synchronized(AppAnalytics) {
                        prefs.edit()
                            .putString(KEY_OFFLINE_EVENTS, "[]")
                            .putLong(KEY_LAST_FLUSH_TIME, now)
                            .apply()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isFlushing.set(false)
            }
        }
    }

    private fun syncUserCohort(context: Context, force: Boolean = false) {
        scope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSync = prefs.getLong(KEY_LAST_COHORT_SYNC_TIME, 0L)
            val now = System.currentTimeMillis()

            if (!force && (now - lastSync) < MIN_COHORT_INTERVAL_MS) {
                return@launch
            }

            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            if (supabaseUrl.isBlank() || anonKey.isBlank()) return@launch

            try {
                val anonId = getAnonymousId(context)
                val userId = AuthManager.getUserId(context)
                val isAuth = !AuthManager.isGuest(context) && !userId.isNullOrBlank()
                val firstSeen = prefs.getLong(KEY_FIRST_SEEN, now)
                val totalStudySecs = prefs.getLong(KEY_TOTAL_STUDY_SECS, 0L)
                val totalSessions = prefs.getLong(KEY_TOTAL_SESSIONS, 0L)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val payload = JSONObject().apply {
                    put("anonymous_id", anonId)
                    put("user_id", if (isAuth) userId else JSONObject.NULL)
                    put("is_authenticated", isAuth)
                    put("first_seen", firstSeen)
                    put("last_active_at", now)
                    put("last_active_date", todayStr)
                    put("app_version", BuildConfig.VERSION_NAME)
                    put("version_code", BuildConfig.VERSION_CODE)
                    put("android_sdk", Build.VERSION.SDK_INT)
                    put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                    put("total_study_secs", totalStudySecs)
                    put("total_sessions", totalSessions)
                    put("updated_at", now)
                }

                val url = URL("$supabaseUrl/rest/v1/app_user_cohorts?on_conflict=anonymous_id")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                if (conn.responseCode in 200..299) {
                    prefs.edit().putLong(KEY_LAST_COHORT_SYNC_TIME, now).apply()
                }
            } catch (_: Exception) {}
        }
    }
}