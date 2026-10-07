# 🎬 StudyTimer — Master Animation & UI Polish Architecture Plan

This document details every screen, view, micro-animation, interpolator, timing, and tactile feedback mechanism designed for the StudyTimer Android application (`com.madeby.JAI`).

---

## 📐 Animation Design System & Principles

1. **Snappy & Natural:** Fast entrance durations (`110ms` - `220ms`) using spring physics (`OvershootInterpolator`) to prevent feeling sluggish.
2. **Tactile Haptic Pairing:** Every visual button spring compress is paired with a micro-haptic click (`HapticFeedbackConstants.KEYBOARD_TAP` or `LONG_PRESS`).
3. **Hardware Acceleration:** All view animations leverage `.animate().scaleX().scaleY().alpha()` to run cleanly on the RenderThread at 60/120fps.
4. **Theme Harmony:** Ambient glows and pulse rings inherit the active `ThemeCoordinator` color tokens (AMOLED, Neon, Pastel, Pure OLED).

---

## 📱 Screen-by-Screen Animation Architecture

### 1. ⏱️ Main Focus Screen (`FocusPanelBuilder.kt` & `TimerRingView.kt`)

#### A. Primary Action Button (Play / Pause / Resume)
* **Trigger:** Touch down / release on main start/pause button.
* **Touch Down:** Compress to `0.96x` scale over `90ms` with `DecelerateInterpolator()` + trigger micro-haptic tick.
* **Touch Release:** Elastic spring bounce to `1.0x` scale over `220ms` with `OvershootInterpolator(1.3f)`.
* **State Transition:** Icon morph crossfade between Play (▶) and Pause (❚❚) over `150ms`.

#### B. Timer Progress Ring Sweep & Breathing Tip Glow (`TimerRingView.kt`)
* **Trigger:** Active study session state (`TimerState.STUDYING`).
* **Progress Arc Sweep:** Canvas arc sweep path smoothly interpolated frame-by-frame.
* **Tip Glow Pulse:** Radial gradient pulse at the arc tip breathing between `0.70` and `1.00` alpha over a 3-second sinusoidal wave (`3000ms`).

#### C. Finish Session Button (`HoldRingButton.kt`)
* **Trigger:** Touch and hold on `"Hold to Finish"`.
* **Ring Fill:** Circular ring outline fills 0% to 100% over the hold duration.
* **Haptic Escalation:** Haptic tick rate increases exponentially as ring fill approaches 100%, completing with a distinct long-press vibration burst.

---

### 2. 📅 Planner & Goals Panel (`PlannerPanelBuilder.kt`)

#### A. Goal Checkbox Spring Pop & Strikethrough
* **Trigger:** Tapping a daily goal check circle or card row.
* **Elastic Pop:** Checkmark circle expands to `1.35x` scale over `110ms` with `OvershootInterpolator(2.5f)` + haptic tap feedback.
* **Elastic Return:** Returns to `1.0x` scale over `90ms` before committing state.
* **Text Strikethrough:** Animated paint flag strikethrough line drawn across goal title with text alpha reducing to `0.5`.
* **Progress Bar Fill:** Top daily goal progress bar animates percentage fill (`0%` to `100%`) over `350ms`.

#### B. Urgent Exam Countdown Border Pulse
* **Trigger:** Exams occurring within `< 3 days`.
* **Visual Effect:** Subtle amber/red outer glow border pulsing slowly (`1.0` to `0.4` alpha) every `2000ms` to indicate urgency without disturbing reading flow.

---

### 3. 📊 Insights & Stats Dashboard (`StatsPanelBuilder.kt` & `SubjectPieChartView.kt`)

#### A. Timeframe Switcher Numeric Count-Up
* **Trigger:** Switching tabs between Today / Week / Month / Year.
* **Value Animator:** Numeric values (e.g., `2h 15m` ➔ `14h 30m`, `85% Focus`) count up smoothly over `400ms` using `ValueAnimator` with `DecelerateInterpolator()`.

#### B. Pie Chart Slice Elevation & Center Text Crossfade (`SubjectPieChartView.kt`)
* **Trigger:** Tapping a slice on the subject pie chart.
* **Slice Elevation:** Selected slice pops outward by `12dp` with elastic spring extension.
* **Center Overlay Crossfade:** Center subject title, total duration, and percentage crossfade in `150ms`.
* **Auto-Reset:** Tapping outside the pie chart or switching app screens automatically retracts elevated slices.

#### C. Weekly Bar Chart Height Springs (`WeeklyCardView.kt`)
* **Trigger:** Changing date ranges or refreshing weekly view.
* **Bar Height Transition:** Each day's bar grows upward from `0dp` to its target duration height with staggered `30ms` delays across Mon-Sun bars.

---

### 4. 🏆 Leaderboard & Compare (`LeaderboardPanelBuilder.kt` & `StreakUpAnimationDialog.kt`)

#### A. Top 3 Podium Card Entrance
* **Trigger:** Opening Leaderboard panel.
* **Staggered Slide:** Rank 2 (Silver) and Rank 3 (Bronze) slide up from bottom (`0ms` delay). Rank 1 (Gold) drops in from above with a spring overshoot bounce (`100ms` delay).

#### B. #1 Champion Crown Shimmer
* **Trigger:** Periodic background loop (every 5 seconds).
* **Shimmer Sweep:** A bright metallic sheen path translates across the #1 player's crown badge over `600ms`.

#### C. Streak Level-Up Celebration Modal (`StreakUpAnimationDialog.kt`)
* **5-Stage Celebration Pipeline:**
  1. **Stage 1 (Entrance - 520ms):** Dark glass backdrop fades in + card scales up with `OvershootInterpolator(1.4f)`.
  2. **Stage 2 (Glow Ignition - 400ms):** Outer gold ring ignites with breathing radial pulse.
  3. **Stage 3 (Count Pop - 500ms):** Streak counter pop (`N` ➔ `N+1`) with elastic scale up (`1.4x`) and heavy haptic feedback.
  4. **Stage 4 (Embers - 3200ms):** Floating ember particles spawn and drift upward with gentle wave motion.
  5. **Stage 5 (Dismissal - 300ms):** Smooth alpha fade out on confirm button press.

#### D. Head-to-Head Compare Dialog Spring Entrance
* **Trigger:** Tapping a user row in Leaderboard to compare stats.
* **Dual Card Entrance:** Side-by-side player cards scale in from center (`0.85x` ➔ `1.00x`) with a central `VS` badge pop.

---

### 5. ⚙️ Settings & Customization (`SettingsPanelBuilder.kt`)

#### A. Theme Preset Swatch Spring Ring
* **Trigger:** Tapping a color theme preset chip.
* **Selection Ring Bounce:** Active selection border ring expands outward with `OvershootInterpolator(2.0f)`.
* **Window Background Crossfade:** Smooth `250ms` background color transition matching the newly selected theme palette.

#### B. Ambient Sound Equalizer Bars
* **Trigger:** Active ambient audio playback (Rain, Cafe, White Noise).
* **Equalizer Animation:** 3-bar animated equalizer icon adjacent to active sound chip with randomized vertical bar scaling (`30%` to `100%` height) every `180ms`.

---

## 🛠️ Summary Matrix of Animation Components

| Component | File Location | Interpolator | Duration | Haptic Feedback |
| :--- | :--- | :--- | :--- | :--- |
| Goal Check Pop | `PlannerPanelBuilder.kt` | `OvershootInterpolator(2.5f)` | `110ms` + `90ms` | `KEYBOARD_TAP` |
| Action Button Press | `FocusPanelBuilder.kt` | `Decelerate` / `Overshoot(1.3f)` | `90ms` / `220ms` | `VIRTUAL_KEY` |
| Progress Ring Tip Glow | `TimerRingView.kt` | Sinusoidal Alpha Wave | Continuous (`3s`) | N/A |
| Streak Celebration | `StreakUpAnimationDialog.kt` | `OvershootInterpolator(1.4f)` | Multi-stage (`4s`) | Heavy Pulsed |
| Stat Count-Up | `StatsPanelBuilder.kt` | `DecelerateInterpolator` | `400ms` | N/A |
| Pie Slice Elevation | `SubjectPieChartView.kt` | `OvershootInterpolator` | `150ms` | `KEYBOARD_TAP` |
