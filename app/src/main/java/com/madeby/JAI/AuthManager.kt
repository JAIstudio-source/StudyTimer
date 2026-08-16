package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences

object AuthManager {
    private const val PREF_NAME = "studytimer_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_ACCESS_TOKEN = "access_token"

    private const val KEY_USER_ID = "user_id"

    private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"

    private const val KEY_DEVICE_LINKED_USER_ID = "device_linked_user_id"
    private const val KEY_DEVICE_LINKED_USER_NAME = "device_linked_user_name"
    private const val KEY_DEVICE_LINKED_USER_EMAIL = "device_linked_user_email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) || prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun isGuest(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_GUEST, false)
    }

    fun saveUserSession(context: Context, email: String?, name: String?, token: String?, userId: String? = null) {
        val actualUserId = if (!userId.isNullOrEmpty()) userId else email
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, false)
            putString(KEY_USER_EMAIL, email)
            if (!getUserName(context).isNullOrEmpty() && name.isNullOrEmpty()) {
                // preserve updated user name
            } else {
                putString(KEY_USER_NAME, name)
            }
            putString(KEY_ACCESS_TOKEN, token)
            if (!actualUserId.isNullOrEmpty()) {
                putString(KEY_USER_ID, actualUserId)
                putString(KEY_DEVICE_LINKED_USER_ID, actualUserId)
            }
            if (!email.isNullOrEmpty()) putString(KEY_DEVICE_LINKED_USER_EMAIL, email)
            if (!name.isNullOrEmpty()) putString(KEY_DEVICE_LINKED_USER_NAME, name)
            apply()
        }
        AppAnalytics.associateUser(context, actualUserId)
    }

    fun updateUserName(context: Context, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_DEVICE_LINKED_USER_NAME, name)
            apply()
        }
        AppAnalytics.associateUser(context, getUserId(context))
    }

    fun saveProfileImageUri(context: Context, uriString: String) {
        getPrefs(context).edit().putString(KEY_PROFILE_IMAGE_URI, uriString).apply()
    }

    fun getProfileImageUri(context: Context): String? {
        return getPrefs(context).getString(KEY_PROFILE_IMAGE_URI, null)
    }

    fun setGuestMode(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_IS_GUEST, true)
            apply()
        }
        AppAnalytics.associateUser(context, getLinkedUserId(context))
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_NAME, null)
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null) ?: getUserEmail(context)
    }

    fun getLinkedUserId(context: Context): String? {
        return getUserId(context) ?: getPrefs(context).getString(KEY_DEVICE_LINKED_USER_ID, null) ?: getUserEmail(context) ?: getPrefs(context).getString(KEY_DEVICE_LINKED_USER_EMAIL, null)
    }

    fun getLinkedUserName(context: Context): String? {
        return getUserName(context) ?: getPrefs(context).getString(KEY_DEVICE_LINKED_USER_NAME, null)
    }

    fun getLinkedUserEmail(context: Context): String? {
        return getUserEmail(context) ?: getPrefs(context).getString(KEY_DEVICE_LINKED_USER_EMAIL, null)
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun logout(context: Context) {
        AppAnalytics.onLogout(context)
        val linkedId = getPrefs(context).getString(KEY_DEVICE_LINKED_USER_ID, null)
        val linkedName = getPrefs(context).getString(KEY_DEVICE_LINKED_USER_NAME, null)
        val linkedEmail = getPrefs(context).getString(KEY_DEVICE_LINKED_USER_EMAIL, null)
        
        getPrefs(context).edit().apply {
            clear()
            if (!linkedId.isNullOrEmpty()) putString(KEY_DEVICE_LINKED_USER_ID, linkedId)
            if (!linkedName.isNullOrEmpty()) putString(KEY_DEVICE_LINKED_USER_NAME, linkedName)
            if (!linkedEmail.isNullOrEmpty()) putString(KEY_DEVICE_LINKED_USER_EMAIL, linkedEmail)
            apply()
        }
    }

    fun deleteLocalUserData(context: Context) {
        logout(context)
        context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        TimelineLogger.importRaw(context, null)
        try {
            java.io.File(context.filesDir, "study_timer_backup.dat").delete()
        } catch (_: Exception) {}
    }
}
