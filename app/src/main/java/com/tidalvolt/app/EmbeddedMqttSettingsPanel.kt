package com.tidalvolt.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmbeddedMqttSettingsPanel(card: Color, fg: Color, accent: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val brokerStatus by EmbeddedMqttBrokerManager.status
    val address by EmbeddedMqttBrokerManager.networkAddress
    val password by EmbeddedMqttBrokerManager.brokerPassword
    val telemetryStatus by MqttTelemetryManager.status

    Panel(card) {
        Text("Vehicle data connection", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("TidalVolt Local Receiver", color = accent, fontWeight = FontWeight.SemiBold)
        Text(
            "TidalVolt hosts its own password-protected receiver on this phone. No cloud broker or separate server account is needed.",
            color = fg.copy(alpha = .7f)
        )
        Spacer(Modifier.height(12.dp))
        Text("Receiver: $brokerStatus", color = accent, fontWeight = FontWeight.SemiBold)
        Text("Phone network address", color = fg.copy(alpha = .65f), fontSize = 12.sp)
        SelectionContainer { Text("$address", color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        Text("Port: 1883", color = fg)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                EmbeddedMqttBrokerManager.start(context)
            },
            enabled = brokerStatus == "Stopped" || brokerStatus.startsWith("Could not start"),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start local receiver") }
        OutlinedButton(
            onClick = {
                MqttTelemetryManager.disconnect()
                EmbeddedMqttBrokerManager.stop()
            },
            enabled = brokerStatus.startsWith("Running") || brokerStatus == "Starting…",
            modifier = Modifier.fillMaxWidth()
        ) { Text("Stop receiver") }

        if (brokerStatus.startsWith("Running")) {
            Spacer(Modifier.height(12.dp))
            Text("OverDrive connection details", color = fg, fontWeight = FontWeight.SemiBold)
            Text("Enter these in OverDrive → MQTT → Add Connection:", color = fg.copy(alpha = .7f), fontSize = 12.sp)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Broker URL: $address", color = fg)
                    Text("Port: 1883", color = fg)
                    Text("Username: ${EmbeddedMqttBrokerManager.username()}", color = fg)
                    Text("Password: $password", color = fg)
                    Text("Topic: overdrive/vehicle/telemetry", color = fg)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "In OverDrive, keep Home Assistant discovery OFF (Aggregate mode) and Allow vehicle control OFF. Connect the BYD head unit to your phone's Wi-Fi hotspot so it can reach this receiver.",
                color = fg.copy(alpha = .65f), fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    MqttTelemetryManager.connect(
                        "127.0.0.1", 1883, "overdrive/vehicle/telemetry",
                        EmbeddedMqttBrokerManager.username(), password
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Connect TidalVolt to its receiver") }
            Text("TidalVolt subscriber: $telemetryStatus", color = fg.copy(alpha = .75f), fontSize = 12.sp)
        }
        Text(
            "Security: the local receiver requires a generated password. Keep it private and only share it with OverDrive on your own vehicle. Vehicle commands are not enabled by TidalVolt.",
            color = fg.copy(alpha = .55f), fontSize = 11.sp
        )
    }
}
