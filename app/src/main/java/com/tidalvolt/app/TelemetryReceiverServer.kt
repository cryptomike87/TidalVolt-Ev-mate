package com.tidalvolt.app

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local-only receiver for OverDrive telemetry.
 * The phone hosts this endpoint; OverDrive on the head unit connects as a WebSocket client.
 */
object TelemetryReceiverServer {
    private const val PORT = 8765
    private const val PREFS = "tidalvolt_receiver"
    private var server: WebSocketServer? = null
    private var pairingToken: String = ""
    private val running = AtomicBoolean(false)

    val status = mutableStateOf("Stopped")
    val endpoint = mutableStateOf("Start receiver to show connection address")
    val connected = mutableStateOf(false)

    @Synchronized
    fun start(context: Context) {
        if (running.get()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pairingToken = prefs.getString("pairing_token", null) ?: UUID.randomUUID().toString().replace("-", "").take(16).also {
            prefs.edit().putString("pairing_token", it).apply()
        }
        val address = discoverLocalIpv4()
        endpoint.value = if (address != null) "ws://$address:$PORT/tidalvolt?token=$pairingToken" else "Waiting for Wi-Fi or hotspot IP"
        status.value = "Starting receiver…"

        val instance = object : WebSocketServer(InetSocketAddress("0.0.0.0", PORT)) {
            override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
                val expected = "/tidalvolt?token=$pairingToken"
                if (handshake.resourceDescriptor != expected) {
                    conn.close(1008, "Invalid pairing token")
                    return
                }
                connected.value = true
                status.value = "OverDrive connected"
            }

            override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
                connected.value = false
                status.value = "Waiting for OverDrive"
            }

            override fun onMessage(conn: WebSocket, message: String) {
                try {
                    val root = JSONObject(message)
                    val payload = if (root.has("telemetry") && root.opt("telemetry") is JSONObject) root.getJSONObject("telemetry").toString() else message
                    val data = EvTelemetry.fromJson(payload)
                    TelemetryStore.latest.value = data
                    TelemetryHistory.record(context.applicationContext, data)
                    status.value = "Receiving vehicle data"
                } catch (error: Exception) {
                    status.value = "Connected • unrecognized data format"
                }
            }

            override fun onError(conn: WebSocket?, ex: Exception) {
                status.value = "Receiver error: ${ex.message ?: "unknown"}"
            }

            override fun onStart() {
                running.set(true)
                status.value = "Waiting for OverDrive"
            }
        }
        server = instance
        try {
            instance.start()
        } catch (error: Exception) {
            running.set(false)
            status.value = "Could not start: ${error.message ?: "unknown error"}"
        }
    }

    @Synchronized
    fun stop() {
        try { server?.stop(1000) } catch (_: Exception) { }
        server = null
        running.set(false)
        connected.value = false
        status.value = "Stopped"
    }

    fun isRunning(): Boolean = running.get()

    private fun discoverLocalIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                if (!network.isUp || network.isLoopback) continue
                val addresses = network.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                        val ip = address.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) return ip
                    }
                }
            }
            null
        } catch (_: Exception) { null }
    }
}

object TelemetryHistory {
    private const val PREFS = "tidalvolt_telemetry_history"

    fun record(context: Context, data: EvTelemetry) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val line = listOf(
            data.receivedAtMillis.toString(),
            data.soc?.toString().orEmpty(),
            data.rangeMiles?.toString().orEmpty(),
            data.speedMph?.toString().orEmpty(),
            data.odometerMiles?.toString().orEmpty(),
            data.isCharging?.toString().orEmpty(),
            data.chargingPowerKw?.toString().orEmpty()
        ).joinToString("|")
        val history = prefs.getString("samples", "").orEmpty().lines().filter { it.isNotBlank() }.toMutableList()
        history.add(line)
        prefs.edit().putString("samples", history.takeLast(5000).joinToString("\n")).apply()
    }
}
