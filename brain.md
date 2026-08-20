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
│           ├── AppConfig.kt              # Feature flags & Google Play build configuration
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
   - **Pomodoro Freedom Mode (`pomodoro_freedom_mode`)**: Boolean key (default `false`). When enabled, disables auto-transition to breaks and session count caps, extends focus interval configuration up to 24 hours (1440 minutes), and logs focus sessions continuously uninterrupted without forcing a break state upon interval completion.
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
- **Developer Menu Secret Triggers**:
  - **Footer Credit 5-Tap Gesture**: Tapping the footer credit text (`"Developed by Pushkar Saini"`) 5 times consecutively unlocks developer mode (`isDevModeUnlocked = true`), displays a `"Developer Mode Activated"` toast, and immediately navigates to `AppSettingsTab.DEVELOPER` (`Developer & Advanced`).
  - **Settings Header Long-Press**: Long-pressing the top "Settings" header text also unlocks developer mode.
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
### K. Top-Level Navigation Backstack & Settings Gesture Contract
- **Primary Root Anchor (`AppPanel.FOCUS` / Timer Screen)**:
  - Serves as the primary start destination and base anchor of the application.
  - The exit confirmation / double-tap exit flow (`finish()`) is strictly scoped to `AppPanel.FOCUS`.
- **Settings Single-View Architecture**:
  - Horizontal drag / swipe pager gestures are completely removed from Settings.
  - Settings renders as a clean single vertical scroll view. Sub-screens (User Profile, Developer Tools, Preferences) are navigated strictly via explicit card/button clicks.

### L. Full-Screen Landscape Timer & Sensor Rotation
- **Orientation & Immersive Mode**:
  - `MainActivity` dynamically reacts to sensor rotation via `onConfigurationChanged`.
  - In **Landscape Mode**:
    - Switches to an immersive edge-to-edge layout (`applyImmersiveModeForLandscape` / `hideSystemUI`).
    - Top headers, navigation bars, quotes, and floating icons are hidden.
    - Timer digits expand to 104sp bold monospace centered in pure OLED black.
    - Minimalist compact bottom action row provides centered Start/Pause and Reset/Hold controls.
  - In **Portrait Mode**:
    - Restores standard system UI bars and full interactive components.
- **Secondary Top-Level Tabs (`AppPanel.SETTINGS`, `AppPanel.STATS` / Insights)**:
  - Pressing the system Back button on secondary top-level destinations immediately routes the user back to the start destination (`AppPanel.FOCUS`).
  - Secondary top-level screens never intercept back presses to force an exit confirmation dialog.
### L. Insights Screen Layout Hierarchy & Swiping Structure
- **Three-Tier Layout Hierarchy**:
  1. **Fixed Top Header**: Title "Insights" and dismissible daily quote ribbon fixed at the top outside the scrollable body, remaining persistent and visible without duplicating across tabs or scrolling away.
  2. **Scrollable/Swipable Tab Body**: Underneath the fixed header (`weight = 1f`), containing exclusively the active tab's cards/lists (`Overview`, `Timeline/History`, `Planner`) with `88dp` bottom padding so content is never obscured.
  3. **Floating Bottom Pill Nav Bar (`InsightsPillNavBar.kt`)**: Anchored at the bottom overlay (`Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL`, margins: start=24dp, end=24dp, bottom=16dp).
- **Design System & AMOLED Polish**:
  - Cards elevated with deep slate surface (`#121318` to `#161820`), subtle 1dp border outline (`#282A36`), and 26dp corner radius over pure AMOLED black.
  - Geometric, clean sans-serif typography (`sans-serif-medium` / bold metrics) replacing handwritten fonts.
- **Insights Sub-Tab Floating Pill Bar (`InsightsPillNavBar.kt`)**:
  - Styled with neutral AMOLED slate capsule background (`#16171D`), 1dp border (`#2A2B36`), 32dp corner radius, and 20dp elevation shadow.
  - **Decoupled Theme Accent**: Uses an independent, dedicated cool slate-indigo analytics accent (`ThemeCoordinator.INSIGHTS_NAV_ACCENT = #6366F1`) for indicator pills and active icon states rather than inheriting the user's global `focusAccent`, preventing monochromatic color overload.
  - **Fluid Spring Physics**: Tuned with `OvershootInterpolator(1.18f)` (damping ratio `0.80f`) for snappy, responsive sliding without sluggish drag.
  - **Micro-Interactions & Haptics**: Touch scale bounce ($0.92\times$ on touch down $\rightarrow 1.0\times$ on touch up), icon scaling (`1.15x`), label cross-fade expansion, and precise haptic feedback (`HapticFeedbackConstants.KEYBOARD_TAP`).
  - **Pre-Caching & Instant Memory-Retention**:
    - `StatsSnapshot` is computed asynchronously on background threads and cached in `statsSnapshotCache`.
    - Sub-tab views (`Overview`, `Timeline/History`, `Planner`) are pre-warmed and retained in memory via `tabPageCache` (`getOrBuildTabPage`).
    - Switching tabs executes zero blocking main-thread calculations/DB queries, swapping instantly from pre-warmed memory pages.
  - Dynamic scroll auto-hide: Scrolling down hides the Insights pill bar; scrolling up restores it smoothly.
- **Root Screen Bottom Bar Removal**:
  - The application features zero fixed bottom navigation bars on Timer, Settings, or other root screens, maximizing full-screen immersion.

### M. Google Play Policy Compliance & User Data Rights
- **Account & Cascading Data Deletion**:
  - **In-App Deletion Flow**: Accessible in Settings $\rightarrow$ Account & Profile. Requires a severe warning modal with explicit confirmation by typing `"DELETE"`.
  - **Cascading Deletion Scope**:
    1. Remote Cloud Wipe: `CloudSyncManager.deleteUserCloudData()` purges all remote Supabase/backend records and storage files associated with `userId`.
    2. Local Data Wipe: `AuthManager.deleteLocalUserData()` clears local SharedPreferences, resets Room database/timeline logs, deletes backup files, and clears session tokens.
    3. Navigation Reset: Clears task stack and routes user to `LoginActivity`.
  - **Web Deletion URL**: Dedicated web portal URL (`https://get-studytimer.vercel.app/delete-account.html`) accessible in-app and in the Google Play Data Safety section.
- **Legal & Privacy Policy Compliance**:
  - Direct in-app clickable links under Settings $\rightarrow$ Legal & Policies:
    - Privacy Policy: `https://get-studytimer.vercel.app/privacy.html`
    - Terms of Service: `https://get-studytimer.vercel.app/terms.html`
    - Account Deletion Portal: `https://get-studytimer.vercel.app/delete-account.html`
- **Notification Permission (Android 13+ / API 33+)**:
  - Before starting any focus session or break timer from `IDLE` state, checks `Manifest.permission.POST_NOTIFICATIONS`.
  - If ungranted, presents a themed rationale dialog explaining that notifications are required to alert the user when focus intervals or breaks conclude even when the screen is locked, before launching `ActivityCompat.requestPermissions`.
- **Foreground Service Manifest Declaration**:
  - `TimerService` explicitly declared in `AndroidManifest.xml` with `android:foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` for persistent background study session tracking.

### N. Security Architecture & Defensive Standards
- **Secrets Management**:
  - Production backend endpoints load Supabase credentials exclusively from `process.env.SUPABASE_URL` and `process.env.SUPABASE_ANON_KEY`.
  - Zero hardcoded fallback credentials in production API routes.
  - Template variables documented in `.env.example` with `.env` permanently excluded from version control in `.gitignore`.
- **Sliding-Window Rate Limiting (`api/feedback.js`)**:
  - 20 requests per minute per client IP using an in-memory sliding window cache.
  - Automatically emits standard rate headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, and `Retry-After` upon HTTP 429 breach.
- **Strict Input Validation & Sanitization**:
  - Schema-enforced category whitelist (`['BUG_REPORT', 'FEATURE_REQUEST', 'GENERAL_FEEDBACK']`).
  - Max length capping on messages (5 to 2000 chars) and contact identifiers (120 chars).
  - HTML character sanitization on user-supplied strings before database insertion to eliminate stored XSS.
  - Whitelist-only schema filtering on diagnostic metadata.
- **HTTP Security Response Headers (`vercel.json`)**:
  - Enforces `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, and `Strict-Transport-Security`.
- **Sanitized Error Responses**:
  - API responses strictly return generic, user-safe error messages with zero database internals, schemas, or stack traces leaked to clients.

### O. Authentication & Onboarding Screen Visual Design (`LoginActivity.kt`)
- **Aesthetic & Brand Polish**:
  - **Background**: Pure `#000000` AMOLED canvas with edge-to-edge system bars.
  - **Ambient Brand Glow**: Logo centered over an ambient cyan/blue radial gradient glow (`bg_logo_ambient_glow.xml`), removing all hard bounding boxes.
  - **Typography & Hierarchy**:
    - App Header: Crisp bold `29sp` with tight letter spacing (`-0.02`) in modern `sans-serif-medium`.
    - Subtitle: `Focus, Track & Master Your Habits` in muted slate gray (`#94A3B8`).
    - Recommendation Note: `12sp` body in light slate (`#64748B`).
- **Interactive Action Buttons**:
  - **Google Sign-In**: Pill container (`27dp` corner radius) with clean `#FFFFFF` fill, bold `#0F172A` text, official 4-color Google G icon (`ic_google_logo.xml`), and `0.97f` press-scale animation.
  - **Guest Mode CTA**: Secondary outlined pill button (`#334155` border, transparent fill, `#CBD5E1` text) with `0.97f` touch bounce.
- **Legal Compliance**:
  - Interactive legal notice: `"By continuing, you agree to our Terms of Service and Privacy Policy."`
  - Inline accent links (`#818CF8`) launching in-app Custom Tabs (`androidx.browser.customtabs.CustomTabsIntent`) with system browser fallback.
- **Footer**:
  - Grounded bottom note: `"© Pushkar Studio • 100% Offline & Secure"` in `#475569`.

### P. Settings Hub Schema & Unified Card Architecture
- **Unified Card Layout Architecture (`SettingsPanelBuilder.kt`)**:
  - **Top Navigation Back Header**: Persistent `←` back arrow icon in the top header row next to the Settings title (navigates back to Timer from Hub, and to Hub from sub-screens).
  - **Fixed Floating Back Button Overlay (Box / FrameLayout)**:
    - Root `FrameLayout` wraps all Settings views and anchors a true floating overlay `settingsBackFab` at `Gravity.BOTTOM or Gravity.END` (margins: `end=20dp, bottom=20dp`).
    - Pill button dimensions: Height `48dp`, corner radius `24dp`, `6dp` elevation, `24dp` horizontal padding, primary accent fill (`themeCoordinator.primaryColor`), and bold `14.5f` typography.
    - Floating overlay is consistent across the main Hub and all sub-screens without any artificial footer container or background bar.
  - **Main Settings Hub Only Developer Credits (Natural End-of-Scroll)**:
    - Sub-settings screens contain **zero** developer credits for a focused, uncluttered experience.
    - Main Settings Hub appends the credits cleanly as the final scrollable item (`top margin = 24dp`) with `"Developed by Pushkar Saini"` (5-tap secret trigger to unlock the Developer Menu) and `"Special thanks to Nikhil Tyagi"`.
  - **Zero-Overlap Content Padding**: All settings scroll containers enforce `setPadding(dp(16), 0, dp(16), dp(96))` so that bottom items and cards scroll clear of the floating Back button.
  - **Unified "PREFERENCES" Card**: Combines all core settings categories into a single elevated card container with subtle `#22232B` divider lines:
    1. Timer & Focus Controls (`AppSettingsTab.TIMER`)
    2. Sound & Ambience (`AppSettingsTab.AMBIENCE`)
    3. Analytics & Goals (`AppSettingsTab.ANALYTICS`)
    4. Cloud, Sync & Backups (`AppSettingsTab.CLOUD`)
    5. Theme & Appearance (`AppSettingsTab.THEME`)
    6. Developer & Advanced (`AppSettingsTab.DEVELOPER`, visible when unlocked)
  - **Unified "ABOUT & LEGAL" Expandable Card (Default Collapsed)**: Condenses all secondary and policy items into a single collapsible card accordion (defaults to collapsed `isAboutExpanded = false`):
    1. **How to Use / App Guide**: Launches in-app user guide modal (`MainActivity.showAppGuideDialog`).
    2. **Report a Problem & Feedback**: Launches interactive feedback and bug triage modal (`MainActivity.showFeedbackReportDialog`).
    3. **Version & Check for Updates**: Displays `Version v${currentVersionName()} (Build ${currentVersionCodeLong()})` with manual check chip gated by `AppConfig.ENABLE_GITHUB_UPDATE_CHECK`.
    4. **Privacy Policy**: Opens external web policy (`https://get-studytimer.vercel.app/privacy.html`).
    5. **Terms of Service**: Opens terms & licensing (`https://get-studytimer.vercel.app/terms.html`).
    6. **Account Deletion Web Portal**: Direct link for Google Play Data Safety compliance (`https://get-studytimer.vercel.app/delete-account.html`).
- **Configurable Update Check Flag (`AppConfig.kt`)**:
  ```kotlin
  object AppConfig {
      // Set to false before releasing to Google Play Store
      const val ENABLE_GITHUB_UPDATE_CHECK = true
  }
  ```
  - When `ENABLE_GITHUB_UPDATE_CHECK = false`:
    - In-app manual update checks display an immediate "up to date" toast without network calls to GitHub.
    - Background startup update checks (`checkForUpdates(manual = false)`) are completely bypassed.
    - Ensures 100% compliance with Google Play Store Developer Program policies regarding third-party APK update distribution.

### Q. Google Play Data Safety & Compliance ([PLAY_STORE_DATA_SAFETY.md](file:///d:/Download/My%20app/website/New%20folder/StudyTimer/PLAY_STORE_DATA_SAFETY.md))
- **App Compliance Source of Truth**:
  - Maintained in root [`PLAY_STORE_DATA_SAFETY.md`](file:///d:/Download/My%20app/website/New%20folder/StudyTimer/PLAY_STORE_DATA_SAFETY.md).
  - Target audience: 13+ (Students / General Audience, COPPA compliant).
  - 100% Free: No ads, no in-app purchases, no subscriptions, no user-facing GenAI.
  - Data collection: Optional (Guest mode default; Supabase Auth & Cloud Sync on login only).
  - Encryption: TLS 1.3 / HTTPS for all in-transit data.
  - Account & Data deletion: In-app deletion + Web Portal (`https://get-studytimer.vercel.app/delete-account.html`).
  - Legal contact: `studytimer737@gmail.com`.

---

## 5. UI/Theme Conventions

- **AMOLED Dark Mode**: True `#000000` background with `#121212` elevated cards for maximum battery preservation on OLED screens.
- **Eclipse Mode**: Dark Slate Navy (`#0F172A` background, `#1E293B` containers).
- **Light Mode**: Clean White (`#FFFFFF` background, `#EDF0F5` containers).
- **Dual Accent Color Architecture (`ThemeCoordinator.kt`)**:
  - **Focus Accent Token (`customPrimary`)**: Drives focus timer progress rings, active study session badges, and primary action buttons.
  - **Break Accent Token (`customSecondary`)**: Drives break countdown rings, resting badges, and break state transitions.
  - **Insights Navigation Accent (`INSIGHTS_NAV_ACCENT`)**: `#6366F1` (cool slate-indigo) decoupled analytics token.
- **Random Theme / Accent Generator**:
  - `🎲 Roll` action button inside Theme & Appearance settings.
  - Instantly samples distinct harmonious colors from `SOFT_FOCUS_PALETTE` and `SOFT_BREAK_PALETTE` for both `focusAccent` and `breakAccent`, persists values immediately to SharedPreferences (`customPrimary`, `customSecondary`, `customHue`, `customSecondaryHue`), and triggers immediate live theme propagation.
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

## 5. Performance, Rendering & Architecture Standards

### A. Discrete Tab State Architecture & In-Memory Tab Caching
- **Discrete Tab State Switching (`AppStatsTab`)**:
  - Replaces heavy multi-page gesture swipe pagers with discrete, instantaneous Enum state-driven rendering (`AppStatsTab.OVERVIEW`, `AppStatsTab.TIMELINE`, `AppStatsTab.PLANNER`).
  - Child tab builders (`renderOverviewTabContent`, `CalendarTimeline(this).build`, `renderPlannerTabContent`) are **100% pure stateless views** receiving a pre-computed `StatsSnapshot` data model without internal coroutines, background collection loops, or database lookups.
- **In-Memory Tab View Retention (`tabPageCache`)**:
  - `MainActivity.tabPageCache` maintains fully constructed View hierarchies for all Insights tabs (`AppStatsTab.OVERVIEW`, `AppStatsTab.TIMELINE` / Calendar, `AppStatsTab.PLANNER`) in memory.
  - `prewarmTabPages()` pre-builds adjacent tabs upon entering Insights, eliminating view destruction/re-instantiation on tab switch.
  - Switching between Overview, History/Calendar, and Planner renders cached layouts instantly with 0ms latency and **zero redraw flicker**.
- **Memoized Snapshot Engine (`statsSnapshotCache` & `statsSnapshotGen`)**:
  - Heavy calculations (date matrix generation, monthly heatmap grid mapping, weekly trend aggregations, subject distribution ratios) are computed once and stored in `statsSnapshotCache`.
  - Data calculations do NOT re-run on tab switches. Invalidation only occurs when:
    1. A new timer session completes.
    2. The user returns to the main timer screen.
    3. The app resumes from background with new logged data.
- **Unified Screen-Scoped Grid Animation Architecture**:
  - `hasPlayedStatsEntranceAnimation: Boolean` tracks whether the entrance animation has played for the current Insights screen session.
  - When opening the Insights / History screen on a fresh session, a **single unified `ValueAnimator`** (650ms, `DecelerateInterpolator`) drives the 0.0f $\rightarrow$ 1.0f progress multiplier across all 35/42 calendar day cells.
  - Each `SegmentRing` in `CalendarTimeline` reads `progressSupplier = { gridAnim.animatedValue as? Float ?: 1f }` and updates directly without spawning individual per-cell timer threads or coroutines.
  - On tab switching within the same screen visit (Overview $\leftrightarrow$ History $\leftrightarrow$ Planner), `hasPlayedStatsEntranceAnimation` is `true`, snapping progress immediately to `1.0f` with zero animation delay or GC overhead.
  - When navigating back to the Focus/Timer or Settings screens, or when the app is minimized (`onStop`), `hasPlayedStatsEntranceAnimation` resets to `false`, ensuring returning to Insights smoothly replays the entrance animation.
  - **Leak-Free Tab View Caching (`tabViews: EnumMap<AppStatsTab, ScrollView>`)**: Tab views are created once per screen session and instantly remounted with `tabHost.removeAllViews()` on tab clicks, avoiding animator frame stacking, memory leaks, and GC spikes.

### B. Isolated Rendering, Zero-Allocation Canvas Drawing & Hardware Acceleration
- **Zero-Allocation `onDraw()` Architecture**:
  - All temporary heap allocations (`RectF`, `Path`, `LinearGradient`, `Paint`, dynamic `Color.argb` computations, array allocations) have been moved out of `onDraw()` into reusable class fields and `onSizeChanged()` hooks across all custom views (`TimerRingView`, `HeatmapView`, `SubjectPieChartView`, `HoldRingButton`, `BarTrackView`, `SegmentRing`).
  - Completely eliminates runtime GC pressure and memory churn during rapid tab swiping and countdown ticking.
- **Isolated 1-Second Ticking**: Active timer countdown ticking updates strictly inside `TimerRingView` and `HoldRingButton` via hardware-accelerated Canvas `invalidate()` without triggering layout measure/re-pass on the parent screen.
- **Hardware Layer Caching**: Custom Canvas views (`HeatmapView`, `SubjectPieChartView`, `WeeklyCardView`, `TimerRingView`, `HoldRingButton`) explicitly utilize `setLayerType(View.LAYER_TYPE_HARDWARE, null)` to eliminate frame drops during animations and scroll interactions.

### C. Typography & Font Weight Standardization
- **Strict Typography Scale**:
  - **Screen Titles & Major Headers**: `titleLarge` / `headlineSmall` (FontWeight 700 / Bold, 20–24sp).
  - **Card Headers & Category Labels**: `titleMedium` (FontWeight 600 / SemiBold, 16–18sp).
  - **Subheaders & Interactive Buttons / Chips**: `labelLarge` (FontWeight 500 / Medium, 13–14.5sp).
  - **Body Text & Secondary Metadata**: `bodyMedium` / `bodySmall` (FontWeight 400 / Normal, 11–13sp).
- **Typeface Mappings**: Standardized through `ThemeCoordinator` using `sans-serif-medium` for bold/semi-bold elements and `sans-serif` for clean normal body text.

### D. Offline-First Optimistic State Updates & Scaled Data Aggregations
- **Instant Local Mutation**: Habit toggles, goal completions, and timer session records mutate local storage immediately on user tap.
- **Optimized Aggregations & Zero UI Blocking**: Daily focus and break metrics are pre-aggregated and stored via direct daily keys (`${dateStr}_focus_total`, `${dateStr}_break_total`), avoiding expensive table scans across thousands of raw historical entries. Asynchronous calculations and Supabase cloud synchronization run strictly on background worker threads without stalling the main thread.

### E. Animation Fluidity & Spring Physics (60/120 FPS Target)
- **Spring Physics Tuning**: Screen transitions, tab settle, and floating pill indicator morphing use low-stiffness spring overshoot physics (`dampingRatio = 0.8f`, `OvershootInterpolator(1.18f)` / `PathInterpolator(0.18f, 0.9f, 0.2f, 1.0f)`).
- **Touch Micro-Interactions**: Primary buttons, hold rings, and card surfaces use responsive press-scale physics (`0.96f` on press $\rightarrow$ `1.0f` on release with spring overshoot).
- **Zero Main-Thread Blocking**: `MainActivity.onCreate()` initializes only root UI elements and `ThemeCoordinator`. Non-critical services (`AppAnalytics`, `CrashReporter`, `BackupManager` restore checks, `NotificationChannels`, `GoalReminderScheduler`) execute asynchronously on background worker threads.

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
- [x] Comprehensive Insights UI/UX Redesign: AMOLED slate cards, 2x2 Highlights grid, circular goal ring, and 7-day planner consistency.
- [x] Floating Pill Tab Switcher exclusively inside Insights screen with spring animations and clean top header.
- [x] Focus Pattern visibility preference (`show_focus_pattern`) reactive toggle binding with quick hide card action.
- [x] Pomodoro Freedom Mode (`pomodoro_freedom_mode`): continuous extended focus up to 24h without forced break transitions or session caps.
- [x] Restored Random Theme / Accent Generator in Theme Settings (`🎲 Roll` action).
- [x] Decoupled Insights Floating Nav Bar Theme Accent (`#6366F1` cool slate-indigo on `#16171D` AMOLED slate container).
- [x] App-wide animation physics tuning (damping ratio 0.8f, 0.96f $\rightarrow$ 1.0f press scale).
- [x] Zero-blocking startup latency reduction with asynchronous background worker threads.
- [x] Hardware layer rendering optimization (`LAYER_TYPE_HARDWARE` on `HeatmapView`, `SubjectPieChartView`, `WeeklyCardView`, `TimerRingView`).
- [x] Memory cache engine for instantaneous 0ms analytics tab transitions.
- [x] Restored "About & Support" card group with App Guide, Feedback Report, and Version / Update Checker.
- [x] Integrated configurable feature flag `AppConfig.ENABLE_GITHUB_UPDATE_CHECK` for instant Google Play Store release compliance.
- [x] Restored Developer Menu secret 5-tap trigger on footer credit text ("Developed by Pushkar Saini").
- [x] Removed 7-Day Goal Consistency card from Planner tab layout (flows directly from Daily Goals to Planner Insights).
- [x] Refactored Settings with unified Preferences card, default-collapsed About & Legal accordion, and static bottom Back pill button.
- [x] Updated "Report a Problem" dialog: Replaced GitHub Issues button with clean bottom-right Dismiss/Close action.
- [x] Added Sign-Out Confirmation Dialog with clear data safety notice ("Your local study records will remain on this device, but cloud sync will pause until you sign in again.").
- [x] Removed redundant Privacy & Data card from User Profile; consolidated all legal and compliance links into the Settings About & Legal hub.
- [x] Implemented zero-latency `LocalAvatarManager` with local storage (`profile_avatar.jpg`), smart bounds downsampling, square center-cropping, circular masked rendering, and instant zero-network profile updates.
- [x] Polished User Profile UI: Centered 88dp circular avatar with camera edit badge (`📷`), user credentials, account status chip (`✓ Google Account` / `👤 Guest Session`), sync status, red-tinted sign-out, and permanent deletion action.



