package com.rqboop.deskpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.DisplayMetrics

class OverlayService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var params: WindowManager.LayoutParams? = null
    private var lastTap = 0L
    private var pollJob: Runnable? = null

    companion object {
        const val CHANNEL_ID = "pet_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val PET_SIZE_DP = 180
        const val PET_HEIGHT_DP = 240
        const val POLL_INTERVAL_MS = 30000L
    }

    inner class JsBridge {
        @JavascriptInterface
        fun onGesture(eventType: String) {
            val payload = """{"from":"android-overlay"}"""
            SupabaseClient.pushEvent(eventType, payload)
        }
        @JavascriptInterface
        fun setMood(mood: String) {
            // mood state is read from Supabase; for local UI only
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, "桌面宠物",
                    NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("桌面小猫")
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(pi)
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW)
        }
        return builder.build()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        val wPx = (PET_SIZE_DP * dm.density).toInt()
        val hPx = (PET_HEIGHT_DP * dm.density).toInt()
        params = WindowManager.LayoutParams(
            wPx, hPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 320
        }

        overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            addJavascriptInterface(JsBridge(), "AndroidPet")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView, url: String
                ): Boolean = true
            }
            loadUrl("file:///android_asset/pet.html")

            setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val now = System.currentTimeMillis()
                        if (now - lastTap < 300) {
                            handler.post { overlayView?.evaluateJavascript(
                                "window.petEngine && window.petEngine.onDoubleTap()", null) }
                        } else {
                            handler.post { overlayView?.evaluateJavascript(
                                "window.petEngine && window.petEngine.onTap()", null) }
                        }
                        lastTap = now
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.post {
                            val lx = (ev.x * params!!.width / wPx).toInt()
                            val ly = (ev.y * params!!.height / hPx).toInt()
                            SupabaseClient.pushEvent(
                                "gesture_log",
                                """{"gesture":"tap","x":$lx,"y":$ly}"""
                            )
                        }
                    }
                }
                true
            }
        }

        windowManager?.addView(overlayView, params)
        startPolling()
    }

    private fun startPolling() {
        val job = object : Runnable {
            override fun run() {
                applyState()
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        pollJob = job
        handler.post(job)
    }

    private fun applyState() {
        handler.postDelayed({
            Thread {
                val st = SupabaseClient.fetchState()
                st?.let {
                    val mood = it.optString("mood", "happy")
                    val whisper = it.optString("whisper", "")
                    val gr = it.optString("gesture_reaction", "")
                    handler.post {
                        overlayView?.evaluateJavascript(
                            "window.petEngine && window.petEngine.setState('$mood','${whisper.replace("'","\\'")}', '${gr.replace("'","\\'")}')",
                            null)
                    }
                }
            }.start()
        }, 0)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        pollJob?.let { handler.removeCallbacks(it) }
        overlayView?.let {
            windowManager?.removeView(it)
            it.destroy()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}