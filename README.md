# TidalVolt

A clean Android companion app foundation for BYD EV owners, designed to expand to other EV brands.

## Current foundation
- Jetpack Compose Android app
- Pearl White and Obsidian appearance themes
- Vehicle overview with clearly labelled demo values
- Charging cost estimator using editable battery and electricity settings
- EV-versus-petrol savings estimator
- Nassau map screen foundation
- GitHub Actions workflow for debug APK builds

## Build
Open this repository in Android Studio, or run the **TidalVolt Android CI** workflow from the Actions tab. Successful runs upload a `TidalVolt-debug-apk` artifact.

## Live vehicle telemetry

### Built-in local receiver (recommended)
TidalVolt can host a password-protected MQTT broker directly on the phone. This removes the need for a cloud broker account or a separate always-on server.

1. Open TidalVolt → More → Vehicle data connection → **Start local receiver**.
2. Connect the BYD DiLink head unit to the phone's Wi-Fi hotspot. The phone and car must be on the same local network.
3. In OverDrive → MQTT → Add Connection, enter the phone network address, port `1883`, the generated username/password shown by TidalVolt, and topic `overdrive/vehicle/telemetry`.
4. Keep Home Assistant discovery OFF (Aggregate mode) and **Allow vehicle control OFF**.
5. In TidalVolt, tap **Connect TidalVolt to its receiver**.

The broker runs inside the TidalVolt app process and listens on the phone's network interfaces. Keep TidalVolt open during this initial test; a future foreground-service implementation is needed for robust background operation. The local MQTT connection is not TLS-encrypted, so use it only on a trusted private hotspot and do not share the generated password.

### Data handling
TidalVolt accepts OverDrive fields including SOC, EV range, speed, motor and charging power, charge state, battery temperature, odometer, SOH, HV pack voltage, cell-voltage extrema and charging ETA. Missing fields remain unavailable; OverDrive's estimated SOH is kept distinct from OEM-reported SOH.

The integration is read-only from TidalVolt's side: TidalVolt subscribes to telemetry and never publishes vehicle commands. The OverDrive control option must remain disabled.

## Compatibility and licensing
BYD Trip Stats compatibility must be confirmed on the target vehicle/head-unit firmware. Its source is licensed under BUSL-1.1; TidalVolt uses its documented MQTT data interface and does not copy or redistribute its source code.

## Important
Vehicle values are shown as unavailable until a real telemetry message is received. No sample BMS readings are presented as live.
