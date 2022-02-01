package me.lucky.re

import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import java.util.*
import kotlin.concurrent.timerTask

class DialogWindow(private val service: CallRedirectionService) {
    companion object {
        private const val CANCEL_DELAY = 3000L
    }

    private val windowManager = service
        .applicationContext
        .getSystemService(WindowManager::class.java)
    @Suppress("InflateParams")
    private val floatView = LayoutInflater
        .from(service.applicationContext)
        .inflate(R.layout.popup, null)
    private val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        gravity = Gravity.BOTTOM
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        y = 384
    }
    private var data: Uri? = null
    private var timer: Timer? = null

    init {
        floatView.setOnClickListener {
            timer?.cancel()
            service.placeCallUnmodified()
            remove()
        }
    }

    fun show(uri: Uri?) {
        remove()
        timer?.cancel()
        timer = Timer()
        timer?.schedule(timerTask {
            service.cancelCall()
            remove()
            Intent(Intent.ACTION_VIEW).also {
                it.data = data
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                service.startActivity(it)
            }
            data = null
        }, CANCEL_DELAY)
        data = uri
        windowManager?.addView(floatView, layoutParams)
    }

    private fun remove() {
        try {
            windowManager?.removeView(floatView)
        } catch (exc: IllegalArgumentException) {}
    }
}
