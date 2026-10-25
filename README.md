# DevWidgets: LeetCode & GitHub Android Home Screen Dashboard

A native Android home screen widget built with **Kotlin**, **Jetpack Compose**, **RemoteViews**, and **WorkManager** that brings your live developer activity straight to your home screen.

Designed directly from the architectural layout in [`idea/ui_idea.png`](idea/ui_idea.png):
- **Top Section — LeetCode**:
  - Circular Solved Count & Difficulty Arc Gauge (Easy / Medium / Hard)
  - Submission Activity Heatmap (7 rows x N weeks)
  - Last 10 Submissions list with verdict & timestamp
- **Bottom Section — GitHub**:
  - Contribution Type Breakdown Map (Commits, Pull Requests, Issues, Reviews cross/quadrant chart)
  - GitHub Green Contribution Heatmap (7 rows x N weeks)
  - Last 10 Activities / Events log

---

## Architecture & Features

```
┌────────────────────────────────────────────────────────────────────────┐
│ LeetCode Section                                                       │
│ ┌──────────────┐  ┌─────────────────────────────────┐  ┌─────────────┐ │
│ │  Total Count │  │   Submission Activity Heatmap   │  │   Last 10   │ │
│ │  & Breakdown │  │       (7 rows x N weeks)        │  │ Submissions │ │
│ └──────────────┘  └─────────────────────────────────┘  └─────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│ GitHub Section                                                         │
│ ┌──────────────┐  ┌─────────────────────────────────┐  ┌─────────────┐ │
│ │ Contribution │  │   Contribution Activity Heatmap │  │   Last 10   │ │
│ │  Type Map    │  │       (7 rows x N weeks)        │  │   Events    │ │
│ └──────────────┘  └─────────────────────────────────┘  └─────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

1. **User Profile Setup UI (`MainActivity.kt` & `MainScreen.kt`)**:
   - Prompts the user on first launch to enter their **LeetCode Username** and **GitHub Username** (with optional GitHub token for private stats/rate limit increase).
   - "Save & Sync Widget" button saves the handles and triggers immediate background synchronization.
   - Includes a "Use Sample" demo mode and an interactive **Live Widget Preview**.
   - Includes a direct "Pin Widget to Home Screen" shortcut.

2. **Custom Canvas Bitmap Rendering (`WidgetBitmapRenderer.kt`)**:
   - Android `RemoteViews` doesn't natively support dynamic HTML/Canvas directly in launchers without high latency. To solve this, `WidgetBitmapRenderer` renders pixel-perfect high-DPI bitmaps for:
     - LeetCode circular progress gauge and difficulty arcs.
     - 7×18 submission activity heatmaps.
     - GitHub 4-quadrant coordinate/radar type-map cross.
     - Signature GitHub contribution matrix.

3. **Data Fetching Layer (`DevApiService.kt`)**:
   - **LeetCode**: Official GraphQL endpoint (`https://leetcode.com/graphql`) querying `matchedUser` for `submitStatsGlobal`, `submissionCalendar`, and `recentAcSubmissionList`.
   - **GitHub**: Public REST API (`https://api.github.com/users/{username}/events`) and GitHub GraphQL / contributions endpoints.
   - Intelligent fallback to cached or sample data when offline.

4. **Background Periodic Sync (`WidgetSyncWorker.kt`)**:
   - Uses Android `WorkManager` with network constraints to refresh data automatically every 1 hour in the background, consuming minimal battery.

---

## Opening and Running the Project

### Using Android Studio
1. Launch **Android Studio** (installed at `/home/sangrechy/main/devtools/android/android-studio/bin/studio.sh` or from your system applications menu).
2. Click **Open** and select `/mnt/7A96AE5396AE0FA5/projects/project_dev_android_widgets`.
3. Allow Gradle to sync dependencies.
4. Select your connected device or emulator and hit **Run** (`Shift + F10`).
5. Open the app, enter your LeetCode and GitHub IDs, and click **Save & Sync Widget**!
6. Long-press your home screen, search for **DevWidgets**, and place the widget on your home screen.

### Command Line
```bash
# Build Debug APK
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```
