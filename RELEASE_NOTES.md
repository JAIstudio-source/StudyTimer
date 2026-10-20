# StudyTimer 3.3.1 (versionCode 45)

### What's New in Version 3.3.1 🚀
- **Google Sign-In Reliability & Loop Fix:** Resolved the account selection loop where selecting a Google account looped back to the sign-in screen. Added robust fallback handling for missing Play Services tokens and Google OAuth deep links, ensuring users reliably enter the app.
- **Settings UI Simplification & Redundancy Clean-up:**
  - **Eliminated Duplicate Settings:** Removed redundant cloud backup controls scattered in Profile settings; all cloud backups and sync management are now unified in the dedicated "Cloud & Backups" tab.
  - **Streamlined Card Layouts:** Consolidated crowded sub-screens (Timer reduced from 5 cards to 2; Goals and Community streamlined into 2 focused cards each) for a modern, clutter-free look.
  - **Conditional/Dynamic Visibility:** Sub-options only reveal when their parent switch is active (e.g., auto-pause on face up only displays when Flip to Focus is enabled, reminder time picker only appears when Daily Goal Reminder is toggled on, and live study status only reveals when participating in leaderboards).
- **Planner & Goals UI Enhancements:**
  - **Yellow Goal Completion Styling:** Completed goals and planner checklist items now render with crisp `#FACC15` yellow accents and badges.
  - **Preserved Duration & Remaining Time Display:** Completed goals properly show elapsed and remaining time (e.g. `15/30m (15m left)`) rather than hiding duration details.
  - **History Grid Matrix Fix:** Cross marks (`✕`) now strictly only appear on past missed days, never on today's active goals (which display the in-progress `•` indicator or `✓` upon completion).
  - **Metrics Clean-up & Daily Inspiration:** Removed "Best Streak" and "Most Consistent" metrics from beneath planner goals, replacing them with a sleek **Daily Inspiration** motivational quote card right below the goals list.

---

## Compatibility
- Android 9 (API 28) through Android 16 (API 36)

## Installation
Install `StudyTimer-release.apk` (v3.3.1). Updating preserves all your study logs, streaks, and settings.

