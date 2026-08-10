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
            val timelineJson = timelineToJsonString(TimelineLogger.load(context))

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("prefs_data", prefsJson.toString())
                put("timeline_data", timelineJson)
                put("updated_at", System.currentTimeMillis())
            }

            // Upsert into Supabase 'user_sync_data' table
            val url = URL("$supabaseUrl/rest/v1/user_sync_data?on_conflict=user_id")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            Log.d("CloudSyncManager", "Cloud sync response code: $code")
            code in 200..299
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
            val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId&select=*")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(responseStr)
                if (jsonArray.length() == 0) return@withContext false

                val record = jsonArray.getJSONObject(0)
                val prefsStr = record.optString("prefs_data")
                val timelineStr = record.optString("timeline_data")

                if (prefsStr.isNotEmpty()) {
                    val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    val prefsObj = JSONObject(prefsStr)
                    val keys = prefsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = prefsObj.get(k)
                        when (v) {
                            is Boolean -> editor.putBoolean(k, v)
                            is Int -> editor.putInt(k, v)
                            is Long -> editor.putLong(k, v)
                            is Float -> editor.putFloat(k, v.toFloat())
                            is Double -> editor.putLong(k, v.toLong())
                            is String -> editor.putString(k, v)
                        }
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
}
