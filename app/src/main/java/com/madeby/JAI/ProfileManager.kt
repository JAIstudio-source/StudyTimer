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
    val pendingDisplayName: String? = null,
    val bio: String = "",
    val targetExam: String = "Self-Study",
    val dailyGoalMinutes: Int = 120,
    val avatarUrl: String = "",
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
                avatarUrl = AuthManager.getProfileImageUri(context) ?: ""
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

            UserProfile(
                displayName = json.optString("displayName", defaultName),
                pendingDisplayName = if (json.has("pendingDisplayName") && !json.isNull("pendingDisplayName")) json.getString("pendingDisplayName") else null,
                bio = json.optString("bio", ""),
                targetExam = json.optString("targetExam", "Self-Study"),
                dailyGoalMinutes = json.optInt("dailyGoalMinutes", 120),
                avatarUrl = json.optString("avatarUrl", AuthManager.getProfileImageUri(context) ?: ""),
                avatarPresetId = json.optString("avatarPresetId", "avatar_default"),
                moderationStatus = status,
                rejectionReason = if (json.has("rejectionReason") && !json.isNull("rejectionReason")) json.getString("rejectionReason") else null,
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UserProfile JSON", e)
            UserProfile(displayName = defaultName)
        }
    }

    fun saveProfile(context: Context, profile: UserProfile) {
        val json = JSONObject().apply {
            put("displayName", profile.displayName)
            put("pendingDisplayName", profile.pendingDisplayName ?: JSONObject.NULL)
            put("bio", profile.bio)
            put("targetExam", profile.targetExam)
            put("dailyGoalMinutes", profile.dailyGoalMinutes)
            put("avatarUrl", profile.avatarUrl)
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

    fun getEffectiveAvatarUrl(context: Context): String {
        val profile = getProfile(context)
        val custom = profile.avatarUrl.trim()
        if (custom.isNotBlank() && (custom.startsWith("http://") || custom.startsWith("https://") || custom.startsWith("data:"))) {
            return custom
        }
        val preset = profile.avatarPresetId.trim()
        if (preset.isNotBlank()) {
            val emoji = LocalAvatarManager.resolvePresetToEmoji(preset)
            if (emoji != preset || emoji.length <= 4) {
                return emoji
            }
        }
        if (custom.isNotBlank()) {
            val customEmoji = LocalAvatarManager.resolvePresetToEmoji(custom)
            if (customEmoji != custom || customEmoji.length <= 4) {
                return customEmoji
            }
        }
        val authUri = AuthManager.getProfileImageUri(context) ?: ""
        if (authUri.startsWith("http://") || authUri.startsWith("https://")) {
            return authUri
        }
        return "🐱"
    }

    fun updateFromCloudRecord(context: Context, cloudRecord: JSONObject): Boolean {
        return try {
            val statusStr = cloudRecord.optString("profile_status", "").trim().lowercase()
            val current = getProfile(context)
            val cloudUserName = cloudRecord.optString("user_name", "").trim()
            val cloudImageUri = cloudRecord.optString("profile_image_uri", "").trim()

            val newStatus = when (statusStr) {
                "approved" -> ModerationStatus.APPROVED
                "rejected" -> ModerationStatus.REJECTED
                "pending" -> ModerationStatus.PENDING_APPROVAL
                else -> current.moderationStatus
            }

            val approvedName = if (newStatus == ModerationStatus.APPROVED) {
                if (cloudUserName.isNotBlank() && cloudUserName != "Student" && cloudUserName != "null") {
                    cloudUserName
                } else {
                    current.pendingDisplayName ?: current.displayName
                }
            } else {
                current.displayName
            }

            val updated = current.copy(
                displayName = approvedName,
                pendingDisplayName = if (newStatus == ModerationStatus.APPROVED || newStatus == ModerationStatus.REJECTED) null else current.pendingDisplayName,
                avatarUrl = if (cloudImageUri.isNotBlank() && cloudImageUri != "null") cloudImageUri else current.avatarUrl,
                moderationStatus = newStatus,
                rejectionReason = if (newStatus == ModerationStatus.REJECTED) "Your recent profile edit was rejected by moderators. Please use a respectful display name." else null,
                updatedAt = System.currentTimeMillis()
            )

            saveProfile(context, updated)

            if (newStatus == ModerationStatus.APPROVED && approvedName.isNotBlank() && approvedName != "Student") {
                AuthManager.updateUserName(context, approvedName)
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
            val finalName = if (status == ModerationStatus.APPROVED && incomingName.isNotBlank() && incomingName != "Student" && incomingName != "null") {
                incomingName
            } else if (incomingName.isNotBlank() && incomingName != "null") {
                incomingName
            } else {
                current.displayName
            }

            val updated = current.copy(
                displayName = finalName,
                pendingDisplayName = if (status == ModerationStatus.APPROVED || status == ModerationStatus.REJECTED) null else (if (profileJsonObj.has("pendingDisplayName") && !profileJsonObj.isNull("pendingDisplayName")) profileJsonObj.getString("pendingDisplayName") else current.pendingDisplayName),
                bio = profileJsonObj.optString("bio", profileJsonObj.optString("mood", current.bio)),
                targetExam = profileJsonObj.optString("targetExam", profileJsonObj.optString("exam_target", current.targetExam)),
                dailyGoalMinutes = profileJsonObj.optInt("dailyGoalMinutes", current.dailyGoalMinutes),
                avatarUrl = profileJsonObj.optString("avatarUrl", profileJsonObj.optString("avatar_url", profileJsonObj.optString("avatarPreset", current.avatarUrl))),
                moderationStatus = status,
                rejectionReason = if (status == ModerationStatus.REJECTED) "Your recent profile edit was rejected by moderators. Please use a respectful display name." else null,
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
}
