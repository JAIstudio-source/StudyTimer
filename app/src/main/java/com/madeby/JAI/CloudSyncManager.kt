package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncManager {

    suspend fun syncDataToCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val accessToken = AuthManager.getAccessToken(context)

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            Log.d("CloudSyncManager", "Skipping cloud sync: Not logged in to Supabase or missing credentials")
            return@withContext false
        }

        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val prefsJson = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                prefsJson.put(key, value)
            }
            AuthManager.getUserName(context)?.let { prefsJson.put("auth_user_name", it) }
            AuthManager.getProfileImageUri(context)?.let { prefsJson.put("auth_profile_image_uri", it) }
            val timelineJson = timelineToJsonString(TimelineLogger.load(context))

            val userName = AuthManager.getUserName(context) ?: ""
            val profileImg = AuthManager.getProfileImageUri(context) ?: ""

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("user_name", userName)
                put("profile_image_uri", profileImg)
                put("prefs_data", prefsJson.toString())
                put("timeline_data", timelineJson)
                put("updated_at", System.currentTimeMillis())
            }

            // Try Upsert POST
            var url = URL("$supabaseUrl/rest/v1/user_sync_data?on_conflict=user_id")
            var conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            var code = conn.responseCode
            Log.d("CloudSyncManager", "Cloud sync POST response code: $code")

            // Fallback to PATCH if 403 or 409
            if (code !in 200..299) {
                url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                code = conn.responseCode
                Log.d("CloudSyncManager", "Cloud sync PATCH fallback response code: $code")
            }

            val success = code in 200..299
            success
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to sync data to cloud", e)
            false
        }
    }

    suspend fun restoreDataFromCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val accessToken = AuthManager.getAccessToken(context)

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            return@withContext false
        }

        try {
            var url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId&select=*")
            var conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")

            var code = conn.responseCode
            Log.d("CloudSyncManager", "Restore GET response code: $code")

            var responseStr = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
            var jsonArray = if (responseStr.isNotEmpty()) org.json.JSONArray(responseStr) else org.json.JSONArray()

            if (jsonArray.length() == 0) {
                // Fallback to fetch latest record if user_id formatting varied
                url = URL("$supabaseUrl/rest/v1/user_sync_data?select=*&order=updated_at.desc&limit=1")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                code = conn.responseCode
                if (code in 200..299) {
                    responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    jsonArray = org.json.JSONArray(responseStr)
                }
            }

            if (jsonArray.length() > 0) {

                val record = jsonArray.getJSONObject(0)
                val prefsStr = record.optString("prefs_data")
                val timelineStr = record.optString("timeline_data")
                val recordUserName = record.optString("user_name")
                val recordProfileImg = record.optString("profile_image_uri")

                if (recordUserName.isNotEmpty()) {
                    AuthManager.updateUserName(context, recordUserName)
                }
                if (recordProfileImg.isNotEmpty()) {
                    AuthManager.saveProfileImageUri(context, recordProfileImg)
                }

                if (prefsStr.isNotEmpty()) {
                    val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    val prefsObj = JSONObject(prefsStr)
                    val keys = prefsObj.keys()
                    val intPrefKeys = setOf(
                        "customBg", "customPrimary", "customHue", "customSecondary", "customSecondaryHue",
                        "current_streak", "selected_days_filter", "reminder_hour", "reminder_minute"
                    )
                    while (keys.hasNext()) {
                        val k = keys.next()
                        when (k) {
                            "timerState" -> editor.putString(k, "IDLE")
                            "accumulatedStudy", "currentBreakSeconds", "lastTimestamp", "focus_remaining_secs", "pre_pause_state" -> {
                                editor.putLong(k, 0L)
                            }
                            else -> {
                                val v = prefsObj.get(k)
                                when (v) {
                                    is Boolean -> editor.putBoolean(k, v)
                                    is Number -> {
                                        if (k in intPrefKeys) {
                                            editor.putInt(k, v.toInt())
                                        } else {
                                            editor.putLong(k, v.toLong())
                                        }
                                    }
                                    is String -> editor.putString(k, v)
                                }
                            }
                        }
                    }
                    editor.putString("timerState", "IDLE")
                    editor.putLong("accumulatedStudy", 0L)
                    editor.putLong("currentBreakSeconds", 0L)
                    editor.putLong("lastTimestamp", 0L)
                    editor.putLong("focus_remaining_secs", 0L)
                    val cloudName = prefsObj.optString("auth_user_name")
                    if (cloudName.isNotEmpty()) {
                        AuthManager.updateUserName(context, cloudName)
                    }
                    val cloudImg = prefsObj.optString("auth_profile_image_uri")
                    if (cloudImg.isNotEmpty()) {
                        AuthManager.saveProfileImageUri(context, cloudImg)
                    }
                    editor.commit()
                }

                if (timelineStr.isNotEmpty()) {
                    TimelineLogger.importRaw(context, timelineStr)
                }

        BackupManager(context).runSilentAutoBackup()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to restore data from cloud", e)
        }
        false
    }

    suspend fun uploadProfileImageToStorage(context: Context, imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context) ?: return@withContext null

        try {
            val fileName = "$userId.jpg"
            val url = URL("$supabaseUrl/storage/v1/object/avatars/$fileName")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.setRequestProperty("x-upsert", "true")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(imageBytes)
            }

            val code = conn.responseCode
            Log.d("CloudSyncManager", "Supabase storage upload code: $code")
            if (code in 200..299) {
                return@withContext "$supabaseUrl/storage/v1/object/public/avatars/$fileName"
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to upload image to Supabase Storage", e)
        }
        null
    }
}
