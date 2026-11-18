package com.tidalvolt.app

import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/**
 * Normalized vehicle telemetry model. Missing fields remain null; estimated SOH stays distinct from OEM SOH.
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
}

