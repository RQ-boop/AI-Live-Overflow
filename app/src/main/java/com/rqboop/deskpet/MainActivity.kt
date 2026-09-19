package com.rqboop.deskpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
class MainActivity : Activity() {
    private lateinit var startBtn: Button
    private lateinit var statusTv: TextView
    private lateinit var grantBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "哥哥的像素猫\n\n点下面按钮，允许「显示在其他应用上层」。\n\n浅蓝白的它，会趴在你屏幕角落，看着你。"
            textSize = 16f
            setPadding(32, 48, 32, 16)
        }
        statusTv = TextView(this).apply { text = "" }
        startBtn = Button(this).apply { text = "让它趴到屏幕上" }
        grantBtn = Button(this).apply {
            text = "去授权悬浮窗"
            setOnClickListener { requestOverlayPermission() }
        }
        startBtn.setOnClickListener {
            if (canDrawOverlays()) {
                startService(Intent(this@MainActivity, OverlayService::class.java))
                finish()
            } else {
                grantBtn.visibility = View.VISIBLE
            }
        }
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(tv)
            addView(statusTv)
            addView(startBtn)
            addView(grantBtn)
        }
        setContentView(root)
    }
    private fun requestOverlayPermission() {
        val i = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + packageName)
        )
        startActivity(i)
    }
    override fun onResume() {
        super.onResume()
        if (canDrawOverlays()) {
            statusTv.text = "已授权，可以直接启动"
            startBtn.visibility = View.VISIBLE
        } else {
            statusTv.text = "还未授权，点上面「去授权悬浮窗」"
        }
    }
    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }
}
