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

    fun saveUserSession(context: Context, email: String?, name: String?, token: String?) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, false)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_ACCESS_TOKEN, token)
            apply()
        }
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

    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
