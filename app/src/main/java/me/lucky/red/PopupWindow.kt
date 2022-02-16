package me.lucky.red

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.timerTask

class PopupWindow(
    private val ctx: Context,
    private val service: WeakReference<CallRedirectionService>?,
) {
    private val prefs = Preferences(ctx)
    private val windowManager = ctx.getSystemService(WindowManager::class.java)
    private val audioManager = ctx.getSystemService(AudioManager::class.java)
    @Suppress("InflateParams")
    private val view = LayoutInflater.from(ctx).inflate(R.layout.popup, null)
    private val layoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        gravity = Gravity.BOTTOM
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        y = prefs.popupPosition
    }
    private var timer: Timer? = null

    init {
        view.setOnClickListener {
            cancel()
            service?.get()?.placeCallUnmodified()
        }
    }

    fun preview() {
        remove()
        layoutParams.y = prefs.popupPosition
        val destinations = mutableListOf(
            R.string.destination_signal,
            R.string.destination_telegram,
            R.string.destination_threema,
        )
        if (prefs.isFallbackChecked) destinations.add(R.string.fallback_destination_whatsapp)
        setDescription(destinations.random())
        add()
    }

    fun show(uri: Uri, destinationId: Int) {
        val service = service?.get() ?: return
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
        }, prefs.redirectionDelay)
        setDescription(destinationId)
        if (!add()) {
            timer?.cancel()
            service.placeCallUnmodified()
        }
    }

    private fun setDescription(id: Int) {
        view.findViewById<TextView>(R.id.description).text = ctx.getString(
            R.string.popup,
            ctx.getString(id),
        )
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    private fun call(data: Uri) {
        Intent(Intent.ACTION_VIEW).let {
            it.data = data
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ctx.startActivity(it)
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
