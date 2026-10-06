package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

object ProfileSyncService {
    private const val TAG = "ProfileSyncService"
    private val TELEGRAM_BOT_TOKEN get() = BuildConfig.TELEGRAM_BOT_TOKEN
    private val TELEGRAM_CHAT_ID get() = BuildConfig.TELEGRAM_CHAT_ID

    sealed class SubmissionResult {
        data class Success(val isAutoApproved: Boolean, val message: String) : SubmissionResult()
        data class RejectedLocally(val reason: String) : SubmissionResult()
        data class NetworkError(val message: String) : SubmissionResult()
    }

    private var lastSubmitTimestamp = 0L
    private const val SUBMIT_COOLDOWN_MS = 15_000L

    suspend fun submitProfile(
        context: Context,
        displayName: String,
        bio: String,
        targetExam: String,
        dailyGoalMinutes: Int,
        avatarPresetId: String,
        avatarUrl: String
    ): SubmissionResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastSubmitTimestamp
        if (elapsed < SUBMIT_COOLDOWN_MS) {
            val waitSecs = ((SUBMIT_COOLDOWN_MS - elapsed) / 1000L).coerceAtLeast(1L)
            return@withContext SubmissionResult.NetworkError("Please wait $waitSecs seconds before submitting another profile update.")
        }
        lastSubmitTimestamp = now

        // Step 1: Pre-sanitization profanity check
        val nameCheck = ProfanityFilter.checkName(displayName)
        if (!nameCheck.isClean) {
            return@withContext SubmissionResult.RejectedLocally(
                nameCheck.reason ?: "Display name contains prohibited or inappropriate words."
            )
        }

        val currentProfile = ProfileManager.getProfile(context)
        val isNameChanged = displayName.trim().isNotBlank() && displayName.trim() != currentProfile.displayName.trim()
        val isBioChanged = bio.trim() != currentProfile.bio.trim()
        val isPhotoPending = LocalAvatarManager.isAvatarPendingUpload(context)
        val isPhotoChanged = isPhotoPending && LocalAvatarManager.hasCustomAvatar(context)

        val rawUserId = AuthManager.getUserId(context)
        val userEmail = AuthManager.getUserEmail(context) ?: ""
        val userId = if (!rawUserId.isNullOrBlank()) {
            rawUserId
        } else {
            val androidId = try {
                android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { null }
            "guest_${androidId ?: System.currentTimeMillis().toString()}"
        }

        var uploadedPublicPhotoUrl: String? = null
        val avatarFile = if (isPhotoChanged) LocalAvatarManager.getAvatarFile(context) else null

        // Step 2: If photo was actually changed, upload compressed image to Supabase Storage
        if (isPhotoChanged && avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
            try {
                uploadedPublicPhotoUrl = uploadAvatarToSupabaseStorage(userId, avatarFile)
            } catch (e: Exception) {
                Log.w(TAG, "Supabase storage upload notice: ${e.message}")
            }
        }

        val fallbackPresetEmoji = LocalAvatarManager.resolvePresetToEmoji(avatarPresetId)
        val candidateAvatar = if (avatarUrl.isNotBlank() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
            avatarUrl
        } else {
            currentProfile.avatarUrl
        }
        val effectivePublicAvatar = uploadedPublicPhotoUrl ?: if (candidateAvatar.isNotBlank() && (candidateAvatar.startsWith("http://") || candidateAvatar.startsWith("https://"))) {
            candidateAvatar
        } else {
            fallbackPresetEmoji
        }

        val newStatus = if (isNameChanged || isPhotoChanged) ModerationStatus.PENDING_APPROVAL else currentProfile.moderationStatus
        val pendingName = if (isNameChanged) displayName.trim() else null

        val updatedProfile = currentProfile.copy(
            displayName = if (isNameChanged) currentProfile.displayName else displayName.trim(),
            pendingDisplayName = pendingName,
            bio = bio.trim().take(150),
            targetExam = targetExam.trim().take(40),
            dailyGoalMinutes = dailyGoalMinutes.coerceIn(15, 960),
            avatarPresetId = avatarPresetId,
            avatarUrl = effectivePublicAvatar,
            moderationStatus = newStatus,
            rejectionReason = if (isNameChanged) null else currentProfile.rejectionReason,
            updatedAt = System.currentTimeMillis()
        )

        // Save locally first
        ProfileManager.saveProfile(context, updatedProfile)

        // Clear photo pending flag since we processed it
        LocalAvatarManager.markAvatarPendingUpload(context, false)

        // Step 3: Push to Supabase Cloud user_sync_data (if logged in)
        val isLoggedIn = AuthManager.isLoggedIn(context) && !rawUserId.isNullOrBlank()
        if (isLoggedIn) {
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY

                if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                    val pendingJson = JSONObject().apply {
                        put("display_name", displayName.trim())
                        put("displayName", displayName.trim())
                        put("mood", bio.trim())
                        put("bio", bio.trim())
                        put("exam_target", targetExam.trim())
                        put("targetExam", targetExam.trim())
                        put("avatar_preset", avatarPresetId)
                        put("avatarPreset", avatarPresetId)
                        put("avatar_url", effectivePublicAvatar)
                        put("avatarUrl", effectivePublicAvatar)
                        put("photo_changed", isPhotoChanged)
                        put("submitted_at", System.currentTimeMillis())
                    }

                    val patchObj = JSONObject().apply {
                        if (isNameChanged || isPhotoChanged) {
                            put("profile_status", "pending")
                            put("pending_profile_json", pendingJson.toString())
                        }
                        put("profile_image_uri", effectivePublicAvatar)
                        put("updated_at", System.currentTimeMillis())
                    }

                    val encodedUserId = java.net.URLEncoder.encode(rawUserId, "UTF-8")
                    val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$encodedUserId")
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

                    // Trigger cloud sync to update prefs_data
                    CloudSyncManager.syncDataToCloud(context, force = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing profile to Supabase user_sync_data", e)
            }
        }

        // Step 4: Direct guaranteed Telegram Bot Dispatch (Only send changed info; photo only if changed)
        val requiresModeration = isNameChanged || isBioChanged || isPhotoChanged
        if (requiresModeration) {
            try {
                sendDirectTelegramApprovalCard(
                    userId = userId,
                    oldName = currentProfile.displayName,
                    newName = displayName.trim(),
                    oldBio = currentProfile.bio,
                    newBio = bio.trim(),
                    isNameChanged = isNameChanged,
                    isBioChanged = isBioChanged,
                    isPhotoChanged = isPhotoChanged,
                    userEmail = userEmail,
                    avatarFile = avatarFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Telegram direct dispatch failed", e)
            }
        }

        val successMsg = if (isNameChanged || isPhotoChanged) {
            "✓ Profile submitted for review! Changes will update shortly."
        } else {
            "✓ Profile updated successfully."
        }

        return@withContext SubmissionResult.Success(
            isAutoApproved = !isNameChanged && !isPhotoChanged,
            message = successMsg
        )
    }

    suspend fun refreshProfileStatus(context: Context): UserProfile = withContext(Dispatchers.IO) {
        val current = ProfileManager.getProfile(context)
        // Guard: If not pending moderation, skip network request entirely to save server load
        if (current.moderationStatus != ModerationStatus.PENDING_APPROVAL) {
            return@withContext current
        }

        val rawUserId = AuthManager.getUserId(context)
        if (rawUserId.isNullOrBlank()) return@withContext current

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return@withContext current

        try {
            val encodedUserId = java.net.URLEncoder.encode(rawUserId, "UTF-8")
            // Fetch status, username, image, and pending json to correctly reconcile upon moderator approval
            var url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$encodedUserId&select=profile_status,user_name,profile_image_uri,pending_profile_json,updated_at")
            var conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            var responseStr = if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
            var arr = if (responseStr.isNotEmpty()) JSONArray(responseStr) else JSONArray()

            // Fallback: If not found by user_id, search by user_email
            val userEmail = AuthManager.getUserEmail(context)
            if (arr.length() == 0 && !userEmail.isNullOrBlank() && userEmail != rawUserId) {
                val encodedEmail = java.net.URLEncoder.encode(userEmail, "UTF-8")
                val fallbackUrl = URL("$supabaseUrl/rest/v1/user_sync_data?or=(user_id.eq.$encodedEmail,user_email.eq.$encodedEmail)&select=profile_status,user_name,profile_image_uri,pending_profile_json,updated_at")
                val fallbackConn = fallbackUrl.openConnection() as HttpURLConnection
                fallbackConn.requestMethod = "GET"
                fallbackConn.setRequestProperty("apikey", anonKey)
                fallbackConn.setRequestProperty("Authorization", "Bearer $anonKey")
                fallbackConn.connectTimeout = 6000
                fallbackConn.readTimeout = 6000
                if (fallbackConn.responseCode in 200..299) {
                    val fallbackStr = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                    if (fallbackStr.isNotEmpty()) {
                        arr = JSONArray(fallbackStr)
                    }
                }
            }

            if (arr.length() > 0) {
                val record = arr.getJSONObject(0)
                val changed = ProfileManager.updateFromCloudRecord(context, record)
                Log.d(TAG, "Profile status successfully refreshed from Supabase: status=${record.optString("profile_status")}, changed=$changed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh profile status from cloud: ${e.message}")
        }

        return@withContext ProfileManager.getProfile(context)
    }

    internal fun uploadAvatarToSupabaseStorage(userId: String, file: File): String? {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return null

        val fileName = "user_${userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}_${System.currentTimeMillis()}.jpg"
        val uploadUrl = URL("$supabaseUrl/storage/v1/object/avatars/$fileName")
        val conn = uploadUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("apikey", anonKey)
        conn.setRequestProperty("Authorization", "Bearer $anonKey")
        conn.setRequestProperty("Content-Type", "image/jpeg")
        conn.setRequestProperty("x-upsert", "true")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.doOutput = true

        FileInputStream(file).use { fis ->
            conn.outputStream.use { os ->
                fis.copyTo(os)
            }
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val publicUrl = "$supabaseUrl/storage/v1/object/public/avatars/$fileName"
            Log.i(TAG, "Successfully uploaded avatar to Supabase Storage: $publicUrl")
            return publicUrl
        } else {
            Log.w(TAG, "Supabase storage upload failed with HTTP $code")
            return null
        }
    }

    private fun sendDirectTelegramApprovalCard(
        userId: String,
        oldName: String,
        newName: String,
        oldBio: String,
        newBio: String,
        isNameChanged: Boolean,
        isBioChanged: Boolean,
        isPhotoChanged: Boolean,
        userEmail: String,
        avatarFile: File?
    ) {
        try {
            val header = "🛡️ <b>[PROFILE APPROVAL REQUEST]</b>\n\n"
            val nameSection = if (isNameChanged) {
                "👤 <b>Display Name:</b>\n<code>${escapeHtml(oldName)}</code> ➔ <b><code>${escapeHtml(newName)}</code></b>\n\n"
            } else ""

            val bioSection = if (isBioChanged && (newBio.isNotBlank() || oldBio.isNotBlank())) {
                "💬 <b>Bio / Motto:</b>\n<i>\"${escapeHtml(oldBio.ifBlank { "None" })}\"</i> ➔ <b><i>\"${escapeHtml(newBio.ifBlank { "None" })}\"</i></b>\n\n"
            } else ""

            val photoSection = if (isPhotoChanged) {
                "📸 <b>Profile Photo:</b> ⚠️ <code>New Photo Uploaded</code>\n\n"
            } else ""

            val footer = "──────────────────\n" +
                    "🆔 <b>User ID:</b> <code>${escapeHtml(userId)}</code>\n" +
                    "📱 <b>Source:</b> Android App" + (if (userEmail.isNotBlank()) " • 📧 <code>${escapeHtml(userEmail)}</code>" else "")

            val fullCaption = (header + nameSection + bioSection + photoSection + footer).take(1024)

            val keyboard = JSONObject().apply {
                val row = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "✅ Approve")
                        put("callback_data", "approve:$userId")
                    })
                    put(JSONObject().apply {
                        put("text", "❌ Reject")
                        put("callback_data", "reject:$userId")
                    })
                }
                put("inline_keyboard", JSONArray().apply { put(row) })
            }

            // ONLY send photo if the photo was actually changed/newly picked
            if (isPhotoChanged && avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
                val boundary = "==Boundary_${System.currentTimeMillis()}=="
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val url = URL("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendPhoto")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.doOutput = true

                val outputStream: OutputStream = conn.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                // chat_id param
                writer.append(twoHyphens).append(boundary).append(lineEnd)
                writer.append("Content-Disposition: form-data; name=\"chat_id\"").append(lineEnd).append(lineEnd)
                writer.append(TELEGRAM_CHAT_ID).append(lineEnd)

                // caption param
                writer.append(twoHyphens).append(boundary).append(lineEnd)
                writer.append("Content-Disposition: form-data; name=\"caption\"").append(lineEnd).append(lineEnd)
                writer.append(fullCaption).append(lineEnd)

                // parse_mode param
                writer.append(twoHyphens).append(boundary).append(lineEnd)
                writer.append("Content-Disposition: form-data; name=\"parse_mode\"").append(lineEnd).append(lineEnd)
                writer.append("HTML").append(lineEnd)

                // reply_markup param
                writer.append(twoHyphens).append(boundary).append(lineEnd)
                writer.append("Content-Disposition: form-data; name=\"reply_markup\"").append(lineEnd).append(lineEnd)
                writer.append(keyboard.toString()).append(lineEnd)

                // photo file param
                writer.append(twoHyphens).append(boundary).append(lineEnd)
                writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"avatar.jpg\"").append(lineEnd)
                writer.append("Content-Type: image/jpeg").append(lineEnd).append(lineEnd)
                writer.flush()

                FileInputStream(avatarFile).use { fis ->
                    fis.copyTo(outputStream)
                }
                outputStream.flush()

                writer.append(lineEnd)
                writer.append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd)
                writer.flush()
                writer.close()

                val code = conn.responseCode
                Log.i(TAG, "Telegram sendPhoto multipart status: $code")
                if (code in 200..299) return
            }

            // If photo was not changed, send clean text message with only changed fields
            val msgUrl = URL("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage")
            val conn = msgUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("chat_id", TELEGRAM_CHAT_ID)
                put("text", fullCaption)
                put("parse_mode", "HTML")
                put("reply_markup", keyboard)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            Log.i(TAG, "Direct Telegram sendMessage response status: $code")
        } catch (e: Exception) {
            Log.e(TAG, "Direct Telegram dispatch error", e)
        }
    }

    private fun escapeHtml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
