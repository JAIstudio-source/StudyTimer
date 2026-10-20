# StudyTimer 3.3.3 (versionCode 47)

### What's New in Version 3.3.3 🚀
- **Fixed Pomodoro Overtime Vibration:**
  - Resolved an issue where the phone would vibrate continuously every second once a Pomodoro timer hit 00:00 during overtime. Vibration and sound now trigger cleanly once as designed.
- **Type Safety & Crash Prevention:**
  - Hardened SharedPreferences number parsing to eliminate `ClassCastException` runtime crashes when loading cloud sync or backup data (e.g. wallpaper dimmer alpha and streak values).
- **Streamlined App Footprint:**
  - Completely purged legacy visual Summary Card view, layouts, and bitmap generators.
  - Completely removed CSV spreadsheet export and cleaned up settings menus for a cleaner, leaner experience.
- **Deep Codebase Hardening:**
  - Integrated midnight rollover fixes, cloud sync clock-skew tolerances, and reliable room member polling.

---

## Compatibility
- Android 9 (API 28) through Android 16 (API 36)

## Installation
Install `StudyTimer-release.apk` (v3.3.3). Updating preserves all your study logs, streaks, and settings.


