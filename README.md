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
TidalVolt can subscribe to the BYD Trip Stats MQTT state topic. Install and configure BYD Trip Stats on a compatible BYD DiLink head unit, enable its MQTT publisher, and point both apps at the same broker. The default TidalVolt topic is `byd-trip-stats/yuanpro/state`; set the BYD Trip Stats device friendly name to `yuanpro` or change the topic in TidalVolt → More → Integrations. HiveMQ Cloud offers a free tier; a self-hosted Mosquitto broker is another option. Broker hosting and network access are separate from the free MQTT protocol/client libraries.

The integration is read-only: TidalVolt subscribes to telemetry and does not send commands to the vehicle. Missing vehicle fields remain unavailable rather than being fabricated. Route planning and arrival-state-of-charge prediction are still future work; this MQTT integration replaces the proposed ABRP telemetry dependency, not the complete ABRP navigation product.

## Compatibility and licensing
BYD Trip Stats compatibility must be confirmed on the target vehicle/head-unit firmware. Its source is licensed under BUSL-1.1; TidalVolt uses its documented MQTT data interface and does not copy or redistribute its source code.

## Important
Vehicle values are shown as unavailable until a real telemetry message is received. No sample BMS readings are presented as live.
