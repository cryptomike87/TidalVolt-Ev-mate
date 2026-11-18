# TidalVolt

A phone-first EV companion for BYD vehicles, with a clean Tesla-inspired / Apple-like interface. OverDrive remains on the BYD head unit and acts as the telemetry collector; TidalVolt runs on the phone.

## Product direction

- Phone-only companion app; no Android Auto interface.
- Map feature removed completely, including map UI and location permissions.
- Minimal vehicle dashboard, charging, trip history, insights and settings.
- Local-first storage for telemetry samples and vehicle history.
- No MQTT broker subscription or cloud relay required for local data transfer.

## OverDrive connection

TidalVolt hosts a token-protected WebSocket receiver on the phone (port 8765). For remote access across different networks, install Tailscale on both the phone and BYD head unit, sign both devices into the same Tailscale account/tailnet, and leave Tailscale connected. Start the receiver in More → OverDrive connection. When Tailscale is active on the phone, TidalVolt detects its 100.64.0.0/10 address and displays a remote WebSocket URL; copy that exact URL into OverDrive's sender configuration. The receiver binds to all phone interfaces so the Tailscale VPN interface can reach it. If Tailscale is not connected, the app may show a local-network address, which will not work across separate networks.

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

The receiver currently runs while the TidalVolt app process is alive. Keep TidalVolt open during the first remote-connection test. A foreground service and automatic reconnect lifecycle are still required before relying on unattended background collection; Android may stop an ordinary app process.

### Tailscale setup

1. Install the official Tailscale Android app on the phone and the BYD head unit (Android 8.0 or newer is supported).
2. Sign in on both devices using the same identity provider/account so they join the same tailnet.
3. Approve both devices in the Tailscale admin console if prompted, then confirm both show connected.
4. On the phone, open TidalVolt → More → OverDrive connection → Start phone receiver.
5. Copy the `ws://100.x.y.z:8765/tidalvolt?token=...` address shown by TidalVolt into OverDrive's WebSocket sender. Use the actual address shown in the app; do not copy an example address.
6. Keep Tailscale enabled on both devices. The head unit must have internet access and remain awake/online to send live data.

Official installation instructions: https://tailscale.com/docs/install/android. For manual APK installation, use the official stable Android package page: https://dl.tailscale.com/stable/.

Tailscale provides the private encrypted network; it does not collect vehicle data itself. OverDrive remains the data sender, and TidalVolt remains a read-only receiver. No MQTT broker, public port forwarding, or public WebSocket endpoint is needed.

## Parking and activity tracking

Parking time remains part of the product. TidalVolt will use OverDrive's existing parking state/timer data rather than introducing a separate location or motion-tracking dependency. The current activity screen is a placeholder; the exact OverDrive parking payload field still needs to be confirmed on the head unit before completed parking durations can be recorded reliably.

## Charging costs and comparison

The charging estimator automatically includes a 5% charging-loss allowance (95% charging efficiency) and displays the energy stored in the battery, estimated loss, energy drawn from the charger and estimated cost. EV-versus-petrol comparisons are retained and include the same charging-loss allowance in EV energy costs.

## Android notifications

- No WhatsApp integration, phone number, Business API, MQTT broker or paid messaging service.
- Android notifications are generated locally from received vehicle telemetry.
- Charging-start, charging-stop, low-battery threshold and a conservative stopped-for-three-minutes trip-completion heuristic are implemented.
- Notification preferences are stored on the phone. Android 13+ notification permission is requested on first launch.
- Daily-report notification scheduling is not yet implemented.
- Trip detection is provisional and must be validated against real OverDrive update cadence before being relied on.

## Android home-screen widgets

TidalVolt includes three resizable Android home-screen widgets:
- Battery: state of charge, estimated range and last update time.
- Charging: charging state, battery level, charging power and estimated time to full.
- Vehicle overview: battery, range, speed and charging state.

Widgets refresh whenever a new OverDrive telemetry packet is received. They display unavailable values as unavailable rather than inventing readings. Tap a widget to open TidalVolt. Add them from the Android launcher widget picker.

## Local history

Incoming telemetry samples are stored locally on the phone (up to 5,000 recent samples in the current implementation). The app will retain history while the vehicle is disconnected. Trip segmentation and automated charging-session lifecycle will be enabled after the incoming OverDrive fields and update cadence are validated.

## Build

Open this repository in Android Studio, or run the TidalVolt Android CI workflow from the Actions tab. Successful runs upload a TidalVolt-debug-apk artifact.
