# Dev Life: LeetCode & GitHub Android Home Screen Dashboard

[![Download APK](https://img.shields.io/badge/Download%20APK-v1.0.0-success?style=for-the-badge&logo=android)](https://github.com/sangrechy/project_dev_android_widgets/releases/download/v1.0.0/dev-life-v1.0.apk)
[![Latest Release](https://img.shields.io/github/v/release/sangrechy/project_dev_android_widgets?style=for-the-badge&logo=github)](https://github.com/sangrechy/project_dev_android_widgets/releases/latest)

A native Android home screen widget built with **Kotlin**, **Jetpack Compose**, **RemoteViews**, and **WorkManager** that brings your live developer telemetry directly to your home screen with a sleek glassmorphic aesthetic.

---

## 📥 Download & Install

You can download the pre-compiled APK directly:
- **Direct APK Download**: [**`dev-life-v1.0.apk`** (v1.0.0)](https://github.com/sangrechy/project_dev_android_widgets/releases/download/v1.0.0/dev-life-v1.0.apk)
- **GitHub Releases Page**: [Releases · sangrechy/project_dev_android_widgets](https://github.com/sangrechy/project_dev_android_widgets/releases/tag/v1.0.0)

---

## ✨ Features

- **LeetCode Glass Card**:
  - **3/4 Circular Speedometer Gauge**: Displays total solved count with Easy, Medium, and Hard proportional color arcs and clean legend tags.
  - **Frosted Stat Badges**: Dedicated glassmorphic pills for Easy, Medium, Hard, and Total Solved counts.
  - **Authentic Brand Emblem**: Official 3-part LeetCode emblem (white curve, silver divider, `#FFA116` chevron).
  - **2-Tier 1-Year Heatmap**: 52 weeks / 365 days stacked in two 6-month tiers (Nov–Apr and May–Oct) with authentic month separation gaps, weekday indicators (Mon, Wed, Fri), and submission count.

- **GitHub Glass Card**:
  - **4-Quadrant Spider Radar Chart**: Coordinate axes (`+`), concentric guidance rings, green radar polygon, and center telemetry hub (`842 CONTRIBS`).
  - **Frosted Stat Badges**: Dedicated glassmorphic pills for Commits, Pull Requests, Issues, and Code Reviews.
  - **Official Octocat Logo**: Crisp vector GitHub branding.
  - **2-Tier 1-Year Contribution Matrix**: 52 weeks of contributions rendered in glowing emerald cells with distinct month boundaries.

- **Responsive Multi-Size Layout**:
  - Uses modern Android `RemoteViews(mapOf(SizeF ...))` to automatically transition between an expanded dashboard and a compact layout as the widget is resized on your launcher.

- **Background Synchronization**:
  - Uses Android `WorkManager` with network constraints to refresh data automatically every 1 hour, plus one-tap manual sync via the widget header or app.

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
