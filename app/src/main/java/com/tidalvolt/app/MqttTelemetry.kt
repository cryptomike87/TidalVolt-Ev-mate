package com.tidalvolt.app

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

/**
 * Normalized, read-only vehicle telemetry from OverDrive or BYD Trip Stats.
 * Missing fields remain null. OverDrive's estimated SOH is kept distinct from OEM SOH.
 */
data class EvTelemetry(
    val soc: Double? = null,
    val rangeMiles: Double? = null,
    val speedMph: Double? = null,
    val motorPowerKw: Double? = null,
    val chargingPowerKw: Double? = null,
    val isCharging: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryTempC: Double? = null,
    val odometerMiles: Double? = null,
    val batterySoh: Double? = null,
    val batterySohOem: Double? = null,
    val packVoltageV: Double? = null,
    val cellVoltageMinV: Double? = null,
    val cellVoltageMaxV: Double? = null,
    val cellVoltageDeltaV: Double? = null,
    val batteryCapacityKwh: Double? = null,
    val cellTempMinC: Double? = null,
    val cellTempMaxC: Double? = null,
    val chargingEtaMinutes: Double? = null,
    val receivedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        private const val KM_TO_MILES = 0.621371

        fun fromJson(payload: String): EvTelemetry {
            val json = JSONObject(payload)

            fun number(vararg keys: String): Double? {
                for (key in keys) {
                    if (!json.has(key) || json.isNull(key)) continue
                    val value = json.opt(key)
                    val parsed = when (value) {
                        is Number -> value.toDouble()
                        is String -> value.trim().toDoubleOrNull()
                        else -> null
                    }
                    if (parsed != null && parsed.isFinite()) return parsed
                }
                return null
            }

            fun boolean(vararg keys: String): Boolean? {
                for (key in keys) {
                    if (!json.has(key) || json.isNull(key)) continue
                    return when (val value = json.opt(key)) {
                        is Boolean -> value
                        is Number -> value.toDouble() != 0.0
                        is String -> when (value.trim().lowercase()) {
                            "1", "true", "yes", "on", "charging" -> true
                            "0", "false", "no", "off", "idle", "not_charging" -> false
                            else -> null
                        }
                        else -> null
                    }
                }
                return null
            }

            val etaMinutes = number("charging_eta_minutes")
                ?: number("charging_eta_hours")?.times(60.0)

            return EvTelemetry(
                soc = number("soc", "soc_panel"),
                rangeMiles = number("ev_range_km", "electric_driving_range_km")?.times(KM_TO_MILES),
                speedMph = number("speed")?.times(KM_TO_MILES),
                motorPowerKw = number("power", "engine_power"),
                chargingPowerKw = number("charge_power", "charging_power"),
                isCharging = boolean("is_charging"),
                latitude = number("lat", "location_latitude"),
                longitude = number("lon", "location_longitude"),
                batteryTempC = number("batt_temp", "battery_pack_temp"),
                odometerMiles = number("odometer")?.times(KM_TO_MILES),
                batterySoh = number("soh"),
                batterySohOem = number("soh_oem"),
                packVoltageV = number("hv_pack_v"),
                cellVoltageMinV = number("cell_v_min"),
                cellVoltageMaxV = number("cell_v_max"),
                cellVoltageDeltaV = number("cell_v_delta"),
                batteryCapacityKwh = number("capacity"),
                cellTempMinC = number("cell_t_min"),
                cellTempMaxC = number("cell_t_max"),
                chargingEtaMinutes = etaMinutes
            )
        }
    }
}

object TelemetryStore {
    val latest = mutableStateOf<EvTelemetry?>(null)
    val historyCount = mutableStateOf(0)
    val lastSavedAt = mutableStateOf(0L)
    private const val PREFS = "tidalvolt_vehicle_archive"
    private const val LATEST_KEY = "latest_snapshot"
    private const val HISTORY_KEY = "snapshot_history"
    private const val LATEST_TIME_KEY = "latest_saved_at"
    private const val MAX_SNAPSHOTS = 2000
    @Volatile private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = app.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        runCatching {
            prefs.getString(LATEST_KEY, null)?.let { raw ->
                val savedTime = prefs.getLong(LATEST_TIME_KEY, 0L)
                latest.value = EvTelemetry.fromJson(raw).copy(receivedAtMillis = savedTime)
            }
            val history = org.json.JSONArray(prefs.getString(HISTORY_KEY, "[]"))
            historyCount.value = history.length()
            lastSavedAt.value = prefs.getLong(LATEST_TIME_KEY, 0L)
        }
    }

    fun save(payload: String, telemetry: EvTelemetry) {
        val context = appContext ?: return
        runCatching {
            val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            val history = org.json.JSONArray(prefs.getString(HISTORY_KEY, "[]"))
            history.put(org.json.JSONObject().put("receivedAt", telemetry.receivedAtMillis).put("payload", payload))
            val trimmed = org.json.JSONArray()
            val start = (history.length() - MAX_SNAPSHOTS).coerceAtLeast(0)
            for (i in start until history.length()) trimmed.put(history.getJSONObject(i))
            prefs.edit().putString(LATEST_KEY, payload).putLong(LATEST_TIME_KEY, telemetry.receivedAtMillis).putString(HISTORY_KEY, trimmed.toString()).commit()
            latest.value = telemetry
            historyCount.value = trimmed.length()
            lastSavedAt.value = telemetry.receivedAtMillis
        }
    }

    fun recentSnapshots(limit: Int = 30): List<EvTelemetry> {
        val context = appContext ?: return emptyList()
        return runCatching {
            val history = org.json.JSONArray(context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(HISTORY_KEY, "[]"))
            val result = mutableListOf<EvTelemetry>()
            for (i in (history.length() - limit).coerceAtLeast(0) until history.length()) {
                val entry = history.getJSONObject(i)
                result.add(EvTelemetry.fromJson(entry.getString("payload")).copy(receivedAtMillis = entry.optLong("receivedAt", 0L)))
            }
            result.reversed()
        }.getOrDefault(emptyList())
    }
}

/**
 * Read-only MQTT subscriber. OverDrive publishes a complete JSON snapshot in
 * Aggregate mode; BYD Trip Stats can publish its documented state JSON topic too.
 */
object MqttTelemetryManager {
    val status = mutableStateOf("Not connected")
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var mqttClient: MqttAsyncClient? = null

    fun connect(host: String, port: Int, topic: String, username: String, password: String) {
        disconnect(updateStatus = false)
        val rawHost = host.trim()
        val explicitScheme = when {
            rawHost.startsWith("ssl://", ignoreCase = true) -> "ssl://"
            rawHost.startsWith("tcp://", ignoreCase = true) -> "tcp://"
            else -> null
        }
        val cleanHost = rawHost
            .removePrefix("ssl://")
            .removePrefix("SSL://")
            .removePrefix("tcp://")
            .removePrefix("TCP://")
            .trimEnd('/')
        if (cleanHost.isBlank() || topic.isBlank() || port !in 1..65535) {
            setStatus("Invalid broker settings")
            return
        }
        val scheme = explicitScheme ?: if (port == 8883) "ssl://" else "tcp://"
        val uri = "$scheme$cleanHost:$port"
        setStatus("Connecting…")
        try {
            val client = MqttAsyncClient(uri, MqttClient.generateClientId(), MemoryPersistence())
            mqttClient = client
            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    setStatus(if (reconnect) "Reconnected" else "Connected")
                    try {
                        client.subscribe(topic, 0, null, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                setStatus("Connected • listening")
                            }
                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                setStatus("Connected, but topic subscription failed")
                            }
                        })
                    } catch (_: Exception) {
                        setStatus("Connected, but topic subscription failed")
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    setStatus("Connection lost • reconnecting")
                }

                override fun messageArrived(receivedTopic: String, message: MqttMessage) {
                    try {
                        val parsed = EvTelemetry.fromJson(message.payload.toString(Charsets.UTF_8))
                        mainHandler.post {
                            TelemetryStore.save(message.payload.toString(Charsets.UTF_8), parsed)
                            setStatus("Connected • live data saved on device")
                        }
                    } catch (_: Exception) {
                        setStatus("Connected • received an unreadable telemetry message")
                    }
                }

                override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) = Unit
            })
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 15
                keepAliveInterval = 30
                if (username.isNotBlank()) userName = username
                if (password.isNotEmpty()) setPassword(password.toCharArray())
            }
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    setStatus("Connected")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    setStatus("Connection failed: ${exception?.message ?: "check broker settings"}")
                }
            })
        } catch (exception: Exception) {
            setStatus("Connection failed: ${exception.message ?: "check broker settings"}")
        }
    }

    fun disconnect(updateStatus: Boolean = true) {
        val client = mqttClient
        mqttClient = null
        try {
            if (client?.isConnected == true) client.disconnect()
            client?.close()
        } catch (_: Exception) {
            // A disconnect during an in-flight connection is harmless.
        }
        if (updateStatus) setStatus("Disconnected")
    }

    private fun setStatus(value: String) {
        mainHandler.post { status.value = value }
    }
}
