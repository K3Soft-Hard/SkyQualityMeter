# 🌌 Sky Quality Meter (SQM) Android App

**Version 12.8.0 (Build 18)**

An advanced, native Android application designed for astronomers, astrophotographers, and dark-sky enthusiasts to connect to local Sky Quality Meter (SQM) hardware (such as ESP32/ESP8266 or TSL237 sensors) for live sky brightness telemetry, light metering, keogram visualization, and background data logging.

> 🤖 **AI-Assisted Development**: Portions of this application's architecture, data calculations, and Jetpack Compose user interfaces were generated and refined using **Google Gemini** and the **Android Studio Agent**.

---

## ✨ Features List

### 🔭 1. Live Astronomy SQM Telemetry
* **Sky Brightness Measurements**: Real-time reading of MPSAS (`mag/arcsec²`).
* **Bortle Scale Classification**: Instant Bortle class assignment with configurable decimal precision (up to 3 decimals or raw JSON values).
* **Comprehensive Metrics**: Displays sensor frequency (`Hz`), period (`ms`), Schaefer NELM, luminance flux (`cd/m²`), FOV, artificial brightness (`µcd/m²`), and network ping RTT.
* **Astro Red Theme**: Full 650nm night-vision-friendly red theme to preserve dark-adapted vision during field sessions.

### 💡 2. Dedicated Light Meter Mode
* **Illuminance & Foot-Candles**: Displays illuminance in Lux (`lx`), Foot-Candles (`fc`), and `cd/m²`.
* **Light Environment Ratings**: Categorizes ambient light levels (from Pitch Dark / Starlight to Precision Workspace and Direct Daylight) with practical usage recommendations.
* **Gray Pixel Preview Box**: Optional visual box reflecting the physical light level.

### 🔄 3. Persistent Background Capture Service
* **Continuous Background Recording**: Powered by an Android Foreground Service (`SqmCaptureService`), enabling video and timelapse telemetry logging even when the app is minimized, closed, or the screen is off.
* **Ongoing Status Notification**: Real-time notification tray banner showing elapsed capture time and frame counters.
* **Direct Tray Actions**: Includes a quick "Stop Capture" button right inside the notification shade.

### 📸 4. Capture & Recording Modes
* **Photo Snapshot**: Single telemetry capture with instant metadata saving.
* **Video Recording**: High-frequency telemetry logging at customizable intervals (`0.1s`, `0.25s`, `0.5s`, `1.0s`).
* **Timelapse Mode**: Long-duration interval logging with custom interval timers.
* **Auto-Naming**: Option to automatically name recordings using ISO timestamps.

### 📊 5. Visualizations & Tools
* **Interactive Live Keograms**: Displays live time-series color spectrums representing sky brightness over time. Tap anywhere on the keogram to jump to a specific frame.
* **Clean Fullscreen Keogram Mode**: Pure immersive keogram view with zero UI clutter (exit anytime using the device back button).
* **Dual-Axis Telemetry Charts**: Real-time graphs for SQM and frequency tracking.
* **Night Vision Pixel Square**: Center focus square for dark-sky focus and alignment.

### 🌑 6. Astronomical Calculations
* **Dark Observing Window**: Displays live calculations for Astronomical Dawn, Astronomical Dusk, Moonrise, and Moonset.
* **Lunar Phase Tracker**: Displays current lunar phase name, phase emoji, and exact illumination percentage.

### 📳 7. Differentiated Haptic Feedback
* Multi-tiered vibration feedback patterns across the app:
  * **Light Tap**: Swiping pages and toggling switches.
  * **Medium Click**: Button presses and server selection.
  * **Double Pulse / Heavy**: Starting/stopping recordings and saving observations.
* Fully toggleable in **Developer Tools**.

### 💾 8. Backup, Export & Records Management
* **Full App Backup & Restore**: Export and import complete app state to JSON files (backs up all Settings, Preferences, Servers, and Records).
* **CSV Telemetry Export**: Export session histories and live telemetry to standard CSV files.
* **Record Management**: Rename records directly from the list, search by location/Bortle level, filter by media type, and mark favorites.

---

## 🛰️ Sensor Endpoint & Mock JSON Specification

The application communicates with sensors over local Wi-Fi or LAN via plain HTTP JSON endpoints. You can also paste this JSON into **Developer Tools $\rightarrow$ Mock Telemetry** to test the app without hardware.

### Expected JSON Payload Format
```json
{
  "sqm": 21.85,
  "frequencyHz": 12.45,
  "bortle": 2.1,
  "bortleDesc": "Typical truly dark site",
  "brightnessMcd": 0.21,
  "artifBrightUcd": 15.4,
  "periodMs": 80.32,
  "fovDeg": 20.0,
  "lensTrans": 0.85,
  "timeoutS": 5.0,
  "minFreqLimit": 0.1,
  "mode": "Continuous",
  "uptimeS": 86400,
  "valid": true,
  "tooDark": false
}
