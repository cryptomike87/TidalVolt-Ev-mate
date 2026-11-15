package com.tidalvolt.app

import android.os.Bundle
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
                listOf("Vehicle", "Map", "Charge", "Savings", "More").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(when(item) { "Vehicle" -> Icons.Default.DirectionsCar; "Map" -> Icons.Default.Map; "Charge" -> Icons.Default.Bolt; "Savings" -> Icons.Default.Savings; else -> Icons.Default.Settings }, contentDescription = item) },
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
                    "Map" -> MapScreen(card, fg, accent)
                    "Charge" -> ChargeScreen(card, fg, accent)
                    "Savings" -> SavingsScreen(card, fg, accent)
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
    Panel(card) {
        Text("2026 BYD Yuan Pro", color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Vehicle overview • Sample configuration", color = fg.copy(alpha=.65f))
        Spacer(Modifier.height(20.dp))
        Text("72%", color = accent, fontSize = 54.sp, fontWeight = FontWeight.Bold)
        Text("Battery level • DEMO VALUE", color = fg.copy(alpha=.65f))
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { .72f }, modifier = Modifier.fillMaxWidth(), color = accent)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric("Estimated range", "176 mi", fg)
            Metric("Efficiency", "20 kWh/100 mi", fg)
        }
    }
    Spacer(Modifier.height(14.dp))
    Panel(card) {
        Text("Vehicle connection", color = fg, fontWeight = FontWeight.SemiBold)
        Text("Not connected. Dashboard figures are examples only; live BMS telemetry is not yet integrated.", color = fg.copy(alpha=.7f))
    }
}

@Composable
private fun Metric(label: String, value: String, fg: Color) {
    Column { Text(label, color = fg.copy(alpha=.6f), fontSize = 12.sp); Text(value, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}

@Composable
private fun MapScreen(card: Color, fg: Color, accent: Color) {
    Panel(card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = accent)
            Text("Nassau, The Bahamas", color = fg, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(210.dp).background(Color(0xFFE5E8E1), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF397D78), modifier = Modifier.size(42.dp))
                Text("Map integration coming next", color = Color(0xFF26302F), fontWeight = FontWeight.SemiBold)
                Text("Road-based range and charger data are not connected", color = Color(0xFF52605D), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Range preview will use your battery level, reserve and driving efficiency. No route reachability is claimed yet.", color = fg.copy(alpha=.7f))
    }
}

@Composable
private fun ChargeScreen(card: Color, fg: Color, accent: Color) {
    var current by remember { mutableStateOf("30") }
    var target by remember { mutableStateOf("90") }
    var rate by remember { mutableStateOf("0.55") }
    val from = current.toDoubleOrNull()?.coerceIn(0.0,100.0) ?: 0.0
    val to = target.toDoubleOrNull()?.coerceIn(from,100.0) ?: from
    val tariff = rate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val gridKwh = 45.1 * (to-from) / 100.0 / .95
    Panel(card) {
        Text("Charging estimator", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        Spacer(Modifier.height(12.dp))
        NumberField("Current battery %", current, { current=it }, fg)
        NumberField("Target battery %", target, { target=it }, fg)
        NumberField("Electricity price (BSD/kWh)", rate, { rate=it }, fg)
        Spacer(Modifier.height(14.dp))
        Text("Estimated grid energy: %.2f kWh".format(gridKwh), color = fg)
        Text("Estimated cost: BSD $%.2f".format(gridKwh*tariff), color = accent, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Based on 45.1 kWh battery and 95% charging efficiency.", color = fg.copy(alpha=.6f), fontSize = 11.sp)
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
private fun MoreScreen(card: Color, fg: Color, accent: Color, dark: Boolean, setDark: (Boolean)->Unit) {
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
