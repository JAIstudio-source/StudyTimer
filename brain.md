# StudyTimer Architecture & Codebase Memory (brain.md)

> **Instruction for Future Context:**
> Always read this file first at the start of a session, and update it whenever adding new architectural patterns, database migrations, or core business rules.

---

## 1. Project Overview & Tech Stack

### Core Purpose
**StudyTimer** is a high-performance, distraction-free Android study and productivity timer application tailored for students and focused professionals. It features stopwatch & countdown modes, scheduled timetable/class synchronization, ambient soundscapes, subject tracking, detailed timeline logging, weekly performance cards, streak tracking, milestone celebrations, and cloud backup/sync.

### Tech Stack
- **Platform**: Android Native
- **Language**: Kotlin
- **UI Architecture**: Custom Programmatic Views & ViewBuilders with Hardware-Accelerated Custom Canvas Graphics (`TimerRingView`, `HoldRingButton`, `WeeklyCardView`, `SubjectPieChartView`, `HeatmapView`, `CalendarTimeline`). Dynamic panel building architecture (`FocusPanelBuilder`, `StatsPanelBuilder`, `SettingsPanelBuilder`).
- **Data Persistence**: Lightweight high-speed file storage + JSON (`TimelineLogger` with `focus_timeline.json`), `SharedPreferences` (`StudyTimerPrefs`, `studytimer_subject_tags`, `studytimer_celebration_prefs`).
- **Background Execution & Services**: Foreground `TimerService` with custom notification channels, `WakeLock`, high-precision ticking loop, and broadcast receivers (`GoalReminderReceiver`, `StudyWidgetProvider`).
- **Sync & Analytics**: Supabase/Cloud API integration via `CloudSyncManager` and `AuthManager`, telemetry via `AppAnalytics`, local diagnostic logging via `CrashReporter`.

---

## 2. Directory & File Hierarchy

```
StudyTimer/
├── admin/                         # Web admin & analytics dashboard scripts
├── assets/                        # App logo, featured images, branding screenshots
├── StudyTimer-android/            # Main Android application
│   └── app/src/main/
│       ├── AndroidManifest.xml    # Permissions, service declarations, widget receiver
│       ├── res/                   # Drawables, layouts, values, strings, XML configs
│       └── java/com/madeby/JAI/   # Source code
│           ├── MainActivity.kt           # Central hub, UI container, panel router, dialogs
│           ├── SplashActivity.kt         # Splash screen and launch routing
│           ├── LoginActivity.kt          # Supabase cloud authentication screen
│           ├── StudyTimerApp.kt          # Application class
│           │
│           ├── TimerService.kt           # Foreground timer service, state engine, notifications
│           ├── StudyWidgetProvider.kt    # Android homescreen widget update provider
│           ├── GoalReminderScheduler.kt  # AlarmManager goal & study reminder scheduler
│           ├── GoalReminderReceiver.kt   # BroadcastReceiver handling goal notification alarms
│           │
│           ├── FocusPanelBuilder.kt      # Main timer panel UI builder (Stopwatch, Pomodoro, Timetable)
│           ├── StatsPanelBuilder.kt      # Analytics panel container & tabs
│           ├── SettingsPanelBuilder.kt   # Comprehensive settings, presets, soundscape UI
│           ├── DeveloperToolsHelper.kt   # Developer diagnostics, timeline editor, time injection
│           │
│           ├── StatsEngine.kt            # Aggregation engine for day blocks, streaks, averages
│           ├── StreakCalculator.kt       # Multi-day streak calculation logic
│           ├── SubjectTagManager.kt      # Subject catalog, duration tracker, and tag manager
│           ├── TimelineLogger.kt         # Timeline file logger (JSON-backed timeline persistence)
│           ├── TimelineJson.kt           # Custom zero-dependency JSON parser for timeline blocks
│           ├── TimeFormat.kt             # 12h/24h wall clock formatting utilities
│           ├── WeekHelper.kt             # Monday-offset and calendar date math helpers
│           │
│           ├── CelebrationEngine.kt      # Confetti particle engine and milestone celebration dialog
│           ├── AmbientSoundEngine.kt     # Background soundscapes (Rain, White Noise, Cafe, etc.)
│           ├── ThemeCoordinator.kt       # OLED black, Eclipse, Bubble 3D theme management
│           ├── PrefsSafe.kt              # Type-safe SharedPreferences extension helpers
│           ├── UpdateChecker.kt          # In-app GitHub release update validator
│           ├── BackupManager.kt          # Local JSON backup and import/export manager
│           ├── CloudSyncManager.kt       # Remote Supabase cloud synchronization
│           ├── CrashReporter.kt          # Uncaught exception handler and error logger
│           ├── AppAnalytics.kt           # Privacy-safe usage and session telemetry
│           │
│           ├── DataModels.kt             # Enums, PlannerGoal, LectureScheduleItem, Badge models
│           ├── StatsModels.kt            # BlockInfo, StatsSnapshot, MonthBucket data models
│           │
│           ├── TimerRingView.kt          # Custom Canvas circular countdown/progress ring
│           ├── HoldRingButton.kt         # Interactive hold-to-stop tactile action button
│           ├── SubjectPieChartView.kt    # Dual-mode animated Canvas donut/pie chart
│           ├── HeatmapView.kt            # GitHub-style yearly study intensity heatmap
│           ├── CalendarTimeline.kt       # Interactive day session timeline block renderer
│           ├── WeeklyCardView.kt         # Shareable 9:16 weekly summary graphic renderer
│           └── Soft3DBubbleDrawable.kt   # Custom neumorphic/glassmorphic background drawable
├── schema_analytics.sql           # Remote database analytics schema
├── fix_delete_permissions.sql     # Database permissions migration
└── version.json                   # Web release and update metadata
```

---

---

## 3. Core Data Models & Storage Schema

### `TimerState` & `AppPanel`
```kotlin
enum class TimerState { IDLE, STUDYING, BREAK, PAUSED, LECTURE_ENDED }
enum class AppPanel { FOCUS, STATS, SETTINGS, HEATMAP }
enum class AppStatsTab { OVERVIEW, TIMELINE, PLANNER }
enum class AppSettingsTab { HUB, TIMER, AMBIENCE, ANALYTICS, CLOUD, THEME, PROFILE, DEVELOPER }
```

### Timer Operation Modes (`timer_mode`)
1. **`STOPWATCH`**: Free-running elapsed timer counting upwards (`accumulatedStudy`), with untagged or tagged focus support.
2. **`COUNTDOWN`** (Pomodoro): Countdown interval timer (`focusRemainingSecs` / `focusCountdownSecs`) alternating between active focus intervals, short breaks, and cycle-based long breaks. Pomodoro duration customizer (Focus/Short Break/Long Break/Cycles) is rendered conditionally only when `timer_mode == "COUNTDOWN"`.
3. **`SUBJECT`**: Subject-dedicated focus tracking emphasizing subject selection chip and pie chart breakdowns.
4. **`LECTURE`**: Scheduled Timetable Mode. Tied to `lecture_schedules_json`. Displays current class countdown and timetable management button (`showLectureScheduleManagerDialog`).

### Core Entities (`DataModels.kt` & `StatsModels.kt`)
1. **`BlockInfo` (Unified Session Record)**:
   - `startMs: Long`: Epoch timestamp in milliseconds when focus/break started.
   - `endMs: Long`: Epoch timestamp in milliseconds when block ended.
   - `secs: Long`: Total duration in seconds (`(endMs - startMs) / 1000L`).
   - `running: Boolean`: Flag if session is currently active.
   - `manual: Boolean`: Flag if block was created/adjusted via developer editor or manual log.
   - `subjectId: String?`: Nullable subject ID (e.g. `"math"` or custom ID). `null` indicates pure untagged focus ("Focus"), distinct from explicit `"General"` subject tag.
   - `subjectName: String?`: Nullable subject display name.
   - `subjectColor: String?`: Nullable hex color code (e.g. `"#10B981"`).

2. **`TimelineEntry` (`TimelineLogger.kt` & `TimelineJson.kt`)**:
   - Stored in `focus_timeline.json` as `[{"t": Long, "s": String, "id": String?, "subId": String?, "subName": String?, "subColor": String?}]`
   - States: `"STUDYING"`, `"BREAK"`, `"MANUAL_FOCUS"`, `"MANUAL_BREAK"`, `"IDLE"`, `"LECTURE_ENDED"`.
   - Carries subject metadata directly so history logs, day dialogs, and pie chart analytics aggregate directly from the single source of truth without desynchronization.

3. **`SubjectTag` (`SubjectTagManager.kt`)**:
   - `id: String`: e.g. `"math"`, `"physics"`, `"chemistry"`, `"revision"`, or `"custom_..."`
   - `name: String`, `iconEmoji: String`, `colorHex: String`, `isCustom: Boolean`.
   - Aggregations stored in `studytimer_subject_tags` under `subject_durations_json` and `daily_subject_durations_json`.

4. **`LectureScheduleItem`**:
   - `id: String`, `title: String`, `startTime: "HH:mm"`, `endTime: "HH:mm"`, `enabled: Boolean`, `subjectId: String`.
   - Stored in `StudyTimerPrefs` under `lecture_schedules_json`.

5. **`PlannerGoal` / `PlannerGoalSnapshot`**:
   - `id: String`, `title: String`, `targetMinutes: Int`, `completed: Boolean`, `checkedAt: Long`.

---

## 4. Key Business Logic & Edge Case Rules

### A. Timer Duration & Timestamp Calculations
- **Stopwatch & Countdown Execution**: `TimerService` runs a high-precision 1-second `Handler` loop. Real elapsed time is computed as `gap = now - lastTimestamp`.
- **Accurate Timestamps**:
  - Distinct session intervals record their true `startMs` and `endMs` in `TimelineLogger`.
  - When sessions are resumed from pause or break, a new state transition entry is written to `TimelineLogger`, ensuring timeline segments are strictly accurate and never overlap or inherit falsified wall-clock times.
- **Detailed Session Analytics Timestamps**:
  - In `MainActivity.kt` (`showPieChartDetailsModal`), session ranges are formatted directly via `TimeFormat.formatWallClock(context, startMs) — TimeFormat.formatWallClock(context, endMs)`.

### B. Analytics & Pie Chart Filtering
- **Strict Date Boundary Scoping**:
  - Daily session aggregations enforce strict Unix timestamp boundaries: `startMs` (00:00:00.000) to `endMs` (23:59:59.999) calculated from the user's local timezone.
  - Pie charts query only unified sessions within the target date (`dayBlocks(dateStr)`).
  - Fallbacks in `SubjectTagManager.getSubjectDurationsForDate(context, dateKey)` strictly inspect the specified `dateKey` without defaulting to lifetime global subject totals, preventing past or future manual session injections from bleeding across day boundaries into other days' pie charts.
- **Untagged vs. General Subject Separation**:
  - Untagged sessions (`subjectId == null`) are labeled as neutral "Focus" in history logs and are excluded from the Subject Sessions Pie Chart so they do not artificially pollute subject proportions.
  - "General" is an explicit user subject tag and only appears when deliberately tagged.
- **Sub-Minute Threshold**: Subjects with less than 60 seconds total focus (`secs < 60L`) are filtered out of the Subject Sessions Pie Chart (`SubjectPieChartView`) and the breakdown list so brief accidental timer taps do not clutter visualizations.
- **Focus Depth Classification**:
  - Deep Focus: `duration >= 2400L` (≥ 40 minutes, `#10B981`)
  - Standard Focus: `900L <= duration < 2400L` (15 to 40 minutes, `#3B82F6`)
  - Light Focus: `duration < 900L` (< 15 minutes, `#F59E0B`)

### C. Timetable / Scheduled Classes vs. Active Study Time
- **Class Schedules (`LectureScheduleItem`)**: Provide automated schedule reminders and optional prompt to switch mode.
- **Subject Tag Isolation & Lock**:
  - When entering / running Lecture Mode, the session's active `subjectId` is bound strictly to the scheduled class entity (`active_lecture_subject_id`).
  - While Lecture Mode is active (`STUDYING` or `PAUSED`), the main focus panel's subject tag selector is locked (displays `🔒 [Subject]`) preventing manual timer controls from mutating or overwriting the class's predetermined subject metadata.
  - When the lecture session finishes, it records to the database under the scheduled lecture's exact subject details.
- **Nested In-Place Subject Creation in Timetable**:
  - The "Add / Edit Class" panel includes an in-place `+ Add New Subject` action.
  - Users can create a new subject (custom name + 360° Hue Bar / palette swatches) directly from the class setup dialog without losing entered class times or having to navigate elsewhere.
- **Goal Separation**: Scheduled class durations or countdown lengths **never** count toward daily focus goals automatically. Only real elapsed focus seconds (`accumulatedStudy`) logged while in `STUDYING` state count toward goals and streaks.
- **Goal Celebration Triggering**:
  - `checkCelebration()` evaluates `todayTotalFocus >= dailyGoal` using stored daily focus + active session focus.
  - `CelebrationEngine` persists `last_celebrated_goal_date = todayStr` in `studytimer_celebration_prefs` so the celebratory particle dialog displays exactly once per calendar day when the milestone is first crossed.

### D. Navigation Hierarchy & Backpress Handling
- **Hub & Spoke Settings Architecture**: `SettingsDashboardScreen` serves as the category root with top User Profile card and categorized category cards.
- **Backpress Rules**:
  - Sub-screens (`TIMER`, `AMBIENCE`, `ANALYTICS`, `CLOUD`, `THEME`, `PROFILE`, `DEVELOPER`) pop back one level to `AppSettingsTab.HUB`.
  - Root destinations (`SettingsDashboardScreen`, `StatsPanel`, `FocusPanel`) do not pop up into Timer; they follow root Android behavior (double-tap exit toast).
  - Ghost horizontal swiping is completely disabled on Settings to avoid blank tab drift.

### E. Developer Tools & Deterministic Mock Data
- **Manual Session Logger**: Supports custom date picker, start time picker, and end time picker with live duration calculation and explicit untagged vs. custom subject options.
- **Deterministic Batch Mocking**: 7-day and 30-day mock seed generator generates realistic study sessions distributed reliably across Math (35%), Physics (30%), Chemistry (20%), and Revision (15%) with natural daily variances and timeline entries.

### F. App Launch Routing & Guest / Authenticated Flow
- **Startup Router (`SplashActivity.kt`)**:
  - Checks `AuthManager.isLoggedIn(context) || AuthManager.hasCompletedOnboarding(context)`.
  - If authenticated or guest/onboarding completed: routes directly to `MainActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` (bypassing the login screen and preventing backstack recreation).
  - If initial launch / unauthenticated: routes to `LoginActivity`. Once the user signs in or chooses "Continue as Guest", `has_completed_onboarding = true` is persisted so all subsequent cold starts land on `MainActivity` (Timer screen).

### G. Cloud Data Sync & Server Payload Integrity
- **Unified Payload Serialization & Auth Verification (`CloudSyncManager.kt`)**:
  - Verifies active authenticated session status (`AuthManager.isLoggedIn(context)`) before network dispatch. If not signed in, throws explicit user-facing `UNAUTHENTICATED` warning rather than ambiguous generic failure toasts.
  - Matches the exact 7-column schema of the Supabase `user_sync_data` table: `user_id`, `user_name`, `user_email`, `profile_image_uri`, `prefs_data`, `timeline_data`, and `updated_at`.
  - Subject tags are safely embedded into `prefs_data` under `__subject_tags_data__` ensuring zero schema mismatch errors while retaining complete subject tag and duration backup.
  - Untagged sessions (`subId == null`, `subName == null`, `subColor == null`) serialize safely to JSON.
  - Comprehensive debug logging outputs exact payload sizes, entry counts, and HTTP status codes / response bodies on POST and PATCH fallbacks.

### H. User Feedback, Backend API & Admin Triage System
- **Feedback & Bug Report Modal (`MainActivity.showFeedbackReportDialog`)**:
  - Accessible via "Report a Problem & Feedback" under the "About & Support" card on `SettingsDashboardScreen`.
  - Offers category selector chips: `[🐛 Bug Report]`, `[💡 Feature Request]`, `[💭 General Feedback]`.
  - Multi-line description field for detailed bug reproduction steps or feature requests.
  - **Rate Limiting & Cooldown Protection**:
    - **Cooldown Threshold**: 5 minutes (300,000 ms) tracked locally via preference key `last_feedback_submission_epoch` in `studytimer_feedback_prefs`.
    - **Live Countdown & Warning**: When active, disables the submit button (`sendBtn.isEnabled = false`, `alpha = 0.5f`) and renders a real-time updating countdown (`⏳ You recently sent a report. Please wait Mm Ss before submitting another.`).
    - **Dev Mode Bypass**: Bypasses cooldown automatically when `BuildConfig.DEBUG == true` or developer suite is unlocked (`isDevModeUnlocked == true`).
    - **Urgent Help Fallback**: Shows an immediate shortcut: `"Need urgent help? [Email studytimer737@gmail.com]"` which opens direct mail without cooldown restrictions or copies to clipboard if no client is present.
  - Optional toggle to include anonymous device diagnostics:
    - App Version & Build Code (`BuildConfig.VERSION_NAME` / `VERSION_CODE`)
    - Android Release & API level (`Build.VERSION.RELEASE` / `SDK_INT`)
    - Device Manufacturer & Model (`Build.MANUFACTURER` / `MODEL`)
    - Active Timer Mode (`timerMode`)
    - Database & Cloud Sync Status (Signed In / Guest)
  - Dispatches `ACTION_SENDTO` (mailto) intent to `studytimer737@gmail.com` wrapped in `Intent.createChooser()`.
  - Graceful fallback: If no email client application is installed on the device, copies the pre-formatted feedback subject and diagnostic block directly to the system clipboard with an informative toast.
  - Provides a direct action link to open GitHub Issues for open-source users.

- **Backend Feedback Endpoint (`POST /api/feedback`)**:
  - Located in [api/feedback.js](file:///d:/Download/My%20app/website/New%20folder/StudyTimer/api/feedback.js) (Vercel Serverless Function).
  - Validates payload category (`BUG_REPORT`, `FEATURE_REQUEST`, `GENERAL_FEEDBACK`), user contact, message length (5–2000 chars), and structured diagnostics object.
  - IP-based rate limiting (5 minutes cooldown) returning `HTTP 429 Too Many Requests` with standard `Retry-After` header.
  - Persists directly into Supabase `feedback_reports` table (`id`, `type`, `status`, `user_contact`, `message`, `diagnostics`, `admin_notes`, `created_at`, `updated_at`).
  - Supports optional webhook notification dispatch (`FEEDBACK_ALERT_WEBHOOK` for Discord/Telegram/Email triggers) on new `BUG_REPORT` events.

- **Admin Feedback Dashboard (`/admin/#tabFeedback`)**:
  - Located in [admin/index.html](file:///d:/Download/My%20app/website/New%20folder/StudyTimer/admin/index.html).
  - Dedicated navigation tab with real-time `NEW` reports unread badge counter.
  - Category filters (`All`, `Bug Reports`, `Feature Requests`, `General Feedback`) and Status filters (`All`, `New`, `In Progress`, `Resolved`, `Archived`).
  - Search bar integration across user messages, diagnostic metadata, and admin notes.
  - Detailed Report Inspection Modal:
    - Formatted user description readout.
    - Diagnostic grid (App Version, Android OS, Device Model, Timer Mode, Sync State, Epoch).
    - 1-Tap email response button (`mailto:{user_contact}?subject=Re: Your StudyTimer Feedback`).
### I. Subject Tagging & Stopwatch Mode Isolation
- **Stopwatch Mode Untagged Rule**:
  - When running in standard **Stopwatch Mode** (`timerMode == "STOPWATCH"`), sessions are strictly logged without subject metadata (`subjectId = null`, `subjectName = null`, `subjectColor = null`).
  - Stopwatch mode never falls back to or mutates the default `"general"` subject tag.
  - In `TimerService.kt`, background time accumulator loops bypass `recordSubjectStudyTime` and `recordSubjectBreakTime` while in `STOPWATCH` mode, ensuring stopwatch time remains raw focus time and never pollutes subject breakdowns or pie chart distributions.
  - In `StatsEngine.kt` and `MainActivity.kt`, subject aggregation algorithms strictly filter by `subjectId != null`, isolating stopwatch logs while preserving total daily focus duration on hero/overview cards.

### J. Pie Chart Visibility Preference Binding
- **Setting Key (`show_subject_pie_chart`)**:
  - Configurable via "Subject Pie Chart" toggle in Settings under the "Charts & Visualization" section.
  - Exposed through `StatsSnapshot.showPieChart` (populated dynamically in `StatsEngine.kt`).
### K. Top-Level Navigation Backstack Contract
- **Primary Root Anchor (`AppPanel.FOCUS` / Timer Screen)**:
  - Serves as the primary start destination and base anchor of the application.
  - The exit confirmation / double-tap exit flow (`finish()`) is strictly scoped to `AppPanel.FOCUS`.
- **Secondary Top-Level Tabs (`AppPanel.SETTINGS`, `AppPanel.STATS` / Insights)**:
  - Pressing the system Back button on secondary top-level destinations immediately routes the user back to the start destination (`AppPanel.FOCUS`).
  - Secondary top-level screens never intercept back presses to force an exit confirmation dialog.
- **Nested & Sub-Screens (Settings Sub-screens, Full-Screen Heatmap)**:
  - Pressing Back on nested screens pops the navigation stack one level up to the parent container (e.g., `AppPanel.HEATMAP` returns to `AppPanel.STATS`, and nested settings sub-screens like `TIMER`, `THEME`, `DATA`, etc. return to `AppSettingsTab.HUB`).

---

## 5. UI/Theme Conventions

- **AMOLED Dark Mode**: True `#000000` background with `#121212` elevated cards for maximum battery preservation on OLED screens.
- **Eclipse Mode**: Dark Slate Navy (`#0F172A` background, `#1E293B` containers).
- **Light Mode**: Clean White (`#FFFFFF` background, `#EDF0F5` containers).
- **Dual Accent Color Architecture (`ThemeCoordinator.kt`)**:
  - **Focus Accent Token (`customPrimary`)**: Drives focus timer progress rings, active study session badges, and primary action buttons.
  - **Break Accent Token (`customSecondary`)**: Drives break countdown rings, resting badges, and break state transitions.
- **Curated Soft / Aesthetic Color Palettes**:
  - **Focus Soft Palette**:
    - `#818CF8` (Soft Lavender), `#60A5FA` (Soft Sky Blue), `#38BDF8` (Pastel Cyan), `#A78BFA` (Light Purple), `#F472B6` (Pastel Rose), `#FB7185` (Soft Coral), `#FB923C` (Pastel Peach), `#FBBF24` (Warm Amber), `#34D399` (Soft Mint), `#2DD4BF` (Aqua Teal)
  - **Break Soft Palette**:
    - `#34D399` (Mint Green), `#2DD4BF` (Soothing Teal), `#38BDF8` (Calm Sky Blue), `#A7F3D0` (Soft Seafoam), `#6EE7B7` (Pastel Emerald), `#818CF8` (Relaxing Lavender), `#F472B6` (Rose Quartz), `#FDE047` (Warm Lemon), `#FDBA74` (Light Apricot), `#94A3B8` (Zen Slate)
- **Continuous 360° Hue Slider & Live Preview**: Real-time interactive HSV sliders for granular fine-tuning of exact focus and break hue values with immediate local persistence.
- **Instant Live Theme Propagation & Layout Stabilization**:
  - Color updates immediately synchronize into `ThemeCoordinator`, update the active session timer displays, reload cached views, and recolor interactive action elements in real-time.
  - Color picker card containers enforce a fixed `minimumHeight` and `setSingleLine(true)` with `TextOverflow.Ellipsis` on label headers, eliminating container layout shifts and card resizing jitter during slider interaction.

---

## 6. Current Status & Verified Systems

- [x] Fixed sub-minute sessions (< 60s) showing up in Subject Sessions Pie Chart.
- [x] Fixed broken timestamps and session continuation logic in Detailed Session Analytics.
- [x] Distinguish untagged focus (`subjectId == null`) from explicit `"General"` subject.
- [x] Hub & Spoke Category Settings refactor with User Profile top header and sub-screen.
- [x] Conditional Pomodoro interval customizer (renders strictly in `COUNTDOWN` mode).
- [x] Fixed Lecture mode selection, persistence, and timetable dialog / schedule integration.
- [x] Deterministic 7D / 30D mock history data generator with realistic subject ratios.
- [x] Daily Goal Reached particle celebration dialog with date-persisted single-fire trigger.
- [x] Restored custom 360° Hue Bar and color swatches for subject creation/editing.
- [x] App Launch Routing fix: Prevent app from forcing Sign-In screen on every startup.
- [x] Cloud Data Sync & Server Payload integrity audit with atomic upload and debug logs.
- [x] Dual Focus/Break custom theme pickers with soft color presets and 360° hue sliders.
- [x] Supabase remote authentication and cloud sync validated.
- [x] Dual-mode Subject & Focus Depth pie charts fully operational.
- [x] Report a Problem & Feedback modal with pre-filled device diagnostics and clipboard fallback.
- [x] Feedback rate limiting with 5-minute local cooldown, live countdown, and urgent help fallback.
- [x] Backend Feedback API (`POST /api/feedback`) with IP rate limiting and validation.
- [x] Feedback Reports table in Supabase (`feedback_reports`) with RLS policies.
- [x] Web Admin Feedback Dashboard (`/admin`) with filtering, inspection modal, 1-tap email replies, status transitions, and admin resolution notes.
- [x] Pie Chart visibility preference (`show_subject_pie_chart`) reactive toggle binding.
- [x] Complete Stopwatch mode isolation: raw focus time logging without subject tags or pie chart pollution.
- [x] Standard bottom navigation backstack contract: Focus/Timer is root anchor, secondary tabs pop to Focus, nested sub-screens pop to parent.
