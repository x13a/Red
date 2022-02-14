package me.lucky.red

import android.Manifest
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresPermission
import java.util.*
import kotlin.concurrent.timerTask

class PopupWindow(private val service: CallRedirectionService) {
    private val windowManager = service
        .applicationContext
        .getSystemService(WindowManager::class.java)
    private val audioManager = service
        .applicationContext
        .getSystemService(AudioManager::class.java)
    @Suppress("InflateParams")
    private val view = LayoutInflater
        .from(service.applicationContext)
        .inflate(R.layout.popup, null)
    private val layoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        gravity = Gravity.BOTTOM
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        y = service.prefs.popupPosition
    }
    private var timer: Timer? = null

    init {
        view.setOnClickListener {
            cancel()
            service.placeCallUnmodified()
        }
    }

    fun show(uri: Uri, destinationId: Int) {
        if (!remove()) {
            service.placeCallUnmodified()
            return
        }
        timer?.cancel()
        timer = Timer()
        timer?.schedule(timerTask {
            if (!remove()) {
                service.placeCallUnmodified()
                return@timerTask
            }
            if (audioManager?.mode != AudioManager.MODE_IN_CALL) {
                service.placeCallUnmodified()
                return@timerTask
            }
            try {
                call(uri)
            } catch (exc: SecurityException) {
                service.placeCallUnmodified()
                return@timerTask
            }
            service.cancelCall()
        }, service.prefs.redirectionDelay)
        view.findViewById<TextView>(R.id.description).text = String.format(
            service.getString(R.string.popup),
            service.getString(destinationId),
        )
        if (!add()) {
            timer?.cancel()
            service.placeCallUnmodified()
        }
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    private fun call(data: Uri) {
        Intent(Intent.ACTION_VIEW).let {
            it.data = data
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            service.startActivity(it)
        }
    }

    private fun add(): Boolean {
        try {
            windowManager?.addView(view, layoutParams)
        } catch (exc: WindowManager.BadTokenException) { return false }
        return true
    }

    private fun remove(): Boolean {
        try {
            windowManager?.removeView(view)
        } catch (exc: IllegalArgumentException) {
        } catch (exc: WindowManager.BadTokenException) { return false }
        return true
    }

    fun cancel() {
        timer?.cancel()
        remove()
    }
}
