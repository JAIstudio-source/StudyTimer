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
    private const val TELEGRAM_BOT_TOKEN = "8755792560:AAFrTNyOjveVTV9vtRgwVD6tkNMwfRBDG2k"
    private const val TELEGRAM_CHAT_ID = "6326462250"

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
        val isBioChanged = bio.trim() != currentProfile.bio.trim()
        val isAvatarChanged = avatarPresetId.trim() != currentProfile.avatarPresetId.trim() || avatarUrl.trim() != currentProfile.avatarUrl.trim()

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

        val isCustomPhoto = LocalAvatarManager.hasCustomAvatar(context) || avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")

        var uploadedPublicPhotoUrl: String? = null
        val avatarFile = if (LocalAvatarManager.hasCustomAvatar(context)) LocalAvatarManager.getAvatarFile(context) else null

        // Step 2: If a custom local photo exists, upload compressed image to Supabase Storage
        if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
            try {
                uploadedPublicPhotoUrl = uploadAvatarToSupabaseStorage(context, userId, avatarFile)
            } catch (e: Exception) {
                Log.w(TAG, "Supabase storage upload notice: ${e.message}")
            }
        }

        val effectivePublicAvatar = uploadedPublicPhotoUrl ?: avatarUrl.ifBlank { avatarPresetId }

        val newStatus = if (isNameChanged || isCustomPhoto) ModerationStatus.PENDING_APPROVAL else currentProfile.moderationStatus
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

        // Step 3: Push to Supabase Cloud user_sync_data (if logged in)
        val isLoggedIn = AuthManager.isLoggedIn(context) && !rawUserId.isNullOrBlank()
        if (isLoggedIn) {
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val anonKey = BuildConfig.SUPABASE_ANON_KEY

                if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                    val pendingJson = JSONObject().apply {
                        put("display_name", displayName.trim())
                        put("mood", bio.trim())
                        put("bio", bio.trim())
                        put("exam_target", targetExam.trim())
                        put("avatar_preset", avatarPresetId)
                        put("avatarPreset", avatarPresetId)
                        put("avatar_url", effectivePublicAvatar)
                        put("submitted_at", System.currentTimeMillis())
                    }

                    val patchObj = JSONObject().apply {
                        if (isNameChanged || isCustomPhoto) {
                            put("profile_status", "pending")
                            put("pending_profile_json", pendingJson.toString())
                        }
                        put("profile_image_uri", effectivePublicAvatar)
                        put("updated_at", System.currentTimeMillis())
                    }

                    val url = URL("$supabaseUrl/rest/v1/user_sync_data?user_id=eq.$rawUserId")
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

        // Step 4: Direct guaranteed Telegram Bot Dispatch (Instant Request with no manual commands)
        val requiresModeration = isNameChanged || isBioChanged || isAvatarChanged || isCustomPhoto
        if (requiresModeration) {
            try {
                sendDirectTelegramApprovalCard(
                    userId = userId,
                    oldName = currentProfile.displayName,
                    newName = displayName.trim(),
                    oldBio = currentProfile.bio,
                    newBio = bio.trim(),
                    oldAvatar = ProfileManager.getEffectiveAvatarUrl(context),
                    newAvatar = effectivePublicAvatar,
                    userEmail = userEmail,
                    avatarFile = avatarFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Telegram direct dispatch failed", e)
            }
        }

        val successMsg = if (isNameChanged || isCustomPhoto) {
            "✓ Profile submitted for verification! Approval request sent to Telegram."
        } else {
            "✓ Profile updated successfully."
        }

        return@withContext SubmissionResult.Success(
            isAutoApproved = !isNameChanged && !isCustomPhoto,
            message = successMsg
        )
    }

    private fun uploadAvatarToSupabaseStorage(context: Context, userId: String, file: File): String? {
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
        oldAvatar: String,
        newAvatar: String,
        userEmail: String,
        avatarFile: File?
    ) {
        try {
            val nameChanged = newName.isNotBlank() && oldName.isNotBlank() && newName.trim() != oldName.trim()
            val bioChanged = newBio.trim() != oldBio.trim()
            val hasCustomPhoto = (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) || newAvatar.startsWith("http")

            val header = "🛡️ <b>[PROFILE APPROVAL REQUEST]</b>\n\n"
            val nameSection = if (nameChanged) {
                "👤 <b>Display Name:</b>\n<code>${escapeHtml(oldName)}</code> ➔ <b><code>${escapeHtml(newName)}</code></b>\n\n"
            } else {
                "👤 <b>Display Name:</b> <b><code>${escapeHtml(newName)}</code></b> <i>(Unchanged)</i>\n\n"
            }

            val bioSection = if (bioChanged && (newBio.isNotBlank() || oldBio.isNotBlank())) {
                "💬 <b>Bio / Motto:</b>\n<i>\"${escapeHtml(oldBio.ifBlank { "None" })}\"</i> ➔ <b><i>\"${escapeHtml(newBio.ifBlank { "None" })}\"</i></b>\n\n"
            } else if (newBio.isNotBlank()) {
                "💬 <b>Bio / Motto:</b> <i>\"${escapeHtml(newBio)}\"</i> <i>(Unchanged)</i>\n\n"
            } else ""

            val avatarSection = if (hasCustomPhoto) {
                "📸 <b>Profile Photo:</b> ⚠️ <code>Custom Photo Uploaded</code>\n\n"
            } else if (oldAvatar != newAvatar) {
                "🎨 <b>Avatar Sticker:</b> <code>${escapeHtml(oldAvatar)}</code> ➔ <b><code>${escapeHtml(newAvatar)}</code></b>\n\n"
            } else ""

            val footer = "──────────────────\n" +
                    "🆔 <b>User ID:</b> <code>${escapeHtml(userId)}</code>\n" +
                    "📱 <b>Source:</b> Android App" + (if (userEmail.isNotBlank()) " • 📧 <code>${escapeHtml(userEmail)}</code>" else "")

            val fullCaption = (header + nameSection + bioSection + avatarSection + footer).take(1024)

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

            // If a custom image file exists locally, send directly via multipart sendPhoto
            if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
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

            // If remote photo URL exists, send via JSON sendPhoto
            if (newAvatar.startsWith("http")) {
                try {
                    val photoUrl = URL("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendPhoto")
                    val pConn = photoUrl.openConnection() as HttpURLConnection
                    pConn.requestMethod = "POST"
                    pConn.setRequestProperty("Content-Type", "application/json")
                    pConn.connectTimeout = 10000
                    pConn.readTimeout = 10000
                    pConn.doOutput = true

                    val photoPayload = JSONObject().apply {
                        put("chat_id", TELEGRAM_CHAT_ID)
                        put("photo", newAvatar)
                        put("caption", fullCaption)
                        put("parse_mode", "HTML")
                        put("reply_markup", keyboard)
                    }

                    pConn.outputStream.use { os ->
                        os.write(photoPayload.toString().toByteArray(Charsets.UTF_8))
                    }

                    val pCode = pConn.responseCode
                    Log.i(TAG, "Telegram sendPhoto URL status: $pCode")
                    if (pCode in 200..299) return
                } catch (pe: Exception) {
                    Log.w(TAG, "Telegram sendPhoto URL failed: ${pe.message}")
                }
            }

            // Fallback / standard Text message via sendMessage
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
