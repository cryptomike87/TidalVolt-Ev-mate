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
 * TidalVolt's normalized, read-only view of the BYD Trip Stats MQTT state payload.
 * Missing values stay null; they are never displayed as zero.
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
    val receivedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        private const val KM_TO_MILES = 0.621371

        fun fromJson(payload: String): EvTelemetry {
            val json = JSONObject(payload)
            fun number(key: String): Double? {
                if (!json.has(key) || json.isNull(key)) return null
                return json.optDouble(key).takeIf { it.isFinite() }
            }
            return EvTelemetry(
                soc = number("soc") ?: number("soc_panel"),
                rangeMiles = number("electric_driving_range_km")?.times(KM_TO_MILES),
                speedMph = number("speed")?.times(KM_TO_MILES),
                motorPowerKw = number("engine_power"),
                chargingPowerKw = number("charging_power"),
                isCharging = if (json.has("is_charging") && !json.isNull("is_charging")) json.optBoolean("is_charging") else null,
                latitude = number("location_latitude"),
                longitude = number("location_longitude"),
                batteryTempC = number("battery_pack_temp"),
                odometerMiles = number("odometer")?.times(KM_TO_MILES)
            )
        }
    }
}

object TelemetryStore {
    val latest = mutableStateOf<EvTelemetry?>(null)
}

/**
 * MQTT subscriber for the documented BYD Trip Stats state topic.
 * The car-side BYD Trip Stats app publishes telemetry; TidalVolt only subscribes.
 */
object MqttTelemetryManager {
    val status = mutableStateOf("Not connected")
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var mqttClient: MqttAsyncClient? = null

    fun connect(host: String, port: Int, topic: String, username: String, password: String) {
        disconnect(updateStatus = false)
        val cleanHost = host.trim().removePrefix("ssl://").removePrefix("tcp://").trimEnd('/')
        if (cleanHost.isBlank() || topic.isBlank() || port !in 1..65535) {
            setStatus("Invalid broker settings")
            return
        }
        val scheme = if (port == 8883 || port == 8884) "ssl://" else "tcp://"
        val uri = "$scheme$cleanHost:$port"
        setStatus("Connecting…")
        try {
            val client = MqttAsyncClient(uri, MqttClient.generateClientId(), MemoryPersistence())
            mqttClient = client
            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    setStatus("Connected")
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
                        mainHandler.post { TelemetryStore.latest.value = parsed }
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
                    // connectComplete performs the subscription for both initial and automatic reconnects.
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
