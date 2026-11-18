package com.tidalvolt.app

import android.os.Bundle
import android.content.Intent
import android.content.Context
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
                listOf("Vehicle", "Charging", "Trips", "Insights", "More").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(when(item) { "Vehicle" -> Icons.Default.DirectionsCar; "Charging" -> Icons.Default.Bolt; "Trips" -> Icons.Default.Route; "Insights" -> Icons.Default.QueryStats; else -> Icons.Default.Settings }, contentDescription = item) },
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
                    "Charging" -> ChargeScreen(card, fg, accent)
                    "Trips" -> TripsScreen(card, fg, accent)
                    "Insights" -> StatsScreen(card, fg, accent)
                    else -> MoreScreen(card, fg, accent, dark) { dark = it }
                }
            }
        }
    }
}

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
        Text(if (telemetry != null) "Battery level • Live vehicle telemetry" else "Battery level • Waiting for live vehicle data", color = fg.copy(alpha=.65f))
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
        Text(if (telemetry != null) "Receiving live vehicle data. Confirm readings against the vehicle display during initial testing." else "No live BYD telemetry has been received yet. Run the head-unit connection check in More.", color = fg.copy(alpha=.7f))
        Text("Connection: ${DirectBydAdbManager.status.value}", color = accent, fontSize = 12.sp)
    }
}

@Composable
private fun Metric(label: String, value: String, fg: Color) {
    Column { Text(label, color = fg.copy(alpha=.6f), fontSize = 12.sp); Text(value, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}


private data class ChargeRecord(
    val timestamp: Long,
    val from: Double,
    val to: Double,
    val kwh: Double,
    val cost: Double
)

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val connectionStatus by DirectBydAdbManager.status
    val diagnosticDetails by DirectBydAdbManager.details

    Panel(card) {
        Text("Direct BYD connection", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Text("TidalVolt is preparing a local ADB connection to the vehicle's own Android debugging service. No MQTT broker, OverDrive connection, cloud server or external phone is used.", color = fg.copy(alpha=.7f))
        Spacer(Modifier.height(10.dp))
        Text("Connection status: $connectionStatus", color = accent, fontWeight = FontWeight.SemiBold)
        if (diagnosticDetails.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(diagnosticDetails, color = fg.copy(alpha=.7f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { DirectBydAdbManager.runDiagnostics(context) },
            enabled = connectionStatus != "Checking local ADB…",
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (connectionStatus == "Checking local ADB…") "Checking…" else "Check head-unit connection") }
        Text("First connection may show an Android debugging authorization prompt. Select Always allow from this computer, then tap Allow. This phase runs read-only identity and firmware checks; it does not query or control vehicle systems.", color = fg.copy(alpha=.6f), fontSize = 11.sp)
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
        Text("About TidalVolt", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Text("Version 1.2.0 • Phone companion", color = fg.copy(alpha=.7f))
        Text("Designed for BYD vehicles first, with room to expand to other EV brands.", color = fg.copy(alpha=.7f))
    }
    Spacer(Modifier.height(12.dp))
    WhatsAppPanel(card, fg, accent)
}



@Composable
private fun WhatsAppPanel(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tidalvolt_whatsapp", Context.MODE_PRIVATE) }
    val telemetry by TelemetryStore.latest
    var chargingStart by remember { mutableStateOf(prefs.getBoolean("charging_start", true)) }
    var chargingComplete by remember { mutableStateOf(prefs.getBoolean("charging_complete", true)) }
    var batteryThreshold by remember { mutableStateOf(prefs.getBoolean("battery_threshold", true)) }
    var tripComplete by remember { mutableStateOf(prefs.getBoolean("trip_complete", true)) }
    var dailyReport by remember { mutableStateOf(prefs.getBoolean("daily_report", true)) }
    var threshold by remember { mutableStateOf(prefs.getString("threshold_percent", "20") ?: "20") }

    fun save(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share with WhatsApp"))
    }
    fun statusText(): String = buildString {
        appendLine("⚡ TidalVolt vehicle update")
        appendLine("BYD Yuan Pro")
        appendLine("Battery: " + (telemetry?.soc?.let { "%.0f%%".format(it) } ?: "Not available"))
        appendLine("Estimated range: " + (telemetry?.rangeMiles?.let { "%.0f mi".format(it) } ?: "Not available"))
        appendLine("Charging: " + when (telemetry?.isCharging) { true -> "Active"; false -> "Inactive"; null -> "Unknown" })
        appendLine("Charging power: " + (telemetry?.chargingPowerKw?.let { "%.1f kW".format(it) } ?: "Not available"))
        appendLine("Updated: " + SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date()))
        append("Shared from TidalVolt")
    }

    Panel(card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null, tint = accent)
            Spacer(Modifier.width(8.dp))
            Text("WhatsApp sharing", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        }
        Text("Send a clean vehicle status update or daily summary through WhatsApp's Android share sheet.", color = fg.copy(alpha=.7f))
        Spacer(Modifier.height(10.dp))
        Button(onClick = { share(statusText()) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Chat, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share vehicle status")
        }
        OutlinedButton(onClick = { share("📊 TidalVolt daily vehicle report\\n\\n" + statusText()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Share daily report")
        }
        Spacer(Modifier.height(12.dp))
        Text("Automatic event preferences", color = fg, fontWeight = FontWeight.SemiBold)
        Text("Choose which events you want TidalVolt to prepare for automatic delivery.", color = fg.copy(alpha=.65f), fontSize = 12.sp)
        WhatsAppToggle("Charging started", chargingStart) { chargingStart=it; save("charging_start",it) }
        WhatsAppToggle("Charging completed", chargingComplete) { chargingComplete=it; save("charging_complete",it) }
        WhatsAppToggle("Battery threshold reached", batteryThreshold) { batteryThreshold=it; save("battery_threshold",it) }
        WhatsAppToggle("Trip completed", tripComplete) { tripComplete=it; save("trip_complete",it) }
        WhatsAppToggle("Daily report", dailyReport) { dailyReport=it; save("daily_report",it) }
        OutlinedTextField(
            value=threshold,
            onValueChange={ value -> if (value.isEmpty() || value.toIntOrNull() in 1..100) { threshold=value; prefs.edit().putString("threshold_percent",value).apply() } },
            label={ Text("Battery alert threshold (%)") },
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
            singleLine=true,
            modifier=Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Manual sharing works with the regular WhatsApp app. Automatic WhatsApp delivery requires a WhatsApp Business Platform account, an approved message template and secure API configuration. These preferences are saved on this phone; automatic delivery is not active until that service is connected.", color=fg.copy(alpha=.65f), fontSize=11.sp)
    }
}

@Composable
private fun WhatsAppToggle(label: String, checked: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
        Text(label, fontSize=14.sp)
        Switch(checked=checked, onCheckedChange=onChange)
    }
}

@Composable
private fun TripsScreen(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("tidalvolt_trip_history", Context.MODE_PRIVATE) }
    val savedTrips = remember { prefs.getString("history", "").orEmpty() }
    Panel(card) {
        Text("Trip history", color=fg, fontSize=19.sp, fontWeight=FontWeight.SemiBold)
        Text("Completed trips will be stored on this phone when Overdrive telemetry is connected and trip detection is enabled.", color=fg.copy(alpha=.7f))
        Spacer(Modifier.height(10.dp))
        if (savedTrips.isBlank()) {
            Text("No trips recorded yet.", color=fg.copy(alpha=.65f))
        } else {
            Text(savedTrips, color=fg)
        }
        Text("Trip recording is not active until verified live odometer and vehicle-state data are available.", color=fg.copy(alpha=.6f), fontSize=11.sp)
    }
}
