package com.madeby.JAI

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private fun getBackupFile(): File = File(context.filesDir, "study_timer_backup.dat")

    private fun putTimeline(json: JSONObject) {
        json.put("focus_timeline", timelineToJsonString(TimelineLogger.load(context)))
    }

    private fun restoreTimeline(importedJsonObject: JSONObject) {
        val raw = importedJsonObject.optString("focus_timeline", null)?.takeIf { it.isNotEmpty() }
        TimelineLogger.importRaw(context, raw)
    }

    fun runSilentAutoBackup() {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            putTimeline(json)
            getBackupFile().writeText(json.toString())
        } catch (_: Exception) {}
    }

    fun triggerAutoRestoreIfPresent() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (sharedPrefs.all.isNotEmpty()) return

        try {
            val file = getBackupFile()
            if (!file.exists()) return

            val jsonString = file.readText()
            val importedJsonObject = JSONObject(jsonString)
            val editor = sharedPrefs.edit()
            sanitizeAndBuildPreferences(importedJsonObject, editor)
            editor.apply()
            restoreTimeline(importedJsonObject)
        } catch (_: Exception) {}
    }

    data class BackupMetadata(
        val schemaVersion: Int,
        val backupCreatedAt: Long,
        val lastModifiedTimestamp: Long,
        val entryCount: Int
    )

    fun getLastModifiedTimestamp(): Long {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val prefTs = sharedPrefs.getLong("last_data_modified_timestamp", 0L)
        val timelineLastTs = TimelineLogger.load(context).maxOfOrNull { it.timestamp } ?: 0L
        return maxOf(prefTs, timelineLastTs, System.currentTimeMillis() - 86400000L)
    }

    fun markDataModified() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_data_modified_timestamp", System.currentTimeMillis()).apply()
    }

    fun inspectBackupMetadata(uri: Uri): BackupMetadata? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val jsonString = BufferedReader(InputStreamReader(stream)).readText()
                val json = JSONObject(jsonString)
                val schemaVer = json.optInt("schema_version", 1)
                val createdAt = json.optLong("backup_created_at", 0L)
                val lastMod = json.optLong("last_modified_timestamp", createdAt)
                val timelineRaw = json.optString("focus_timeline", "")
                val entryCount = if (timelineRaw.isNotEmpty()) parseTimelineJson(timelineRaw).size else 0
                BackupMetadata(
                    schemaVersion = schemaVer,
                    backupCreatedAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
                    lastModifiedTimestamp = if (lastMod > 0L) lastMod else createdAt,
                    entryCount = entryCount
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun exportDataToJSON(uri: Uri): Boolean {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            putTimeline(json)
            val now = System.currentTimeMillis()
            val lastMod = getLastModifiedTimestamp()
            json.put("schema_version", 2)
            json.put("backup_created_at", now)
            json.put("last_modified_timestamp", maxOf(lastMod, now))

            val outputStream = context.contentResolver.openOutputStream(uri, "w")
                ?: return false
            outputStream.use { stream ->
                stream.write(json.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            return true
        } catch (_: Exception) {}
        return false
    }

    fun importDataFromJSON(uri: Uri, allowCloudSync: Boolean = true): Boolean {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = BufferedReader(InputStreamReader(inputStream)).readText()
                val importedJsonObject = JSONObject(jsonString)
                val editor = sharedPrefs.edit()
                editor.clear()
                sanitizeAndBuildPreferences(importedJsonObject, editor)

                val importedLastMod = importedJsonObject.optLong("last_modified_timestamp",
                    importedJsonObject.optLong("backup_created_at", System.currentTimeMillis()))
                editor.putLong("last_data_modified_timestamp", importedLastMod)

                val committed = editor.commit() // Synchronous commit to ensure immediate UI update
                restoreTimeline(importedJsonObject)
                runSilentAutoBackup()

                if (allowCloudSync && AuthManager.isLoggedIn(context)) {
                    // Safe async sync check handled by caller or background worker
                    Thread {
                        kotlinx.coroutines.runBlocking {
                            CloudSyncManager.syncWithConflictCheck(context)
                        }
                    }.start()
                }

                return committed
            }
        } catch (_: Exception) {}
        return false
    }

    private val intPrefKeys = setOf(
        "customBg", "customPrimary", "customHue", "customSecondary", "customSecondaryHue",
        "current_streak", "selected_days_filter", "reminder_hour", "reminder_minute"
    )

    fun exportDataToCSV(uri: Uri): Boolean {
        try {
            val logs = TimelineLogger.load(context)
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            val sb = StringBuilder()
            sb.append("Timestamp,Date,Time,State,ID\n")

            for (item in logs) {
                val dateStr = sdfDate.format(Date(item.timestamp))
                val timeStr = sdfTime.format(Date(item.timestamp))
                val state = item.state
                val idStr = item.id ?: ""

                sb.append("${item.timestamp},\"$dateStr\",\"$timeStr\",\"$state\",\"$idStr\"\n")
            }

            val outputStream = context.contentResolver.openOutputStream(uri, "w")
                ?: return false
            outputStream.use { stream ->
                stream.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun sanitizeAndBuildPreferences(sourceJson: JSONObject, editor: android.content.SharedPreferences.Editor) {
        val keys = sourceJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (key) {
                "timerState" -> editor.putString(key, "IDLE")
                "focus_timeline" -> {
                    // Restored via file storage (TimelineLogger.importRaw), not prefs.
                }
                "accumulatedStudy", "currentBreakSeconds", "lastTimestamp", "focus_remaining_secs", "pre_pause_state", "streak_last_calculated" -> {
                    // Never resurrect an in-flight session from a backup; streak is recomputed on next stats open.
                }
                else -> when (val value = sourceJson.get(key)) {
                    is Number -> {
                        if (key in intPrefKeys) {
                            editor.putInt(key, value.toInt())
                        } else {
                            editor.putLong(key, value.toLong())
                        }
                    }
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                }
            }
        }
    }
}
