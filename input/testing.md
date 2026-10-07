# StudyTimer Comprehensive Test & Audit Suite (`testing.md`)

This document outlines the systematic, end-to-end testing roadmap and real-device audit strategy for **StudyTimer**. Testing is executed live via the sandboxed Developer Test Engine API (`http://10.73.183.155:8080`) using structured JSON data and headless UI hierarchy inspection.

---

## 1. Test Architecture & Methodology

### Sandboxed Developer API Tooling
- **`GET /status`**: Device connection, build version, current active `Activity` state.
- **`GET /ui`**: Live JSON dump of complete UI element tree (class names, resource IDs, bounds, visibility, clickability, text).
- **`GET /click?id=X|text=Y|x=X&y=Y`**: Synthetic click execution with fuzzy text matching.
- **`GET /input?id=X&text=Y`**: Direct text insertion into target `EditText` fields.
- **`GET /nav?panel=FOCUS|STATS|SETTINGS|LEADERBOARD`**: Rapid programmatic navigation across top-level panels.
- **`GET /back`**: Back navigation & back-stack dispatcher evaluation.
- **`GET /dev_unlock`**: Instant developer suite activation.
- **`GET /state` & `GET /logs`**: Runtime diagnostics and log output.

---

## 2. Comprehensive Test Modules & Test Cases

### Module A: Timer Modes & Time Calculations
- [ ] **Stopwatch Mode**: Start, pause, resume, hold-to-finish. Verify elapsed seconds match display `TextView` (`00:00:00`).
- [ ] **Target Countdown Mode**: Target duration selection, countdown precision, completion alert trigger.
- [ ] **Pomodoro Mode**: Work/break cycle switching, auto-break triggers, long break intervals, count tracking.
- [ ] **Lecture Mode**: Fixed class schedule duration locking, active lecture subject binding.
- [ ] **Midnight Rollover & Boundary Verification**:
  - Test session start before 00:00 and finish after 00:00.
  - Verify `openFocusStart` is strictly constrained to the current day (`startMs until endMs`).
  - Verify unclosed session gap capping (max 4 hours).
  - Verify zero-jump timer behavior on first start of the day.

### Module B: Subject & Tag Management
- [ ] **Subject Selection**: Switching active subject tag on Focus panel.
- [ ] **Custom Subject Creation**: Emoji selector, color picker, custom name validation, duplicate handling.
- [ ] **Subject Deletion & Re-assignment**:
  - Delete custom subject with historical focus time.
  - Verify historical durations and timeline logs auto-reassign to `"general"`.
  - Verify `General` subject cannot be deleted.
- [ ] **Subject Re-addition & Hidden Subject Resolution**:
  - Re-add previously deleted custom subject name.
  - Verify subject is unhidden cleanly without duplicate key conflicts.

### Module C: Insights, Stats Engine & Data Integrity
- [ ] **Self-Healing Sanitization**: Verify `sanitizeAndHealHistoricalTotals()` auto-corrects corrupted SharedPreferences totals on startup.
- [ ] **Timeline Logging (`TimelineLogger.kt`)**: Verify timeline block creation, editing, deduction, and JSON serialization integrity.
- [ ] **Pie Chart & Segment Ring (`SubjectPieChartView.kt`, `SegmentRing`)**: Verification of percentage calculations and legend breakdown matching raw duration totals.
- [ ] **Streak & Active Days Calculation**: Streak continuation, grace period rules, rest day handling.

### Module D: Leaderboard, Supabase & Cloud Sync
- [ ] **Leaderboard Rank Calculation**: Verification of total focus score, user display name formatting, avatar loading.
- [ ] **Profile Sync (`ProfileSyncService.kt`)**: Syncing local focus metrics to cloud database cleanly.

### Module E: Planner, Goals & Exam Countdowns
- [ ] **Goal Scheduler (`GoalReminderScheduler.kt`)**: Creating daily goals, progress tracking against active timers.
- [ ] **Exam Countdown Manager (`ExamCountdownManager.kt`)**: Date calculation accuracy, days remaining formatting.

---

## 3. UI / UX Design & Ergonomics Audit

- [ ] **Visual Spacing & Grid Alignment**: Inspect padding, margins, card alignment across screen sizes.
- [ ] **Contrast & Dark/AMOLED Mode**: Enforce minimum 4.5:1 text-to-background contrast ratio for readability.
- [ ] **Touch Target Sizing**: Ensure interactive buttons meet minimum 48x48dp touch target guidelines.
- [ ] **Information Architecture**: Ease of navigating between Focus, Insights, Leaderboard, and Settings.

---

## 4. Typography, Copywriting & Text Polish Audit

- [ ] **Typeface Hierarchy**: Consistent use of `sans-serif-medium` and `sans-serif-bold` across headers, subheaders, and body text.
- [ ] **Text Scaling & Truncation**: Test long subject names and metrics for truncation (`ellipsize = TruncateAt.END`) without clipping.
- [ ] **Micro-Copy Tone & Clarity**: Standardize toast messages, confirmation dialogs, and empty states.

---

## 5. Animation, Fluidity & Micro-Interactions Audit

- [ ] **Timer Ring Animation (`TimerRingView.kt`)**: Smooth progress ring rendering, zero jitter during second updates.
- [ ] **Hold-to-Finish Button (`HoldRingButton.kt`)**: Fluid hold fill animation and touch cancel behavior.
- [ ] **Panel Transitions & Modal Sheet Entrance**: Evaluation of glassmorphism dialog animations and entrance easing.

---

## 6. Real-Device Execution Log & Findings

| Test ID | Module / Feature | Status | Observed Behavior | Required Improvement / Action |
|---|---|---|---|---|
| TC-01 | Test API Connection | PASS | Endpoint responding with JSON on port 8080 (`http://10.73.183.155:8080`) | Operational |
| TC-02 | Historical Totals Healing | PASS | Corrupt total reset to 0h 0m on startup | Operational |
| TC-03 | Local Network Security | PASS | `network_security_config.xml` updated | HTTP OTA allowed |
| TC-04 | Subject Selection & UI Pill | PASS | `📖 General ▾` pill rendered cleanly with touch action | Operational |
| TC-05 | Timer Mode Operations | PASS | Start, Pause, and Resume state transitions verified live on device | Operational |
| TC-06 | Multi-Panel Navigation | PASS | `/nav` endpoint smoothly switches between Focus, Stats, Leaderboard, Settings | Operational |
| TC-07 | UI/UX & Spacing Audit | PASS | Pure OLED theme, 48dp touch targets, clean card paddings verified | Operational |
| TC-08 | Typography & Contrast | PASS | High contrast text, crisp custom font hierarchy verified via screenshot audit | Operational |
