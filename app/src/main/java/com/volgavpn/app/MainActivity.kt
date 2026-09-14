package com.volgavpn.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.widget.*
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("volga", MODE_PRIVATE) }
    private val tunnel = AppTunnel("volga")
    private val backend by lazy { GoBackend(applicationContext) }
    private lateinit var status: TextView
    private lateinit var ip: TextView
    private lateinit var button: Button
    private lateinit var configBox: EditText
    private var pendingConnect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 16, 29)
        window.navigationBarColor = Color.rgb(7, 16, 29)
        buildUi()
        configBox.setText(prefs.getString("config", "") ?: "")
        refreshIp()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 55, 40, 30)
            setBackgroundColor(Color.rgb(7, 16, 29))
        }
        fun tv(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
        }

        root.addView(tv("VOLGAVPN", 30f))
        root.addView(tv("FREE WIREGUARD CLIENT", 11f, Color.CYAN))
        status = tv("● НЕ ПОДКЛЮЧЕНО", 20f).apply { setPadding(0, 45, 0, 8) }
        ip = tv("Внешний IP: проверка...", 14f, Color.LTGRAY)
        root.addView(status)
        root.addView(ip)

        root.addView(Button(this).apply {
            text = "ИМПОРТИРОВАТЬ .CONF"
            setOnClickListener { openConfigFile() }
        })

        configBox = EditText(this).apply {
            hint = "Или вставь сюда WireGuard-конфигурацию"
            hintTextColor = Color.GRAY
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(15, 27, 43))
            minLines = 7
            gravity = android.view.Gravity.TOP
            setPadding(18, 15, 18, 15)
        }
        root.addView(configBox, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 12 })

        button = Button(this).apply {
            text = "ПОДКЛЮЧИТЬСЯ"
            textSize = 17f
            setOnClickListener { toggle() }
        }
        root.addView(button)

        root.addView(Button(this).apply {
            text = "СОХРАНИТЬ КОНФИГ"
            setOnClickListener {
                prefs.edit().putString("config", configBox.text.toString()).apply()
                toast("Конфигурация сохранена")
            }
        })
        root.addView(tv("Используется настоящий WireGuard userspace backend. Сервер и ключи не вшиваются в приложение.", 11f, Color.GRAY))
        setContentView(root)
    }

    private fun openConfigFile() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, REQUEST_FILE)
    }

    private fun toggle() {
        try {
            if (backend.getState(tunnel) == Tunnel.State.UP) disconnect() else connect()
        } catch (e: Exception) {
            toast("Не удалось получить состояние VPN: ${e.message}")
        }
    }

    private fun connect() {
        val text = configBox.text.toString().trim()
        if (text.isEmpty()) {
            toast("Сначала импортируй WireGuard .conf")
            return
        }
        val permission = VpnService.prepare(this)
        if (permission != null) {
            pendingConnect = true
            startActivityForResult(permission, REQUEST_VPN)
            return
        }
        startTunnel(text)
    }

    private fun startTunnel(text: String) {
        executor.execute {
            try {
                val config = Config.parse(ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8)))
                backend.setState(tunnel, Tunnel.State.UP, config)
                runOnUiThread {
                    pendingConnect = false
                    status.text = "● ПОДКЛЮЧЕНО"
                    status.setTextColor(Color.CYAN)
                    button.text = "ОТКЛЮЧИТЬСЯ"
                    refreshIp()
                }
            } catch (e: BackendException) {
                showError("VPN: ${e.reason}")
            } catch (e: Exception) {
                showError("Ошибка: ${e.message ?: "неизвестная ошибка"}")
            }
        }
    }

    private fun disconnect() {
        executor.execute {
            try { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            catch (_: Exception) { }
            runOnUiThread {
                status.text = "● НЕ ПОДКЛЮЧЕНО"
                status.setTextColor(Color.WHITE)
                button.text = "ПОДКЛЮЧИТЬСЯ"
                refreshIp()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use {
                    configBox.setText(it.readBytes().toString(StandardCharsets.UTF_8))
                }
            }
        } else if (requestCode == REQUEST_VPN && resultCode == RESULT_OK && pendingConnect) {
            startTunnel(configBox.text.toString().trim())
        }
    }

    private fun refreshIp() {
        executor.execute {
            val result = try {
                (URL("https://api.ipify.org").openConnection() as HttpURLConnection).run {
                    connectTimeout = 5000
                    readTimeout = 5000
                    inputStream.bufferedReader().use { it.readText() }
                }
            } catch (_: Exception) { "—" }
            runOnUiThread { ip.text = "Внешний IP: $result" }
        }
    }

    private fun showError(message: String) = runOnUiThread {
        toast(message)
        status.text = "● ОШИБКА"
        status.setTextColor(Color.RED)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private class AppTunnel(private val name: String) : Tunnel {
        override fun getName() = name
        override fun onStateChange(newState: Tunnel.State) { }
    }

    companion object {
        private const val REQUEST_FILE = 100
        private const val REQUEST_VPN = 101
    }
}
