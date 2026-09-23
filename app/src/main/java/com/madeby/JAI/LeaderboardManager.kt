package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object LeaderboardManager {
    private const val TAG = "LeaderboardManager"
    private const val CACHE_TTL_MS = 25_000L // 25 seconds

    private data class CacheRecord(
        val timestamp: Long,
        val entries: List<LeaderboardEntry>
    )

    private val cache = mutableMapOf<LeaderboardPeriod, CacheRecord>()

    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }

    suspend fun fetchLeaderboard(
        context: Context,
        period: LeaderboardPeriod,
        forceRefresh: Boolean = false
    ): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            synchronized(cache) {
                val cached = cache[period]
                if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
                    return@withContext Result.success(cached.entries)
                }
            }
        }

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            return@withContext Result.failure(Exception("Supabase credentials are not configured."))
        }

        try {
            val rpcName = when (period) {
                LeaderboardPeriod.DAILY -> "get_daily_leaderboard"
                LeaderboardPeriod.WEEKLY -> "get_weekly_leaderboard"
                LeaderboardPeriod.MONTHLY -> "get_monthly_leaderboard"
            }

            val url = URL("$supabaseUrl/rest/v1/rpc/$rpcName")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 9000
            conn.readTimeout = 9000
            conn.doOutput = true

            val body = JSONObject().apply {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                val todayStr = sdf.format(cal.time)

                when (period) {
                    LeaderboardPeriod.DAILY -> {
                        put("p_date", todayStr)
                        put("p_limit", 50)
                    }
                    LeaderboardPeriod.WEEKLY -> {
                        val endStr = todayStr
                        cal.add(Calendar.DAY_OF_YEAR, -6)
                        val startStr = sdf.format(cal.time)
                        put("p_start_date", startStr)
                        put("p_end_date", endStr)
                        put("p_limit", 50)
                    }
                    LeaderboardPeriod.MONTHLY -> {
                        val endStr = todayStr
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val startStr = sdf.format(cal.time)
                        put("p_start_date", startStr)
                        put("p_end_date", endStr)
                        put("p_limit", 50)
                    }
                }
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = if (responseStr.isNotBlank()) JSONArray(responseStr) else JSONArray()
                val list = mutableListOf<LeaderboardEntry>()

                val seenUsers = mutableSetOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawRank = obj.optInt("rank", i + 1)
                    val userId = obj.optString("user_id", "")
                    val userName = obj.optString("user_name", "Student")
                    val avatarUrl = obj.optString("avatar_url", "")
                    val totalSecs = obj.optInt("total_seconds", 0)
                    val isStudying = obj.optBoolean("is_studying", false)
                    val currentSubject = obj.optString("current_subject", "")
                    val subjectColor = obj.optString("subject_color", "#3b82f6")
                    val lastActiveAt = obj.optString("last_active_at", "")

                    val dedupeKey = if (userId.isNotBlank()) userId.trim().lowercase() else userName.trim().lowercase()
                    if (!seenUsers.contains(dedupeKey)) {
                        seenUsers.add(dedupeKey)
                        list.add(
                            LeaderboardEntry(
                                rank = list.size + 1,
                                userId = userId,
                                userName = if (userName.isNotBlank()) userName else "Student",
                                avatarUrl = avatarUrl,
                                totalSeconds = totalSecs,
                                isStudying = isStudying,
                                currentSubject = currentSubject,
                                subjectColor = subjectColor,
                                lastActiveAt = lastActiveAt
                            )
                        )
                    }
                }

                synchronized(cache) {
                    cache[period] = CacheRecord(now, list)
                }
                Result.success(list)
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Fetch leaderboard ($period) failed: HTTP $code: $errBody")
                Result.failure(Exception("HTTP $code: $errBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leaderboard ($period)", e)
            Result.failure(e)
        }
    }

    fun isParticipating(context: Context): Boolean {
        return context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .getBoolean("leaderboard_participate", true)
    }

    fun isLiveStatusSharingEnabled(context: Context): Boolean {
        return context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .getBoolean("leaderboard_share_live_status", true)
    }

    fun setParticipating(context: Context, enabled: Boolean) {
        context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("leaderboard_participate", enabled)
            .apply()
    }

    fun setLiveStatusSharingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("leaderboard_share_live_status", enabled)
            .apply()
    }

    suspend fun updateStudyPresence(
        context: Context,
        isStudying: Boolean,
        subject: String = "",
        color: String = "#3b82f6"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context)) {
            return@withContext false // Gated: Guests do not publish presence
        }

        val shareLive = isLiveStatusSharingEnabled(context)
        val participating = isParticipating(context)
        val effectiveIsStudying = if (shareLive && participating) isStudying else false
        val effectiveSubject = if (shareLive && participating && effectiveIsStudying) subject else ""
        val effectiveColor = if (shareLive && participating && effectiveIsStudying) color else "#3b82f6"

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val userName = (AuthManager.getUserName(context) ?: "Student").trim().take(30)
        val avatarUrl = (AuthManager.getProfileImageUri(context) ?: "").take(250)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext false
        }

        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/update_study_presence")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("p_user_id", userId)
                put("p_user_name", userName)
                put("p_avatar_url", avatarUrl)
                put("p_is_studying", effectiveIsStudying)
                put("p_current_subject", effectiveSubject.trim().take(40))
                put("p_subject_color", effectiveColor.trim().take(10))
                put("p_study_date", todayStr)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                clearCache()
                true
            } else {
                Log.w(TAG, "updateStudyPresence failed with code $code")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "updateStudyPresence non-fatal error", e)
            false
        }
    }

    suspend fun syncStudyProgress(
        context: Context,
        incrementalSeconds: Int,
        isStudying: Boolean,
        subject: String = "",
        color: String = "#3b82f6"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context)) {
            return@withContext false
        }
        if (!isParticipating(context)) {
            return@withContext false // Gated: Leaderboard participation disabled
        }

        val shareLive = isLiveStatusSharingEnabled(context)
        val effectiveIsStudying = if (shareLive) isStudying else false
        val effectiveSubject = (if (shareLive) subject else "").trim().take(40)
        val effectiveColor = (if (shareLive) color else "#3b82f6").trim().take(10)

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val userName = (AuthManager.getUserName(context) ?: "Student").trim().take(30)
        val avatarUrl = (AuthManager.getProfileImageUri(context) ?: "").take(250)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext false
        }

        val clampedSeconds = incrementalSeconds.coerceIn(0, 14400) // Max 4 hours in a single increment

        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/sync_study_progress_leaderboard")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("p_user_id", userId)
                put("p_user_name", userName)
                put("p_avatar_url", avatarUrl)
                put("p_incremental_seconds", clampedSeconds)
                put("p_is_studying", effectiveIsStudying)
                put("p_current_subject", effectiveSubject)
                put("p_subject_color", effectiveColor)
                put("p_study_date", todayStr)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                clearCache()
                true
            } else if (code == 404 || code == 400) {
                if (clampedSeconds > 0) {
                    recordStudySession(context, clampedSeconds, effectiveSubject, effectiveColor)
                }
                updateStudyPresence(context, effectiveIsStudying, effectiveSubject, effectiveColor)
            } else {
                Log.w(TAG, "syncStudyProgress failed with code $code")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncStudyProgress non-fatal error", e)
            false
        }
    }

    suspend fun recordStudySession(
        context: Context,
        durationSeconds: Int,
        subject: String = "",
        color: String = "#3b82f6"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context) || durationSeconds <= 0) {
            return@withContext false // Gated: Only authenticated sessions are published
        }
        if (!isParticipating(context)) {
            return@withContext false // Gated: Leaderboard participation disabled
        }

        val shareLive = isLiveStatusSharingEnabled(context)
        val effectiveSubject = (if (shareLive) subject else "").trim().take(40)
        val effectiveColor = (if (shareLive) color else "#3b82f6").trim().take(10)
        val clampedDuration = durationSeconds.coerceIn(1, 86400) // Max 24 hours per session

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val userName = (AuthManager.getUserName(context) ?: "Student").trim().take(30)
        val avatarUrl = (AuthManager.getProfileImageUri(context) ?: "").take(250)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext false
        }

        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/record_study_session_leaderboard")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("p_user_id", userId)
                put("p_user_name", userName)
                put("p_avatar_url", avatarUrl)
                put("p_duration_seconds", clampedDuration)
                put("p_subject", effectiveSubject)
                put("p_subject_color", effectiveColor)
                put("p_study_date", todayStr)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                clearCache()
                Log.i(TAG, "Recorded session of $durationSeconds seconds for $userName ($userId)")
                true
            } else {
                Log.w(TAG, "recordStudySession failed with code $code")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "recordStudySession non-fatal error", e)
            false
        }
    }

    suspend fun overrideLeaderboardDailyTotal(
        context: Context,
        totalSeconds: Int,
        subject: String = "",
        color: String = "#3b82f6"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context)) {
            return@withContext false
        }
        if (!isParticipating(context)) {
            return@withContext false // Gated: Leaderboard participation disabled
        }

        val shareLive = isLiveStatusSharingEnabled(context)
        val effectiveSubject = if (shareLive) subject else ""
        val effectiveColor = if (shareLive) color else "#3b82f6"

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val userName = AuthManager.getUserName(context) ?: "Student"
        val avatarUrl = AuthManager.getProfileImageUri(context) ?: ""
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext false
        }

        try {
            val url = URL("$supabaseUrl/rest/v1/daily_leaderboard?on_conflict=user_id,study_date")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val nowIso = isoFmt.format(Date())

            val body = JSONObject().apply {
                put("user_id", userId)
                put("user_name", userName)
                put("avatar_url", avatarUrl)
                put("study_date", todayStr)
                put("total_seconds", totalSeconds.coerceIn(0, 86400))
                put("is_studying", false)
                put("current_subject", effectiveSubject)
                put("subject_color", effectiveColor)
                put("last_active_at", nowIso)
                put("updated_at", nowIso)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                clearCache()
                true
            } else {
                Log.w(TAG, "overrideLeaderboardDailyTotal failed with code $code")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "overrideLeaderboardDailyTotal non-fatal error", e)
            false
        }
    }

    fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0m"
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            mins > 0 -> "${mins}m"
            else -> "${secs}s"
        }
    }

    fun isCurrentUser(entry: LeaderboardEntry, context: Context): Boolean {
        val currentUserId = AuthManager.getUserId(context)
        val currentEmail = AuthManager.getUserEmail(context)
        val currentName = AuthManager.getUserName(context)

        if (!currentUserId.isNullOrBlank() && entry.userId.equals(currentUserId, ignoreCase = true)) return true
        if (!currentEmail.isNullOrBlank() && entry.userId.equals(currentEmail, ignoreCase = true)) return true
        if (!currentName.isNullOrBlank() && entry.userName.equals(currentName, ignoreCase = true)) return true
        return false
    }

    /**
     * Calculates the real, verified timer focus seconds for a given date.
     * Manual adjustments added by standard users are excluded from this total.
     * Developer-authorized leaderboard additions (dev_leaderboard_bonus_secs) are included.
     */
    fun getRealTimerFocusSecondsForDate(context: Context, dateStr: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = runCatching { sdf.parse(dateStr) }.getOrNull() ?: Date()
        val startCal = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStartMs = startCal.timeInMillis
        val dayEndMs = dayStartMs + 24L * 3600_000L - 1000L

        val entries = TimelineLogger.load(context)
        val parsedAll = parseDayBlocks(entries)
        val daySessions = parsedAll.sessions.filter { it.startMs in dayStartMs..dayEndMs }
        
        // Sum only real timer sessions (non-manual)
        val realSessionSum = daySessions.filter { !it.manual }.sumOf { it.secs }

        val isToday = dateStr == sdf.format(Date())
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val runningStudy = if (isToday) prefs.getLong("accumulatedStudy", 0L) else 0L
        val devBonus = prefs.getLong("dev_leaderboard_bonus_secs_$dateStr", 0L)

        return (realSessionSum + runningStudy + devBonus).coerceAtLeast(0L)
    }
}
