# TidalVolt

A clean Android companion app foundation for BYD EV owners, designed to expand to other EV brands.

## Current foundation
- Jetpack Compose Android app
- Pearl White and Obsidian appearance themes
- Vehicle overview with clearly labelled unavailable values until real telemetry arrives
- Charging cost estimator using editable battery and electricity settings
- EV-versus-petrol savings estimator
- Nassau map screen foundation
- GitHub Actions workflow for debug APK builds

## Direct BYD connection (in development)

TidalVolt is moving away from MQTT, external brokers, OverDrive publishing and cloud relays. The target architecture is a native, read-only connection to the BYD DiLink head unit's local ADB debugging service, followed by a small helper process that queries the BYD vehicle API.

### Phase 1: local ADB diagnostics
1. Install and open TidalVolt on the BYD head unit.
2. Open More → Direct BYD connection.
3. Tap **Check head-unit connection**.
4. If DiLink displays an ADB authorization dialog, tick **Always allow from this computer** and tap **Allow**.
5. Review the diagnostic result.

The diagnostic checks the local endpoint at 127.0.0.1:5555, creates and stores a TidalVolt-specific ADB key in the app-private directory, authenticates that key, and runs only read-only Android identity/build-property commands. It does not query or change vehicle systems.

Each application has its own ADB identity. Authorization previously granted to OverDrive, BYDMate or Electro does not automatically authorize TidalVolt; DiLink may show a separate first-use prompt.

### Planned phases
- Phase 1: local ADB endpoint discovery, TidalVolt key creation, authorization and read-only shell verification.
- Phase 2: launch an isolated helper process under the ADB shell identity.
- Phase 3: probe the presence of BYD vehicle services and required read permissions.
- Phase 4: implement read-only telemetry queries and validate readings against the instrument display.
- Phase 5: connect verified telemetry to the TidalVolt dashboard.

### Safety and compatibility
- No vehicle-control commands, setters, actuator calls, or configuration changes are implemented in Phase 1.
- The BYD vehicle API is undocumented and firmware-dependent. DiLink 3.0 compatibility must be verified on the target Yuan Pro before any telemetry is labelled live.
- ADB authorization is security-sensitive. Keep the ADB key inside TidalVolt's private app storage and do not export or share it.
- If local ADB is unavailable, the app reports that result rather than guessing or presenting demo values as live.

## Build
Open this repository in Android Studio, or run the **TidalVolt Android CI** workflow from the Actions tab. Successful runs upload a `TidalVolt-debug-apk` artifact.
