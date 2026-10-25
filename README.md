# Dev Life: LeetCode & GitHub Android Home Screen Dashboard

[![Download APK](https://img.shields.io/badge/Download%20APK-v1.1.0-success?style=for-the-badge&logo=android)](https://github.com/sangrechy/project_dev_android_widgets/releases/download/v1.1.0/dev-life-v1.1.0.apk)
[![Latest Release](https://img.shields.io/github/v/release/sangrechy/project_dev_android_widgets?style=for-the-badge&logo=github)](https://github.com/sangrechy/project_dev_android_widgets/releases/latest)

A native Android home screen widget built with **Kotlin**, **Jetpack Compose**, **RemoteViews**, and **WorkManager** that brings your live developer telemetry directly to your home screen with a sleek glassmorphic aesthetic.

---

## 📥 Download & Install

You can download the pre-compiled APK directly:
- **Direct APK Download**: [**`dev-life-v1.1.0.apk`** (v1.1.0)](https://github.com/sangrechy/project_dev_android_widgets/releases/download/v1.1.0/dev-life-v1.1.0.apk)
- **GitHub Releases Page**: [Releases · sangrechy/project_dev_android_widgets](https://github.com/sangrechy/project_dev_android_widgets/releases/tag/v1.1.0)

---

## ✨ Features

- **Dev Life Identity & Chibi Jane Branding**:
  - Custom chibi mascot emblem in the header and app launcher.
  - Interactive status badges displaying live verification state (`Linked (X solved)`, `Linked (X contribs)`).

- **LeetCode Glass Card**:
  - **3/4 Circular Gauge**: Displays total solved count with Easy, Medium, and Hard proportional color arcs and clean legend tags.
  - **Authentic Brand Emblem**: Official 3-part LeetCode emblem (white curve, silver divider, `#FFA116` chevron).
  - **Dynamic Complete-Month 1-Year Heatmap**: 52 weeks / 365 days arranged in complete month blocks with authentic month separation gaps, weekday indicators (Mon, Wed, Fri), and responsive 1-, 2-, or 3-tier layouts (e.g. 4-4-4 months).

- **GitHub Glass Card**:
  - **Authentic Activity Overview (Spider/Radar Chart)**: 4-quadrant coordinate axes (`+`), concentric guidance rings, emerald radar polygon, and center telemetry hub (`CONTRIBS`).
  - **Clean Cardinal Breakdown**: Non-colliding labels for Commits (Emerald), Pull Requests (Purple), Issues (Blue), and Code Reviews (Amber).
  - **Official Octocat Logo**: Crisp vector GitHub branding.
  - **Dynamic Complete-Month 1-Year Heatmap**: 52 weeks of contributions rendered in glowing emerald cells with distinct month boundaries and no split months across tiers.

- **Responsive Multi-Size Layout**:
  - Uses modern Android `RemoteViews(mapOf(SizeF ...))` to automatically transition between an expanded dashboard and a compact layout as the widget is resized on your launcher.

- **Reliable Background Synchronization & Hardened Architecture**:
  - Android `WorkManager` with exponential backoff and retry bounds to conserve battery.
  - OkHttp connection lifecycle management preventing socket and response leaks.
  - One-tap manual sync via the widget header or app with spinning activity indicator.
  - Comprehensive blackbox unit and instrumented tests verifying bounds, empty profile persistence, and math partitioning.

---

## 🛠️ Building From Source

### Prerequisites
- Android Studio Ladybug / Meerkat or Android SDK (API 36)
- Java 17+

### Command Line
```bash
# Build Debug APK
./gradlew assembleDebug

# Install directly on a connected device or emulator via ADB
./gradlew installDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
