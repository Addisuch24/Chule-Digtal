package com.chuledigita.mobile.data.local

import android.content.Context

object SessionPreferences {
    private const val PREF_NAME = "chule_session_pref"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }
}
