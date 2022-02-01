package me.lucky.re

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }
}
