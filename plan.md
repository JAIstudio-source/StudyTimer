# 📱 Plan: StudyTimer Android App — Complete Profile, Username & Moderation System Implementation

## 🎯 Executive Summary & Objective
Port the entire **Student Profile, Custom Display Name, Avatar Management, Moderation Lifecycle, and Telegram Approval Bot** workflow from `StudyTimer-website` into `StudyTimer-app` (Android).

This ensures:
1. Android students can configure custom display names, bios, target exams/grades, and avatar presets.
2. Custom display names undergo automated local pre-moderation (English + Hindi + Hinglish profanity scanning) and are submitted to the **`Studytimer_approval` Telegram Bot** with interactive review cards.
3. Real-time status tracking (`APPROVED`, `PENDING_APPROVAL`, `REJECTED`) is displayed with native badges and banners in the app.
4. Background cloud sync (`CloudSyncManager.kt`) and leaderboards (`LeaderboardManager.kt`) always use the **approved display name**, permanently eliminating old Google account name fallbacks while keeping 100% of study time, streaks, and timeline histories intact.

---

## 🏛️ System Architecture & Bot Separation Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            STUDYTIMER ECOSYSTEM                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   📱 Android App (StudyTimer-app)        💻 Admin Console (StudyTimer-admin)│
│   ├─ ProfileManager.kt                   ├─ User Audit Modal                │
│   ├─ ProfanityFilter.kt                  ├─ Soft Reset & Profile Sanitizer  │
│   ├─ CloudSyncManager.kt                 └─ 1-Click Database Restore        │
│   └─ ProfileBottomSheetDialog.kt                                            │
│                 │                                      │                    │
│                 ▼                                      ▼                    │
│   ┌─────────────────────────────────────────────────────────────┐           │
│   │            Supabase Cloud Database (user_sync_data)         │           │
│   │            - user_name (Approved / Active Display Name)     │           │
│   │            - prefs_data.__user_profile__ (Full Profile JSON)│           │
│   │            - profile_backups (Point-in-Time Snapshots)      │           │
│   │            - daily_leaderboard (Live Public Rankings)       │           │
│   └─────────────────────────────────────────────────────────────┘           │
│                 ▲                                                           │
│                 │                                                           │
│   🛡️ Profile Approval Bot (Studytimer_approval)                            │
│   ├─ Token: 8755792560:AAFrTNyOjveVTV9vtRgwVD6tkNMwfRBDG2k                  │
│   ├─ /queue, /audit, /backup, /restore commands                             │
│   └─ Inline Webhook [✅ Approve] / [❌ Reject] Callback Actions              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

> [!CRITICAL]
> **BOT SEPARATION RULE (AGENTS.md):**
> - Profile moderation is handled **ONLY** by `Studytimer_approval` (`8755792560:...`).
> - `StudyTimer_post` (`8707816892:...`) is **ONLY** for Threads/Bluesky auto-posting and must never be touched or mixed into profile approval logic.

---

## 📋 Phase-by-Phase Implementation Plan

### Phase 1: Data Model & Local Persistence (`ProfileManager.kt`)
- **File:** `StudyTimer-app/app/src/main/java/com/madeby/JAI/ProfileManager.kt`
- **Responsibilities:**
  - Define `UserProfile` data class:
    ```kotlin
    data class UserProfile(
        val displayName: String,
        val pendingDisplayName: String? = null,
        val bio: String = "",
        val targetExam: String = "",
        val targetDailyHours: Float = 2.0f,
        val avatarUrl: String = "",
        val avatarPresetId: String = "avatar_default",
        val moderationStatus: ModerationStatus = ModerationStatus.APPROVED,
        val rejectionReason: String? = null,
        val updatedAt: Long = System.currentTimeMillis()
    )
    
    enum class ModerationStatus {
        APPROVED,
        PENDING_APPROVAL,
        REJECTED,
        NONE
    }
    ```
  - Manage reading/writing `__user_profile__` JSON to `StudyTimerPrefs`.
  - Maintain backward compatibility with `AuthManager.getUserName()` and `custom_display_name`.

---

### Phase 2: Profanity & Vulgarity Filter (`ProfanityFilter.kt`)
- **File:** `StudyTimer-app/app/src/main/java/com/madeby/JAI/ProfanityFilter.kt`
- **Responsibilities:**
  - Port multi-language profanity detection rules from website (English + Hindi + Hinglish).
  - Check user input before submission. If blocked, provide immediate helpful feedback to the student before sending to the approval queue.
  - Sanitization rules: Trim whitespace, strip control characters, limit length to 30 characters.

---

### Phase 3: Profile Submission & Telegram Approval Webhook Integration
- **File:** `StudyTimer-app/app/src/main/java/com/madeby/JAI/ProfileSyncService.kt`
- **Responsibilities:**
  - When a student updates their display name:
    1. If the name is changed and differs from the approved name:
       - Set `moderationStatus = ModerationStatus.PENDING_APPROVAL`.
       - Store `pendingDisplayName` in local profile JSON.
       - Embed in `prefs_data.__user_profile__` during cloud sync.
    2. Dispatch an approval request card to the Telegram Approval Bot webhook endpoint (or via Cloudflare Worker `studytimer-approval-bot`):
       - Formatted review message: Student ID, Previous Name, Requested Name, Target Exam, Device Model, Submission Timestamp.
       - Interactive inline buttons: `[✅ Approve]` and `[❌ Reject]`.
    3. Update local state and trigger UI banner in Android app: `"⏳ Display name submitted for approval"`.

---

### Phase 4: Modern Android UI — Profile Editor & Status Center
- **Files:**
  - `StudyTimer-app/app/src/main/java/com/madeby/JAI/ui/ProfileBottomSheetDialog.kt`
  - `StudyTimer-app/app/src/main/res/layout/dialog_user_profile.xml`
  - `StudyTimer-app/app/src/main/java/com/madeby/JAI/SettingsPanelBuilder.kt`
- **UI Components:**
  - **Header Avatar & Status Ring:** Displays current avatar with a subtle color ring indicating status (Green = Approved, Amber = Pending, Red = Action Required).
  - **Display Name Input:** Input box with character counter, live validation, and status tag.
  - **Target Exam / Grade Selector:** Dropdown/chips (e.g. JEE, NEET, UPSC, Board Exams, College, Self-Study).
  - **Daily Study Target Slider:** Direct focus goal configuration (1h to 16h).
  - **Student Bio Input:** Short multi-line bio.
  - **Moderation Alert Banner:**
    - `APPROVED`: `"✅ Verified Student Display Name"`.
    - `PENDING_APPROVAL`: `"⏳ Your name '@new_name' is being reviewed by moderators. Active leaderboard name: '@approved_name'"`.
    - `REJECTED`: `"⚠️ Name change rejected: [Reason]. Please select another name."`.

---

### Phase 5: Cloud Sync & Leaderboard Harmonization (`CloudSyncManager.kt` & `LeaderboardManager.kt`)
- **Files:**
  - `StudyTimer-app/app/src/main/java/com/madeby/JAI/CloudSyncManager.kt`
  - `StudyTimer-app/app/src/main/java/com/madeby/JAI/LeaderboardManager.kt`
- **Sync Logic:**
  - When pushing to `user_sync_data` and `daily_leaderboard`:
    - If `moderationStatus == APPROVED`: Use `profile.displayName`.
    - If `moderationStatus == PENDING_APPROVAL`: Use `profile.displayName` (last approved) as public active name, while embedding `pendingDisplayName` in `prefs_data`.
  - When pulling cloud sync data in `mergeCloudAndLocalData()` or `restoreDataFromCloud()`:
    - Inspect `prefs_data.__user_profile__`.
    - If moderator approved the name on Telegram, update local `moderationStatus` to `APPROVED`, set `profile.displayName = newName`, update `AuthManager.updateUserName()`, and show celebratory toast: `"🎉 Your display name has been approved!"`.
    - If moderator rejected, update local `moderationStatus` to `REJECTED` and store rejection reason.

---

### Phase 6: Verification, Testing & Git Deployment
- **Verification Checklist:**
  1. Profile edit in Android app correctly updates local JSON and reflects immediately in the UI.
  2. Profanity filter blocks inappropriate words with instant UI warning.
  3. Submission creates a pending record in `user_sync_data` and sends a review card to `Studytimer_approval` Telegram bot.
  4. Moderation approval via Telegram bot correctly syncs back to the Android app on the next sync/poll without resetting any study data.
  5. Leaderboard displays the approved custom name and avatar correctly.
  6. Git commit and push cleanly to `StudyTimer-app` repo without touching unrelated bots.

---

## 🛠️ Execution Checklist for AI Agent

- [ ] **Step 1:** Create `ProfileManager.kt` with `UserProfile` model and preference serialization.
- [ ] **Step 2:** Create `ProfanityFilter.kt` with Hindi, English, and Hinglish blocklists.
- [ ] **Step 3:** Implement profile review webhook dispatch in `ProfileSyncService.kt` targeting the Approval Bot.
- [ ] **Step 4:** Build `ProfileBottomSheetDialog.kt` and layout resources with StudyTimer dark theme styling.
- [ ] **Step 5:** Integrate Profile Editor button into `SettingsPanelBuilder.kt` and `MainActivity.kt`.
- [ ] **Step 6:** Harmonize `CloudSyncManager.kt` and `LeaderboardManager.kt` to pull and push profile moderation statuses.
- [ ] **Step 7:** Compile, verify with Gradle / Android CLI, and commit to `StudyTimer-app`.
