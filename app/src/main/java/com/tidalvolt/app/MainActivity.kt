package com.tidalvolt.app

import android.os.Bundle
import android.location.LocationManager
import android.widget.Toast
import android.content.Context
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

private val Pearl = Color(0xFFF7F6F2)
private val PearlCard = Color(0xFFFFFDF8)
private val Ink = Color(0xFF26302F)
private val Teal = Color(0xFF397D78)
private val Obsidian = Color(0xFF171614)
private val DarkCard = Color(0xFF211F1B)
private val Sage = Color(0xFFA8B99A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TidalVoltApp() }
    }
}

@Composable
fun TidalVoltApp() {
    var dark by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Vehicle") }
    val bg = if (dark) Obsidian else Pearl
    val card = if (dark) DarkCard else PearlCard
    val fg = if (dark) Color(0xFFE7E0D4) else Ink
    val accent = if (dark) Sage else Teal

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = accent, background = bg, surface = card, onSurface = fg) else lightColorScheme(primary = accent, background = bg, surface = card, onSurface = fg)) {
        Scaffold(containerColor = bg, bottomBar = {
            NavigationBar(containerColor = card) {
                listOf("Vehicle", "Energy", "Stats", "Tools", "More").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(when(item) { "Vehicle" -> Icons.Default.DirectionsCar; "Energy" -> Icons.Default.Bolt; "Stats" -> Icons.Default.QueryStats; "Tools" -> Icons.Default.Build; else -> Icons.Default.Settings }, contentDescription = item) },
                        label = { Text(item) })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("TIDALVOLT", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(tab, color = fg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                when(tab) {
                    "Vehicle" -> VehicleScreen(card, fg, accent)
                    "Energy" -> ChargeScreen(card, fg, accent)
                    "Stats" -> StatsScreen(card, fg, accent)
                    "Tools" -> FeatureHubScreen(card, fg, accent)
                    else -> MoreScreen(card, fg, accent, dark) { dark = it }
                }
            }
        }
    }
}

private data class ChargeRecord(
    val timestamp: Long,
    val from: Double,
    val to: Double,
    val kwh: Double,
    val cost: Double
)

@Composable
private fun Panel(card: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun VehicleScreen(card: Color, fg: Color, accent: Color) {
    val telemetry by TelemetryStore.latest
    Panel(card) {
        Text("2026 BYD Yuan Pro", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Live vehicle overview", color = fg.copy(alpha=.65f))
        Spacer(Modifier.height(20.dp))
        Text(telemetry?.soc?.let { "%.0f%%".format(it) } ?: "—", color = accent, fontSize = 54.sp, fontWeight = FontWeight.Bold)
        Text(if (telemetry != null) "Battery level • Live OverDrive telemetry" else "Battery level • Waiting for live vehicle data", color = fg.copy(alpha=.65f))
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { ((telemetry?.soc ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = accent)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Estimated range", telemetry?.rangeMiles?.let { "%.0f mi".format(it) } ?: "Unavailable", fg)
            Metric("Speed", telemetry?.speedMph?.let { "%.0f mph".format(it) } ?: "Unavailable", fg)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Motor power", telemetry?.motorPowerKw?.let { "%.1f kW".format(it) } ?: "Unavailable", fg)
            Metric("Charging", if (telemetry?.isCharging == true) "Active" else if (telemetry?.isCharging == false) "Inactive" else "Unknown", fg)
        }
        telemetry?.batteryTempC?.let {
            Spacer(Modifier.height(8.dp))
            Metric("Battery temperature", "%.1f °C".format(it), fg)
        }
        telemetry?.let { data ->
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Battery health", data.batterySohOem?.let { "%.1f%% OEM".format(it) }
                    ?: data.batterySoh?.let { "%.1f%% estimated".format(it) } ?: "Unavailable", fg)
                Metric("Pack voltage", data.packVoltageV?.let { "%.1f V".format(it) } ?: "Unavailable", fg)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("Charging power", data.chargingPowerKw?.let { "%.1f kW".format(it) } ?: "Unavailable", fg)
                Metric("Odometer", data.odometerMiles?.let { "%.0f mi".format(it) } ?: "Unavailable", fg)
            }
            if (data.cellVoltageMinV != null || data.cellVoltageMaxV != null) {
                Spacer(Modifier.height(8.dp))
                Metric("Cell voltage min / max",
                    "${data.cellVoltageMinV?.let { "%.3f".format(it) } ?: "—"} / ${data.cellVoltageMaxV?.let { "%.3f".format(it) } ?: "—"} V", fg)
            }
            if (data.chargingEtaMinutes != null) {
                Spacer(Modifier.height(8.dp))
                Metric("Estimated time to full", "%.0f min".format(data.chargingEtaMinutes), fg)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel(card) {
        Text("Vehicle connection", color = fg, fontWeight = FontWeight.SemiBold)
        Text(if (telemetry != null) "Receiving live vehicle data from OverDrive. Confirm readings against the vehicle display during initial testing." else "Direct BYD head-unit connection is being prepared. No external receiver is required for the planned approach.", color = fg.copy(alpha=.7f))
        Text("Connection: ${MqttTelemetryManager.status.value}", color = accent, fontSize = 12.sp)
    }
}

@Composable
private fun Metric(label: String, value: String, fg: Color) {
    Column { Text(label, color = fg.copy(alpha=.6f), fontSize = 12.sp); Text(value, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}

@Composable
private fun ChargeScreen(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tidalvolt_charge_history", Context.MODE_PRIVATE) }
    var current by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("90") }
    var rate by remember { mutableStateOf("0.55") }
    var distanceSinceCharge by remember { mutableStateOf(prefs.getFloat("distance_since_charge", 0f).toString()) }
    var petrolPrice by remember { mutableStateOf(prefs.getString("petrol_price", "6.85") ?: "6.85") }
    var sessionStart by remember { mutableStateOf(prefs.getLong("active_start_time", 0L)) }
    var sessionFrom by remember { mutableStateOf(prefs.getFloat("active_start_soc", -1f).toDouble()) }
    val history = remember {
        mutableStateListOf<ChargeRecord>().apply {
            prefs.getString("history", "").orEmpty().lines().filter { it.isNotBlank() }.forEach { line ->
                val p = line.split("|")
                if (p.size == 5) runCatching {
                    add(ChargeRecord(p[0].toLong(), p[1].toDouble(), p[2].toDouble(), p[3].toDouble(), p[4].toDouble()))
                }
            }
        }
    }
    val from = current.toDoubleOrNull()?.coerceIn(0.0,100.0)
    val to = target.toDoubleOrNull()?.coerceIn(0.0,100.0)
    val tariff = rate.toDoubleOrNull()?.coerceAtLeast(0.0)
    val gridKwh = if (from != null && to != null && to >= from) 45.1 * (to-from) / 100.0 / .95 else null
    val estimate = if (gridKwh != null && tariff != null) gridKwh * tariff else null
    Panel(card) {
        Text("Charging estimator", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Text("Update your current battery level to recalculate the estimate.", color = fg.copy(alpha=.65f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        NumberField("Current battery %", current, { current=it }, fg)
        NumberField("Target battery %", target, { target=it }, fg)
        NumberField("Electricity price (BSD/kWh)", rate, { rate=it }, fg)
        Spacer(Modifier.height(14.dp))
        if (gridKwh != null && estimate != null) {
            Text("Estimated grid energy: %.2f kWh".format(gridKwh), color = fg)
            Text("Estimated cost: BSD $%.2f".format(estimate), color = accent, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("Enter a valid current battery level and a target at or above it to see your estimate.", color = fg.copy(alpha=.7f))
        }
        Text("Calculation uses the 45.1 kWh battery capacity and an estimated 95% charging efficiency. Adjust the electricity rate if needed.", color = fg.copy(alpha=.6f), fontSize = 11.sp)
    }
    Spacer(Modifier.height(14.dp))
    Panel(card) {
        Text("Charging session", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        if (sessionStart == 0L) {
            Text("Start a session when you plug in. TidalVolt will save the session when you finish it.", color = fg.copy(alpha=.7f))
            Button(
                onClick = {
                    val soc = current.toDoubleOrNull()?.coerceIn(0.0,100.0)
                    if (soc != null) {
                        sessionFrom = soc
                        sessionStart = System.currentTimeMillis()
                        prefs.edit().putLong("active_start_time", sessionStart).putFloat("active_start_soc", soc.toFloat()).apply()
                    }
                },
                enabled = from != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start charging session") }
        } else {
            Text("Session in progress • Started at %.0f%%".format(sessionFrom), color = accent, fontWeight = FontWeight.SemiBold)
            Text("Enter the latest battery level above, then finish the session to save it.", color = fg.copy(alpha=.7f))
            Button(
                onClick = {
                    val endSoc = current.toDoubleOrNull()?.coerceIn(sessionFrom,100.0)
                    if (endSoc != null) {
                        val energy = 45.1 * (endSoc-sessionFrom) / 100.0 / .95
                        val cost = energy * (rate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0)
                        val record = ChargeRecord(sessionStart, sessionFrom, endSoc, energy, cost)
                        history.add(0, record)
                        val serialized = history.take(50).joinToString("\n") { "${it.timestamp}|${it.from}|${it.to}|${it.kwh}|${it.cost}" }
                        prefs.edit().putString("history", serialized).putFloat("distance_since_charge", 0f).remove("active_start_time").remove("active_start_soc").apply()
                        distanceSinceCharge = "0"
                        sessionStart = 0L
                        sessionFrom = -1.0
                    }
                },
                enabled = from != null && from >= sessionFrom,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Finish and save session") }
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel(card) {
        Text("Savings since last charge", color=fg, fontWeight=FontWeight.SemiBold, fontSize=19.sp)
        Text("Enter the miles driven since your previous charge. This counter resets when you finish and save a charging session.", color=fg.copy(alpha=.65f), fontSize=12.sp)
        NumberField("Miles driven since last charge", distanceSinceCharge, {
            distanceSinceCharge = it
            it.toFloatOrNull()?.coerceAtLeast(0f)?.let { value -> prefs.edit().putFloat("distance_since_charge", value).apply() }
        }, fg)
        NumberField("Petrol price (BSD/gallon)", petrolPrice, {
            petrolPrice = it
            prefs.edit().putString("petrol_price", it).apply()
        }, fg)
        val miles = distanceSinceCharge.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val gas = petrolPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val evCost = miles * 20.0 / 100.0 * (rate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.55)
        val petrolCost = miles / 15.0 * gas
        Spacer(Modifier.height(8.dp))
        Metric("Estimated EV cost", "BSD $%.2f".format(evCost), fg)
        Metric("Estimated petrol cost", "BSD $%.2f".format(petrolCost), fg)
        Text("Estimated savings: BSD $%.2f".format(petrolCost - evCost), color=accent, fontSize=24.sp, fontWeight=FontWeight.Bold)
        Text("Estimate uses 20 kWh/100 mi, 15 MPG and your electricity and petrol prices. Mileage is currently entered manually; automatic odometer tracking is not connected.", color=fg.copy(alpha=.6f), fontSize=11.sp)
    }
    Spacer(Modifier.height(14.dp))
    Panel(card) {
        Text("Charge history", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        if (history.isEmpty()) {
            Text("No saved charging sessions yet. Completed sessions will appear here.", color = fg.copy(alpha=.7f))
        } else {
            history.take(20).forEach { item ->
                val date = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(item.timestamp))
                Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                    Text(date, color = fg.copy(alpha=.65f), fontSize = 12.sp)
                    Text("%.0f%% → %.0f%%  •  %.2f kWh".format(item.from, item.to, item.kwh), color = fg, fontWeight = FontWeight.SemiBold)
                    Text("Estimated cost: BSD $%.2f".format(item.cost), color = accent)
                }
                HorizontalDivider()
            }
            Text("History is stored on this phone. Sessions are currently started and finished in the app; automatic detection requires live vehicle or charger integration.", color = fg.copy(alpha=.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, change: (String)->Unit, fg: Color) {
    OutlinedTextField(value=value, onValueChange=change, label={Text(label)}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal), singleLine=true, modifier=Modifier.fillMaxWidth())
}

@Composable
private fun SavingsScreen(card: Color, fg: Color, accent: Color) {
    var miles by remember { mutableStateOf("500") }
    var petrol by remember { mutableStateOf("6.85") }
    val distance = miles.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val fuel = petrol.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val evCost = distance * 20.0 / 100.0 * .55
    val gasCost = distance / 15.0 * fuel
    Panel(card) {
        Text("EV vs petrol savings", color = fg, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        NumberField("Distance per month (miles)", miles, { miles=it }, fg)
        NumberField("Petrol price (BSD/gallon)", petrol, { petrol=it }, fg)
        Spacer(Modifier.height(12.dp))
        Metric("Estimated EV energy cost", "BSD $%.2f".format(evCost), fg)
        Spacer(Modifier.height(8.dp))
        Metric("Estimated petrol cost", "BSD $%.2f".format(gasCost), fg)
        Text("Estimated monthly difference: BSD $%.2f".format(max(0.0,gasCost-evCost)), color=accent, fontSize=24.sp, fontWeight=FontWeight.Bold)
        Text("Illustrative assumptions: 20 kWh/100 mi, BSD $0.55/kWh, 15 MPG. Edit assumptions in a future settings update.", color=fg.copy(alpha=.6f), fontSize=11.sp)
    }
}

@Composable
private fun StatsScreen(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tidalvolt_stats", Context.MODE_PRIVATE) }
    val labels = listOf("Driving", "Parked", "At work", "At home", "At street lights")
    Panel(card) {
        Text("Vehicle activity", color=fg, fontSize=19.sp, fontWeight=FontWeight.SemiBold)
        Text("Daily time summaries", color=fg.copy(alpha=.65f), fontSize=12.sp)
        Spacer(Modifier.height(10.dp))
        labels.forEach { label ->
            val mins = prefs.getLong("minutes_" + label, 0L)
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                Text(label, color=fg)
                Text("%dh %02dm".format(mins/60, mins%60), color=accent, fontWeight=FontWeight.Bold)
            }
            HorizontalDivider()
        }
        Text("These totals are not live yet. Automatic classification requires location permission, geofences for work/home, and activity recognition. Time stopped at traffic lights can only be estimated.", color=fg.copy(alpha=.65f), fontSize=11.sp)
    }
    Spacer(Modifier.height(12.dp))
    Panel(card) {
        Text("Tracking requirements", color=fg, fontWeight=FontWeight.SemiBold)
        Text("Work and home time need saved locations. Driving and parked time need motion recognition. Red-light time needs a stop-detection model. TidalVolt will not invent activity totals.", color=fg.copy(alpha=.7f))
    }
}
@Composable
private fun MoreScreen(card: Color, fg: Color, accent: Color, dark: Boolean, setDark: (Boolean)->Unit) {
    Panel(card) {
        Text("Vehicle data connection", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Text("Direct BYD head-unit access is the current development priority. The MQTT receiver setup is temporarily hidden while we test the vehicle's built-in data services.", color = fg.copy(alpha=.7f))
    }
    Spacer(Modifier.height(12.dp))
    Panel(card) {
        Text("Appearance", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if(dark) "Obsidian Black" else "Pearl White", color = fg)
            Switch(checked=dark, onCheckedChange=setDark)
        }
        Text("Choose the look that feels comfortable. Theme persistence will be added in a later build.", color = fg.copy(alpha=.65f))
    }
    Spacer(Modifier.height(12.dp))
    Panel(card) {
        Text("About TidalVolt", color = fg, fontWeight = FontWeight.SemiBold)
        Text("Version 1.0.0 • Android foundation", color = fg.copy(alpha=.7f))
        Text("Designed for BYD vehicles first, with room to expand to other EV brands.", color = fg.copy(alpha=.7f))
    }
}

@Composable
private fun MqttSettingsPanel(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tidalvolt_mqtt", Context.MODE_PRIVATE) }
    var host by remember { mutableStateOf(prefs.getString("host", "") ?: "") }
    var port by remember { mutableStateOf(prefs.getString("port", "8883") ?: "8883") }
    var topic by remember { mutableStateOf(prefs.getString("topic", "overdrive/vehicle/telemetry") ?: "overdrive/vehicle/telemetry") }
    var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("password", "") ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    val connectionStatus by MqttTelemetryManager.status

    Panel(card) {
        Text("Integrations", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("OverDrive / BYD Trip Stats • MQTT telemetry", color = accent, fontWeight = FontWeight.SemiBold)
        Text("Connect TidalVolt to OverDrive's live vehicle telemetry or BYD Trip Stats. TidalVolt only subscribes; it never sends vehicle commands.", color = fg.copy(alpha=.7f))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("MQTT broker hostname") }, placeholder = { Text("Broker hostname or Tailscale IP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, label = { Text("Broker port") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Telemetry topic") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Broker username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Broker password") }, singleLine = true, visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") } }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("OVERDRIVE SETUP: Open OverDrive → MQTT → Add Connection. Use this same broker, credentials and topic: overdrive/vehicle/telemetry. Select Aggregate mode by leaving Home Assistant discovery OFF, and keep Allow vehicle control OFF. Set the publish interval to 5–10 seconds while driving and a slower heartbeat when parked. Enable OverDrive's Tailscale proxy if the broker is reachable through your tailnet. Port 8883 uses TLS; 1883 is unencrypted.", color = fg.copy(alpha=.6f), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                prefs.edit().putString("host", host.trim()).putString("port", port.trim()).putString("topic", topic.trim()).putString("username", username.trim()).putString("password", password).apply()
                MqttTelemetryManager.connect(host.trim(), port.toIntOrNull() ?: 8883, topic.trim(), username.trim(), password)
            },
            enabled = host.isNotBlank() && topic.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save and connect") }
        OutlinedButton(onClick = { MqttTelemetryManager.disconnect() }, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
        Text("Status: $connectionStatus", color = if (connectionStatus == "Connected") accent else fg.copy(alpha=.7f), fontWeight = FontWeight.SemiBold)
        Text("For BYD Trip Stats, select its state topic instead (for example byd-trip-stats/yuanpro/state). Live telemetry is not shown until a valid JSON message arrives. Credentials are stored locally on this device. Keep OverDrive vehicle-control publishing disabled; TidalVolt is read-only.", color = fg.copy(alpha=.55f), fontSize = 11.sp)
    }
}

private val tidalVoltMapPage = """
<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<style>html,body,#map{height:100%;width:100%;margin:0;padding:0;background:#e8eee8}.leaflet-control-attribution{font-size:9px}</style></head><body><div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script><script>
const map=L.map("map",{zoomControl:false}).setView([25.055,-77.35],12);
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",{maxZoom:19,attribution:"© OpenStreetMap contributors"}).addTo(map);
L.control.zoom({position:"bottomright"}).addTo(map);
const chargerLayer=L.layerGroup().addTo(map), rangeLayer=L.layerGroup();
function addCharger(lat,lon,name,info){L.marker([lat,lon]).addTo(chargerLayer).bindPopup("<b>"+name+"</b><br>"+info);}
fetch("https://overpass-api.de/api/interpreter?data="+encodeURIComponent('[out:json][timeout:20];(node["amenity"="charging_station"](24.95,-77.58,25.13,-77.18);way["amenity"="charging_station"](24.95,-77.58,25.13,-77.18););out center;'))
.then(r=>r.json()).then(d=>d.elements.forEach(e=>{let lat=e.lat||e.center?.lat,lon=e.lon||e.center?.lon;if(lat&&lon){let t=e.tags||{},s=Object.keys(t).filter(k=>k.startsWith("socket:")).map(k=>k.substring(7)+": "+t[k]).join(", ");addCharger(lat,lon,t.name||"EV charging station",s||"Connector details not listed");}})).catch(()=>{});
window.centerOn=function(lat,lon){map.setView([lat,lon],15);L.circleMarker([lat,lon],{radius:8,color:"#ffffff",weight:3,fillColor:"#16877e",fillOpacity:1}).addTo(map).bindPopup("Your current location").openPopup();};
window.setLayer=function(layer){
if(layer==="chargers"){if(map.hasLayer(chargerLayer))map.removeLayer(chargerLayer);else map.addLayer(chargerLayer);}
if(layer==="range"){if(map.hasLayer(rangeLayer))map.removeLayer(rangeLayer);else{L.circle([25.055,-77.35],{radius:12000,color:"#16877e",fillColor:"#39b9aa",fillOpacity:.12}).addTo(rangeLayer);map.addLayer(rangeLayer);}}
if(layer==="weather")alert("Live weather overlay is not connected yet.");
if(layer==="trip")alert("TidalVolt route planning is not connected yet.");
};
</script></body></html>
"""
