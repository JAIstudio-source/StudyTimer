package com.madeby.JAI

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TermsConsentManager {
    private const val TAG = "TermsConsentManager"

    /**
     * Records and persists user's legal agreement & consent to Terms of Service & Privacy Policy
     * both locally in AuthManager and remotely on Supabase with exact immutable timestamp.
     */
    suspend fun syncConsentToServer(
        context: Context,
        userId: String,
        email: String,
        name: String,
        termsVersion: String = AuthManager.CURRENT_TERMS_VERSION
    ): Boolean = withContext(Dispatchers.IO) {
        val epochMillis = AuthManager.getTermsAcceptedEpoch(context).let {
            if (it > 0L) it else System.currentTimeMillis()
        }
        val isoTimestamp = AuthManager.getTermsAcceptedAt(context) ?: formatIsoUtc(epochMillis)
        
        // Ensure local storage is up to date
        AuthManager.recordTermsConsent(context, termsVersion, isoTimestamp, epochMillis)

        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            Log.w(TAG, "Supabase config missing, saved consent locally.")
            return@withContext true
        }

        val hardwareId = AppAnalytics.getHardwareDeviceId(context)
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val appVersion = BuildConfig.VERSION_NAME

        // 1. Post to user_terms_consents audit table
        var success = false
        try {
            val url = URL("$supabaseUrl/rest/v1/user_terms_consents")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("user_id", userId)
                put("user_email", email)
                put("user_name", name)
                put("terms_version", termsVersion)
                put("accepted_at", isoTimestamp)
                put("accepted_at_iso", isoTimestamp)
                put("accepted_at_epoch", epochMillis)
                put("device_hardware_id", hardwareId)
                put("device_model", deviceModel)
                put("app_version", appVersion)
                put("platform", "android")
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                success = true
                Log.d(TAG, "Successfully recorded user consent to Supabase: $isoTimestamp")
            } else {
                Log.w(TAG, "Failed to record user consent on Supabase HTTP $code")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting consent audit record", e)
        }

        // 2. Track analytics event
        try {
            val eventProps = mapOf(
                "terms_version" to termsVersion,
                "accepted_at" to isoTimestamp,
                "accepted_at_epoch" to epochMillis,
                "device_hardware_id" to hardwareId,
                "device_model" to deviceModel
            )
            AppAnalytics.trackEvent(context, "terms_and_privacy_accepted", eventProps)
        } catch (_: Exception) {}

        success
    }

    private fun formatIsoUtc(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}
