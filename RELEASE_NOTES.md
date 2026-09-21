# StudyTimer 2.9.4 (versionCode 26)

What's New in Version 2.9.4 🚀
1. **Fixed Login & Account Isolation Bug**:
   - Resolved multi-user data merge when switching Google accounts. Each account now maintains completely separated local and cloud study records, stats, custom subjects, and timeline logs.
   - Fixed account identity detection in OAuth deep-link callbacks and dynamic user profile synchronization.

2. **Clean Account Switch & Reset**:
   - Logging out or switching accounts now resets local data cleanly before restoring cloud backups, preventing old profile information or study stats from leaking to new accounts.
   - Isolated guest mode sessions from authenticated cloud user sessions.

3. **Enhanced Cloud Sync & Security**:
   - Strict URL-encoded user ID isolation ensures multi-device cloud synchronization is 100% secure and isolated per account.
   - Clean local storage reset during cloud restoration prevents non-matching keys from previous accounts from lingering.

## Compatibility
- Android 9 (API 28) through Android 16 (API 36)

## Installation
Install `StudyTimer-release.apk` (v2.9.4). Updating preserves all your study logs, streaks, and settings.

