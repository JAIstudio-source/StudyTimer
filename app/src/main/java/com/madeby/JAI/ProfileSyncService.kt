package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ProfileSyncService {
    private const val TAG = "ProfileSyncService"
    private const val APPROVAL_WORKER_URL = "https://studytimer-approval-bot.jaistudio.workers.dev/notify-new-profile"

    sealed class SubmissionResult {
        data class Success(val isAutoApproved: Boolean, val message: String) : SubmissionResult()
        data class RejectedLocally(val reason: String) : SubmissionResult()
        data class NetworkError(val message: String) : SubmissionResult()
    }

    suspend fun submitProfile(
        context: Context,
        displayName: String,
        bio: String,
        targetExam: String,
        dailyGoalMinutes: Int,
        avatarPresetId: String,
        avatarUrl: String
    ): SubmissionResult = withContext(Dispatchers.IO) {
        // Step 1: Pre-sanitization profanity check
        val nameCheck = ProfanityFilter.checkName(displayName)
        if (!nameCheck.isClean) {
            return@withContext SubmissionResult.RejectedLocally(
                nameCheck.reason ?: "Display name contains prohibited or inappropriate words."
            )
        }

        val currentProfile = ProfileManager.getProfile(context)
        val isNameChanged = displayName.trim() != currentProfile.displayName.trim()
        val isNewPending = isNameChanged && displayName.trim() != currentProfile.pendingDisplayName

        val newStatus = if (isNameChanged) ModerationStatus.PENDING_APPROVAL else currentProfile.moderationStatus
        val pendingName = if (isNameChanged) displayName.trim() else null

        val updatedProfile = currentProfile.copy(
            displayName = if (isNameChanged) currentProfile.displayName else displayName.trim(),
            pendingDisplayName = pendingName,
            bio = bio.trim().take(150),
            targetExam = targetExam.trim().take(40),
            dailyGoalMinutes = dailyGoalMinutes.coerceIn(15, 960),
            avatarPresetId = avatarPresetId,
            avatarUrl = avatarUrl,
            moderationStatus = newStatus,
            rejectionReason = if (isNameChanged) null else currentProfile.rejectionReason,
            updatedAt = System.currentTimeMillis()
        )

        // Save locally first
        ProfileManager.saveProfile(context, updatedProfile)

        // If not logged in, local save is complete
        val userId = AuthManager.getUserId(context)
        if (userId.isNullOrBlank() || !AuthManager.isLoggedIn(context)) {
            return@withContext SubmissionResult.Success(
                isAutoApproved = !isNameChanged,
                message = "Profile saved locally."
            )
        }

        // Step 2: Push to Supabase Cloud
        try {
            val supabaseUrl = BuildConfig.SUPABASE_URL
            val anonKey = BuildConfig.SUPABASE_ANON_KEY

            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                val pendingJson = JSONObject().apply {
                    put("display_name", displayName.trim())
                    put("mood", bio.trim())
                    put("exam_target", targetExam.trim())
                    put("avatar_preset", avatarPresetId)
                    put("avatarPreset", avatarPresetId)
                    put("submitted_at", System.currentTimeMillis())
                }

                val patchObj = JSONObject().apply {
                    if (isNameChanged) {
                        put("profile_status", "pending")
                        put("pending_profile_json", pendingJson.toString())
                    }
                    put("profile_image_uri", avatarUrl)
                    put("updated_at", System.currentTimeMillis())
                }

                // Patch Supabase user_sync_data
                val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$userId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Authorization", "Bearer $anonKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(patchObj.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                Log.d(TAG, "Profile update PATCH response code: $code")

                // Step 3: Trigger sync to embed full prefs_data
                CloudSyncManager.syncDataToCloud(context, force = true)

                // Step 4: Optional notify to approval worker
                if (isNameChanged) {
                    try {
                        val notifyUrl = URL(APPROVAL_WORKER_URL)
                        val notifyConn = notifyUrl.openConnection() as HttpURLConnection
                        notifyConn.requestMethod = "POST"
                        notifyConn.setRequestProperty("Content-Type", "application/json")
                        notifyConn.connectTimeout = 4000
                        notifyConn.readTimeout = 4000
                        notifyConn.doOutput = true
                        val notifyPayload = JSONObject().apply {
                            put("user_id", userId)
                            put("display_name", displayName.trim())
                            put("previous_name", currentProfile.displayName)
                            put("target_exam", targetExam.trim())
                            put("source", "android_app")
                        }
                        notifyConn.outputStream.use { it.write(notifyPayload.toString().toByteArray(Charsets.UTF_8)) }
                        notifyConn.responseCode // Fire and forget
                    } catch (_: Exception) {}
                }
            }

            SubmissionResult.Success(
                isAutoApproved = !isNameChanged,
                message = if (isNameChanged) "Display name submitted to moderators for approval." else "Profile updated successfully."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting profile to cloud", e)
            SubmissionResult.NetworkError("Failed to sync profile with cloud: ${e.localizedMessage}")
        }
    }
}
