package com.madeby.JAI

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class TimerService : Service() {

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "study_timer_channels"
    private val COMPLETION_CHANNEL_ID = "study_timer_completion"
    private val COMPLETION_NOTIFICATION_ID = 1002

    private var currentTimerState = TimerState.IDLE
    private var lastTimestamp: Long = 0
    private var accumulatedStudy: Long = 0
    private var currentBreakSeconds: Long = 0

    private var timerMode: String = "STOPWATCH"
    private var focusCountdownSecs: Long = 1500L
    private var focusRemainingSecs: Long = 0L
    private var breakCountdownSecs: Long = 300L
    private var breakRemainingSecs: Long = 0L
    private var prePauseState: TimerState = TimerState.STUDYING

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var foregroundStarted = false
    private var cachedTogglePendingIntent: PendingIntent? = null
    private var cachedOpenAppPendingIntent: PendingIntent? = null
    private var cachedStopPendingIntent: PendingIntent? = null

    private var lecturePromptTimestamp: Long = 0L
    private var lectureModeEnabled: Boolean = false

    companion object {
        const val ACTION_TOGGLE = "com.madeby.JAI.ACTION_TOGGLE"
        const val ACTION_STOP = "com.madeby.JAI.ACTION_STOP"
        const val ACTION_STOP_SILENT = "com.madeby.JAI.ACTION_STOP_SILENT"
        const val ACTION_PAUSE = "com.madeby.JAI.ACTION_PAUSE"
        const val ACTION_EXTEND_LECTURE = "com.madeby.JAI.ACTION_EXTEND_LECTURE"
        const val ACTION_START_BREAK = "com.madeby.JAI.ACTION_START_BREAK"

        // If the gap since the last tick exceeds this, the process was almost
        // certainly killed and restarted by START_STICKY; don't count the dead time.
        private const val MAX_ACCEPTABLE_GAP_SECS = 600L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadSavedState()
        startBackgroundLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundNotification()
        when (intent?.action) {
            ACTION_TOGGLE -> handleToggle()
            ACTION_STOP -> handleStop()
            ACTION_STOP_SILENT -> handleStopSilent()
            ACTION_PAUSE -> handlePause()
            ACTION_EXTEND_LECTURE -> {
                val extendSecs = intent.getLongExtra("EXTEND_SECS", 300L)
                handleExtendLecture(extendSecs)
            }
            ACTION_START_BREAK -> {
                val breakSecs = intent.getLongExtra("BREAK_SECS", 300L)
                handleStartBreak(breakSecs)
            }
        }
        return START_STICKY
    }

    private fun updateForegroundNotification() {
        if (cachedTogglePendingIntent == null) {
            val toggleIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("NOTIFICATION_TOGGLE_TRIGGER", true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            cachedTogglePendingIntent = PendingIntent.getActivity(
                this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (cachedOpenAppPendingIntent == null) {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            cachedOpenAppPendingIntent = PendingIntent.getActivity(
                this, 1, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (cachedStopPendingIntent == null) {
            val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
            cachedStopPendingIntent = PendingIntent.getService(
                this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val title = when (currentTimerState) {
            TimerState.STUDYING -> getString(R.string.notif_title_studying)
            TimerState.BREAK -> getString(R.string.notif_title_break)
            TimerState.PAUSED -> getString(R.string.notif_title_paused)
            TimerState.LECTURE_ENDED -> "Lecture Completed"
            TimerState.IDLE -> "Scheduled Lecture Standby"
        }
        val content = when (currentTimerState) {
            TimerState.IDLE -> "Monitoring class schedule in the background..."
            TimerState.LECTURE_ENDED -> "Tap to start break or extend lecture"
            else -> if (timerMode == "COUNTDOWN" && currentTimerState == TimerState.STUDYING) {
                getString(R.string.notif_content_countdown, formatTime(focusRemainingSecs), formatTime(currentBreakSeconds))
            } else {
                getString(R.string.notif_content_stopwatch, formatTime(accumulatedStudy), formatTime(currentBreakSeconds))
            }
        }
        val actionText = when (currentTimerState) {
            TimerState.STUDYING -> getString(R.string.notif_action_switch_to_break)
            TimerState.BREAK -> getString(R.string.notif_action_resume_focus)
            else -> getString(R.string.notif_action_resume)
        }

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val primaryColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sharedPrefs.getBoolean("dynamic_color", false)) {
            getColor(android.R.color.system_accent1_500)
        } else {
            sharedPrefs.safeInt("customPrimary", 0xFF7DD3FC.toInt())
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(cachedOpenAppPendingIntent)
            .setColor(primaryColor)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (currentTimerState != TimerState.IDLE) {
            builder.addAction(android.R.drawable.ic_media_next, actionText, cachedTogglePendingIntent)
            builder.addAction(R.drawable.ic_clock, getString(R.string.notif_action_end_session), cachedStopPendingIntent)
        }

        val notification = builder.build()

        if (foregroundStarted) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
            foregroundStarted = true
        }
    }


    private fun handleExtendLecture(extendSecs: Long) {
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.STUDYING
        focusRemainingSecs = extendSecs
        lecturePromptTimestamp = 0L
        lastTimestamp = now - 1L
        TimelineLogger.record(this, TimerState.STUDYING)
        saveState()
        updateForegroundNotification()
        postCountdownComplete()
        StudyWidgetProvider.refresh(this)

        startBackgroundLoop()
    }




    private fun handleStartBreak(breakSecs: Long = 300L) {
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.BREAK
        focusRemainingSecs = 0L
        breakCountdownSecs = breakSecs
        breakRemainingSecs = breakSecs
        lecturePromptTimestamp = 0L
        lastTimestamp = now - 1L
        TimelineLogger.record(this, TimerState.BREAK)
        saveState()
        updateForegroundNotification()
        StudyWidgetProvider.refresh(this)

        startBackgroundLoop()
    }



    private fun handleToggle() {
        val now = System.currentTimeMillis() / 1000
        when (currentTimerState) {
            TimerState.IDLE -> {
                currentTimerState = TimerState.STUDYING
                accumulatedStudy = 0L
                currentBreakSeconds = 0L
                if (timerMode == "COUNTDOWN" || timerMode == "LECTURE") focusRemainingSecs = focusCountdownSecs
            }
            TimerState.STUDYING -> currentTimerState = TimerState.BREAK
            TimerState.LECTURE_ENDED -> currentTimerState = TimerState.BREAK
            TimerState.BREAK -> {
                currentTimerState = TimerState.STUDYING
                if (timerMode == "COUNTDOWN" || timerMode == "LECTURE") focusRemainingSecs = focusCountdownSecs
            }
            TimerState.PAUSED -> currentTimerState = prePauseState
        }

        lastTimestamp = now
        TimelineLogger.record(this, currentTimerState)
        saveState()
        updateForegroundNotification()
        StudyWidgetProvider.refresh(this)
    }

    private fun handlePause() {
        if (currentTimerState != TimerState.STUDYING && currentTimerState != TimerState.BREAK) return
        prePauseState = currentTimerState
        currentTimerState = TimerState.PAUSED
        lastTimestamp = System.currentTimeMillis() / 1000
        TimelineLogger.record(this, TimerState.IDLE)
        saveState()
        updateForegroundNotification()
        StudyWidgetProvider.refresh(this)
    }

    private fun handleStop() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val savedFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val savedBreak = sharedPrefs.getLong("${todayStr}_break_total", 0L)

        sharedPrefs.edit().apply {
            putLong("${todayStr}_focus_total", savedFocus + accumulatedStudy)
            putLong("${todayStr}_break_total", savedBreak + currentBreakSeconds)
            putLong("${todayStr}_goal_secs", sharedPrefs.getLong("daily_goal_secs", 2700L))
            apply()
        }

        currentTimerState = TimerState.IDLE
        lastTimestamp = 0L
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        focusRemainingSecs = 0L
        TimelineLogger.record(this, TimerState.IDLE)
        saveState()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        StudyWidgetProvider.refresh(this)
    }

    private fun handleStopSilent() {
        currentTimerState = TimerState.IDLE
        lastTimestamp = 0L
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        focusRemainingSecs = 0L
        saveState()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        StudyWidgetProvider.refresh(this)
    }

    private fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<LectureScheduleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(LectureScheduleItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", "Lecture"),
                    startTime = obj.optString("startTime", "09:00"),
                    endTime = obj.optString("endTime", "10:00"),
                    enabled = obj.optBoolean("enabled", true)
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }
        return null
    }

    private fun triggerVibrationPattern(pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        triggerVibrationPattern(longArrayOf(0, 500, 250, 500))
    }

    private fun triggerGoalVibration() {
        triggerVibrationPattern(longArrayOf(0, 200, 100, 200, 100, 400))
    }

    private fun checkScheduledLectures(nowSecs: Long) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }
        if (items.isEmpty()) return

        val cal = Calendar.getInstance().apply { timeInMillis = nowSecs * 1000 }
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val endMins = parseTimeToMinutes(item.endTime) ?: continue

            if (currentMins in startMins until endMins) {
                if (currentTimerState == TimerState.IDLE || currentTimerState == TimerState.BREAK) {
                    currentTimerState = TimerState.STUDYING
                    timerMode = "LECTURE"
                    lectureModeEnabled = true
                    accumulatedStudy = 0L
                    focusRemainingSecs = ((endMins - currentMins) * 60).toLong()
                    lecturePromptTimestamp = 0L
                    lastTimestamp = nowSecs - 1L
                    TimelineLogger.record(this, TimerState.STUDYING)
                    saveState()
                    updateForegroundNotification()
                    postLectureStartedNotification(item.title)
                    StudyWidgetProvider.refresh(this)
                    break
                }
            } else if (currentMins >= endMins && currentTimerState == TimerState.STUDYING && focusRemainingSecs <= 0L) {

                currentTimerState = TimerState.LECTURE_ENDED
                lecturePromptTimestamp = nowSecs
                focusRemainingSecs = 0L
                lastTimestamp = nowSecs
                triggerVibration()
                TimelineLogger.record(this, TimerState.LECTURE_ENDED)
                saveState()
                updateForegroundNotification()
                postCountdownComplete()
                StudyWidgetProvider.refresh(this)
                break
            }
        }
    }

    private fun postLectureStartedNotification(lectureTitle: String) {
        ensureCompletionChannel()
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 5, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎓 Lecture Started")
            .setContentText("Scheduled class '$lectureTitle' has started.")
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(COMPLETION_NOTIFICATION_ID, notification)
    }


    private fun startBackgroundLoop() {
        if (::timerRunnable.isInitialized) {
            handler.removeCallbacks(timerRunnable)
        }
        timerRunnable = Runnable {


            val now = System.currentTimeMillis() / 1000
            checkScheduledLectures(now)
            if (currentTimerState == TimerState.LECTURE_ENDED) {
                if (lecturePromptTimestamp > 0L && (now - lecturePromptTimestamp) >= 15L) {
                    currentTimerState = TimerState.BREAK
                    focusRemainingSecs = 0L
                    lastTimestamp = now
                    TimelineLogger.record(this, TimerState.BREAK)
                    saveState()
                    updateForegroundNotification()
                    StudyWidgetProvider.refresh(this)
                }
            } else if (currentTimerState != TimerState.IDLE && lastTimestamp > 0L) {

                val gap = now - lastTimestamp
                if (gap > MAX_ACCEPTABLE_GAP_SECS) {
                    // Process restart after death/reboot: skip the dead period.
                    lastTimestamp = now
                    saveState()
                } else if (gap > 0L) {
                    when (currentTimerState) {
                        TimerState.STUDYING -> {
                            accumulatedStudy += gap
                            if (timerMode == "COUNTDOWN" || timerMode == "LECTURE" || lectureModeEnabled) {
                                focusRemainingSecs -= gap
                                if (focusRemainingSecs <= 0L) {
                                    triggerVibration()
                                    if (timerMode == "LECTURE" || lectureModeEnabled) {
                                        currentTimerState = TimerState.LECTURE_ENDED
                                        lecturePromptTimestamp = now
                                        focusRemainingSecs = 0L
                                        lastTimestamp = now
                                        TimelineLogger.record(this, TimerState.LECTURE_ENDED)
                                        saveState()
                                        updateForegroundNotification()
                                        postCountdownComplete()
                                        StudyWidgetProvider.refresh(this)
                                        return@Runnable
                                    } else {
                                        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
                                        val autoBreak = prefs.getBoolean("pomodoro_auto_break", true)
                                        if (autoBreak) {
                                            currentTimerState = TimerState.BREAK
                                            focusRemainingSecs = 0L
                                            lastTimestamp = now
                                            TimelineLogger.record(this, TimerState.BREAK)
                                        } else {
                                            currentTimerState = TimerState.IDLE
                                            focusRemainingSecs = 0L
                                            lastTimestamp = 0L
                                            TimelineLogger.record(this, TimerState.IDLE)
                                        }
                                        saveState()
                                        updateForegroundNotification()
                                        postCountdownComplete()
                                        StudyWidgetProvider.refresh(this)
                                        return@Runnable
                                    }
                                }
                            }
                        }
                        TimerState.BREAK -> {
                            currentBreakSeconds += gap
                            if (breakRemainingSecs > 0L) {
                                breakRemainingSecs -= gap
                                if (breakRemainingSecs <= 0L) {
                                    currentTimerState = TimerState.IDLE
                                    breakRemainingSecs = 0L
                                    lastTimestamp = 0L
                                    triggerVibration()
                                    TimelineLogger.record(this, TimerState.IDLE)
                                    saveState()
                                    updateForegroundNotification()
                                    postCountdownComplete()
                                    StudyWidgetProvider.refresh(this)
                                    return@Runnable
                                }
                            }
                        }
                        else -> {}
                    }
                    lastTimestamp = now
                    saveState()
                    updateForegroundNotification()
                    maybeFireGoalReached()
                }
            }
            handler.postDelayed(timerRunnable, 1000)
        }
        handler.post(timerRunnable)
    }




    private fun postCountdownComplete() {
        ensureCompletionChannel()
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 4, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle(getString(R.string.notif_complete_title))
            .setContentText(getString(R.string.notif_complete_text))
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun ensureCompletionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(COMPLETION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(COMPLETION_CHANNEL_ID, getString(R.string.channel_completion_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = getString(R.string.channel_completion_desc)
                    enableVibration(true)
                    setSound(null, null)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun maybeFireGoalReached() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (prefs.getString("goal_pinged_date", null) == todayStr) return
        val focus = prefs.getLong("${todayStr}_focus_total", 0L) + accumulatedStudy
        val goal = prefs.getLong("${todayStr}_goal_secs", prefs.getLong("daily_goal_secs", 2700L))
        if (focus < goal) return
        prefs.edit().putString("goal_pinged_date", todayStr).apply()
        triggerGoalVibration()

        GoalReminderScheduler.ensureChannel(this)
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = androidx.core.app.NotificationCompat.Builder(this, GoalReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.notif_goal_title))
            .setContentText(getString(R.string.notif_goal_text, goal / 3600, (goal % 3600) / 60))
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(3002, notification)
    }

    private fun loadSavedState() {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        currentTimerState = runCatching { TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE") }.getOrDefault(TimerState.IDLE)
        lastTimestamp = sharedPrefs.getLong("lastTimestamp", 0L)
        accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
        currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
        timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
        focusCountdownSecs = sharedPrefs.getLong("focus_countdown_secs", 1500L)
        focusRemainingSecs = sharedPrefs.getLong("focus_remaining_secs", 0L)
        breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
        breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)
        lectureModeEnabled = sharedPrefs.getBoolean("lecture_mode_enabled", false)
        lecturePromptTimestamp = sharedPrefs.getLong("lecture_prompt_timestamp", 0L)
        prePauseState = runCatching { TimerState.valueOf(sharedPrefs.getString("pre_pause_state", "STUDYING") ?: "STUDYING") }.getOrDefault(TimerState.STUDYING)
    }

    private fun saveState() {
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("timerState", currentTimerState.name)
            putString("timer_mode", timerMode)
            putLong("lastTimestamp", lastTimestamp)
            putLong("accumulatedStudy", accumulatedStudy)
            putLong("currentBreakSeconds", currentBreakSeconds)
            putLong("focus_remaining_secs", focusRemainingSecs)
            putLong("break_countdown_secs", breakCountdownSecs)
            putLong("break_remaining_secs", breakRemainingSecs)
            putBoolean("lecture_mode_enabled", lectureModeEnabled)
            putLong("lecture_prompt_timestamp", lecturePromptTimestamp)
            putString("pre_pause_state", prePauseState.name)
            apply()
        }
    }


    private fun formatTime(totalSeconds: Long): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.channel_control_name), NotificationManager.IMPORTANCE_LOW
            ).apply { 
                description = getString(R.string.channel_control_desc)
                setShowBadge(false) 
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
