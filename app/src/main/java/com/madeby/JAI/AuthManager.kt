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
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, false)
            putString(KEY_USER_EMAIL, email)
            if (!getUserName(context).isNullOrEmpty() && name.isNullOrEmpty()) {
                // preserve updated user name
            } else {
                putString(KEY_USER_NAME, name)
            }
            putString(KEY_ACCESS_TOKEN, token)
            if (!userId.isNullOrEmpty()) putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun updateUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply()
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

    fun getAccessToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
