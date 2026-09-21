package com.madeby.JAI

import android.content.Context
import android.content.SharedPreferences

object AuthManager {
    private const val PREF_NAME = "studytimer_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_IS_GUEST = "is_guest"
    private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    private const val KEY_LAST_ACTIVE_USER_ID = "last_active_user_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun hasCompletedOnboarding(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false) || isLoggedIn(context) || prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun setOnboardingCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        val hasLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        return hasLoggedIn && !email.isNullOrEmpty()
    }

    fun isGuest(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_GUEST, false) || !isLoggedIn(context)
    }

    fun isSameUser(context: Context, newUserId: String?, newEmail: String?): Boolean {
        val currentUserId = getUserId(context)
        val currentEmail = getUserEmail(context)
        if (!newUserId.isNullOrEmpty() && currentUserId == newUserId) return true
        if (!newEmail.isNullOrEmpty() && currentEmail.equals(newEmail, ignoreCase = true)) return true
        return false
    }

    fun saveUserSession(context: Context, email: String?, name: String?, token: String?, userId: String? = null) {
        val actualUserId = if (!userId.isNullOrEmpty()) userId else email
        val effectiveName = if (!name.isNullOrBlank()) name else (email?.substringBefore("@") ?: "Student")
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, false)
            putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, effectiveName)
            putString(KEY_ACCESS_TOKEN, token)
            if (!actualUserId.isNullOrEmpty()) {
                putString(KEY_USER_ID, actualUserId)
                putString(KEY_LAST_ACTIVE_USER_ID, actualUserId)
            }
            apply()
        }
        AppAnalytics.associateUser(context, actualUserId)
    }

    fun updateUserName(context: Context, name: String) {
        if (name.isBlank()) return
        getPrefs(context).edit().apply {
            putString(KEY_USER_NAME, name)
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
            putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_PROFILE_IMAGE_URI)
            apply()
        }
        AppAnalytics.associateUser(context, null)
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

    fun getLastActiveUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_ACTIVE_USER_ID, null)
    }

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun resetLocalUserData(context: Context) {
        try {
            // Reset main study preferences
            context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().clear().apply()
            // Reset custom subjects and tags
            context.getSharedPreferences("studytimer_subject_tags", Context.MODE_PRIVATE).edit().clear().apply()
            // Clear timeline logs
            TimelineLogger.importRaw(context, null)
            // Delete local avatar
            LocalAvatarManager.deleteAvatar(context)
            // Remove local backup dat
            val backupFile = java.io.File(context.filesDir, "study_timer_backup.dat")
            if (backupFile.exists()) backupFile.delete()
        } catch (_: Exception) {}
    }

    fun logout(context: Context) {
        AppAnalytics.onLogout(context)
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_IS_GUEST, true)
            putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_PROFILE_IMAGE_URI)
            apply()
        }
        resetLocalUserData(context)
    }

    fun deleteLocalUserData(context: Context) {
        logout(context)
    }
}
