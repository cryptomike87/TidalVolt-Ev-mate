package com.tidalvolt.app

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import io.moquette.broker.Server
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Collections
import java.util.Properties
import java.util.concurrent.Executors

/**
 * Hosts a small, password-protected MQTT broker inside TidalVolt.
 * OverDrive publishes to the phone; TidalVolt subscribes through localhost.
 * No cloud broker or external server is required.
 */
object EmbeddedMqttBrokerManager {
    private const val PREFS = "tidalvolt_local_broker"
    private const val USERNAME = "tidalvolt"
    private const val PORT = 1883

    val status = mutableStateOf("Stopped")
    val networkAddress = mutableStateOf("Not available")
    val brokerPassword = mutableStateOf("")

    @Volatile private var server: Server? = null
    private val worker = Executors.newSingleThreadExecutor()

    fun start(context: Context) {
        if (server != null || status.value == "Starting…") return
        val appContext = context.applicationContext
        status.value = "Starting…"
        worker.execute {
            try {
                val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                var password = prefs.getString("password", null)
                if (password.isNullOrBlank()) {
                    password = newPassword()
                    prefs.edit().putString("password", password).apply()
                }
                val credentials = java.io.File(appContext.filesDir, "tidalvolt-mqtt-passwords")
                credentials.writeText("$USERNAME:${sha256(password)}\n")

                val config = Properties().apply {
                    setProperty("host", "0.0.0.0")
                    setProperty("port", PORT.toString())
                    setProperty("allow_anonymous", "false")
                    setProperty("password_file", credentials.absolutePath)
                    setProperty("persistence_enabled", "false")
                }
                val broker = Server()
                broker.startServer(config)
                server = broker
                brokerPassword.postValueCompat(password)
                networkAddress.postValueCompat(findLocalAddress())
                status.postValueCompat("Running • port $PORT")
            } catch (error: Exception) {
                status.postValueCompat("Could not start: ${error.message ?: "broker error"}")
            }
        }
    }

    fun stop() {
        worker.execute {
            try {
                server?.stopServer()
            } catch (_: Exception) {
            } finally {
                server = null
                status.postValueCompat("Stopped")
            }
        }
    }

    fun isRunning(): Boolean = server != null

    fun username(): String = USERNAME
    fun port(): Int = PORT

    private fun newPassword(): String {
        val bytes = ByteArray(18)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun findLocalAddress(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val wifiAddresses = interfaces
                .filter { it.isUp && (it.name.contains("wlan", true) || it.name.contains("ap", true) || it.name.contains("wifi", true)) }
                .flatMap { Collections.list(it.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
            val anyPrivate = interfaces
                .filter { it.isUp }
                .flatMap { Collections.list(it.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            (wifiAddresses.firstOrNull() ?: anyPrivate)?.hostAddress ?: "Check phone hotspot IP"
        } catch (_: Exception) {
            "Check phone hotspot IP"
        }
    }

    private fun <T> androidx.compose.runtime.MutableState<T>.postValueCompat(value: T) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { this.value = value }
    }
}
