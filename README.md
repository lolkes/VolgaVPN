# VolgaVPN Android

Modern Android VPN client foundation for VolgaVPN.

## Included
- Native Kotlin Android app
- Dark premium UI
- Server selection
- Android `VpnService` lifecycle
- Persistent connection state
- Connection timer and traffic counters UI
- Settings screen
- Clean separation between UI and VPN service

## Important
The Android `VpnService` API creates the local VPN tunnel interface, but a real public-IP-changing VPN still needs a VPN protocol/backend (for example WireGuard) and a provisioned server. No private keys or server credentials are stored in this repository.

## Build
Open the project in Android Studio and run the `app` module. Minimum Android version: 8.0 (API 26).

## Project
`com.volgavpn.app` • version 1.0.0
