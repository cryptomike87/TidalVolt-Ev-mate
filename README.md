# TidalVolt

A clean Android companion app foundation for BYD EV owners, designed to expand to other EV brands.

## Current foundation
- Jetpack Compose Android app
- Pearl White and Obsidian appearance themes
- Vehicle overview with clearly labelled demo values
- Charging cost estimator using editable battery and electricity settings
- EV-versus-petrol savings estimator
- GitHub Actions workflow for debug APK builds

## Build
Open this repository in Android Studio, or run the **TidalVolt Android CI** workflow from the Actions tab. Successful runs upload a `TidalVolt-debug-apk` artifact.

## EV tools hub

The Tools tab now includes the first integrated feature surfaces for:
- Smart charging forecasts using editable battery capacity, target SOC, tariff and loss assumptions.
- Petrol-versus-EV ownership cost comparisons, including free workplace charging.
- Parking session timing and manually observed parked SOC change.
- Trip energy and estimated arrival SOC calculations.
- Battery diagnostics readiness with unavailable values clearly identified until telemetry supplies them.
- Indicative driving-efficiency scoring and a parked battery-drain calculator.
- Locally stored maintenance items.
- On-device EV guidance for charging, range, savings and battery questions.
- Local/Tailscale endpoint preference settings.
- Editable vehicle profile assumptions and share-sheet export.

These tools are deliberately honest about their data sources. Calculators and manually entered records work offline; they do not claim to be live telemetry. The assistant is currently a local guidance tool, not a cloud LLM. The connection screen saves endpoint preferences but does not yet implement HTTP polling or automatic local-to-Tailscale failover. Parking automation, background notifications, live route directions, weather integration, BMS fault retrieval and multi-vehicle telemetry adapters still require their respective platform/data integrations.

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

## Weather data source

TidalVolt will use [Open-Meteo](https://open-meteo.com/) for automatic weather context. Its forecast API requires no API key and provides current conditions and hourly forecasts. The hosted free tier is for non-commercial use (up to 10,000 calls/day); displayed data must include attribution to Open-Meteo under CC BY 4.0. Commercial distribution requires an appropriate Open-Meteo plan or a self-hosted deployment. The server source is AGPLv3.
