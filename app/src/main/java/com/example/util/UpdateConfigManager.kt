package com.example.util

import android.content.Context
import android.content.SharedPreferences

class UpdateConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var githubOwner: String
        get() = prefs.getString(KEY_OWNER, DEFAULT_OWNER) ?: DEFAULT_OWNER
        set(value) = prefs.edit().putString(KEY_OWNER, value.trim()).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_REPO, DEFAULT_REPO) ?: DEFAULT_REPO
        set(value) = prefs.edit().putString(KEY_REPO, value.trim()).apply()

    var lastCheckTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    companion object {
        private const val PREFS_NAME = "acme_app_update_prefs"
        private const val KEY_OWNER = "github_owner"
        private const val KEY_REPO = "github_repo"
        private const val KEY_LAST_CHECK = "last_check_timestamp"

        // Default configured official GitHub repository
        const val DEFAULT_OWNER = "chiwaturhonald"
        const val DEFAULT_REPO = "customer-portal"
    }
}
