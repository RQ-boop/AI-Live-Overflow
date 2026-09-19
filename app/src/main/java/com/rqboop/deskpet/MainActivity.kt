package com.rqboop.deskpet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "DeskPet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        val title = TextView(this).apply {
            text = "桌面小猫 · 浅粉像素款"
            textSize = 20f
        }
        val hint = TextView(this).apply {
            text = "先开好悬浮窗权限，再点下方按钮把小猫放出来。"
            textSize = 14f
        }

        val startBtn = Button(this).apply { text = getString(R.string.start_pet_btn) }
        val stopBtn = Button(this).apply { text = getString(R.string.stop_pet_btn) }
        val grantBtn = Button(this).apply { text = getString(R.string.grant_btn) }

        startBtn.setOnClickListener {
            if (!hasOverlayPermission()) {
                grantBtn.performClick()
                return@setOnClickListener
            }
            stopService(Intent(this, OverlayService::class.java))
            startService(Intent(this, OverlayService::class.java))
            Log.d(TAG, "start overlay")
        }
        stopBtn.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
        grantBtn.setOnClickListener {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        if (hasOverlayPermission()) grantBtn.text = "权限已开"

        root.addView(title)
        root.addView(hint)
        root.addView(grantBtn)
        root.addView(startBtn)
        root.addView(stopBtn)
        return root
    }

    private fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        Settings.canDrawOverlays(this)
}