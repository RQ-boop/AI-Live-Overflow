package com.rqboop.deskpet

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var petView: WebView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTI_ID, buildNoti())
        setupPet()
    }

    private fun setupPet() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            96,
            112,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 480
        }
        petView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {}
            loadUrl("file:///android_asset/pet.html")
        }
        petView?.let { addDragListener(it) }
        wm?.addView(petView, params)
    }

    private fun addDragListener(v: View) {
        var lastX = 0f
        var lastY = 0f
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = e.rawX
                    lastY = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.let { p ->
                        val dx = e.rawX - lastX
                        val dy = e.rawY - lastY
                        p.x += dx.toInt()
                        p.y += dy.toInt()
                        lastX = e.rawX
                        lastY = e.rawY
                        wm?.updateViewLayout(petView, p)
                    }
                    runJs("document.dispatchEvent(new CustomEvent('dragged'))")
                }
                MotionEvent.ACTION_UP -> {
                    runJs("document.dispatchEvent(new CustomEvent('tap'))")
                }
            }
            true
        }
    }

    @SuppressLint("NewApi")
    private fun runJs(js: String) {
        val wv = petView ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                wv.evaluateJavascript(js, null)
            } else {
                @Suppress("DEPRECATION")
                wv.loadUrl("javascript:$js")
            }
        } catch (_: Exception) { }
    }

    private fun buildNoti(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CH_ID, "然芊的猫", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(channel)
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val title = "然芊的猫在屏幕角落"
        val text = "点这里回到设置页"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CH_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
                .build()
        }
    }

    override fun onDestroy() {
        petView?.let { wm?.removeView(it) }
        petView?.destroy()
        super.onDestroy()
    }

    companion object {
        private const val CH_ID = "pet_overlay_channel"
        private const val NOTI_ID = 2001
    }
}
