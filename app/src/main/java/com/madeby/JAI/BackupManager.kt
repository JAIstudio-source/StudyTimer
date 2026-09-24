package com.madeby.JAI

import android.content.Context
import android.net.Uri
import org.json.JSONArray
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
        val raw = if (importedJsonObject.has("focus_timeline")) importedJsonObject.optString("focus_timeline").takeIf { it.isNotEmpty() } else null
        TimelineLogger.importRaw(context, raw)
    }

    private fun putSubjectTags(json: JSONObject) {
        try {
            val subjectPrefs = context.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
            val subjectJson = JSONObject()
            for ((key, value) in subjectPrefs.all) {
                if (value != null) {
                    when (value) {
                        is Boolean -> subjectJson.put(key, value)
                        is Int -> subjectJson.put(key, value)
                        is Long -> subjectJson.put(key, value)
                        is Float -> subjectJson.put(key, value.toDouble())
                        is Double -> subjectJson.put(key, value)
                        is Set<*> -> {
                            val arr = JSONArray()
                            for (item in value) {
                                if (item != null) arr.put(item.toString())
                            }
                            subjectJson.put(key, arr)
                        }
                        else -> subjectJson.put(key, value.toString())
                    }
                }
            }
            json.put("subject_tags_data", subjectJson)
            json.put("__subject_tags_data__", subjectJson.toString())
        } catch (_: Exception) {}
    }

    private fun restoreSubjectTags(importedJsonObject: JSONObject) {
        try {
            val subjectJsonObj = importedJsonObject.optJSONObject("subject_tags_data")
                ?: runCatching {
                    val raw = importedJsonObject.optString("__subject_tags_data__", "")
                    if (raw.isNotEmpty()) JSONObject(raw) else null
                }.getOrNull()
                ?: runCatching {
                    val raw = importedJsonObject.optString("subject_tags_data", "")
                    if (raw.isNotEmpty()) JSONObject(raw) else null
                }.getOrNull()

            if (subjectJsonObj != null) {
                val subPrefs = context.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE)
                val subEditor = subPrefs.edit()
                subEditor.clear()
                val subKeys = subjectJsonObj.keys()
                while (subKeys.hasNext()) {
                    val k = subKeys.next()
                    val v = subjectJsonObj.get(k)
                    when (v) {
                        is Boolean -> subEditor.putBoolean(k, v)
                        is JSONArray -> {
                            val set = HashSet<String>()
                            for (i in 0 until v.length()) {
                                set.add(v.getString(i))
                            }
                            subEditor.putStringSet(k, set)
                        }
                        is String -> {
                            if (k == "hidden_subjects_set") {
                                val set = HashSet<String>()
                                try {
                                    val trimmed = v.trim()
                                    if (trimmed.startsWith("[")) {
                                        val arr = JSONArray(trimmed)
                                        for (i in 0 until arr.length()) {
                                            set.add(arr.getString(i))
                                        }
                                    } else {
                                        val cleaned = trimmed.removeSurrounding("[", "]")
                                        cleaned.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }.forEach { set.add(it) }
                                    }
                                    subEditor.putStringSet(k, set)
                                } catch (_: Exception) {
                                    subEditor.putStringSet(k, emptySet())
                                }
                            } else {
                                subEditor.putString(k, v)
                            }
                        }
                        is Number -> subEditor.putLong(k, v.toLong())
                    }
                }
                subEditor.commit()
            }
        } catch (_: Exception) {}
    }

    private fun putExamCountdowns(json: JSONObject) {
        try {
            val examPrefs = context.getSharedPreferences("studytimer_exam_countdowns", Context.MODE_PRIVATE)
            val raw = examPrefs.getString("exams_list_json", "[]") ?: "[]"
            json.put("exam_countdowns_json", raw)
            json.put("__exam_countdowns_data__", raw)
        } catch (_: Exception) {}
    }

    private fun restoreExamCountdowns(importedJsonObject: JSONObject) {
        try {
            val raw = if (importedJsonObject.has("exam_countdowns_json")) {
                importedJsonObject.optString("exam_countdowns_json")
            } else if (importedJsonObject.has("__exam_countdowns_data__")) {
                importedJsonObject.optString("__exam_countdowns_data__")
            } else {
                ""
            }
            if (raw.isNotEmpty()) {
                val examPrefs = context.getSharedPreferences("studytimer_exam_countdowns", Context.MODE_PRIVATE)
                examPrefs.edit().putString("exams_list_json", raw).commit()
            }
        } catch (_: Exception) {}
    }

    private fun getSafetyBackupsDir(): File {
        val dir = File(context.filesDir, "safety_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getPreAuthSafetyFile(): File = File(context.filesDir, "pre_auth_safety_snapshot.dat")

    fun hasLocalStudyData(): Boolean {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val hasStudyKeys = sharedPrefs.all.keys.any { it.endsWith("_focus_total") && (sharedPrefs.getLong(it, 0L) > 0L || sharedPrefs.getInt(it, 0) > 0) }
            val accumulated = sharedPrefs.getLong("accumulatedStudy", 0L) > 0L
            val timelineEntries = TimelineLogger.load(context)
            return hasStudyKeys || accumulated || timelineEntries.isNotEmpty()
        } catch (_: Exception) {
            return false
        }
    }

    fun createPreAuthSafetySnapshot(tag: String = "pre_auth"): File? {
        return try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            putTimeline(json)
            putSubjectTags(json)
            putExamCountdowns(json)
            val now = System.currentTimeMillis()
            json.put("schema_version", 2)
            json.put("snapshot_tag", tag)
            json.put("backup_created_at", now)
            json.put("last_modified_timestamp", getLastModifiedTimestamp())

            val jsonStr = json.toString()

            // 1. Write current pre-auth safety snapshot
            val safetyFile = getPreAuthSafetyFile()
            safetyFile.writeText(jsonStr)

            // 2. Also save into rolling safety_backups directory
            val backupsDir = getSafetyBackupsDir()
            val timestampedFile = File(backupsDir, "safety_backup_${tag}_${now}.dat")
            timestampedFile.writeText(jsonStr)

            // Prune safety backups older than 30 days, while ALWAYS keeping the most recent files
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000L)
            val files = backupsDir.listFiles { f -> f.name.startsWith("safety_backup_") && f.name.endsWith(".dat") }
            if (files != null && files.isNotEmpty()) {
                files.sortBy { it.lastModified() }
                // Always keep at least the 3 newest snapshots regardless of age
                val filesToConsider = files.dropLast(3)
                for (f in filesToConsider) {
                    if (f.lastModified() < thirtyDaysAgo || files.size > 20) {
                        f.delete()
                    }
                }
            }
            safetyFile
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed to create pre-auth safety snapshot", e)
            null
        }
    }

    fun restorePreAuthSafetySnapshot(): Boolean {
        val safetyFile = getPreAuthSafetyFile()
        if (!safetyFile.exists()) return false
        return try {
            val jsonString = safetyFile.readText()
            val importedJsonObject = JSONObject(jsonString)
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.clear()
            sanitizeAndBuildPreferences(importedJsonObject, editor)
            editor.apply()
            restoreTimeline(importedJsonObject)
            restoreSubjectTags(importedJsonObject)
            restoreExamCountdowns(importedJsonObject)
            runSilentAutoBackup()
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed to restore pre-auth safety snapshot", e)
            false
        }
    }

    fun runSilentAutoBackup() {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            putTimeline(json)
            putSubjectTags(json)
            putExamCountdowns(json)
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
            restoreSubjectTags(importedJsonObject)
            restoreExamCountdowns(importedJsonObject)
        } catch (_: Exception) {}
    }

    data class BackupMetadata(
        val schemaVersion: Int,
        val backupCreatedAt: Long,
        val lastModifiedTimestamp: Long,
        val entryCount: Int,
        val subjectCount: Int = 0,
        val goalCount: Int = 0
    )

    fun getLastModifiedTimestamp(): Long {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val prefTs = sharedPrefs.getLong("last_data_modified_timestamp", 0L)
        val timelineLastTs = TimelineLogger.load(context).maxOfOrNull { it.timestamp } ?: 0L
        return maxOf(prefTs, timelineLastTs)
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

                val goalsRaw = json.optString("session_goals_json", "[]")
                val goalCount = runCatching { JSONArray(goalsRaw).length() }.getOrDefault(0)

                val customSubjectsRaw = runCatching {
                    val subObj = json.optJSONObject("subject_tags_data")
                    subObj?.optString("custom_subjects_json", "[]") ?: "[]"
                }.getOrDefault("[]")
                val subjectCount = runCatching { JSONArray(customSubjectsRaw).length() }.getOrDefault(0)

                BackupMetadata(
                    schemaVersion = schemaVer,
                    backupCreatedAt = if (createdAt > 0L) createdAt else System.currentTimeMillis(),
                    lastModifiedTimestamp = if (lastMod > 0L) lastMod else createdAt,
                    entryCount = entryCount,
                    subjectCount = subjectCount,
                    goalCount = goalCount
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
            putSubjectTags(json)
            putExamCountdowns(json)
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
                restoreSubjectTags(importedJsonObject)
                restoreExamCountdowns(importedJsonObject)
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
                "focus_timeline", "subject_tags_data", "__subject_tags_data__", "exam_countdowns_json", "__exam_countdowns_data__" -> {
                    // Restored via separate stores (TimelineLogger, SubjectTagManager, ExamCountdownManager), not StudyTimerPrefs.
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
                    is JSONArray -> editor.putString(key, value.toString())
                    is JSONObject -> editor.putString(key, value.toString())
                }
            }
        }
    }

    enum class AppInstallState {
        FIRST_INSTALL,
        APP_UPDATE,
        SAME_VERSION
    }

    /**
     * Verifies whether the app is on a fresh installation, an update from a previous version,
     * or a normal app start. Ensures data integrity and prevents duplicate backup entries.
     */
    fun verifyAppVersionAndMigrate(): AppInstallState {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
        } catch (_: Exception) {
            1L
        }

        val lastRunVersion = prefs.getLong("last_run_version_code", -1L)

        val installState = when {
            lastRunVersion == -1L -> AppInstallState.FIRST_INSTALL
            lastRunVersion < currentVersionCode -> AppInstallState.APP_UPDATE
            else -> AppInstallState.SAME_VERSION
        }

        if (installState == AppInstallState.APP_UPDATE) {
            // Perform non-destructive migration & reconciliation on update
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            TimelineLogger.reconcileSubjectDurationsFromTimeline(context, todayStr)
            StatsEngine(context).forceReconcileDayTotals(todayStr)
        }

        prefs.edit().putLong("last_run_version_code", currentVersionCode).apply()
        return installState
    }
}
