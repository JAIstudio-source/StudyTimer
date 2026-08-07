# StudyTimer 1.4.3 (versionCode 11)

## What's New (since 1.4.2)
- **Faster Insights loading** — the "crunching numbers" screen now appears for a fraction of a second. The study timeline is parsed once and cached in memory instead of being re-parsed repeatedly (previously it was re-parsed once per day-card in the History tab too), and date lookups use cheap day-range filters instead of per-entry date formatting.
- **Timeline moved out of SharedPreferences** — your session history now lives in its own file (`focus_timeline.json`) with atomic, crash-safe writes. This removes a large blob from the preferences file, which makes every timer tick, session toggle, and settings write noticeably lighter on the storage/CPU side.
- **Safe one-time migration** — existing timelines in the old storage location are migrated automatically on first launch and the old copy is cleaned up; if anything ever fails to write, data falls back to the previous location so nothing is lost.
- **Backup/restore kept fully compatible** — exports still embed the full timeline, and imports (including backups made by older versions) restore it correctly.
- **Smarter writes** — day totals and streak values are only written when they actually change, cutting unnecessary disk writes during every Insights open.
- **More unit tests** — 43 automated tests now also cover the new timeline serialization/parsing and editing logic.

## What's New (since 1.4.1)
- **Full localization** — every user-facing string (timer controls, Insights, settings, dialogs, notifications, widget, onboarding) now lives in `strings.xml` instead of being hardcoded, making the app fully translation-ready.
- **Under-the-hood rebuild** — core statistics logic was extracted into a dedicated engine and all three main screens (Focus, Insights, Settings) moved into their own focused classes, trimming MainActivity from ~5,500 lines to ~4,400. Same app, cleaner code, faster to maintain.

## Compatibility
- Android 9 (API 28) through Android 16 (API 36)
- Signed with the JAI Labs release key

## Installation
Install `StudyTimer-release.apk` (v1.4.3, ~3.0 MB). Updating preserves all study logs, streaks, and settings — your existing timeline is migrated automatically.
