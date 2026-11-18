package com.tidalvolt.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import dadb.AdbKeyPair
import dadb.AdbShellPacket
import dadb.Dadb
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Phase 1 only: verifies the head unit's local ADB endpoint and authenticates
 * TidalVolt's own persistent ADB key. The only shell command is read-only:
 * identity and Android build properties. No BYD controls or vehicle writes.
 */
object DirectBydAdbManager {
    private const val HOST = "127.0.0.1"
    private const val PORT = 5555
    private const val TIMEOUT_MS = 8000

    val status = mutableStateOf("Not checked")
    val details = mutableStateOf("")
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    fun runDiagnostics(context: Context) {
        if (running) return
        running = true
        post("Checking local ADB…", "Testing 127.0.0.1:5555 on this head unit.")
        worker.execute {
            try {
                val open = try {
                    Socket().use { it.connect(InetSocketAddress(HOST, PORT), 400) }
                    true
                } catch (_: Exception) { false }

                if (!open) {
                    post("ADB service unavailable", "Nothing is listening at 127.0.0.1:5555. The head unit's local wireless-debugging service may be disabled or use a different endpoint.")
                    return@execute
                }

                val keyDir = File(context.applicationContext.filesDir, "tidalvolt-adb")
                if (!keyDir.exists()) keyDir.mkdirs()
                val privateKey = File(keyDir, "adbkey")
                val publicKey = File(keyDir, "adbkey.pub")
                if (!privateKey.exists() || !publicKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
                val pair = AdbKeyPair.read(privateKey, publicKey)
                val connection = Dadb.create(HOST, PORT, pair, connectTimeout = TIMEOUT_MS, socketTimeout = TIMEOUT_MS, keepAlive = true)
                try {
                    val shell = connection.openShell("id; getprop ro.build.version.release; getprop ro.build.version.sdk; getprop ro.product.model")
                    val output = StringBuilder()
                    try {
                        var finished = false
                        while (!finished) {
                            when (val packet = shell.read()) {
                                is AdbShellPacket.StdOut -> output.append(String(packet.payload, Charsets.UTF_8))
                                is AdbShellPacket.StdError -> output.append(String(packet.payload, Charsets.UTF_8))
                                is AdbShellPacket.Exit -> finished = true
                            }
                        }
                    } finally { shell.close() }

                    val lines = output.toString().lines().map { it.trim() }.filter { it.isNotBlank() }
                    val identity = lines.firstOrNull { it.startsWith("uid=") } ?: "Shell identity not returned"
                    val android = lines.getOrNull(1)?.let { "Android $it" } ?: "Android version unavailable"
                    val sdk = lines.getOrNull(2)?.let { "SDK $it" } ?: "SDK unavailable"
                    val model = lines.getOrNull(3)?.let { "Model $it" } ?: "Model unavailable"
                    post("ADB authenticated • diagnostic passed", "$identity\n$android • $sdk\n$model\nNo vehicle API has been queried yet.")
                } finally { connection.close() }
            } catch (error: Exception) {
                val message = error.message ?: error.javaClass.simpleName
                val authLikely = message.contains("unauthorized", true) || message.contains("auth", true) || message.contains("denied", true)
                post(
                    if (authLikely) "ADB authorization required" else "ADB connection failed",
                    if (authLikely) "Approve TidalVolt in the head unit's Allow USB debugging dialog. Tick Always allow from this computer, then tap Allow. Details: $message"
                    else "The local ADB endpoint answered, but authentication or the read-only shell check failed. Details: $message"
                )
            } finally { running = false }
        }
    }

    private fun post(newStatus: String, newDetails: String) {
        main.post { status.value = newStatus; details.value = newDetails }
    }
}
