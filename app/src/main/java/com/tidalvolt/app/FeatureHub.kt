package com.tidalvolt.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun FeatureHubScreen(card: Color, fg: Color, accent: Color) {
    var selected by remember { mutableStateOf<String?>(null) }
    val features = listOf(
        "Smart charging" to "Charge targets, grid energy and cost forecasts",
        "Fuel vs EV savings" to "Monthly and annual ownership comparisons",
        "Parking intelligence" to "Parking timer and battery change observations",
        "Range & trip planner" to "Trip energy and estimated arrival charge",
        "Battery intelligence" to "BMS, battery health and diagnostic readiness",
        "Driving efficiency" to "Consumption score and regenerative energy",
        "Battery drain analyzer" to "Measure parked battery loss over time",
        "Maintenance manager" to "Service items, due dates and expenses",
        "AI EV assistant" to "On-device EV guidance and calculators",
        "Connectivity manager" to "Local and Tailscale endpoint settings",
        "Vehicle profiles & export" to "Vehicle assumptions, privacy and report export"
    )
    if (selected == null) {
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("EV tools",color=fg,fontSize=22.sp,fontWeight=FontWeight.Bold)
            Text("TidalVolt ownership toolkit. Live measurements remain unavailable until supplied by a verified vehicle data source.",color=fg.copy(alpha=.65f),fontSize=12.sp)
            features.forEach { feature ->
                Card(onClick={selected=feature.first},colors=CardDefaults.cardColors(containerColor=card),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(feature.first,color=fg,fontWeight=FontWeight.SemiBold)
                        Text(feature.second,color=fg.copy(alpha=.65f),fontSize=12.sp)
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            TextButton(onClick={selected=null}) { Icon(Icons.Default.ArrowBack,null); Text("All EV tools") }
            Text(selected!!,color=fg,fontSize=23.sp,fontWeight=FontWeight.Bold)
            when(selected) {
                "Smart charging" -> ChargeTool(card,fg,accent)
                "Fuel vs EV savings" -> SavingsTool(card,fg,accent)
                "Parking intelligence" -> ParkingTool(card,fg,accent)
                "Range & trip planner" -> TripTool(card,fg,accent)
                "Battery intelligence" -> BatteryTool(card,fg)
                "Driving efficiency" -> EfficiencyTool(card,fg,accent)
                "Battery drain analyzer" -> DrainTool(card,fg,accent)
                "Maintenance manager" -> MaintenanceTool(card,fg)
                "AI EV assistant" -> AssistantTool(card,fg)
                "Connectivity manager" -> ConnectionTool(card,fg,accent)
                else -> ProfileTool(card,fg)
            }
        }
    }
}

@Composable private fun ToolPanel(card:Color,content:@Composable ColumnScope.()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=card),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
    }
}
@Composable private fun ToolField(label:String,value:String,onValue:(String)->Unit,keyboard:KeyboardType=KeyboardType.Decimal) {
    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=keyboard),modifier=Modifier.fillMaxWidth())
}
@Composable private fun ToolMetric(label:String,value:String,fg:Color,accent:Color) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=fg.copy(alpha=.7f),fontSize=13.sp)
        Text(value,color=accent,fontWeight=FontWeight.Bold)
    }
}
private fun featurePrefs(c:Context)=c.getSharedPreferences("tidalvolt_feature_hub",Context.MODE_PRIVATE)

@Composable private fun ChargeTool(card:Color,fg:Color,accent:Color) {
    val p= LocalContext.current.let{remember{featurePrefs(it)}}
    var soc by remember{mutableStateOf(p.getString("charge_soc","42")?:"42")}
    var target by remember{mutableStateOf(p.getString("charge_target","80")?:"80")}
    var rate by remember{mutableStateOf(p.getString("charge_rate","0.55")?:"0.55")}
    var loss by remember{mutableStateOf(p.getString("charge_loss","5")?:"5")}
    val s=soc.toDoubleOrNull();val t=target.toDoubleOrNull();val r=rate.toDoubleOrNull();val l=loss.toDoubleOrNull()
    val k=if(s!=null&&t!=null&&l!=null&&l<100&&t>=s)45.1*(t-s)/100/(1-l/100) else null
    ToolPanel(card) {
        ToolField("Current battery (%)",soc){soc=it};ToolField("Target battery (%)",target){target=it}
        ToolField("Electricity rate (BSD/kWh)",rate){rate=it};ToolField("Estimated charging loss (%)",loss){loss=it}
        if(k!=null){ToolMetric("Grid energy","%.2f kWh".format(k),fg,accent);ToolMetric("Estimated cost",if(r!=null)"BSD $%.2f".format(k*r) else "Invalid rate",fg,accent);ToolMetric("Added estimated range","%.0f mi".format((t!!-s!!)*2.45),fg,accent)}
        Button(onClick={p.edit().putString("charge_soc",soc).putString("charge_target",target).putString("charge_rate",rate).putString("charge_loss",loss).apply()},modifier=Modifier.fillMaxWidth()){Text("Save charging assumptions")}
        Text("Uses the 45.1 kWh Yuan Pro pack and editable loss assumption. Actual energy and charge taper vary.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun SavingsTool(card:Color,fg:Color,accent:Color) {
    var miles by remember{mutableStateOf("500")};var petrol by remember{mutableStateOf("6.85")};var rate by remember{mutableStateOf("0.55")};var mpg by remember{mutableStateOf("15")};var eff by remember{mutableStateOf("20")};var free by remember{mutableStateOf("0")}
    val d=miles.toDoubleOrNull()?.coerceAtLeast(0.0)?:0.0;val g=petrol.toDoubleOrNull();val r=rate.toDoubleOrNull();val m=mpg.toDoubleOrNull();val e=eff.toDoubleOrNull();val f=free.toDoubleOrNull()?.coerceIn(0.0,d)?:0.0
    val ev=if(r!=null&&e!=null)(d-f)*e/100*r else null;val gas=if(g!=null&&m!=null&&m>0)d/m*g else null
    ToolPanel(card) {
        ToolField("Monthly miles",miles){miles=it};ToolField("Petrol price (BSD/gallon)",petrol){petrol=it};ToolField("Electricity price (BSD/kWh)",rate){rate=it}
        ToolField("EV consumption (kWh/100 mi)",eff){eff=it};ToolField("Petrol economy (MPG)",mpg){mpg=it};ToolField("Free charging miles/month",free){free=it}
        if(ev!=null&&gas!=null){ToolMetric("EV monthly cost","BSD $%.2f".format(ev),fg,accent);ToolMetric("Petrol monthly cost","BSD $%.2f".format(gas),fg,accent);ToolMetric("Monthly savings","BSD $%.2f".format(gas-ev),fg,accent);ToolMetric("Annual savings","BSD $%.2f".format((gas-ev)*12),fg,accent)}
        Text("Illustrative comparison. Adjust assumptions to match actual fuel economy, tariffs and free charging.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun ParkingTool(card:Color,fg:Color,accent:Color) {
    val p=LocalContext.current.let{remember{featurePrefs(it)}}
    var start by remember{mutableLongStateOf(p.getLong("parking_start",0L))}
    var before by remember{mutableStateOf(p.getString("parking_soc","")?:"")}
    var after by remember{mutableStateOf("")}
    var place by remember{mutableStateOf(p.getString("parking_place","")?:"")}
    ToolPanel(card) {
        ToolField("Parking location label",place){place=it}
        if(start==0L){
            ToolField("Battery at parking (%)",before){before=it}
            Button(onClick={start=System.currentTimeMillis();p.edit().putLong("parking_start",start).putString("parking_soc",before).putString("parking_place",place).apply()},enabled=(before.toDoubleOrNull()?.let { it in 0.0..100.0 } == true),modifier=Modifier.fillMaxWidth()){Text("Start parking timer")}
        }else{
            ToolMetric("Session elapsed","%d minutes".format((System.currentTimeMillis()-start)/60000),fg,accent)
            ToolField("Battery now (%)",after){after=it}
            Button(onClick={val loss=(before.toDoubleOrNull()?:0.0)-(after.toDoubleOrNull()?:0.0);p.edit().putLong("parking_last_minutes",(System.currentTimeMillis()-start)/60000).putFloat("parking_last_loss",loss.toFloat()).remove("parking_start").apply();start=0L},modifier=Modifier.fillMaxWidth()){Text("Finish parking session")}
        }
        Text("Manual start/stop is active. Automatic movement and background location detection need additional platform integration.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun TripTool(card:Color,fg:Color,accent:Color) {
    var distance by remember{mutableStateOf("25")};var soc by remember{mutableStateOf("80")};var eff by remember{mutableStateOf("20")}
    val mi=distance.toDoubleOrNull();val s=soc.toDoubleOrNull();val e=eff.toDoubleOrNull();val k=if(mi!=null&&e!=null)mi*e/100 else null;val arrival=if(k!=null&&s!=null)s-k/45.1*100 else null
    ToolPanel(card) {
        ToolField("Trip distance (miles)",distance){distance=it};ToolField("Starting battery (%)",soc){soc=it};ToolField("Expected consumption (kWh/100 mi)",eff){eff=it}
        if(k!=null&&arrival!=null){ToolMetric("Trip energy","%.2f kWh".format(k),fg,accent);ToolMetric("Estimated arrival battery","%.1f%%".format(arrival),fg,accent);Text(if(arrival<15)"Low arrival reserve — consider charging." else "Estimated arrival reserve is %.1f%%.".format(arrival),color=fg)}
        Text("Energy and arrival estimate only. Turn-by-turn routing, traffic and live charger availability are not yet connected.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun BatteryTool(card:Color,fg:Color) {
    ToolPanel(card) {
        Text("Battery diagnostic data will be populated from supported OverDrive/BMS telemetry.",color=fg)
        listOf("OEM State of Health","Estimated State of Health","Pack voltage","Battery temperature","Minimum / maximum cell voltage","Cell imbalance","BMS fault codes","Degradation history").forEach{Text("• "+it+": unavailable until received",color=fg)}
        Text("No diagnostic measurements or fault codes are fabricated. OEM and estimated health remain separate.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun EfficiencyTool(card:Color,fg:Color,accent:Color) {
    var consumption by remember{mutableStateOf("20")}
    val c=consumption.toDoubleOrNull();val score=if(c!=null&&c>0)(100-(c-14)*3).toInt().coerceIn(0,100) else null
    ToolPanel(card) {
        ToolField("Average consumption (kWh/100 mi)",consumption){consumption=it}
        if(score!=null){ToolMetric("Indicative efficiency score","$score / 100",fg,accent);ToolMetric("Consumption rating",when{c!!<=16->"Excellent";c<=20.0->"Good";c<=24.0->"Moderate";else->"High"},fg,accent)}
        Text("Indicative comparison score, not a measured driver grade. Live trip and regenerative-braking data depend on telemetry support.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun DrainTool(card:Color,fg:Color,accent:Color) {
    var before by remember{mutableStateOf("80")};var after by remember{mutableStateOf("78")};var hours by remember{mutableStateOf("12")}
    val loss=(before.toDoubleOrNull()?:0.0)-(after.toDoubleOrNull()?:0.0);val h=hours.toDoubleOrNull()
    ToolPanel(card) {
        ToolField("Battery before parking (%)",before){before=it};ToolField("Battery after parking (%)",after){after=it};ToolField("Elapsed hours",hours){hours=it}
        if(h!=null&&h>0&&loss>=0){ToolMetric("Battery loss","%.2f percentage points".format(loss),fg,accent);ToolMetric("Loss per hour","%.3f%%/hr".format(loss/h),fg,accent);ToolMetric("Estimated energy","%.2f kWh".format(45.1*loss/100),fg,accent)}
        Text("Observation-based estimate; temperature, balancing and SOC rounding can affect the result.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun MaintenanceTool(card:Color,fg:Color) {
    val p=LocalContext.current.let{remember{featurePrefs(it)}}
    var item by remember{mutableStateOf("")};var due by remember{mutableStateOf("")};var cost by remember{mutableStateOf("")}
    val items=remember{mutableStateListOf<String>().apply{p.getString("maintenance_items","").orEmpty().lines().filter{it.isNotBlank()}.forEach{add(it)}}}
    ToolPanel(card) {
        ToolField("Maintenance item",item){item=it};ToolField("Due date / odometer",due){due=it};ToolField("Cost (BSD)",cost){cost=it}
        Button(onClick={if(item.isNotBlank()){items.add(item+" | Due: "+due+" | BSD "+cost);p.edit().putString("maintenance_items",items.joinToString("\n")).apply();item="";due="";cost=""}},enabled=item.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Add maintenance item")}
        items.forEachIndexed{index,value->Row(verticalAlignment=Alignment.CenterVertically){Text(value,color=fg,modifier=Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={items.removeAt(index);p.edit().putString("maintenance_items",items.joinToString("\n")).apply()}){Text("Done")}}}
    }
}
@Composable private fun AssistantTool(card:Color,fg:Color) {
    var q by remember{mutableStateOf("")};var answer by remember{mutableStateOf("Ask about charging, range, savings, battery information or parking drain. This initial assistant runs locally and does not send data to an AI service.")}
    ToolPanel(card) {
        OutlinedTextField(value=q,onValueChange={q=it},label={Text("Ask TidalVolt")},modifier=Modifier.fillMaxWidth(),minLines=2)
        Button(onClick={val x=q.lowercase();answer=when{ "charge" in x||"cost" in x->"Open Smart charging and enter your battery level, target and BSD/kWh tariff. The estimate uses your 45.1 kWh battery profile."; "range" in x||"trip" in x->"Open Range & trip planner and enter trip distance, starting SOC and expected consumption."; "saving" in x||"petrol" in x->"Open Fuel vs EV savings and enter mileage, petrol price, electricity tariff and free charging."; "battery" in x||"health" in x->"Open Battery intelligence. Only readings received from supported vehicle telemetry can be shown.";else->"I can guide you through charging, range, savings, battery information and parking. Live AI requires a secure AI service connection."}},enabled=q.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Ask")}
        Text(answer,color=fg)
        Text("Local guidance mode — no external AI request is made.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun ConnectionTool(card:Color,fg:Color,accent:Color) {
    val p=LocalContext.current.let{remember{featurePrefs(it)}}
    var local by remember{mutableStateOf(p.getString("local_endpoint","")?:"")}
    var remote by remember{mutableStateOf(p.getString("tailscale_endpoint","100.110.254.9")?:"100.110.254.9")}
    var port by remember{mutableStateOf(p.getString("tailscale_port","8080")?:"8080")}
    var mode by remember{mutableStateOf(p.getString("connection_mode","AUTO")?:"AUTO")}
    ToolPanel(card) {
        ToolField("Local OverDrive host",local){local=it};ToolField("Tailscale IP / hostname",remote){remote=it};ToolField("Proxy port",port){port=it}
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("AUTO","LOCAL","TAILSCALE").forEach{FilterChip(selected=mode==it,onClick={mode=it},label={Text(it)})}}
        Button(onClick={p.edit().putString("local_endpoint",local).putString("tailscale_endpoint",remote).putString("tailscale_port",port).putString("connection_mode",mode).apply()},modifier=Modifier.fillMaxWidth()){Text("Save connection settings")}
        Text("Remote URL: http://"+remote+":"+port,color=accent,fontWeight=FontWeight.SemiBold)
        Text("This saves endpoint preferences. Automatic failover and HTTP proxy polling are not yet connected to the telemetry transport. Tailscale must be active on both devices. Keep OverDrive vehicle control disabled.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
@Composable private fun ProfileTool(card:Color,fg:Color) {
    val context=LocalContext.current;val p=remember{featurePrefs(context)}
    var vehicle by remember{mutableStateOf(p.getString("vehicle_name","2026 BYD Yuan Pro")?:"2026 BYD Yuan Pro")}
    var capacity by remember{mutableStateOf(p.getString("vehicle_capacity","45.1")?:"45.1")}
    var range by remember{mutableStateOf(p.getString("vehicle_range","245")?:"245")}
    var eff by remember{mutableStateOf(p.getString("vehicle_eff","20")?:"20")}
    var shareLocation by remember{mutableStateOf(p.getBoolean("share_location",false))}
    ToolPanel(card) {
        ToolField("Vehicle name",vehicle){vehicle=it};ToolField("Battery capacity (kWh)",capacity){capacity=it};ToolField("Reference full range (mi)",range){range=it};ToolField("Reference consumption (kWh/100 mi)",eff){eff=it}
        Button(onClick={p.edit().putString("vehicle_name",vehicle).putString("vehicle_capacity",capacity).putString("vehicle_range",range).putString("vehicle_eff",eff).apply()},modifier=Modifier.fillMaxWidth()){Text("Save vehicle profile")}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Include location in reports",color=fg,modifier=Modifier.weight(1f));Switch(checked=shareLocation,onCheckedChange={shareLocation=it;p.edit().putBoolean("share_location",it).apply()})}
        OutlinedButton(onClick={val report="TidalVolt vehicle profile\nVehicle: "+vehicle+"\nBattery: "+capacity+" kWh\nReference range: "+range+" mi\nConsumption: "+eff+" kWh/100 mi";val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"TidalVolt vehicle profile").putExtra(Intent.EXTRA_TEXT,report);context.startActivity(Intent.createChooser(intent,"Export TidalVolt data"))},modifier=Modifier.fillMaxWidth()){Text("Export vehicle profile")}
        Text("These are editable assumptions, not live vehicle readings. Location sharing is off by default.",color=fg.copy(alpha=.6f),fontSize=11.sp)
    }
}
