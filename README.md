# TidalVolt

A phone-first EV companion for BYD vehicles, with a clean Tesla-inspired / Apple-like interface. OverDrive remains on the BYD head unit and acts as the telemetry collector; TidalVolt runs on the phone.

## Product direction

- Phone-only companion app; no Android Auto interface.
- Map feature removed completely, including map UI and location permissions.
- Minimal vehicle dashboard, charging, trip history, insights and settings.
- Local-first storage for telemetry samples and vehicle history.
- No MQTT broker subscription or cloud relay required for local data transfer.

## OverDrive connection

TidalVolt hosts a local WebSocket receiver on the phone (port 8765). Connect the phone and BYD head unit to the same Wi-Fi network or phone hotspot, start the receiver in More → OverDrive connection, and copy the displayed WebSocket address into the OverDrive sender configuration.

The receiver accepts either a flat JSON telemetry object or an object containing a nested telemetry object. The initial normalized fields include:

- soc
- ev_range_km
- speed
- power
- charge_power
- is_charging
- batt_temp
- odometer
- soh, soh_oem
- hv_pack_v
- cell_v_min, cell_v_max
- charging_eta_minutes

The phone generates a pairing token and includes it in the WebSocket URL. The sender must connect to the exact URL shown in TidalVolt. The receiver is read-only: it accepts telemetry and does not send vehicle-control commands.

### Important integration status

The phone receiver and JSON parser are implemented, but the OverDrive sender interface and exact outgoing payload have not yet been verified on the user's BYD head unit. Do not treat sample or unavailable fields as live readings. A live connection is confirmed only after the app receives and displays actual vehicle packets.

The receiver currently runs while the TidalVolt app process is alive. A foreground service / reconnect lifecycle is a later hardening step before relying on unattended collection.

## WhatsApp

- Manual vehicle-status sharing uses Android's share sheet and can be sent through the regular WhatsApp app.
- Manual daily-report sharing is included.
- Preferences are stored for charging started, charging completed, battery threshold, trip completed and daily report, with an editable battery threshold.
- Automatic WhatsApp delivery is not active yet. It requires a WhatsApp Business Platform integration, an approved message template for business-initiated messages, and secure configuration of the sender and recipient. The app must not claim an automatic WhatsApp message was delivered until the provider confirms success.

## Local history

Incoming telemetry samples are stored locally on the phone (up to 5,000 recent samples in the current implementation). The app will retain history while the vehicle is disconnected. Trip segmentation and automated charging-session lifecycle will be enabled after the incoming OverDrive fields and update cadence are validated.

## Build

Open this repository in Android Studio, or run the TidalVolt Android CI workflow from the Actions tab. Successful runs upload a TidalVolt-debug-apk artifact.
