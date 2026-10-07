# 📖 StudyTimer Complete Architecture & Live Testing Guide

> **For AI & Developer Use**: This guide details the complete working of StudyTimer, every UI button/interaction, internal architecture, and instructions for running sandboxed live tests.

---

## 📐 1. Architecture Overview

* **Application ID:** `com.madeby.JAI`
* **Target SDK:** 34 (Android 14) | **Min SDK:** 28 (Android 9.0)
* **Current Version:** `3.1.7` (Build `40`)
* **Core Technologies:** Kotlin, AndroidX, Material Design 3, Coroutines, Supabase (Leaderboard & Cloud Sync), Custom Canvas Views (`TimerRingView`, `HeatmapView`, `SubjectPieChartView`, `HoldRingButton`).

---

## ⚡ 2. Sandboxed Live Test Engine (`TestServerHelper.kt`)

StudyTimer includes an embedded, sandboxed HTTP test server that operates **only** in `debug` builds (`BuildConfig.DEBUG == true`) on port `8080`. It has zero access outside StudyTimer's active activity window.

### 🌐 API Test Endpoints:

| Endpoint | Method | Parameters | Description |
| :--- | :--- | :--- | :--- |
| `/status` | `GET` | None | Returns JSON server status, app version, active activity name. |
| `/screenshot.png` | `GET` | None | Captures pixel-perfect PNG image of StudyTimer screen for AI visual inspection. |
| `/ui_tree` | `GET` | None | Dumps JSON hierarchy of all visible/clickable views, text contents, IDs, and screen bounds. |
| `/click` | `GET` | `?id=<res_id>` OR `?text=<text>` OR `?x=<x>&y=<y>` | Triggers a touch/click action on a view inside StudyTimer. |
| `/input` | `GET` | `?id=<res_id>&text=<text>` OR `?text=<text>` | Inputs text into a focused field or target `EditText`. |
| `/state` | `GET` | None | Returns JSON object of active timer state, session duration, active subject, and timestamp. |
| `/logs` | `GET` | None | Retrieves recent in-memory debug log entries. |
| `/install_update` | `GET` | `?url=http://<laptop-ip>:8000/StudyTimer-debug.apk` | Downloads updated APK over local hotspot network and launches Android installer. |

---

## 📱 3. Screen-by-Screen & Button Interaction Guide

### 🏠 Main Focus Screen (`MainActivity.kt` & `FocusPanelBuilder.kt`)

1. **⚙️ Settings Button (Top-Left Gear Icon)**
   * **View Class:** `ImageView`
   * **Action:** Opens the **Settings & Customization Panel** (`SettingsPanelBuilder.kt`).
   * **Testing:** Call `/click?x=120&y=220` or click settings icon.

2. **🏆 Leaderboard Button (Top-Right Trophy Icon)**
   * **View Class:** `FrameLayout` / `ImageView`
   * **Action:** Opens the **Global & Monthly Leaderboard Panel** (`LeaderboardPanelBuilder.kt`).
   * **Testing:** Call `/click?x=1100&y=220` or click trophy icon.

3. **🏷️ Status Pill Badge**
   * **View Class:** `TextView`
   * **Content:** Displays status like `"Ready when you are"`, `"Focusing..."`, or `"Paused"`.

4. **⏱️ Main Timer Display**
   * **View Class:** `TextView`
   * **Content:** Displays active time formatted as `HH:MM:SS`. Supports Stopwatch and Focus Countdown modes.

5. **📖 Subject Selector Pill**
   * **View Class:** `TextView` (e.g., `📖 General ▾`, `📐 Math ▾`)
   * **Action:** Opens the **Subject Selection & Custom Subject Manager Dialog** (`SubjectDialogHelper.kt`).
   * **Testing:** Call `/click?text=📖 General  ▾` or click subject pill.

6. **▶️ / ⏸️ Primary Action Button**
   * **View Class:** `Button`
   * **Text States:** `"Start Studying"`, `"Pause"`, `"Resume"`.
   * **Action:** 
     * Tapping `"Start Studying"` starts a focus session and launches background `TimerService`.
     * Tapping `"Pause"` pauses the countdown/stopwatch.
     * Tapping `"Resume"` resumes active counting.
   * **Testing:** Call `/click?text=Start%20Studying`, `/click?text=Pause`, `/click?text=Resume`.

7. **🛑 Finish Session Button**
   * **View Class:** `HoldRingButton` (Custom Canvas View)
   * **Text:** `"Hold to Finish"`
   * **Action:** Requires long-press/hold to complete and save the study session to database and update stats (`StatsEngine.kt`).

8. **📊 Analytics Button (Bottom-Right Bar Chart Icon)**
   * **View Class:** `ImageView`
   * **Action:** Opens the **Statistics & Insights Panel** (`StatsPanelBuilder.kt`).
   * **Testing:** Call `/click?x=1030&y=2530` or click bar chart icon.

---

### 📅 Planner & Timeline Panel (`PlannerPanelBuilder.kt`)

* **Day Timeline Button:** Opens `DayTimelineDialogHelper` to view hourly schedule blocks.
* **Lecture & Break Planner:** Opens `BreakAndLectureDialogHelper` to configure automated study/break cycles.
* **Daily Goal Checkboxes & Task List:** Allows adding, toggling, and removing study goals.

---

### 📊 Analytics & Insights Panel (`StatsPanelBuilder.kt`)

* **Subject Pie Chart:** Interactive distribution of study time per subject (`SubjectPieChartView`).
* **Focus Heatmap:** 365-day habit tracker (`HeatmapView`).
* **Weekly Summary Share Card:** Generates image card for social sharing (`WeeklySummaryShareHelper`).

---

### ⚙️ Settings & Customization Panel (`SettingsPanelBuilder.kt`)

* **Theme Switcher:** Toggle between OLED Pure Black, AMOLED, Custom Color Hue, and Bubble UI styles.
* **Timer Modes:** Switch between Subject Mode, Target Goal Mode, and Pure White OLED Mode.
* **Developer Tools:** Debug tools unlocked via `DeveloperToolsHelper.kt`.

---

## 🛠️ 4. Standard AI Live Testing Workflow

Whenever working on a new feature or fix:

1. **Make Code Modifications.**
2. **Rebuild Debug APK:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-23"; .\gradlew assembleDebug; Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "StudyTimer-debug.apk" -Force
   ```
3. **Verify App Connectivity:**
   ```powershell
   curl.exe http://<phone-ip>:8080/status
   ```
4. **Capture Live Screen:**
   ```powershell
   Invoke-WebRequest -Uri "http://<phone-ip>:8080/screenshot.png" -OutFile "C:\Users\user\.gemini\antigravity-ide\brain\<conversation-id>\live_phone_screen.png"
   ```
   *Inspect the PNG image using `view_file` to visually verify UI layout, colors, fonts, and dark mode.*
5. **Inspect UI Elements:**
   ```powershell
   curl.exe http://<phone-ip>:8080/ui_tree
   ```
6. **Execute Actions:**
   ```powershell
   curl.exe "http://<phone-ip>:8080/click?text=Start%20Studying"
   ```
