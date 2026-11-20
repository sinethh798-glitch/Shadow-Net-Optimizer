# Shadow Net Optimizer

Starter Android project for Wi-Fi + mobile-data diagnostics.

## Features
- Detects active Wi-Fi / cellular network and Android's validated-internet status.
- User-triggered 5 MB download speed test (approximate Mbps).
- User-triggered latency check to the test endpoint.
- Shortcuts to Android Private DNS and wireless settings.

## Important limitations
- An app cannot guarantee 10 Mbps or increase the speed beyond the ISP/carrier, plan, signal, congestion, router, and device limits.
- This app does not secretly change DNS, bypass data caps, boost radio power, or modify carrier settings.
- The speed test downloads about 5 MB and may use mobile data. Run it only when you are comfortable with that usage.
- Private DNS is changed by the user in Android Settings; this app only opens the relevant settings screen.

## Build
Use Android Studio or a compatible Android Gradle Plugin 8.7.3 + Gradle 8.9 + JDK 17.
The project targets SDK 34 and compiles with SDK 35. If using Termux, ensure Android SDK platform 35 and build-tools are installed.


V3 adds device-wide live traffic-rate monitoring, a rolling graph, ping/jitter diagnostics, Wi-Fi RSSI display, and manual speed snapshots. Live rates are approximate device-wide counters, not a guaranteed ISP speed or per-app measurement.
