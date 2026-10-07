# StudyTimer Project Overview

## Core Objective
StudyTimer is an Android study timer and habit analytics app (`com.madeby.JAI`) built with Kotlin, AndroidX, Material Design 3, Coroutines, Supabase, and Custom Canvas Views. It features a embedded sandboxed HTTP Developer Test Engine for real-device automated testing and inspection.

## Repositories & Remotes
1. `JAIstudio-source/StudyTimer-android` (`origin`): Android source tree root.
2. `JAIstudio-source/StudyTimer` (`website` remote): Download page, APK host, and version release notes.

## App Specifications
- **Application ID:** `com.madeby.JAI`
- **Target SDK:** 34 (Android 14) | **Min SDK:** 28 (Android 9.0)
- **Current Version:** `3.1.7` (Build `40`)
- **Test Engine Endpoint:** `http://10.73.183.155:8080` (Debug builds only)

## Key Technical Components
- **Timer & Background Service:** `TimerService.kt`, `TimerRingView.kt`, `HoldRingButton.kt`
- **Data & Analytics:** `StatsEngine.kt`, `TimelineLogger.kt`, `SubjectPieChartView.kt`, `HeatmapView.kt`
- **Cloud & Sync:** `LeaderboardManager.kt`, `ProfileSyncService.kt`, Supabase Integration
- **Live Test Engine:** `TestServerHelper.kt` (Endpoints: `/status`, `/screenshot.png`, `/ui_tree`, `/click`, `/input`, `/nav`, `/state`, `/logs`)
