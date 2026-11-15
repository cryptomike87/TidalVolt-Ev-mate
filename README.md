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

## Important
Vehicle values shown in the starter dashboard are examples, not live BMS telemetry. A real vehicle connection and verified data source must be integrated before presenting readings as live.
