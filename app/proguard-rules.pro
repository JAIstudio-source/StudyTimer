# ── App Code ───────────────────────────────────────────────────
# Only keep what's needed for reflection (JSON serialization, enum valueOf)
# instead of keeping EVERYTHING which defeats obfuscation entirely.

# Keep data model classes used with org.json.JSONObject reflection
-keep class com.madeby.JAI.DataModels { *; }
-keep class com.madeby.JAI.StatsModels { *; }
-keep class com.madeby.JAI.UpdateInfo { *; }
-keep class com.madeby.JAI.CloudSyncManager$CloudRecordMetadata { *; }
-keep class com.madeby.JAI.CloudSyncManager$SyncCheckResult { *; }
-keep class com.madeby.JAI.CloudSyncManager$SyncCheckResult$* { *; }
-keep class com.madeby.JAI.CloudSyncManager$SyncResult { *; }
-keep class com.madeby.JAI.ProfileSyncService$SubmissionResult { *; }
-keep class com.madeby.JAI.ProfileSyncService$SubmissionResult$* { *; }
-keep class com.madeby.JAI.LeaderboardEntry { *; }
-keep class com.madeby.JAI.PlannerGoal { *; }
-keep class com.madeby.JAI.PlannerGoalSnapshot { *; }
-keep class com.madeby.JAI.PlannerDayRecord { *; }
-keep class com.madeby.JAI.LectureScheduleItem { *; }
-keep class com.madeby.JAI.AchievementBadge { *; }
-keep class com.madeby.JAI.StatsSnapshot { *; }
-keep class com.madeby.JAI.MonthBucket { *; }
-keep class com.madeby.JAI.BlockInfo { *; }
-keep class com.madeby.JAI.TimelineEntry { *; }
-keep class com.madeby.JAI.SubjectTag { *; }
-keep class com.madeby.JAI.UserProfile { *; }
-keep class com.madeby.JAI.SessionGoal { *; }

# Keep enum values (needed for valueOf deserialization)
-keepclassmembers enum com.madeby.JAI.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BuildConfig fields (accessed at runtime)
-keep class com.madeby.JAI.BuildConfig { *; }

# Keep PrefsSafe extension functions (called from multiple files)
-keep class com.madeby.JAI.PrefsSafeKt { *; }

# Keep Activities, Services, and Receivers referenced in AndroidManifest
-keep class com.madeby.JAI.MainActivity { *; }
-keep class com.madeby.JAI.LoginActivity { *; }
-keep class com.madeby.JAI.SplashActivity { *; }
-keep class com.madeby.JAI.TimerService { *; }
-keep class com.madeby.JAI.GoalReminderReceiver { *; }
-keep class com.madeby.JAI.StudyWidgetProvider { *; }

# ── Debug Log Stripping ───────────────────────────────────────
# Strip debug and verbose log calls in release builds to avoid information disclosure
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
