package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

enum class ModerationStatus {
    APPROVED,
    PENDING_APPROVAL,
    REJECTED,
    NONE
}

data class UserProfile(
    val displayName: String = "Student",
    val lastApprovedDisplayName: String? = null,
    val pendingDisplayName: String? = null,
    val bio: String = "",
    val targetExam: String = "Self-Study",
    val dailyGoalMinutes: Int = 120,
    val avatarUrl: String = "",
    val lastApprovedAvatar: String? = null,
    val avatarPresetId: String = "avatar_default",
    val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
    val rejectionReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

object ProfileManager {
    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val KEY_PROFILE_JSON = "__user_profile__"
    private const val TAG = "ProfileManager"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getProfile(context: Context): UserProfile {
        val prefs = getPrefs(context)
        val rawJson = prefs.getString(KEY_PROFILE_JSON, null)
        val authName = AuthManager.getUserName(context) ?: "Student"
        val customName = prefs.getString("custom_display_name", null)
        val defaultName = if (!customName.isNullOrBlank() && customName != "Student") customName else authName

        if (rawJson.isNullOrBlank()) {
            return UserProfile(
                displayName = defaultName,
                lastApprovedDisplayName = defaultName,
                avatarUrl = AuthManager.getProfileImageUri(context) ?: "",
                lastApprovedAvatar = AuthManager.getProfileImageUri(context) ?: ""
            )
        }

        return try {
            val json = JSONObject(rawJson)
            val statusStr = json.optString("moderationStatus", json.optString("displayNameStatus", "APPROVED"))
            val status = when (statusStr.uppercase()) {
                "PENDING", "PENDING_APPROVAL" -> ModerationStatus.PENDING_APPROVAL
                "REJECTED" -> ModerationStatus.REJECTED
                "NONE" -> ModerationStatus.NONE
                else -> ModerationStatus.APPROVED
            }

            val savedDisplayName = json.optString("displayName", defaultName)
            val savedLastApproved = if (json.has("lastApprovedDisplayName") && !json.isNull("lastApprovedDisplayName")) {
                json.getString("lastApprovedDisplayName")
            } else {
                savedDisplayName
            }

            val savedAvatar = json.optString("avatarUrl", AuthManager.getProfileImageUri(context) ?: "")
            val savedLastAvatar = if (json.has("lastApprovedAvatar") && !json.isNull("lastApprovedAvatar")) {
                json.getString("lastApprovedAvatar")
            } else {
                savedAvatar
            }

            UserProfile(
                displayName = savedDisplayName,
                lastApprovedDisplayName = savedLastApproved,
                pendingDisplayName = if (json.has("pendingDisplayName") && !json.isNull("pendingDisplayName")) json.getString("pendingDisplayName") else null,
                bio = json.optString("bio", ""),
                targetExam = json.optString("targetExam", "Self-Study"),
                dailyGoalMinutes = json.optInt("dailyGoalMinutes", 120),
                avatarUrl = savedAvatar,
                lastApprovedAvatar = savedLastAvatar,
                avatarPresetId = json.optString("avatarPresetId", "avatar_default"),
                moderationStatus = status,
                rejectionReason = if (json.has("rejectionReason") && !json.isNull("rejectionReason")) json.getString("rejectionReason") else null,
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UserProfile JSON", e)
            UserProfile(displayName = defaultName, lastApprovedDisplayName = defaultName)
        }
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val effectiveLastApproved = profile.lastApprovedDisplayName ?: profile.displayName
        val effectiveLastAvatar = profile.lastApprovedAvatar ?: profile.avatarUrl

        val json = JSONObject().apply {
            put("displayName", profile.displayName)
            put("lastApprovedDisplayName", effectiveLastApproved)
            put("pendingDisplayName", profile.pendingDisplayName ?: JSONObject.NULL)
            put("bio", profile.bio)
            put("targetExam", profile.targetExam)
            put("dailyGoalMinutes", profile.dailyGoalMinutes)
            put("avatarUrl", profile.avatarUrl)
            put("lastApprovedAvatar", effectiveLastAvatar)
            put("avatarPresetId", profile.avatarPresetId)
            put("moderationStatus", profile.moderationStatus.name)
            put("rejectionReason", profile.rejectionReason ?: JSONObject.NULL)
            put("updatedAt", profile.updatedAt)
        }

        getPrefs(context).edit().apply {
            putString(KEY_PROFILE_JSON, json.toString())
            putString("custom_display_name", profile.displayName)
            apply()
        }

        // Keep AuthManager aligned with active approved name
        if (profile.moderationStatus == ModerationStatus.APPROVED && profile.displayName.isNotBlank()) {
            AuthManager.updateUserName(context, profile.displayName)
        }

        BackupManager(context).markDataModified()
    }

    fun getEffectiveDisplayName(context: Context): String {
        val profile = getProfile(context)
        return if (profile.displayName.isNotBlank() && profile.displayName != "Student") {
            profile.displayName
        } else {
            AuthManager.getUserName(context) ?: "Student"
        }
    }

    fun getPublicAvatarUrl(context: Context): String {
        val profile = getProfile(context)
        val custom = profile.avatarUrl.trim()
        if (custom.isNotBlank() && (custom.startsWith("http://") || custom.startsWith("https://") || custom.startsWith("data:"))) {
            return custom
        }
        val authUri = AuthManager.getProfileImageUri(context)?.trim() ?: ""
        if (authUri.startsWith("http://") || authUri.startsWith("https://")) {
            return authUri
        }
        return ""
    }

    fun getEffectiveAvatarUrl(context: Context): String {
        val profile = getProfile(context)
        val custom = profile.avatarUrl.trim()
        if (custom.isNotBlank() && (custom.startsWith("http://") || custom.startsWith("https://") || custom.startsWith("data:"))) {
            return custom
        }
        val authUri = AuthManager.getProfileImageUri(context)?.trim() ?: ""
        if (authUri.startsWith("http://") || authUri.startsWith("https://")) {
            return authUri
        }
        if (LocalAvatarManager.hasCustomAvatar(context)) {
            return LocalAvatarManager.getAvatarFile(context).absolutePath
        }
        return ""
    }

    fun updateFromCloudRecord(context: Context, cloudRecord: JSONObject): Boolean {
        return try {
            val statusStr = cloudRecord.optString("profile_status", "").trim().lowercase()
            val current = getProfile(context)
            val cloudUserName = cloudRecord.optString("user_name", "").trim()
            val cloudImageUri = cloudRecord.optString("profile_image_uri", "").trim()
            val pendingJsonStr = cloudRecord.optString("pending_profile_json", "").trim()
            val pendingJson = if (pendingJsonStr.isNotBlank() && pendingJsonStr != "null") {
                try { JSONObject(pendingJsonStr) } catch (_: Exception) { null }
            } else null

            val pendingNameFromCloud = pendingJson?.optString("displayName", pendingJson.optString("display_name", ""))?.trim()
            val pendingBioFromCloud = pendingJson?.optString("bio", pendingJson.optString("mood", ""))?.trim()
            val pendingExamFromCloud = pendingJson?.optString("targetExam", pendingJson.optString("exam_target", ""))?.trim()
            val pendingAvatarFromCloud = pendingJson?.optString("avatarUrl", pendingJson.optString("avatar_url", ""))?.trim()

            val newStatus = when (statusStr) {
                "approved" -> ModerationStatus.APPROVED
                "rejected" -> ModerationStatus.REJECTED
                "pending" -> ModerationStatus.PENDING_APPROVAL
                else -> current.moderationStatus
            }

            val finalDisplayName = when (newStatus) {
                ModerationStatus.APPROVED -> {
                    when {
                        !pendingNameFromCloud.isNullOrBlank() && pendingNameFromCloud != "Student" -> pendingNameFromCloud
                        !current.pendingDisplayName.isNullOrBlank() -> current.pendingDisplayName
                        cloudUserName.isNotBlank() && cloudUserName != "Student" && cloudUserName != "null" -> cloudUserName
                        else -> current.displayName
                    }
                }
                ModerationStatus.REJECTED -> {
                    // Strictly revert to last approved display name
                    current.lastApprovedDisplayName ?: (if (cloudUserName.isNotBlank() && cloudUserName != "Student") cloudUserName else "Student")
                }
                ModerationStatus.PENDING_APPROVAL -> {
                    current.displayName.ifBlank { current.lastApprovedDisplayName ?: "Student" }
                }
                else -> current.displayName
            }

            val finalAvatar = when (newStatus) {
                ModerationStatus.APPROVED -> {
                    when {
                        cloudImageUri.isNotBlank() && cloudImageUri != "null" -> cloudImageUri
                        !pendingAvatarFromCloud.isNullOrBlank() && pendingAvatarFromCloud != "null" -> pendingAvatarFromCloud
                        else -> current.avatarUrl
                    }
                }
                ModerationStatus.REJECTED -> {
                    current.lastApprovedAvatar ?: current.avatarUrl
                }
                else -> {
                    if (cloudImageUri.isNotBlank() && cloudImageUri != "null") cloudImageUri else current.avatarUrl
                }
            }

            val updated = current.copy(
                displayName = finalDisplayName,
                lastApprovedDisplayName = if (newStatus == ModerationStatus.APPROVED) finalDisplayName else current.lastApprovedDisplayName,
                pendingDisplayName = if (newStatus == ModerationStatus.APPROVED || newStatus == ModerationStatus.REJECTED) null else (current.pendingDisplayName ?: pendingNameFromCloud),
                bio = if (newStatus == ModerationStatus.APPROVED && !pendingBioFromCloud.isNullOrBlank()) pendingBioFromCloud else (if (newStatus == ModerationStatus.REJECTED) "" else current.bio),
                targetExam = if (newStatus == ModerationStatus.APPROVED && !pendingExamFromCloud.isNullOrBlank()) pendingExamFromCloud else current.targetExam,
                avatarUrl = finalAvatar,
                lastApprovedAvatar = if (newStatus == ModerationStatus.APPROVED && finalAvatar.isNotBlank()) finalAvatar else current.lastApprovedAvatar,
                moderationStatus = newStatus,
                rejectionReason = if (newStatus == ModerationStatus.REJECTED) "Your recent profile edit was rejected by moderators. Reverted to previous approved profile." else null,
                updatedAt = System.currentTimeMillis()
            )

            saveProfile(context, updated)

            if (newStatus == ModerationStatus.APPROVED && finalDisplayName.isNotBlank() && finalDisplayName != "Student") {
                AuthManager.updateUserName(context, finalDisplayName)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile from cloud record", e)
            false
        }
    }

    fun updateFromCloudJson(context: Context, profileJsonObj: JSONObject): Boolean {
        return try {
            val statusStr = profileJsonObj.optString("moderationStatus", profileJsonObj.optString("profileStatus", profileJsonObj.optString("displayNameStatus", "APPROVED")))
            val status = when (statusStr.uppercase()) {
                "PENDING", "PENDING_APPROVAL" -> ModerationStatus.PENDING_APPROVAL
                "REJECTED" -> ModerationStatus.REJECTED
                "NONE" -> ModerationStatus.NONE
                else -> ModerationStatus.APPROVED
            }

            val current = getProfile(context)
            val incomingName = profileJsonObj.optString("displayName", profileJsonObj.optString("display_name", "")).trim()
            val cloudLastApproved = if (profileJsonObj.has("lastApprovedDisplayName") && !profileJsonObj.isNull("lastApprovedDisplayName")) {
                profileJsonObj.getString("lastApprovedDisplayName")
            } else {
                current.lastApprovedDisplayName
            }

            val finalName = when (status) {
                ModerationStatus.APPROVED -> {
                    if (incomingName.isNotBlank() && incomingName != "Student" && incomingName != "null") {
                        incomingName
                    } else {
                        current.pendingDisplayName ?: current.displayName
                    }
                }
                ModerationStatus.REJECTED -> {
                    // Strictly revert to last approved display name
                    cloudLastApproved ?: current.lastApprovedDisplayName ?: "Student"
                }
                ModerationStatus.PENDING_APPROVAL -> {
                    current.displayName.ifBlank { current.lastApprovedDisplayName ?: "Student" }
                }
                else -> {
                    if (incomingName.isNotBlank() && incomingName != "null") incomingName else current.displayName
                }
            }

            val incomingAvatar = profileJsonObj.optString("avatarUrl", profileJsonObj.optString("avatar_url", profileJsonObj.optString("avatarPreset", ""))).trim()

            val updated = current.copy(
                displayName = finalName,
                lastApprovedDisplayName = if (status == ModerationStatus.APPROVED) finalName else cloudLastApproved,
                pendingDisplayName = if (status == ModerationStatus.APPROVED || status == ModerationStatus.REJECTED) null else (if (profileJsonObj.has("pendingDisplayName") && !profileJsonObj.isNull("pendingDisplayName")) profileJsonObj.getString("pendingDisplayName") else current.pendingDisplayName),
                bio = if (status == ModerationStatus.REJECTED) "" else profileJsonObj.optString("bio", profileJsonObj.optString("mood", current.bio)).ifBlank { current.bio },
                targetExam = profileJsonObj.optString("targetExam", profileJsonObj.optString("exam_target", current.targetExam)).ifBlank { current.targetExam },
                dailyGoalMinutes = profileJsonObj.optInt("dailyGoalMinutes", current.dailyGoalMinutes).coerceAtLeast(15),
                avatarUrl = if (status == ModerationStatus.REJECTED) (current.lastApprovedAvatar ?: current.avatarUrl) else (if (incomingAvatar.isNotBlank()) incomingAvatar else current.avatarUrl),
                moderationStatus = status,
                rejectionReason = if (status == ModerationStatus.REJECTED) "Your recent profile edit was rejected by moderators. Reverted to previous approved profile." else null,
                updatedAt = profileJsonObj.optLong("updatedAt", System.currentTimeMillis())
            )

            saveProfile(context, updated)

            if (status == ModerationStatus.APPROVED && finalName.isNotBlank() && finalName != "Student") {
                AuthManager.updateUserName(context, finalName)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile from cloud JSON", e)
            false
        }
    }

    fun saveDraftForm(context: Context, name: String?, bio: String?, exam: String?) {
        getPrefs(context).edit().apply {
            if (name != null) putString("__draft_profile_name__", name)
            if (bio != null) putString("__draft_profile_bio__", bio)
            if (exam != null) putString("__draft_profile_exam__", exam)
            apply()
        }
    }

    fun getDraftName(context: Context): String? = getPrefs(context).getString("__draft_profile_name__", null)
    fun getDraftBio(context: Context): String? = getPrefs(context).getString("__draft_profile_bio__", null)
    fun getDraftExam(context: Context): String? = getPrefs(context).getString("__draft_profile_exam__", null)

    fun clearDraftForm(context: Context) {
        getPrefs(context).edit().apply {
            remove("__draft_profile_name__")
            remove("__draft_profile_bio__")
            remove("__draft_profile_exam__")
            apply()
        }
    }
}
