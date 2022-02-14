package me.lucky.red

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val REDIRECTION_DELAY = "redirection_delay"
        private const val POPUP_POSITION = "popup_position_y"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }

    var redirectionDelay: Long
        get() = prefs.getLong(REDIRECTION_DELAY, 2000L)
        set(value) = prefs.edit { putLong(REDIRECTION_DELAY, value) }

    var popupPosition: Int
        get() = prefs.getInt(POPUP_POSITION, 333)
        set(value) = prefs.edit { putInt(POPUP_POSITION, value) }
}
