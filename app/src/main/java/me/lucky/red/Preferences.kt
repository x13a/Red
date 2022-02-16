package me.lucky.red

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val ENABLED = "enabled"
        private const val REDIRECTION_DELAY = "redirection_delay"
        private const val POPUP_POSITION = "popup_position_y"
        private const val FALLBACK_CHECKED = "fallback_checked"

        private const val DEFAULT_REDIRECTION_DELAY = 2000L
        private const val DEFAULT_POPUP_POSITION = 333

        // migration
        private const val SERVICE_ENABLED = "service_enabled"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, prefs.getBoolean(SERVICE_ENABLED, false))
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var redirectionDelay: Long
        get() = prefs.getLong(REDIRECTION_DELAY, DEFAULT_REDIRECTION_DELAY)
        set(value) = prefs.edit { putLong(REDIRECTION_DELAY, value) }

    var popupPosition: Int
        get() = prefs.getInt(POPUP_POSITION, DEFAULT_POPUP_POSITION)
        set(value) = prefs.edit { putInt(POPUP_POSITION, value) }

    var isFallbackChecked: Boolean
        get() = prefs.getBoolean(FALLBACK_CHECKED, false)
        set(value) = prefs.edit { putBoolean(FALLBACK_CHECKED, value) }
}
