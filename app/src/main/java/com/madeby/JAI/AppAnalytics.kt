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
            val isCurrentlyLoggedIn = AuthManager.isLoggedIn(context) && !AuthManager.isGuest(context)
            val currentUserId = AuthManager.getUserId(context)
            val linkedName = AuthManager.getLinkedUserName(context)
            val linkedEmail = AuthManager.getLinkedUserEmail(context)
            val now = System.currentTimeMillis()
            val eventId = "${anonId}_${eventName}_${now}_${UUID.randomUUID().toString().substring(0, 6)}"

            val eventJson = JSONObject().apply {
                put("event_id", eventId)
                put("anonymous_id", anonId)
                put("user_id", if (isCurrentlyLoggedIn && !currentUserId.isNullOrBlank()) currentUserId else JSONObject.NULL)
                put("user_name", if (!linkedName.isNullOrBlank()) linkedName else JSONObject.NULL)
                put("user_email", if (!linkedEmail.isNullOrBlank()) linkedEmail else JSONObject.NULL)
                put("is_authenticated", isCurrentlyLoggedIn)
                put("event_name", eventName)
                put("app_version", BuildConfig.VERSION_NAME)
                put("version_code", BuildConfig.VERSION_CODE)
                put("android_sdk", Build.VERSION.SDK_INT)
                put("device_model", getDeviceName())
                put("timestamp", now)
                put("properties", JSONObject(properties))
            }

            enqueueEvent(context, eventJson)
            flushEvents(context, force = true)
        } catch (_: Exception) {}
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        } else {
            "$manufacturer $model".trim()
        }
    }

    private fun enqueueEvent(context: Context, event: JSONObject) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingStr = prefs.getString(KEY_OFFLINE_EVENTS, "[]") ?: "[]"
            val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
            
            // Check for duplicate event_id in local queue
            val newEventId = event.optString("event_id", "")
            if (!newEventId.isNullOrEmpty()) {
                for (i in 0 until array.length()) {
                    val existingEvt = array.optJSONObject(i)
                    if (existingEvt != null && existingEvt.optString("event_id") == newEventId) {
                        return // Skip duplicate action in local buffer
                    }
                }
            }

            if (array.length() >= 100) {
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
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastFlush = prefs.getLong(KEY_LAST_FLUSH_TIME, 0L)
                val now = System.currentTimeMillis()

                if (!force && (now - lastFlush) < MIN_FLUSH_INTERVAL_MS) {
                    return@launch
                }

                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY
                if (supabaseUrl.isBlank() || anonKey.isBlank()) {
                    return@launch
                }

                val eventsToSend = ArrayList<JSONObject>()
                synchronized(AppAnalytics) {
                    val existingStr = prefs.getString(KEY_OFFLINE_EVENTS, "[]") ?: "[]"
                    val array = try { JSONArray(existingStr) } catch (_: Exception) { JSONArray() }
                    if (array.length() == 0) return@launch
                    for (i in 0 until array.length()) {
                        eventsToSend.add(array.getJSONObject(i))
                    }
                    // Immediately clear local queue so events are never re-uploaded if server is cleared
                    prefs.edit().putString(KEY_OFFLINE_EVENTS, "[]").putLong(KEY_LAST_FLUSH_TIME, now).apply()
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
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                android.util.Log.d("AppAnalytics", "flushEvents HTTP response code: $code")
            } catch (e: Exception) {
                android.util.Log.e("AppAnalytics", "flushEvents error", e)
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
                val isCurrentlyLoggedIn = AuthManager.isLoggedIn(context) && !AuthManager.isGuest(context)
                val currentUserId = AuthManager.getUserId(context)
                val linkedName = AuthManager.getLinkedUserName(context)
                val linkedEmail = AuthManager.getLinkedUserEmail(context)
                val firstSeen = prefs.getLong(KEY_FIRST_SEEN, now)
                val totalStudySecs = prefs.getLong(KEY_TOTAL_STUDY_SECS, 0L)
                val totalSessions = prefs.getLong(KEY_TOTAL_SESSIONS, 0L)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val payload = JSONObject().apply {
                    put("anonymous_id", anonId)
                    put("user_id", if (isCurrentlyLoggedIn && !currentUserId.isNullOrBlank()) currentUserId else JSONObject.NULL)
                    put("user_name", if (!linkedName.isNullOrBlank()) linkedName else JSONObject.NULL)
                    put("user_email", if (!linkedEmail.isNullOrBlank()) linkedEmail else JSONObject.NULL)
                    put("is_authenticated", isCurrentlyLoggedIn)
                    put("first_seen", firstSeen)
                    put("last_active_at", now)
                    put("last_active_date", todayStr)
                    put("app_version", BuildConfig.VERSION_NAME)
                    put("version_code", BuildConfig.VERSION_CODE)
                    put("android_sdk", Build.VERSION.SDK_INT)
                    put("device_model", getDeviceName())
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