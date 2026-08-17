package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncManager {

    data class CloudRecordMetadata(
        val userId: String,
        val updatedAt: Long,
        val lastModifiedTimestamp: Long,
        val schemaVersion: Int,
        val userName: String,
        val profileImageUri: String
    )

    sealed class SyncCheckResult {
        object UpToDate : SyncCheckResult()
        object Success : SyncCheckResult()
        data class Conflict(val localTimestamp: Long, val cloudTimestamp: Long, val cloudRecord: JSONObject) : SyncCheckResult()
        object NotLoggedIn : SyncCheckResult()
        data class Error(val message: String) : SyncCheckResult()
    }

    suspend fun fetchRemoteMetadata(context: Context): Pair<CloudRecordMetadata?, JSONObject?> = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext Pair(null, null)
        }

        try {
            val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId&select=*")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = if (responseStr.isNotEmpty()) org.json.JSONArray(responseStr) else org.json.JSONArray()
                if (jsonArray.length() > 0) {
                    val record = jsonArray.getJSONObject(0)
                    val updatedAt = record.optLong("updated_at", 0L)
                    val schemaVer = record.optInt("schema_version", 1)
                    val lastMod = record.optLong("last_modified_timestamp", updatedAt)
                    val uName = record.optString("user_name", "")
                    val pImg = record.optString("profile_image_uri", "")
                    val meta = CloudRecordMetadata(
                        userId = userId,
                        updatedAt = updatedAt,
                        lastModifiedTimestamp = if (lastMod > 0L) lastMod else updatedAt,
                        schemaVersion = schemaVer,
                        userName = uName,
                        profileImageUri = pImg
                    )
                    return@withContext Pair(meta, record)
                }
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "fetchRemoteMetadata error", e)
        }
        Pair(null, null)
    }

    suspend fun syncWithConflictCheck(context: Context): SyncCheckResult = withContext(Dispatchers.IO) {
        val userId = AuthManager.getUserId(context)
        if (userId.isNullOrBlank()) return@withContext SyncCheckResult.NotLoggedIn

        val localTs = BackupManager(context).getLastModifiedTimestamp()
        val (remoteMeta, rawRecord) = fetchRemoteMetadata(context)

        if (remoteMeta != null && rawRecord != null) {
            val cloudTs = maxOf(remoteMeta.lastModifiedTimestamp, remoteMeta.updatedAt)
            // If cloud is newer by more than 2 seconds, raise a conflict
            if (cloudTs > localTs + 2000L) {
                Log.w("CloudSyncManager", "Cloud timestamp ($cloudTs) is newer than local ($localTs). Conflict detected.")
                return@withContext SyncCheckResult.Conflict(localTs, cloudTs, rawRecord)
            }
        }

        val success = syncDataToCloud(context, force = true)
        if (success) SyncCheckResult.Success else SyncCheckResult.Error("Sync failed")
    }

    data class SyncResult(
        val isSuccess: Boolean,
        val errorMessage: String? = null,
        val isUnauthenticated: Boolean = false
    )

    suspend fun syncDataToCloudDetailed(context: Context, force: Boolean = true): SyncResult = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)
        val isLoggedIn = AuthManager.isLoggedIn(context)

        if (!isLoggedIn || userId.isNullOrBlank()) {
            Log.w("CloudSyncManager", "Cloud sync skipped: User is not signed in to a cloud account.")
            return@withContext SyncResult(isSuccess = false, errorMessage = "Please sign in with your Google account first.", isUnauthenticated = true)
        }

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            Log.e("CloudSyncManager", "Cloud sync failed: Missing Supabase URL or Anon Key in configuration.")
            return@withContext SyncResult(isSuccess = false, errorMessage = "Cloud configuration credentials missing.", isUnauthenticated = false)
        }

        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val prefsJson = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                if (value != null) {
                    when (value) {
                        is Boolean -> prefsJson.put(key, value)
                        is Int -> prefsJson.put(key, value)
                        is Long -> prefsJson.put(key, value)
                        is Float -> prefsJson.put(key, value.toDouble())
                        is Double -> prefsJson.put(key, value)
                        else -> prefsJson.put(key, value.toString())
                    }
                }
            }
            AuthManager.getUserName(context)?.let { prefsJson.put("auth_user_name", it) }
            AuthManager.getProfileImageUri(context)?.let { prefsJson.put("auth_profile_image_uri", it) }

            val entries = TimelineLogger.load(context)
            val timelineArr = org.json.JSONArray()
            for (e in entries) {
                val obj = JSONObject()
                obj.put("t", e.timestamp)
                obj.put("s", e.state)
                e.id?.let { obj.put("id", it) }
                e.subId?.let { obj.put("subId", it) }
                e.subName?.let { obj.put("subName", it) }
                e.subColor?.let { obj.put("subColor", it) }
                timelineArr.put(obj)
            }
            val timelineJsonStr = timelineArr.toString()

            val subjectPrefs = context.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
            val subjectPrefsJson = JSONObject()
            for ((key, value) in subjectPrefs.all) {
                if (value != null) {
                    when (value) {
                        is Boolean -> subjectPrefsJson.put(key, value)
                        is Int -> subjectPrefsJson.put(key, value)
                        is Long -> subjectPrefsJson.put(key, value)
                        is Float -> subjectPrefsJson.put(key, value.toDouble())
                        is Double -> subjectPrefsJson.put(key, value)
                        else -> subjectPrefsJson.put(key, value.toString())
                    }
                }
            }

            // Embed subject tags directly into prefs_data to ensure complete backup under schema
            prefsJson.put("__subject_tags_data__", subjectPrefsJson.toString())

            val userName: String = AuthManager.getUserName(context) ?: ""
            val userEmail: String = AuthManager.getUserEmail(context) ?: ""
            val profileImg: String = AuthManager.getProfileImageUri(context) ?: ""
            val now = System.currentTimeMillis()
            val localLastMod = BackupManager(context).getLastModifiedTimestamp()

            // Exact schema columns: user_id, user_name, user_email, profile_image_uri, prefs_data, timeline_data, updated_at
            val payload = JSONObject()
            payload.put("user_id", userId as String)
            payload.put("user_name", userName)
            payload.put("user_email", userEmail)
            payload.put("profile_image_uri", profileImg)
            payload.put("prefs_data", prefsJson.toString())
            payload.put("timeline_data", timelineJsonStr)
            payload.put("updated_at", maxOf(localLastMod, now))

            val payloadString = payload.toString()
            Log.d("CloudSyncManager", "Outgoing Cloud Sync Payload (${payloadString.length} bytes): timeline_entries=${entries.size}, updated_at=${maxOf(localLastMod, now)}")

            // Try Upsert POST
            var url = URL("$supabaseUrl/rest/v1/user_sync_data?on_conflict=user_id")
            var conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(payloadString.toByteArray(Charsets.UTF_8))
            }

            var code = conn.responseCode
            val responseBody = try {
                if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
                else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) { "" }
            Log.d("CloudSyncManager", "Cloud sync POST response code: $code, response: $responseBody")

            // Fallback to PATCH if 403 or 409
            if (code !in 200..299) {
                url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true
                conn.outputStream.use { os ->
                    os.write(payloadString.toByteArray(Charsets.UTF_8))
                }
                code = conn.responseCode
                val patchResponseBody = try {
                    if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
                    else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                Log.d("CloudSyncManager", "Cloud sync PATCH fallback response code: $code, response: $patchResponseBody")
            }

            val success = code in 200..299
            if (success) {
                BackupManager(context).markDataModified()
                Log.i("CloudSyncManager", "Cloud sync successfully completed for user: $userId")
                SyncResult(isSuccess = true)
            } else {
                Log.w("CloudSyncManager", "Cloud sync failed with HTTP $code. Response: $responseBody")
                SyncResult(isSuccess = false, errorMessage = "Cloud sync failed (Server HTTP $code)")
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to sync data to cloud with exception", e)
            SyncResult(isSuccess = false, errorMessage = "Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun syncDataToCloud(context: Context, force: Boolean = true): Boolean {
        return syncDataToCloudDetailed(context, force).isSuccess
    }

    suspend fun mergeCloudAndLocalData(context: Context, cloudRecord: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Merge Timeline Entries
            val cloudTimelineStr = cloudRecord.optString("timeline_data")
            val localTimeline = TimelineLogger.load(context).toMutableList()
            if (cloudTimelineStr.isNotEmpty()) {
                val cloudEntries = parseTimelineJson(cloudTimelineStr)
                val existingTimestamps = localTimeline.map { it.timestamp }.toSet()
                for (ce in cloudEntries) {
                    if (ce.timestamp !in existingTimestamps) {
                        localTimeline.add(ce)
                    }
                }
                localTimeline.sortBy { it.timestamp }
                TimelineLogger.importRaw(context, timelineToJsonString(localTimeline))
            }

            // 2. Merge Preferences (Keep max of totals, merge keys)
            val cloudPrefsStr = cloudRecord.optString("prefs_data")
            if (cloudPrefsStr.isNotEmpty()) {
                val cloudPrefs = JSONObject(cloudPrefsStr)
                val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                val editor = sharedPrefs.edit()
                val keys = cloudPrefs.keys()
                val intPrefKeys = setOf(
                    "customBg", "customPrimary", "customHue", "customSecondary", "customSecondaryHue",
                    "current_streak", "selected_days_filter", "reminder_hour", "reminder_minute"
                )
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.endsWith("_focus_total") || k.endsWith("_break_total")) {
                        val localVal = sharedPrefs.getLong(k, 0L)
                        val cloudVal = cloudPrefs.optLong(k, 0L)
                        editor.putLong(k, maxOf(localVal, cloudVal))
                    } else if (!sharedPrefs.contains(k)) {
                        val v = cloudPrefs.get(k)
                        when (v) {
                            is Boolean -> editor.putBoolean(k, v)
                            is Number -> if (k in intPrefKeys) editor.putInt(k, v.toInt()) else editor.putLong(k, v.toLong())
                            is String -> editor.putString(k, v)
                        }
                    }
                }
                editor.apply()
            }

            // 3. Mark modified and push merged result to cloud
            BackupManager(context).markDataModified()
            syncDataToCloud(context, force = true)
            BackupManager(context).runSilentAutoBackup()
            true
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Merge failed", e)
            false
        }
    }

    suspend fun restoreDataFromCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
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

                val subjectTagsStr = if (record.has("subject_tags_data") && record.optString("subject_tags_data").isNotEmpty()) {
                    record.optString("subject_tags_data")
                } else if (prefsStr.isNotEmpty()) {
                    try {
                        val pObj = JSONObject(prefsStr)
                        pObj.optString("__subject_tags_data__", "")
                    } catch (_: Exception) { "" }
                } else ""

                if (subjectTagsStr.isNotEmpty()) {
                    val subPrefs = context.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                    val subEditor = subPrefs.edit()
                    val subObj = JSONObject(subjectTagsStr)
                    val subKeys = subObj.keys()
                    while (subKeys.hasNext()) {
                        val k = subKeys.next()
                        val v = subObj.get(k)
                        when (v) {
                            is Boolean -> subEditor.putBoolean(k, v)
                            is String -> subEditor.putString(k, v)
                            is Number -> subEditor.putLong(k, v.toLong())
                        }
                    }
                    subEditor.commit()
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
            val bucketName = "profile-pictures"
            val url = URL("$supabaseUrl/storage/v1/object/$bucketName/$fileName")
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
                return@withContext "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
            }
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to upload image to Supabase Storage", e)
        }
        null
    }

    suspend fun deleteUserCloudData(context: Context): Boolean = withContext(Dispatchers.IO) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = AuthManager.getUserId(context)

        if (supabaseUrl.isBlank() || anonKey.isBlank() || userId.isNullOrBlank()) {
            return@withContext true
        }

        var deletedSync = false
        try {
            val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            deletedSync = conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to delete user_sync_data", e)
        }

        try {
            val fileName = "$userId.jpg"
            val bucketName = "profile-pictures"
            val url = URL("$supabaseUrl/storage/v1/object/$bucketName/$fileName")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.responseCode
        } catch (e: Exception) {
            Log.e("CloudSyncManager", "Failed to delete profile picture storage object", e)
        }

        deletedSync
    }
}
