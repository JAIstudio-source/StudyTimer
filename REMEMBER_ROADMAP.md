# 📌 StudyTimer — Future Roadmap, Ideas & Architecture Notes
> **Created:** September 2026  
> **Status:** Pending Review / Staged for Future Releases  
> **Purpose:** Reference document for planned features, device identity solutions, and Telegram integration.

---

## 1. 📱 Device Identity & Guest Re-install Deduplication

### 🎯 Problem Statement
- Android creates a fresh random UUID in `SharedPreferences` on every fresh install or data wipe.
- Multiple uninstalls/re-installs by the same user appear as multiple disconnected "Guest" entries in the Admin Panel and Supabase database.

### 💡 Proposed Solutions

#### A. Persistent Hardware Device ID (Android App)
Use a hardware-tied identifier in `StudyTimer-app` that survives uninstalls and data clears:
- **Option 1 (`Settings.Secure.ANDROID_ID`):**
  ```kotlin
  val persistentDeviceId = Settings.Secure.getString(
      context.contentResolver,
      Settings.Secure.ANDROID_ID
  ) ?: UUID.randomUUID().toString()
  ```
- **Option 2 (Widevine DRM Hardware UUID):**
  A stable hardware UUID generated via Android's MediaDrm API (unique to the physical silicon, 100% persistent across uninstalls).

#### B. Admin Panel "Ghost Install" Filters & Cleaner
- **Smart Filter:** Add a toggle in the Admin Console: `[Hide 0-Study Installs]` (filters out installs with `< 60s` total study time or `0` sessions).
- **Merge by Hardware ID:** Group guest records by `device_hardware_id` so all 5 re-installs of the same phone merge into 1 single physical timeline.
- **Orphan Cleanup Tool:** 1-click purge in Admin Console to delete guest records older than 14 days with zero study time.

#### C. Guest-to-Google Account Linking
- When an anonymous user signs in with Google, send a `link_account` event with both `anonymous_id` and `user_id`.
- Automatically re-assign previous guest sessions to the authenticated account in Supabase.

---

## 2. 🤖 Telegram Bot Integration & Admin Mobile Management

### 🎯 Goal
Manage the StudyTimer platform, view live stats, inspect users, and receive instant crash/feedback alerts directly inside Telegram.

### 💡 Implementation Approaches

#### A. Telegram WebApp (Mini App inside Telegram)
- Embed the Admin Console URL (`https://your-domain.vercel.app/admin/`) directly into a Telegram Bot.
- Tapping a button in Telegram opens the full admin dashboard in a native-feeling mobile sheet with charts, user reports, and cloud rescue downloads.
- **Bot Setup Example:**
  ```python
  from telegram import InlineKeyboardButton, InlineKeyboardMarkup, WebAppInfo
  
  keyboard = [[
      InlineKeyboardButton("📊 Open Admin Console", web_app=WebAppInfo(url="https://your-site.vercel.app/admin/"))
  ]]
  ```

#### B. Bot Chat Commands (Direct Supabase API Interaction)
A lightweight Node.js or Python Telegram Bot communicating with Supabase REST API:

| Command | Action / Response |
| :--- | :--- |
| `/today` or `/stats` | Live summary: Today's DAU, active students studying right now, total focus hours today. |
| `/leaderboard` | Top 10 ranks for today with student names and study times. |
| `/user <name_or_id>` | Instant report: Today's time, lifetime hours, streak, last active device, and snapshot status. |
| `/crashes` | List recent unhandled exceptions, app versions, and stack traces. |
| `/delete_user <id>` | Delete an abusive or test account with inline confirmation buttons. |

#### C. Automated Push Notifications to Admin Telegram Chat
- **🚨 Crash Alert:** Instant Telegram ping when an unhandled crash or exception is logged.
- **💬 Feedback Ticket Alert:** Instant notification when a student submits an in-app bug report or suggestion.
- **🌙 Daily 9 PM Briefing:** Automated evening summary message with today's study totals and top students.

---

## 3. 🛡️ Anti-Cheat & Leaderboard Protections (Hardened & Protected)

- [x] **Max Velocity & Rate Limit Clamp:** Real-world elapsed time velocity verification in `sync_study_progress_leaderboard` and `record_study_session_leaderboard` preventing rapid script/bot flood spam.
- [x] **Timestamp & Date Sanity Check:** Reject sessions with future dates or dates older than 3 days. Maximum daily study time strictly capped at 18 hours (64,800s) human limit.
- [x] **Shadowban & Ban Flag:** Added `is_banned` flag in `daily_leaderboard` and `user_sync_profiles` with auto-filtering in leaderboard RPC queries.

---

## 4. 🗄️ Cloud Sync & Backup Enhancements

- [x] **Idempotent Offline Delta Sync RPC (`sync_offline_study_sessions`):** Safe multi-session batching with Last-Write-Wins (LWW) conflict resolution and `session_uuid` deduplication.
- [ ] **Automated 30-Day Backup Pruning:** Scheduled Supabase Cron (`pg_cron`) to keep database storage lightweight on free tier while safeguarding the latest backup snapshot.

---

## 5. ✅ Completed in Previous Updates
- [x] Fixed REST API DELETE permissions with `Prefer: return=representation` and `fix_admin_permissions.sql`.
- [x] Added Full User Report & Data Audit Modal with date-wise study breakdowns and today's focus time on Backup/Restore page.
- [x] Added multi-source user coalescing across cohorts, sync data, snapshots, delta sessions, and leaderboard.
- [x] Added 1-click CSV spreadsheet exporters for Leaderboard, Cohorts, Backups, Events, and Crashes.
- [x] Added in-app Database Permissions Diagnostics tool.
- [x] Created `harden_backend_security.sql` with full anti-cheat rate-limiting, velocity verification, and offline sync RPC.
