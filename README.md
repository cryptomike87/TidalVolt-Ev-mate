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

TidalVolt supports two read-only MQTT sources:

### Recommended: OverDrive
OverDrive reads vehicle telemetry through the BYD SDK on the DiLink head unit and publishes a full JSON snapshot to MQTT. On the vehicle, open **OverDrive → MQTT → Add Connection** and configure:

- Broker: the same broker hostname/IP, port and credentials entered in TidalVolt.
- Topic: `overdrive/vehicle/telemetry` (or the custom topic you choose).
- **Home Assistant discovery: OFF** — this selects Aggregate mode and publishes the complete JSON snapshot to one topic. Home Assistant mode publishes individual field topics instead.
- **Allow vehicle control: OFF** — TidalVolt is a telemetry viewer only.
- Publish interval: 5–10 seconds while driving; use a slower parked heartbeat to limit data usage.

In TidalVolt, open **More → Integrations**, enter the same broker settings and subscribe to `overdrive/vehicle/telemetry`. Port 8883 uses TLS; port 1883 is unencrypted. The host field also accepts an `ssl://` prefix when using TLS.

For a private broker reachable over Tailscale, enable the **Tailscale proxy** in OverDrive and enter the broker's Tailscale IP or MagicDNS hostname in both apps. The broker itself must be running and reachable from the tailnet; Tailscale is the private network path, not an MQTT broker.

### Alternative: BYD Trip Stats
Install and configure BYD Trip Stats on a compatible BYD DiLink head unit, enable its MQTT publisher, and point both apps at the same broker. Its example state topic is `byd-trip-stats/yuanpro/state`; set the device friendly name to `yuanpro` or enter the actual topic in TidalVolt.

### Data handling
TidalVolt accepts OverDrive fields including SOC, EV range, speed, motor and charging power, charge state, battery temperature, odometer, SOH, HV pack voltage, cell-voltage extrema and charging ETA. BYD Trip Stats field names are also supported. Kilometres and km/h are converted to miles and mph for display. Missing fields remain unavailable; OverDrive's estimated SOH is kept distinct from OEM-reported SOH.

The integration is read-only: TidalVolt subscribes to telemetry and never publishes vehicle commands. MQTT broker hosting and network access are separate from the free MQTT protocol/client libraries.

## Compatibility and licensing
BYD Trip Stats compatibility must be confirmed on the target vehicle/head-unit firmware. Its source is licensed under BUSL-1.1; TidalVolt uses its documented MQTT data interface and does not copy or redistribute its source code.

## Important
Vehicle values are shown as unavailable until a real telemetry message is received. No sample BMS readings are presented as live.
