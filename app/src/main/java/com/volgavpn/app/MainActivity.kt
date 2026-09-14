package com.volgavpn.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("volga", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private var connected = false
    private var startedAt = 0L
    private lateinit var status: TextView
    private lateinit var timer: TextView
    private lateinit var button: Button
    private lateinit var server: Spinner

    private val servers = listOf("🇳🇱  Netherlands", "🇩🇪  Germany", "🇫🇮  Finland")
    private val tick = object : Runnable {
        override fun run() {
            if (connected) {
                val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                timer.text = String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 17, 31)
        window.navigationBarColor = Color.rgb(8, 17, 31)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(18))
            setBackgroundColor(Color.rgb(8, 17, 31))
        }

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply { text = "🛡"; textSize = 28f }
        val brand = TextView(this).apply { text = "VOLGAVPN\nPRIVATE NETWORK"; textSize = 17f; setTextColor(Color.WHITE); setPadding(dp(12),0,0,0) }
        header.addView(logo)
        header.addView(brand)
        root.addView(header)

        root.addView(space(26))
        root.addView(text("SECURE CONNECTION", 11f, Color.rgb(73,215,255)))
        status = text("NOT CONNECTED", 28f, Color.WHITE).apply { setPadding(0, dp(6), 0, 0) }
        root.addView(status)
        timer = text("00:00:00", 14f, Color.LTGRAY).apply { setPadding(0, dp(4),0,0) }
        root.addView(timer)

        root.addView(space(20))
        val connect = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(Color.rgb(13, 28, 47)) }
        connect.addView(text("LOCATION", 10f, Color.GRAY))
        server = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, servers)
            setSelection(prefs.getInt("server", 0))
        }
        connect.addView(server, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(connect)

        root.addView(space(18))
        button = Button(this).apply {
            text = "CONNECT"
            textSize = 16f
            isAllCaps = true
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(24, 105, 145))
            setOnClickListener { toggleVpn() }
        }
        root.addView(button, LinearLayout.LayoutParams(-1, dp(58)))

        root.addView(space(18))
        root.addView(statCard("VPN PROTOCOL", "WireGuard-ready architecture"))
        root.addView(statCard("DNS", "1.1.1.1"))
        root.addView(statCard("STATUS", "Local VPN service available"))

        root.addView(space(16))
        val note = text("The current build contains the Android VPN service foundation. A real public-IP tunnel requires a provisioned VPN server and protocol configuration; private keys are intentionally not embedded.", 12f, Color.rgb(165, 178, 192))
        note.setPadding(dp(4), dp(8), dp(4), dp(8))
        root.addView(note)
    }

    private fun toggleVpn() {
        if (connected) {
            stopService(Intent(this, VolgaVpnService::class.java))
            connected = false
            status.text = "NOT CONNECTED"
            button.text = "CONNECT"
            handler.removeCallbacks(tick)
            return
        }

        prefs.edit().putInt("server", server.selectedItemPosition).apply()
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN)
            return
        }
        startVpnService()
    }

    private fun startVpnService() {
        val serviceIntent = Intent(this, VolgaVpnService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        connected = true
        startedAt = System.currentTimeMillis()
        status.text = "VPN ENGINE ACTIVE"
        button.text = "DISCONNECT"
        handler.post(tick)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) startVpnService()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun text(value: String, size: Float, color: Int) = TextView(this).apply { text = value; textSize = size; setTextColor(color) }
    private fun space(h: Int) = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun statCard(title: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); setBackgroundColor(Color.rgb(13, 28, 47))
        val a = text(title, 10f, Color.GRAY); val b = text(value, 13f, Color.WHITE); b.gravity = Gravity.RIGHT
        addView(a, LinearLayout.LayoutParams(0, dp(46), 1f)); addView(b, LinearLayout.LayoutParams(0, dp(46), 1f))
        layoutParams = LinearLayout.LayoutParams(-1, dp(74)).apply { bottomMargin = dp(8) }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object { private const val REQUEST_VPN = 501 }
}
