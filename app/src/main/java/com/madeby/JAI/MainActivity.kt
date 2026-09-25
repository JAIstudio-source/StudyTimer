package com.madeby.JAI

import android.app.Dialog
import android.view.Window
import android.app.NotificationChannel
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

import android.app.NotificationManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.Gravity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.view.MotionEvent
import android.widget.Toast
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    internal var currentPanel = AppPanel.FOCUS
    internal var currentTimerState = TimerState.IDLE
    internal var currentStatsTab = AppStatsTab.OVERVIEW
    internal var currentSettingsTab = AppSettingsTab.HUB
    private var tabDragSlop = 12
    private var tabDragArmed = false
    private var tabDragActive = false
    private var tabDragSide = 1
    private var tabDragOverlay: FrameLayout? = null
    private var tabDragDownX = 0f
    private var tabDragDownY = 0f
    private var tabDragLastX = 0f
    private var tabDragLastT = 0L
    private var tabDragVelocityX = 0f
    private var tabDragSettling = false
    private var tabDragSettleIsCommit = false
    private var tabDragSettleToken = 0
    private var tabDragCommitStatsTab = AppStatsTab.OVERVIEW
    private var tabDragCommitSettingsTab = AppSettingsTab.SIMPLE
    internal class CachedTabPage(val view: View, val statsGen: Int, val themeSig: String)
    internal val tabPageCache = HashMap<String, CachedTabPage>()
    private var selectedDaysFilter = 7

    internal var accumulatedStudy: Long = 0
    internal var currentBreakSeconds: Long = 0

    internal fun resetRunningSessionAccumulators() {
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putLong("accumulatedStudy", 0L)
            .putLong("currentBreakSeconds", 0L)
            .apply()
    }

    internal var timerMode: String = "STOPWATCH"
    internal var focusCountdownSecs: Long = 1500L
    internal var focusRemainingSecs: Long = 0L
    internal var prePauseState: TimerState = TimerState.STUDYING

    private var lastKeepScreenOn = -1

    private var pauseBlinkAnimator: ValueAnimator? = null

    private var frontFlipAnim: ValueAnimator? = null
    private var backFlipAnim: ValueAnimator? = null

    internal var isDevModeUnlocked = false
    internal var isAdjustingFocusMode = true
    internal var showFocusHueBar = false
    internal var showBreakHueBar = false
    private var updateDialogRef: android.app.Dialog? = null
    private var batteryOptDialogRef: android.app.Dialog? = null
    internal var settingsScrollViewRef: ScrollView? = null
    private var pendingSettingsScrollY = 0

    private val pickAvatarLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = LocalAvatarManager.saveAvatarFromUri(this, uri)
            if (success) {
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                navigateToPanel(AppPanel.SETTINGS)
            } else {
                Toast.makeText(this, "Could not process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun pickProfileAvatar() {
        try {
            pickAvatarLauncher.launch("image/*")
        } catch (_: Exception) {
            Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun showSignOutConfirmDialog(onConfirmed: (() -> Unit)? = null) {
        DeveloperToolsHelper.showThemedConfirmDialog(
            activity = this,
            themeCoordinator = themeCoordinator,
            title = "Sign Out?",
            message = "Your local study records will remain on this device, but cloud sync will pause until you sign in again.",
            confirmText = "Sign Out",
            isDestructive = true
        ) {
            AuthManager.logout(this)
            onConfirmed?.invoke() ?: run {
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                navigateToPanel(AppPanel.SETTINGS)
            }
        }
    }

    internal var statsSnapshotCache: StatsSnapshot? = null
    internal var statsSnapshotGen = 0
    internal var statsDirty = true
    internal var statsInternalRefresh = false
    internal var hasPlayedStatsEntranceAnimation = false
    private var lastStyleKey = ""
    private var lastTickTimerState: TimerState? = null
    private var lastRenderedTimerState: TimerState? = null
    private var lastIsBreakingState: Boolean? = null
    private var lastZenModeState: Boolean? = null
    private var lastShowPauseState: Boolean? = null
    private var lastDayBucket: Long = -1L
    private var cachedTodayStr = ""
    private var lastStatsMinuteTick: Long = -1L

    internal lateinit var themeCoordinator: ThemeCoordinator
    private lateinit var backupManager: BackupManager
    internal val statsEngine by lazy { StatsEngine(this) }
    internal val dateKeyFmt by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    private lateinit var rootLayout: FrameLayout
    internal lateinit var panelContainer: LinearLayout

    internal lateinit var statusBadge: TextView
    internal lateinit var studyTimerDisplay: TextView
    internal lateinit var breakTimerDisplay: TextView
    internal lateinit var timerRing: TimerRingView
    internal lateinit var mainBtn: Button
    internal lateinit var pauseBtn: Button
    internal lateinit var stopBtn: HoldRingButton
    internal lateinit var controlActionContainer: LinearLayout
    private lateinit var panelHost: FrameLayout
    internal lateinit var navHeader: LinearLayout
    internal lateinit var statusBadgeContainer: LinearLayout
    internal var extraControlsContainer: LinearLayout? = null
    internal var statsFloatingIcon: ImageView? = null
    internal var isPortraitFullscreenActive = false
    private var lastConfigOrientation = Configuration.ORIENTATION_UNDEFINED
    private var lastFullscreenToggleTime = 0L
    private var lastActionTriggerTime = 0L
    private var isZenModeActive = false
    private var isNavigatingBack = false
    internal val appPrefs by lazy { getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE) }

    internal fun canExecuteAction(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionTriggerTime < 250L) return false
        lastActionTriggerTime = now
        return true
    }
    internal var calendarYear = 0
    internal var calendarMonth = 0
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    internal val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> showRestoreConfirmDialog(uri) }
        }
    }

    internal fun showEditNameDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(this).apply {
            text = "PROFILE"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Change Account Name"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(14))
        })

        val currentName = AuthManager.getUserName(this) ?: ""
        val inputEdit = android.widget.EditText(this).apply {
            setText(currentName)
            hint = "Enter your name"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        content.addView(inputEdit)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = Button(this).apply {
            text = getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val saveBtn = Button(this).apply {
            text = "SAVE"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(themeCoordinator.primaryColor) }
            setOnClickListener {
                val newName = inputEdit.text.toString().trim()
                if (newName.isNotEmpty()) {
                    AuthManager.updateUserName(this@MainActivity, newName)
                    tabPageCache.remove(settingsTabKey(AppSettingsTab.PROFILE))
                    navigateToPanel(AppPanel.SETTINGS)
                    Thread {
                        kotlinx.coroutines.runBlocking {
                            CloudSyncManager.syncDataToCloud(this@MainActivity)
                        }
                    }.start()
                }
                dialog.dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(saveBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun showExpandedAvatarDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val size = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        val userName = AuthManager.getUserName(this)
        val userEmail = AuthManager.getUserEmail(this)
        val avatarBitmap = LocalAvatarManager.getCircularAvatarBitmap(this, size)

        val frame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        if (avatarBitmap != null) {
            val imgView = android.widget.ImageView(this).apply {
                setImageBitmap(avatarBitmap)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            frame.addView(imgView)
        } else {
            val textView = TextView(this).apply {
                text = if (!userName.isNullOrBlank()) userName.take(1).uppercase() else if (!userEmail.isNullOrBlank()) userEmail.take(1).uppercase() else "G"
                textSize = 90f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            frame.addView(textView)
        }

        frame.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(frame)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(size, size)
        dialog.show()
    }

    internal fun showDeleteAccountDialog() {
        val dialog = Dialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(this).apply {
            text = "⚠️ SEVERE WARNING • PERMANENT DELETION"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11.5f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Delete Account & All Data"
            setTextColor(themeCoordinator.textColor)
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "This action is permanent and will instantly erase all your focus history, subjects, cloud sync documents, and settings. Type 'DELETE' below to confirm."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 13.5f
            setPadding(0, dp(8), 0, dp(12))
        })

        val confirmInput = EditText(this).apply {
            hint = "Type DELETE to confirm"
            setTextColor(themeCoordinator.textColor)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 90))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(Color.parseColor("#EF4444"), 40), 12f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        content.addView(confirmInput)

        val webInfoText = TextView(this).apply {
            text = "🌐 Web Deletion Portal: https://get-studytimer.vercel.app/delete-account.html"
            setTextColor(themeCoordinator.primaryColor)
            alpha = 0.8f
            textSize = 11.5f
            setPadding(0, dp(10), 0, dp(14))
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://get-studytimer.vercel.app/delete-account.html")))
                } catch (_: Exception) {}
            }
        }
        content.addView(webInfoText)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val cancelBtn = Button(this).apply {
            text = getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val deleteBtn = Button(this).apply {
            text = "PERMANENTLY DELETE"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#DC2626"))
            }
            setOnClickListener {
                val inputStr = confirmInput.text.toString().trim()
                if (inputStr != "DELETE") {
                    Toast.makeText(this@MainActivity, "Please type 'DELETE' exactly to confirm deletion", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    rootLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                } catch (_: Exception) {}
                dialog.dismiss()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.deleteUserCloudData(this@MainActivity)
                    }
                    runOnUiThread {
                        AuthManager.deleteLocalUserData(this@MainActivity)
                        Toast.makeText(this@MainActivity, "Account & all data permanently erased.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.2f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(deleteBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showRestoreConfirmDialog(uri: Uri) {
        val meta = backupManager.inspectBackupMetadata(uri)
        val currentLocalTs = backupManager.getLastModifiedTimestamp()
        val isOlderThanLocal = meta != null && meta.lastModifiedTimestamp > 0L && meta.lastModifiedTimestamp < currentLocalTs - 60000L
        val backupDateStr = if (meta != null && meta.backupCreatedAt > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(meta.backupCreatedAt))
        } else "Unknown Date"

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = if (isOlderThanLocal) "STALE BACKUP WARNING" else getString(R.string.restore_backup)
            setTextColor(if (isOlderThanLocal) Color.parseColor("#F59E0B") else themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = if (isOlderThanLocal) "Import Older Backup?" else getString(R.string.restore_backup_title)
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = if (isOlderThanLocal) {
                "⚠️ Warning: This backup file was created on $backupDateStr, which is OLDER than your current study data. Importing this will revert recent local progress."
            } else {
                "Backup from: $backupDateStr\n${getString(R.string.restore_backup_message)}"
            }
            setTextColor(themeCoordinator.textColor)
            alpha = if (isOlderThanLocal) 0.9f else 0.6f
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val cancelBtn = Button(this).apply {
            text = getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val restoreBtn = Button(this).apply {
            text = if (isOlderThanLocal) "REVERT & RESTORE" else getString(R.string.btn_restore)
            setTextColor(themeCoordinator.bgColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = rippleBackground(if (isOlderThanLocal) Color.parseColor("#F59E0B") else themeCoordinator.primaryColor)
            setOnClickListener {
                dialog.dismiss()
                performImportWithCloudConflictCheck(uri)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(restoreBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun performImportWithCloudConflictCheck(uri: Uri) {
        val success = backupManager.importDataFromJSON(uri, allowCloudSync = false)
        if (!success) {
            Toast.makeText(this, getString(R.string.toast_backup_parse_failed), Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, getString(R.string.toast_logs_restored), Toast.LENGTH_SHORT).show()
        themeCoordinator.applyThemeCoordinates()
        tabPageCache.clear()

        if (!AuthManager.isLoggedIn(this)) {
            recreate()
            return
        }

        // Check if there is a conflict with existing cloud data
        Thread {
            kotlinx.coroutines.runBlocking {
                val syncResult = CloudSyncManager.syncWithConflictCheck(this@MainActivity)
                runOnUiThread {
                    when (syncResult) {
                        is CloudSyncManager.SyncCheckResult.Conflict -> {
                            showSyncConflictDialog(syncResult.localTimestamp, syncResult.cloudTimestamp, syncResult.cloudRecord)
                        }
                        is CloudSyncManager.SyncCheckResult.Success -> {
                            Toast.makeText(this@MainActivity, "☁️ Cloud sync updated successfully", Toast.LENGTH_SHORT).show()
                            recreate()
                        }
                        else -> {
                            recreate()
                        }
                    }
                }
            }
        }.start()
    }

    internal fun showSyncConflictDialog(localTs: Long, cloudTs: Long, cloudRecord: org.json.JSONObject) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val cloudDateStr = if (cloudTs > 0L) sdf.format(Date(cloudTs)) else "Recent Cloud State"
        val localDateStr = if (localTs > 0L) sdf.format(Date(localTs)) else "Imported Backup"

        content.addView(TextView(this).apply {
            text = "SYNC CONFLICT DETECTED"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Cloud Has Newer Data"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = "Your Google/Cloud backup ($cloudDateStr) is newer than the imported data ($localDateStr).\n\nChoose how you want to resolve this:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 13f
            setPadding(0, dp(8), 0, dp(14))
        })

        // Option 1: Keep Newer Cloud Data (Recommended)
        val keepCloudBtn = Button(this).apply {
            text = "⚡ Keep Newer Cloud Data (Recommended)"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.restoreDataFromCloud(this@MainActivity)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "☁️ Restored newer cloud data", Toast.LENGTH_SHORT).show()
                            tabPageCache.clear()
                            recreate()
                        }
                    }
                }.start()
            }
        }
        content.addView(keepCloudBtn)

        // Option 2: Merge Both
        val mergeBtn = Button(this).apply {
            text = "🔀 Merge Both (Combine Records)"
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 90), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, 0, 0, dp(8))
            }
            setOnClickListener {
                dialog.dismiss()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        val merged = CloudSyncManager.mergeCloudAndLocalData(this@MainActivity, cloudRecord)
                        runOnUiThread {
                            if (merged) {
                                Toast.makeText(this@MainActivity, "🔀 Merged local and cloud data", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Merge failed, keeping local state", Toast.LENGTH_SHORT).show()
                            }
                            tabPageCache.clear()
                            recreate()
                        }
                    }
                }.start()
            }
        }
        content.addView(mergeBtn)

        // Option 3: Overwrite Cloud with Local
        val overwriteBtn = Button(this).apply {
            text = "⚠️ Overwrite Cloud with Local Data"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = themeCoordinator.createGlassChip(0x33EF4444.toInt(), 12f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            setOnClickListener {
                dialog.dismiss()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(this@MainActivity, force = true)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Cloud overwritten with local backup", Toast.LENGTH_SHORT).show()
                            recreate()
                        }
                    }
                }.start()
            }
        }
        content.addView(overwriteBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = backupManager.exportDataToJSON(uri)
                if (success) {
                    Toast.makeText(this, getString(R.string.toast_logs_exported), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_logs_export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    internal val csvLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = exportCsv(uri)
                if (success) {
                    Toast.makeText(this, getString(R.string.toast_csv_exported), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_csv_export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    internal val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private val HOLD_TO_END_DURATION_MS = 1200L
    internal var holdStartTime = 0L
    internal var isHoldingStop = false

    internal fun resetHoldToEnd() {
        isHoldingStop = false
        if (::stopBtn.isInitialized) {
            stopBtn.isPressed = false
            stopBtn.progress = 0f
        }
        handler.removeCallbacks(holdToEndRunnable)
    }

    internal val holdToEndRunnable = object : Runnable {
        override fun run() {
            if (isDestroyed || !isHoldingStop || !::stopBtn.isInitialized || !stopBtn.isPressed) {
                resetHoldToEnd()
                return
            }
            val elapsed = SystemClock.uptimeMillis() - holdStartTime
            if (elapsed < 0L || elapsed > HOLD_TO_END_DURATION_MS + 1500L) {
                resetHoldToEnd()
                return
            }
            val progress = (elapsed.toFloat() / HOLD_TO_END_DURATION_MS).coerceIn(0f, 1f)
            stopBtn.progress = progress
            if (progress >= 1f) {
                resetHoldToEnd()
                handleStopSession()
                Toast.makeText(this@MainActivity, getString(R.string.toast_session_saved), Toast.LENGTH_SHORT).show()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(this@MainActivity)
                    }
                }.start()
            } else {
                handler.postDelayed(this, 16L)
            }
        }
    }

    private val CHANNEL_ID = "study_timer_channels"

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().remove("notification_perm_prompt_count").apply()
            return
        }
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val promptCount = prefs.safeInt("notification_perm_prompt_count", 0)

        if (promptCount == 0 || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            prefs.edit().putInt("notification_perm_prompt_count", promptCount + 1).apply()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            return
        }

        // Theme-styled rationale dialog if rationale is needed
        showCustomDialog(
            badge = "NOTIFICATION PERMISSION",
            title = getString(R.string.notif_permission_title),
            message = getString(R.string.notif_permission_rationale),
            positiveText = getString(R.string.btn_allow_upper),
            onPositive = {
                prefs.edit().putInt("notification_perm_prompt_count", promptCount + 1).apply()
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            },
            negativeText = getString(R.string.btn_not_now)
        )
    }

    internal fun showCustomDialog(
        badge: String? = null,
        title: String,
        message: String,
        positiveText: String,
        onPositive: () -> Unit,
        negativeText: String? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        if (!badge.isNullOrEmpty()) {
            content.addView(TextView(this).apply {
                text = badge.uppercase()
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
        }
        content.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, if (badge.isNullOrEmpty()) 0 else dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(20), 0, 0)
        }
        if (!negativeText.isNullOrEmpty()) {
            buttonRow.addView(TextView(this).apply {
                text = negativeText
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener {
                    dialog.dismiss()
                    onNegative?.invoke()
                }
            })
        }
        buttonRow.addView(TextView(this).apply {
            text = positiveText
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                dialog.dismiss()
                onPositive.invoke()
            }
        })
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.let {
            it.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            it.setLayout((resources.displayMetrics.widthPixels * 0.88).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun maybePromptBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val opens = prefs.safeInt("battery_opt_opens", 0) + 1
        prefs.edit().putInt("battery_opt_opens", opens).apply()
        if (opens % 3 != 0) return
        showBatteryOptimizationDialog()
    }

    private fun showBatteryOptimizationDialog() {
        if (batteryOptDialogRef?.isShowing == true) return
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.btn_run_in_background)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.battery_title)
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.battery_message)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val laterBtn = TextView(this).apply {
            text = getString(R.string.btn_later)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
            setOnClickListener { dialog.dismiss() }
        }
        val allowBtn = TextView(this).apply {
            text = getString(R.string.btn_allow)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(themeCoordinator.primaryColor) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                } catch (_: Exception) {
                    openBatterySettings()
                }
            }
        }
        buttonRow.addView(laterBtn)
        buttonRow.addView(allowBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        batteryOptDialogRef = dialog
        dialog.show()
    }

    private fun openBatterySettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openNotificationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName")))
        }
    }

    @Suppress("DEPRECATION")
    internal fun currentVersionCodeLong(): Long =
        runCatching { PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0)) }.getOrDefault(0L)

    @Suppress("DEPRECATION")
    internal fun currentVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0.0"

    private fun openUpdateUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.toast_update_page_failed), Toast.LENGTH_SHORT).show()
        }
    }

    internal fun checkForUpdates(manual: Boolean) {
        if (!AppConfig.ENABLE_GITHUB_UPDATE_CHECK) {
            if (manual) {
                Toast.makeText(this, getString(R.string.toast_up_to_date, currentVersionName()), Toast.LENGTH_SHORT).show()
            }
            return
        }
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!manual) {
            val launches = prefs.safeInt("update_launch_count", 0) + 1
            prefs.edit().putInt("update_launch_count", launches).apply()
            if (launches % 5 != 0) return
        }
        if (manual) {
            Toast.makeText(this, getString(R.string.toast_checking_updates), Toast.LENGTH_SHORT).show()
        }
        UpdateChecker.check(this) { info ->
            if (isDestroyed || isFinishing) return@check
            if (info == null) {
                if (manual) {
                    Toast.makeText(this, getString(R.string.toast_update_check_failed), Toast.LENGTH_SHORT).show()
                }
                return@check
            }
            if (info.versionCode <= currentVersionCodeLong()) {
                if (manual) {
                    Toast.makeText(this, getString(R.string.toast_up_to_date, currentVersionName()), Toast.LENGTH_SHORT).show()
                }
                return@check
            }
            if (!manual && prefs.safeInt("update_dismissed_version", -1) == info.versionCode) {
                return@check
            }
            showUpdateAvailableDialog(info)
        }
    }

    private fun showUpdateAvailableDialog(info: UpdateInfo) {
        if (updateDialogRef?.isShowing == true) return
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.update_available)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.update_version_ready, info.versionName)
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        if (info.hasReleaseNotes) {
            content.addView(TextView(this).apply {
                text = info.releaseNotes
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.update_message)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val laterBtn = TextView(this).apply {
            text = getString(R.string.btn_later)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(tintedColor(themeCoordinator.textColor, 18)) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(12), 0) }
            setOnClickListener {
                prefs.edit().putInt("update_dismissed_version", info.versionCode).apply()
                dialog.dismiss()
            }
        }
        val updateBtn = TextView(this).apply {
            text = getString(R.string.btn_update)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 22f; setColor(themeCoordinator.primaryColor) }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                prefs.edit().remove("update_dismissed_version").apply()
                dialog.dismiss()
                openUpdateUrl(info.apkUrl?.takeIf { it.isNotBlank() } ?: info.url)
            }
        }
        buttonRow.addView(laterBtn)
        buttonRow.addView(updateBtn)
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setOnCancelListener { prefs.edit().putInt("update_dismissed_version", info.versionCode).apply() }
        updateDialogRef = dialog
        dialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().remove("notification_perm_prompt_count").apply()
                pendingNotificationAction?.invoke()
            }
            pendingNotificationAction = null
        }
    }

    internal fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    internal fun lightenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * amount).coerceIn(0f, 1f)
        hsv[1] = (hsv[1] * (1f - amount * 0.45f)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    internal fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun darkenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    internal fun rippleBackground(color: Int): android.graphics.drawable.Drawable {
        if (themeCoordinator.isBubbleStyle()) {
            return themeCoordinator.createButtonBackground(color)
        }
        val shape = themeCoordinator.createGlowGradient(color, 80f)
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(90, 255, 255, 255)),
            shape,
            shape
        )
    }

    internal fun outlinedButtonBackground(): android.graphics.drawable.Drawable {
        if (themeCoordinator.isBubbleStyle()) {
            val density = resources.displayMetrics.density
            val fill = if (themeCoordinator.isDarkMode()) 0x1AFFFFFF.toInt() else 0xFFFFFFFF.toInt()
            val stroke = if (themeCoordinator.isDarkMode()) 0x33FFFFFF.toInt() else 0xFFCBD5E1.toInt()
            return GradientDrawable().apply {
                this.cornerRadius = 80f * density
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
        }
        if (!themeCoordinator.isDarkMode()) {
            val density = resources.displayMetrics.density
            return android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x1A0F172A.toInt()),
                GradientDrawable().apply {
                    cornerRadius = 80f * density
                    setColor(0xFFFFFFFF.toInt())
                    setStroke((1.5f * density).toInt(), 0xFFCBD5E1.toInt())
                },
                null
            )
        }
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
            GradientDrawable().apply { cornerRadius = 80f; setColor(0x00000000); setStroke(3, if (themeCoordinator.isGlassStyle()) tintedColor(themeCoordinator.primaryColor, 120) else themeCoordinator.boxColor) },
            null
        )
    }

    internal fun applyBubbleTouchAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    (v.background as? Soft3DBubbleDrawable)?.isPressed = true
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v.background as? Soft3DBubbleDrawable)?.isPressed = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220).setInterpolator(android.view.animation.OvershootInterpolator(1.3f)).start()
                    false
                }
                else -> false
            }
        }
    }

    private fun pauseButtonVisibility(): Int {
        val showPause = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getBoolean("show_pause_button", true)
        return if (showPause) View.VISIBLE else View.GONE
    }

    private fun metricRowBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(tintedColor(color, 64))
        }
    }

    internal fun formatGoalLabel(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return when {
            h > 0 && m > 0 -> getString(R.string.duration_h_m, h, m)
            h > 0 -> getString(R.string.duration_h, h)
            else -> getString(R.string.duration_m, m)
        }
    }

    private fun dailyGoalSecs(): Long = statsEngine.dailyGoalSecs()

    internal fun resolveGoalFor(dateStr: String): Long = statsEngine.resolveGoalFor(dateStr)

    internal fun applyHoldToRepeat(view: View, initialDelayMs: Long = 300L, step: () -> Unit) {
        val repeatHandler = Handler(Looper.getMainLooper())
        var repeatDelay = initialDelayMs
        val repeatRunnable = object : Runnable {
            override fun run() {
                step()
                repeatDelay = max(50L, repeatDelay - 25L)
                repeatHandler.postDelayed(this, repeatDelay)
            }
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    step()
                    repeatDelay = initialDelayMs
                    repeatHandler.removeCallbacks(repeatRunnable)
                    repeatHandler.postDelayed(repeatRunnable, initialDelayMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    repeatHandler.removeCallbacks(repeatRunnable)
                    true
                }
                else -> true
            }
        }
    }

    internal fun pureWhiteTimerEnabled(): Boolean {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pureWhiteTimer", false) && themeCoordinator.activeBgMode != "LIGHT"
    }

    internal fun isPomodoroPureWhiteActive(): Boolean {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("pomodoro_pure_white_theme", false)
        return isEnabled && timerMode == "COUNTDOWN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putString("ui_style", "BUBBLE")
                .putInt("customHue", 255)
                .putInt("customPrimary", Color.parseColor("#A78BFA"))
                .putInt("customSecondaryHue", 199)
                .putInt("customSecondary", Color.parseColor("#38BDF8"))
                .putString("timer_mode", "SUBJECT")
                .putBoolean("enable_subject_tagging", true)
                .putBoolean("show_subject_pie_chart", true)
                .putLong("focus_countdown_secs", 3600L)
                .putInt("reminder_hour", 20)
                .putInt("reminder_minute", 0)
                .putBoolean("reminder_enabled", true)
                .putBoolean("is_landscape_mode_enabled", true)
                .putBoolean("true_fullscreen_landscape", true)
                .putBoolean("keep_screen_on", true)
                .putBoolean("show_pause_button", true)
                .putBoolean("show_focus_heatmap", true)
                .putBoolean("show_focus_pattern", true)
                .putBoolean("pureWhiteTimer", false)
                .apply()
        }

        themeCoordinator = ThemeCoordinator(this)
        backupManager = BackupManager(this)
        themeCoordinator.applyThemeCoordinates()

        Thread {
            try {
                AppAnalytics.init(this)
                CrashReporter.init(this)
                backupManager.triggerAutoRestoreIfPresent()
                createNotificationChannel()
                GoalReminderScheduler.schedule(this)
                migrateHistoricalDailyGoals(this)
                checkAndResetGoalsForNewDay()
                triggerAutoSyncIfEligible(force = true)
            } catch (_: Exception) {}
        }.start()

        requestNotificationPermissionIfNeeded()

        rootLayout = FrameLayout(this).apply {
            background = themeCoordinator.createBackgroundDrawable()
        }

        panelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        panelHost = FrameLayout(this).apply {
            setPadding(dp(16), dp(4), dp(16), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        panelHost.addView(panelContainer)
        rootLayout.addView(panelHost)
        setContentView(rootLayout)
        lastConfigOrientation = resources.configuration.orientation
        updateStatusBarIcons()

        tabDragSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        currentTimerState = TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE")
        accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
        currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
        selectedDaysFilter = sharedPrefs.safeInt("selected_days_filter", 7)

        if (currentTimerState != TimerState.IDLE) {
            val resumeIntent = Intent(this, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(resumeIntent)
            } else {
                startService(resumeIntent)
            }
        }


        navigateToPanel(AppPanel.FOCUS)
        setupTimerLoop()


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var lastBackTime = 0L
            override fun handleOnBackPressed() {
                if (isPortraitFullscreenActive) {
                    exitPortraitFullscreenMode()
                    return
                }
                if (currentPanel == AppPanel.HEATMAP) {
                    // Pop from full-screen Heatmap back to Insights
                    navigateToPanel(AppPanel.STATS)
                } else if (currentPanel == AppPanel.SETTINGS && currentSettingsTab != AppSettingsTab.HUB) {
                    // Nested Settings sub-screen: pop back one level to the main Settings Hub Dashboard
                    currentSettingsTab = AppSettingsTab.HUB
                    navigateToPanel(AppPanel.SETTINGS)
                } else if (currentPanel != AppPanel.FOCUS) {
                    // Secondary Top-Level Tabs (Settings Hub, Insights/Stats): Return to Root Start Destination (Timer/Focus)
                    currentSettingsTab = AppSettingsTab.HUB
                    navigateToPanel(AppPanel.FOCUS)
                } else {
                    // Primary Root Anchor (Timer / Focus Screen): Trigger exit confirmation / double-tap to exit
                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < 2000) {
                        finish()
                    } else {
                        lastBackTime = now
                        Toast.makeText(this@MainActivity, getString(R.string.toast_press_back_again), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        if (intent?.getBooleanExtra("NOTIFICATION_TOGGLE_TRIGGER", false) == true) {
            handleStateToggle()
        }
        if (intent?.getBooleanExtra(TimerService.EXTRA_SWITCH_TO_LECTURE, false) == true) {
            showSwitchToLectureDialog()
        }

        applyImmersiveModeForLandscape()

        rootLayout.post { maybeShowOnboarding() }

    }

    private fun csvCell(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    private fun exportCsv(uri: Uri): Boolean {
        return try {
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val sb = StringBuilder()

            sb.append(csvCell("STUDYTIMER LOG EXPORT")).append('\n')
            sb.append(csvCell("Export Date")).append(',').append(csvCell(sdf.format(Date()))).append('\n')
            sb.append('\n').append('\n')

            val keys = sharedPrefs.all.keys.filter { it.endsWith("_focus_total") }.sorted()
            var totalFocus = 0L
            var totalBreak = 0L
            var activeDays = 0
            var maxDayFocus = 0L
            val sessionLines = StringBuilder()

            sb.append("--- DAILY SUMMARY ---\n")
            sb.append("Date,Day of Week,Focus Time,Break Time,Focus (Sec),Break (Sec),Goal Met,Longest Session\n")
            for (k in keys) {
                val d = k.removeSuffix("_focus_total")
                val f = sharedPrefs.getLong(k, 0L) + (if (d == todayStr) sharedPrefs.getLong("accumulatedStudy", 0L) else 0L)
                val b = sharedPrefs.getLong("${d}_break_total", 0L) + (if (d == todayStr) currentBreakSeconds else 0L)
                if (f <= 0L && b <= 0L) continue
                val parsed = try { sdf.parse(d) } catch (_: Exception) { null }
                val weekday = if (parsed != null) dayNameSdf.format(parsed) else ""
                val (sessions, breaks) = dayBlocks(d)
                val longest = sessions.maxOfOrNull { it.secs } ?: 0L
                sb.append(csvCell(d)).append(',')
                    .append(csvCell(weekday)).append(',')
                    .append(csvCell(formatDuration(f))).append(',')
                    .append(csvCell(formatDuration(b))).append(',')
                    .append(f).append(',')
                    .append(b).append(',')
                    .append(if (f >= resolveGoalFor(d)) "Yes" else "No").append(',')
                    .append(csvCell(formatDuration(longest))).append('\n')

                val rows = ArrayList<Pair<BlockInfo, String>>()
                for (s in sessions) rows.add(Pair(s, "Focus"))
                for (bk in breaks) rows.add(Pair(bk, "Break"))
                rows.sortBy { it.first.startMs }
                for ((blk, type) in rows) {
                    sessionLines.append(csvCell(d)).append(',')
                        .append(csvCell(TimeFormat.formatWallClock(this, blk.startMs))).append(',')
                        .append(csvCell(TimeFormat.formatWallClock(this, blk.endMs))).append(',')
                        .append(csvCell(type)).append(',')
                        .append(csvCell(formatDuration(blk.secs))).append(',')
                        .append(blk.secs).append('\n')
                }

                totalFocus += f
                totalBreak += b
                activeDays++
                if (f > maxDayFocus) maxDayFocus = f
            }
            sb.append('\n').append('\n')

            sb.append("--- INDIVIDUAL SESSIONS ---\n")
            sb.append("Date,Start Time,End Time,Session Type,Duration,Duration (Sec)\n")
            sb.append(sessionLines)
            sb.append('\n').append('\n')

            sb.append("--- ALL-TIME METRICS & STATS ---\n")
            sb.append("Metric,Value\n")
            sb.append(csvCell("Total Focus Time")).append(',').append(csvCell(formatDuration(totalFocus))).append('\n')
            sb.append(csvCell("Total Break Time")).append(',').append(csvCell(formatDuration(totalBreak))).append('\n')
            sb.append(csvCell("Active Study Days")).append(',').append(activeDays).append('\n')
            sb.append(csvCell("Average Daily Focus")).append(',').append(csvCell(if (activeDays > 0) formatDuration(totalFocus / activeDays) else "0m")).append('\n')
            sb.append(csvCell("Longest Single Day Focus")).append(',').append(csvCell(formatDuration(maxDayFocus))).append('\n')
            sb.append(csvCell("Current Daily Goal Target")).append(',').append(csvCell(formatDuration(dailyGoalSecs()))).append('\n')

            contentResolver.openOutputStream(uri, "w")?.use { stream ->
                stream.write(sb.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientationChanged = (newConfig.orientation != lastConfigOrientation)
        lastConfigOrientation = newConfig.orientation

        val sharedPrefs = appPrefs
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled
        if (orientationChanged) {
            isPortraitFullscreenActive = false
            if (isLandscape && currentPanel == AppPanel.FOCUS) {
                applyImmersiveModeForLandscape()
            } else {
                showSystemUI()
            }
            buildCurrentPanel()
            updateVisualStyles()
        } else {
            if (isLandscape && currentPanel == AppPanel.FOCUS) {
                applyImmersiveModeForLandscape()
            } else if (isPortraitFullscreenActive && currentPanel == AppPanel.FOCUS) {
                hideSystemUI()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && handleTabDragTouch(ev)) return true
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("NOTIFICATION_TOGGLE_TRIGGER", false) == true) {
            handleStateToggle()
        }
        if (intent?.getBooleanExtra(TimerService.EXTRA_SWITCH_TO_LECTURE, false) == true) {
            showSwitchToLectureDialog()
        }
    }

    private fun showSwitchToLectureDialog() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val title = prefs.getString("pending_lecture_title", "") ?: ""
        val startTime = prefs.getString("pending_lecture_start", "") ?: ""
        val endTime = prefs.getString("pending_lecture_end", "") ?: ""
        val remainingSecs = prefs.getLong("pending_lecture_remaining_secs", 0L)
        val skipKey = prefs.getString("pending_lecture_skip_key", null)
        if (title.isEmpty() || remainingSecs <= 0L) return

        ongoingLectureDialogShowing = true
        val remainingStr = if (remainingSecs >= 3600) "${remainingSecs / 3600}h ${(remainingSecs % 3600) / 60}m" else "${(remainingSecs % 3600) / 60}m"

        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(28f)
        }

        root.addView(TextView(this).apply {
            text = "🎓 Scheduled Class Starting"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Class \"$title\" ($startTime – $endTime) is now in progress.\nRemaining: $remainingStr\n\nSwitch timer to lecture mode? Your current session data will be saved."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13f
            setPadding(0, dp(10), 0, dp(18))
        })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(TextView(this).apply {
            text = "Keep Current"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener {
                // Mark skipped so service doesn't ask again for this lecture slot
                if (skipKey != null) prefs.edit().putBoolean(skipKey, true).apply()
                ongoingLectureDialogShowing = false
                dialog.dismiss()
            }
        })
        btnRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
        btnRow.addView(TextView(this).apply {
            text = "Switch to Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f)
            setOnClickListener {
                ongoingLectureDialogShowing = false
                dialog.dismiss()
                // Mark skipKey so service knows we handled this lecture
                if (skipKey != null) prefs.edit().putBoolean(skipKey, true).apply()
                val nowSecs = System.currentTimeMillis() / 1000
                prefs.edit()
                    .putString("timer_mode", "LECTURE")
                    .putBoolean("lecture_mode_enabled", true)
                    .putString("timerState", "STUDYING")
                    .putLong("focus_remaining_secs", remainingSecs)
                    .putLong("focusRemainingSecs", remainingSecs)
                    .putLong("focus_countdown_secs", remainingSecs)
                    .putLong("focusCountdownSecs", remainingSecs)
                    .putLong("lastTimestamp", nowSecs)
                    .putLong("accumulatedStudy", 0L)
                    .apply()

                currentTimerState = TimerState.STUDYING
                timerMode = "LECTURE"
                focusRemainingSecs = remainingSecs
                focusCountdownSecs = remainingSecs

                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    action = TimerService.ACTION_RELOAD_STATE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                if (::updateRunnable.isInitialized) updateRunnable.run()
            }
        })

        root.addView(btnRow)
        dialog.setOnDismissListener { ongoingLectureDialogShowing = false }
        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }


    private fun handleTabDragTouch(ev: MotionEvent): Boolean {
        tabDragArmed = false
        tabDragActive = false
        return false
    }

    private fun swapPanelContent(page: View) {
        panelContainer.removeAllViews()
        panelContainer.addView(page)
        if (currentPanel == AppPanel.SETTINGS) {
            settingsScrollViewRef = findScrollViewDescendant(page)
        }
    }

    private fun findScrollViewDescendant(root: View): ScrollView? {
        if (root is ScrollView) return root
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                findScrollViewDescendant(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    internal fun statsTabKey(t: AppStatsTab): String = "S:${t.ordinal}"
    internal fun settingsTabKey(t: AppSettingsTab): String = "ST:${t.ordinal}"

    private fun tabThemeSig(): String {
        val plannerPreset = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("planner_theme_preset", "DEFAULT") ?: "DEFAULT"
        return "${themeCoordinator.primaryColor}|${themeCoordinator.secondaryColor}|${themeCoordinator.bgColor}|${themeCoordinator.uiStyle}|${themeCoordinator.activeBgMode}|$plannerPreset"
    }

    private fun getOrBuildTabPage(key: String): View {
        val cached = tabPageCache[key]
        if (cached != null) {
            val valid = (cached.themeSig == tabThemeSig()) && (key.startsWith("ST:") || cached.statsGen == statsSnapshotGen)
            if (valid && cached.view.parent == null) return cached.view
        }
        val scratch = FrameLayout(this)
        val page = when {
            key.startsWith("ST:") -> {
                val prev = currentSettingsTab
                currentSettingsTab = when (key) {
                    settingsTabKey(AppSettingsTab.THEME) -> AppSettingsTab.THEME
                    settingsTabKey(AppSettingsTab.PROFILE) -> AppSettingsTab.PROFILE
                    else -> AppSettingsTab.SIMPLE
                }
                buildSettingsPanel(scratch, captureScrollRef = false)
                currentSettingsTab = prev
                scratch.getChildAt(0)
            }
            else -> {
                val prev = currentStatsTab
                currentStatsTab = when (key) {
                    statsTabKey(AppStatsTab.TIMELINE) -> AppStatsTab.TIMELINE
                    statsTabKey(AppStatsTab.PLANNER) -> AppStatsTab.PLANNER
                    else -> AppStatsTab.OVERVIEW
                }
                buildStatsPanel(scratch)
                currentStatsTab = prev
                scratch.getChildAt(0)
            }
        }
        scratch.removeView(page)
        tabPageCache[key] = CachedTabPage(page, if (key.startsWith("ST:")) 0 else statsSnapshotGen, tabThemeSig())
        return page
    }

    private fun prewarmTabPages() {
        if (panelHost.childCount > 1 || tabDragSettling) return
        when (currentPanel) {
            AppPanel.STATS -> {
                if (statsSnapshotCache == null) return
                getOrBuildTabPage(statsTabKey(AppStatsTab.OVERVIEW))
                getOrBuildTabPage(statsTabKey(AppStatsTab.TIMELINE))
                getOrBuildTabPage(statsTabKey(AppStatsTab.PLANNER))
            }
            AppPanel.SETTINGS -> {
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.SIMPLE))
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.THEME))
                getOrBuildTabPage(settingsTabKey(AppSettingsTab.PROFILE))
            }
            else -> {}
        }
    }

    private fun swipeStartedOnHorizontalScroll(rawX: Float, rawY: Float): Boolean {
        val loc = IntArray(2)
        panelContainer.getLocationOnScreen(loc)
        val x = rawX - loc[0]
        val y = rawY - loc[1]
        if (x < 0 || y < 0 || x > panelContainer.width || y > panelContainer.height) return false
        val hit = findViewAt(panelContainer, x, y) ?: return false
        var v: View? = hit
        while (v != null) {
            if (v is HorizontalScrollView) return true
            v = v.parent as? View
        }
        return false
    }

    private fun findViewAt(parent: View, x: Float, y: Float): View? {
        if (parent is android.view.ViewGroup) {
            for (i in parent.childCount - 1 downTo 0) {
                val child = parent.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                    return findViewAt(child, x - child.left, y - child.top) ?: child
                }
            }
        }
        return parent
    }

    internal fun buildCurrentPanel() {
        panelContainer.removeAllViews()
        if (currentPanel == AppPanel.FOCUS && isPomodoroPureWhiteActive()) {
            rootLayout.setBackgroundColor(0xFFFFFFFF.toInt())
        } else {
            rootLayout.background = themeCoordinator.createBackgroundDrawable()
        }
        when (currentPanel) {
            AppPanel.FOCUS -> buildFocusPanel()
            AppPanel.STATS -> buildStatsPanel()
            AppPanel.SETTINGS -> buildSettingsPanel()
            AppPanel.HEATMAP -> buildHeatmapFullscreenPanel()
            AppPanel.LEADERBOARD -> buildLeaderboardPanel()
        }
        prewarmTabPages()
        updateStatusBarIcons()
    }

    private fun buildLeaderboardPanel(target: android.view.ViewGroup = panelContainer) {
        LeaderboardPanelBuilder(this).build(target)
    }

    internal fun navigateToPanel(targetPanel: AppPanel) {
        isNavigatingBack = false

        if (isPortraitFullscreenActive && targetPanel != AppPanel.FOCUS) {
            exitPortraitFullscreenMode()
        }

        if (targetPanel == AppPanel.STATS && currentPanel != AppPanel.STATS) {
            currentStatsTab = AppStatsTab.OVERVIEW
            hasPlayedStatsEntranceAnimation = false
        }
        if (targetPanel == AppPanel.STATS || targetPanel == AppPanel.HEATMAP) {
            statsDirty = true
            statsSnapshotCache = null
            tabPageCache.clear()
        }
        statsInternalRefresh = (targetPanel == AppPanel.STATS && currentPanel == AppPanel.STATS)

        if (currentPanel == AppPanel.STATS && targetPanel != AppPanel.STATS && targetPanel != AppPanel.HEATMAP) {
            statsDirty = true
            hasPlayedStatsEntranceAnimation = false
        }

        if (currentPanel == AppPanel.SETTINGS && targetPanel != AppPanel.SETTINGS) {
            Thread { backupManager.runSilentAutoBackup() }.start()
        }

        if (targetPanel == AppPanel.HEATMAP && currentPanel != AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (targetPanel != AppPanel.HEATMAP && currentPanel == AppPanel.HEATMAP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        if (targetPanel != AppPanel.SETTINGS) {
            isDevModeUnlocked = false
        }

        val isSamePanel = targetPanel == currentPanel
        val prevPanel = currentPanel
        currentPanel = targetPanel

        if (isSamePanel || panelContainer.childCount == 0) {
            buildCurrentPanel()
            return
        }

        val slideRight = when {
            prevPanel == AppPanel.FOCUS && targetPanel == AppPanel.SETTINGS -> true
            prevPanel == AppPanel.SETTINGS && targetPanel == AppPanel.FOCUS -> false
            prevPanel == AppPanel.FOCUS && targetPanel == AppPanel.STATS -> false
            prevPanel == AppPanel.STATS && targetPanel == AppPanel.FOCUS -> true
            else -> prevPanel.ordinal < targetPanel.ordinal
        }

        performSlidingTransition(if (slideRight) 1 else -1) {
            buildCurrentPanel()
        }
    }

    private fun performSlidingTransition(exitDir: Int, rebuild: () -> Unit) {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val height = panelContainer.height.takeIf { it > 0 } ?: dp(240)
        val snapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        panelContainer.draw(Canvas(snapshot))

        rebuild()

        val springPhysics = androidx.core.view.animation.PathInterpolatorCompat.create(0.18f, 0.9f, 0.2f, 1.0f)
        val startOffset = (-exitDir * width * 0.35f)

        for (j in 0 until panelContainer.childCount) {
            val child = panelContainer.getChildAt(j)
            child.translationX = startOffset
            child.alpha = 0f
            child.scaleX = 0.96f
            child.scaleY = 0.96f
        }

        val overlay = ImageView(this).apply {
            setImageBitmap(snapshot)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(width, height)
        }
        panelHost.addView(overlay)

        overlay.animate()
            .translationX((exitDir * width * 0.35f))
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(300L)
            .setInterpolator(springPhysics)
            .withLayer()
            .start()

        for (j in 0 until panelContainer.childCount) {
            panelContainer.getChildAt(j).animate()
                .translationX(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300L)
                .setInterpolator(springPhysics)
                .withLayer()
                .start()
        }

        handler.postDelayed({
            if (overlay.parent != null) panelHost.removeView(overlay)
        }, 360L)
    }

    private fun buildFocusPanel() {
        panelContainer.removeAllViews()
        FocusPanelBuilder(this).build()
    }

    internal fun recalculateStreak(todayExtra: Long = 0L) {
        statsEngine.recalculateStreak(todayExtra)
    }

    internal fun computeStatsSnapshot(): StatsSnapshot {
        val cached = statsSnapshotCache
        if (!statsDirty && cached != null) {
            return cached
        }
        val snap = statsEngine.computeStatsSnapshot(currentBreakSeconds)
        statsSnapshotCache = snap
        statsDirty = false
        return snap
    }

    private fun buildStatsPanel(target: android.view.ViewGroup = panelContainer) {
        StatsPanelBuilder(this).build(target)
    }

    internal fun renderStatsContent(statsRoot: FrameLayout, snap: StatsSnapshot, tab: AppStatsTab = currentStatsTab) {
        StatsPanelBuilder(this).renderStatsContent(statsRoot, snap, tab)
    }

    internal fun buildHeatmapData(): Map<String, Long> = statsEngine.buildHeatmapData()

    internal fun resolveSubjectGoalFor(subjectId: String, dateStr: String): Long {
        return StatsPanelBuilder(this).resolveSubjectGoalFor(subjectId, dateStr)
    }

    internal fun buildHeatmapFullscreenPanel() {
        StatsPanelBuilder(this).buildHeatmapFullscreenPanel()
    }

    internal fun refreshHeatmapFilterChips() {
        StatsPanelBuilder(this).refreshHeatmapFilterChips()
    }

    internal fun invalidateStatsCache() {
        StatsPanelBuilder(this).invalidateStatsCache()
    }

    internal fun refreshStatsPanel() {
        StatsPanelBuilder(this).refreshStatsPanel()
    }

    internal fun checkAndResetGoalsForNewDay() {
        StatsPanelBuilder(this).checkAndResetGoalsForNewDay()
    }

    internal fun renderPlannerTabContent(parent: LinearLayout, snap: StatsSnapshot) {
        PlannerPanelBuilder(this).renderPlannerTabContent(parent, snap)
    }

    internal fun showGoalHistoryDialog(goal: PlannerGoal, displayMonthOffset: Int = 0) {
        PlannerPanelBuilder(this).showGoalHistoryDialog(goal, displayMonthOffset)
    }

    internal fun showEditYesterdayGoalsDialog() {
        PlannerPanelBuilder(this).showEditYesterdayGoalsDialog()
    }

    internal fun migrateHistoricalDailyGoals(context: Context) {
        PlannerPanelBuilder(this).migrateHistoricalDailyGoals(context)
    }

    internal fun showPlannerMatrixDialog(startFullscreen: Boolean = false) {
        PlannerPanelBuilder(this).showPlannerMatrixDialog(startFullscreen)
    }

    internal fun showFeedbackReportDialog() {
        PlannerPanelBuilder(this).showFeedbackReportDialog()
    }

    internal fun showAppGuideDialog() {
        PlannerPanelBuilder(this).showAppGuideDialog()
    }

    internal fun showDeletePlannerGoalMatrixDialog(goalId: String, goalTitle: String, onDeleted: () -> Unit) {
        PlannerPanelBuilder(this).showDeletePlannerGoalMatrixDialog(goalId, goalTitle, onDeleted)
    }

    internal fun showAddSessionGoalDialog() {
        PlannerPanelBuilder(this).showAddSessionGoalDialog()
    }

    internal fun showEditSessionGoalDialog(goal: PlannerGoal) {
        PlannerPanelBuilder(this).showEditSessionGoalDialog(goal)
    }

    internal fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return PlannerPanelBuilder(this).loadLectureSchedulesFromJson(jsonStr)
    }

    internal fun saveLectureSchedulesToJson(items: List<LectureScheduleItem>) {
        PlannerPanelBuilder(this).saveLectureSchedulesToJson(items)
    }

    internal fun showLectureScheduleManagerDialog() {
        PlannerPanelBuilder(this).showLectureScheduleManagerDialog()
    }

    internal fun showAddLectureScheduleDialog(editItem: LectureScheduleItem? = null) {
        PlannerPanelBuilder(this).showAddLectureScheduleDialog(editItem)
    }

    internal fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(18), dp(20), dp(16))
        }
        content.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.75f
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(18), 0, 0) }
        }
        buttonRow.addView(Button(this).apply {
            text = getString(R.string.btn_cancel)
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener { dialog.dismiss() }
        })
        buttonRow.addView(Button(this).apply {
            text = getString(R.string.btn_delete)
            setTextColor(0xFFFFF7ED.toInt())
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(0xFFEF4444.toInt()) }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
            setOnClickListener { dialog.dismiss(); onConfirm() }
        })
        content.addView(buttonRow)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun createSectionLabel(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(6), dp(16), 0, dp(8))
        }
    }

    internal fun createSettingsCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(4)) }
        }
    }

    private fun createInsightCard(iconRes: Int, color: Int, title: String, statement: String, subtext: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }

            val iconBox = FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply { cornerRadius = dp(13).toFloat(); setColor(tintedColor(color, 30)) }
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            }
            iconBox.addView(ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                setColorFilter(color)
                contentDescription = title
                layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
            })
            addView(iconBox)

            val textCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, 0, 0) }
            }
            textCol.addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(color)
                textSize = 10f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = statement
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(3), 0, 0)
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = subtext
                setTextColor(themeCoordinator.textColor)
                textSize = 11f
                alpha = 0.55f
                setPadding(0, dp(2), 0, 0)
                visibility = if (subtext.isEmpty()) View.GONE else View.VISIBLE
            })
            addView(textCol)
        }
    }

    private inner class BarTrackView(
        private val ratio: Float,
        private val goalRatio: Float,
        private val trackColor: Int,
        private val fillStart: Int,
        private val fillEnd: Int,
        private val isToday: Boolean,
        private val barHeight: Int
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val trackRect = RectF()
        private val fillRect = RectF()
        private var progress = 0f
        private val maxFillRatio = ratio.coerceIn(0f, 1f)
        private val fillShadowColor = Color.argb(150, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
        private val todayStrokeColor = Color.argb(180, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
        private val tickColor = Color.argb(120, 255, 255, 255)
        private var gradientShader: LinearGradient? = null

        init {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (!hasPlayedStatsEntranceAnimation) {
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 700
                anim.interpolator = android.view.animation.DecelerateInterpolator()
                anim.addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                anim.start()
            } else {
                progress = 1f
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0) {
                gradientShader = LinearGradient(0f, 0f, w.toFloat(), 0f, fillStart, fillEnd, Shader.TileMode.CLAMP)
                fillPaint.shader = gradientShader
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = h / 2f

            trackRect.set(0f, 0f, w, h)
            paint.style = Paint.Style.FILL
            paint.color = trackColor
            canvas.drawRoundRect(trackRect, radius, radius, paint)

            val fillW = w * maxFillRatio * progress
            if (fillW > 0f) {
                fillPaint.setShadowLayer(dp(7).toFloat(), 0f, 0f, fillShadowColor)
                if (fillW >= radius * 2f) {
                    fillRect.set(0f, 0f, fillW, h)
                    canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
                } else {
                    canvas.save()
                    canvas.clipRect(0f, 0f, fillW, h)
                    fillRect.set(0f, 0f, w, h)
                    canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
                    canvas.restore()
                }
                fillPaint.setShadowLayer(0f, 0f, 0f, 0)
            }

            if (goalRatio > 0f) {
                val gx = w * goalRatio
                tickPaint.color = tickColor
                canvas.drawRect(gx - dp(1).toFloat(), 0f, gx + dp(1).toFloat(), h, tickPaint)
            }

            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2) * 0.75f
                paint.color = todayStrokeColor
                canvas.drawRoundRect(trackRect, radius, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    internal inner class SegmentRing(
        private val segments: List<Pair<Float, Int>>,
        private val trackColor: Int,
        private val strokeWidth: Int,
        private val gradient: Pair<Int, Int>?,
        private val animate: Boolean = false,
        private val progressSupplier: (() -> Float)? = null
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arcRect = RectF()
        private var progress = if (animate && progressSupplier == null) 0f else 1f
        private var gradientShader: LinearGradient? = null

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.strokeCap = if (segments.size <= 1) Paint.Cap.ROUND else Paint.Cap.BUTT
            if (animate && progressSupplier == null) {
                val anim = ValueAnimator.ofFloat(0f, 1f)
                anim.duration = 600
                anim.interpolator = android.view.animation.DecelerateInterpolator()
                anim.addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                anim.start()
            } else if (progressSupplier == null) {
                progress = 1f
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0) {
                val cx = w / 2f
                val cy = h / 2f
                val r = min(w.toFloat(), h.toFloat()) / 2f - strokeWidth / 2f
                arcRect.set(cx - r, cy - r, cx + r, cy + r)
                if (gradient != null && segments.size == 1) {
                    gradientShader = LinearGradient(cx - r, cy - r, cx + r, cy + r, gradient.first, gradient.second, Shader.TileMode.CLAMP)
                }
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec)
            setMeasuredDimension(size, size)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val r = min(w, h) / 2f - strokeWidth / 2f
            if (r <= 0f) return

            paint.shader = null
            paint.color = trackColor
            canvas.drawCircle(cx, cy, r, paint)

            val curProgress = progressSupplier?.invoke() ?: progress
            var startAngle = -90f
            for ((frac, color) in segments) {
                val f = frac.coerceIn(0f, 1f)
                if (f <= 0f) continue
                val sweep = f * 360f * curProgress
                if (gradientShader != null) {
                    paint.shader = gradientShader
                } else {
                    paint.shader = null
                    paint.color = color
                }
                canvas.drawArc(arcRect, startAngle, sweep, false, paint)
                startAngle += sweep
                if (segments.size > 1) startAngle += 3f
            }
        }
    }


    internal fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> = DayTimelineDialogHelper(this).dayBlocks(dateStr)
    internal fun reconcileDayTotals(dateStr: String) = DayTimelineDialogHelper(this).reconcileDayTotals(dateStr)
    internal fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long = DayTimelineDialogHelper(this).msForDateAndTime(dateStr, h, m)
    internal fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) = DayTimelineDialogHelper(this).applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
    internal fun showBlockEditDialog(dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) = DayTimelineDialogHelper(this).showBlockEditDialog(dateStr, block, isBreak, onApplied)
    internal fun showDevTimelineEditor() = DayTimelineDialogHelper(this).showDevTimelineEditor()
    internal fun confirmDeleteBlock(dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) = DayTimelineDialogHelper(this).confirmDeleteBlock(dateStr, block, isBreak, onDone)
    internal fun confirmDeleteDay(dateStr: String, label: String) = DayTimelineDialogHelper(this).confirmDeleteDay(dateStr, label)
    internal fun showDayDialog(dateStr: String, label: String) = DayTimelineDialogHelper(this).showDayDialog(dateStr, label)
    internal fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) = DayTimelineDialogHelper(this).showMonthDialog(mName, focusSecs, breakSecs)

    internal fun showSummaryCardPreview() {
        WeeklySummaryShareHelper(this).showSummaryCardPreview()
    }

    internal fun showCustomizeHighlightsDialog() {
        WeeklySummaryShareHelper(this).showCustomizeHighlightsDialog()
    }

    internal fun showWeeklyTrendDetailDialog(thisWeek: Long, prevWeek: Long, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showWeeklyTrendDetailDialog(thisWeek, prevWeek, snap)
    }

    internal fun showActiveDaysDetailDialog(activeDays: Int, longestStreak: Int, currentStreak: Int, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showActiveDaysDetailDialog(activeDays, longestStreak, currentStreak, snap)
    }

    internal fun showBestDayDetailDialog(bestDayLabel: String, bestDaySecs: Long, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showBestDayDetailDialog(bestDayLabel, bestDaySecs, snap)
    }

    internal fun showRecordWeekDetailDialog(bestWeekLabel: String, bestWeekSecs: Long, longestStreak: Int, snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showRecordWeekDetailDialog(bestWeekLabel, bestWeekSecs, longestStreak, snap)
    }

    internal fun showGoalSuccessDetailDialog(snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showGoalSuccessDetailDialog(snap)
    }

    internal fun showAvgSessionDetailDialog(snap: StatsSnapshot) {
        WeeklySummaryShareHelper(this).showAvgSessionDetailDialog(snap)
    }

    internal fun renderViewToBitmap(view: View): android.graphics.Bitmap {
        return WeeklySummaryShareHelper(this).renderViewToBitmap(view)
    }

    internal fun saveBitmapToMediaStore(bitmap: android.graphics.Bitmap): Uri? {
        return WeeklySummaryShareHelper(this).saveBitmapToMediaStore(bitmap)
    }

    internal fun renderWeeklyCardBitmap(view: WeeklyCardView, aspect: Float): android.graphics.Bitmap {
        return WeeklySummaryShareHelper(this).renderWeeklyCardBitmap(view, aspect)
    }

    internal fun writeBitmapToCache(bitmap: android.graphics.Bitmap): Uri? {
        return WeeklySummaryShareHelper(this).writeBitmapToCache(bitmap)
    }

    internal fun createSettingsRow(icon: String, title: String, subtitle: String, endWidget: View? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            isClickable = true
            isFocusable = true
        }

        val iconView: View = if (title.equals("3D look", ignoreCase = true) || title.contains("3D look", ignoreCase = true)) {
            FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(14), 0) }
                val shadow = View(this@MainActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(6), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                        setMargins(0, 0, 0, dp(1))
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.argb(80, 0, 0, 0))
                    }
                }
                addView(shadow)
                val sphere = View(this@MainActivity).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                    val pColor = themeCoordinator.primaryColor
                    val lightHighlight = tintedColor(pColor, 200)
                    val darkDepth = tintedColor(pColor, 50)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        orientation = GradientDrawable.Orientation.TL_BR
                        colors = intArrayOf(lightHighlight, pColor, darkDepth)
                    }
                }
                addView(sphere)
            }
        } else if (title.contains("AMOLED", ignoreCase = true) || icon == "\u2B24" || icon == "⚫") {
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { setMargins(0, 0, dp(14), 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.BLACK)
                    setStroke(dp(2), Color.parseColor("#555555"))
                }
            }
        } else {
            TextView(this).apply {
                text = icon
                textSize = 22f
                setPadding(0, 0, dp(14), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        row.addView(iconView)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textContainer.addView(TextView(this).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 13.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(0, 3, 0, 0)
        })
        row.addView(textContainer)
        if (endWidget != null) {
            row.addView(endWidget)
        }
        return row
    }

    internal fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(28, 0, 28, 0) }
            setBackgroundColor(tintedColor(themeCoordinator.textColor, 22))
        }
    }

    internal fun refreshSettingsPanelPreservingScroll() {
        pendingSettingsScrollY = settingsScrollViewRef?.scrollY ?: 0
        navigateToPanel(AppPanel.SETTINGS)
        settingsScrollViewRef?.post {
            settingsScrollViewRef?.scrollTo(0, pendingSettingsScrollY)
        }
    }

    internal fun applyRandomBothHues() {
        val primaryHue = (0..359).random()
        val secondaryHue = (0..359).random()
        val primaryColor = Color.HSVToColor(floatArrayOf(primaryHue.toFloat(), 0.65f, 0.95f))
        val secondaryColor = Color.HSVToColor(floatArrayOf(secondaryHue.toFloat(), 0.65f, 0.95f))
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putBoolean("dynamic_color", false)
            .putInt("customHue", primaryHue)
            .putInt("customPrimary", primaryColor)
            .putInt("customSecondaryHue", secondaryHue)
            .putInt("customSecondary", secondaryColor)
            .apply()
        themeCoordinator.applyThemeCoordinates()
        refreshSettingsPanelPreservingScroll()
    }

    private fun buildSettingsPanel(target: android.view.ViewGroup = panelContainer, captureScrollRef: Boolean = true) {
        SettingsPanelBuilder(this).build(target, captureScrollRef)
    }

    private fun formatTime(totalSeconds: Long): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun maybeFireForegroundGoalPing(todayStr: String) {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("goal_pinged_date", null) == todayStr) return
        val focus = prefs.getLong("${todayStr}_focus_total", 0L) + accumulatedStudy
        val goal = resolveGoalFor(todayStr)
        if (focus < goal) return
        prefs.edit().putString("goal_pinged_date", todayStr).apply()
        try { rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
        Toast.makeText(this, getString(R.string.toast_goal_reached), Toast.LENGTH_LONG).show()
    }

    private fun setupTimerLoop() {
        updateRunnable = Runnable {
            val sharedPrefs = appPrefs
            accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
            currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
            currentTimerState = TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE")
            timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
            val pomodoroConfiguredSecs = sharedPrefs.safeLong("study_interval_minutes", 25L) * 60L
            focusCountdownSecs = if (timerMode == "LECTURE") {
                sharedPrefs.getLong("focus_countdown_secs", pomodoroConfiguredSecs)
            } else {
                pomodoroConfiguredSecs
            }
            focusRemainingSecs = sharedPrefs.getLong("focus_remaining_secs", 0L)
            prePauseState = runCatching { TimerState.valueOf(sharedPrefs.getString("pre_pause_state", "STUDYING") ?: "STUDYING") }.getOrDefault(TimerState.STUDYING)

            updateKeepScreenOn()

            val dayBucket = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86400000L
            if (dayBucket != lastDayBucket) {
                lastDayBucket = dayBucket
                cachedTodayStr = dateKeyFmt.format(Date())
                statsDirty = true
                if (!sharedPrefs.contains("${cachedTodayStr}_goal_secs")) {
                    sharedPrefs.edit().putLong("${cachedTodayStr}_goal_secs", sharedPrefs.getLong("daily_goal_secs", 2700L)).apply()
                }
                val goals = loadSessionGoalsFromJson(sharedPrefs.getString("session_goals_json", "[]") ?: "[]")
                PlannerHistoryManager.snapshotToday(this, goals)
                checkAndResetGoalsForNewDay()
            }
            val timerStateChanged = currentTimerState != lastTickTimerState
            if (timerStateChanged) {
                lastTickTimerState = currentTimerState
                statsDirty = true
                statsSnapshotCache = null
                tabPageCache.clear()
            }

            val currentMinuteBucket = accumulatedStudy / 60L
            val isMinuteTick = currentMinuteBucket != lastStatsMinuteTick && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK)
            if (isMinuteTick) {
                lastStatsMinuteTick = currentMinuteBucket
                statsDirty = true
                statsSnapshotCache = null
                tabPageCache.clear()
            }

            maybeFireForegroundGoalPing(cachedTodayStr)

            // Check if TimerService stored a pending lecture-switch request
            if (!ongoingLectureDialogShowing
                && sharedPrefs.getBoolean("pending_switch_to_lecture", false)
                && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK)) {
                sharedPrefs.edit().remove("pending_switch_to_lecture").apply()
                showSwitchToLectureDialog()
            }

            // Check if TimerService triggered an anti-cheat inactivity check-in prompt
            if (!inactivityCheckDialogShowing
                && sharedPrefs.getBoolean("pending_inactivity_check", false)
                && currentTimerState == TimerState.STUDYING) {
                showInactivityCheckDialog()
            }

            if (currentPanel == AppPanel.FOCUS) {
                val isLectureModeActive = sharedPrefs.getBoolean("lecture_mode_enabled", false)
                val isStudying = currentTimerState == TimerState.STUDYING ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
                val isBreaking = currentTimerState == TimerState.BREAK ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
                val showCountdown = (timerMode == "COUNTDOWN" || isLectureModeActive) && isStudying

                val nextStudyText = when {
                    showCountdown -> formatCountdown(if (focusRemainingSecs > 0L) focusRemainingSecs else focusCountdownSecs)
                    timerMode == "COUNTDOWN" && currentTimerState == TimerState.IDLE -> formatCountdown(pomodoroConfiguredSecs)
                    else -> formatTime(accumulatedStudy)
                }
                if (studyTimerDisplay.text != nextStudyText) {
                    studyTimerDisplay.text = nextStudyText
                }

                val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
                val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

                val nextBreakText = when {
                    isBreaking && timerMode == "COUNTDOWN" && breakCountdownSecs > 0L -> {
                        formatCountdown(breakRemainingSecs)
                    }
                    else -> formatTime(currentBreakSeconds)
                }
                if (breakTimerDisplay.text != nextBreakText) {
                    breakTimerDisplay.text = nextBreakText
                }

                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    if (lastIsBreakingState != isBreaking) {
                        lastIsBreakingState = isBreaking
                        if (isBreaking) {
                            breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                            breakTimerDisplay.textSize = 96f
                            breakTimerDisplay.setPadding(0, 0, 0, 5)

                            studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                            studyTimerDisplay.textSize = 24f
                            studyTimerDisplay.setPadding(0, 0, 0, 20)
                        } else {
                            studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                            studyTimerDisplay.textSize = 96f
                            studyTimerDisplay.setPadding(0, 0, 0, 5)

                            breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                            breakTimerDisplay.textSize = 24f
                            breakTimerDisplay.setPadding(0, 0, 0, 20)
                        }
                    }
                } else {
                    if (lastIsBreakingState != isBreaking || timerStateChanged) {
                        lastIsBreakingState = isBreaking
                        applyPortraitFullscreenLayout()
                    }
                }

                updateTimerRing(showCountdown)

                val showPause = sharedPrefs.getBoolean("show_pause_button", true)
                if (lastRenderedTimerState != currentTimerState || timerStateChanged || isZenModeActive != lastZenModeState || showPause != lastShowPauseState) {
                    lastRenderedTimerState = currentTimerState
                    lastZenModeState = isZenModeActive
                    lastShowPauseState = showPause
                    updateVisualStyles()
                }
                updatePauseBlink()
                if (currentTimerState == TimerState.STUDYING && accumulatedStudy > 0L) {
                    checkCelebration()
                }
            } else if (currentPanel == AppPanel.STATS) {
                updateStatsLiveViews()
            }
            handler.postDelayed(updateRunnable, 500)
        }
        handler.post(updateRunnable)
    }

    internal fun updateStatsLiveViews() {
        if (currentPanel != AppPanel.STATS) return
        val statsContainer = panelContainer
        val snap = computeStatsSnapshot()
        val todayStr = cachedTodayStr.ifEmpty { dateKeyFmt.format(Date()) }

        when (currentStatsTab) {
            AppStatsTab.PLANNER -> {
                val dailySubjectDurations = SubjectTagManager.getSubjectDurationsForDate(this, todayStr)
                val goalsJson = appPrefs.getString("session_goals_json", "[]") ?: "[]"
                val goalsList = loadSessionGoalsFromJson(goalsJson)
                val progressMap = PlannerHistoryManager.calculateGoalProgress(goalsList, snap.todayFocus, dailySubjectDurations)

                val trulyAchievedIds = mutableSetOf<String>()
                for (goal in goalsList) {
                    val prog = progressMap[goal.id]
                    val targetMins = goal.targetMinutes
                    val actualMins = prog?.actualMinutes ?: 0
                    if (targetMins > 0) {
                        if (actualMins >= targetMins) trulyAchievedIds.add(goal.id)
                    } else if (goal.completed) {
                        trulyAchievedIds.add(goal.id)
                    }
                }
                val completedCount = trulyAchievedIds.size
                val totalCount = goalsList.size
                val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

                val (plannerPrimary, _) = resolvePlannerColors()
                val isGoalReached = progressPct >= 100 && totalCount > 0
                val lineProgressColor = if (isGoalReached) 0xFF43D36E.toInt() else plannerPrimary

                (statsContainer.findViewWithTag<TextView>("planner_completed_text"))?.let {
                    it.text = "$completedCount of $totalCount completed"
                }
                (statsContainer.findViewWithTag<TextView>("planner_pct_text"))?.let {
                    it.text = "$progressPct%"
                    it.setTextColor(lineProgressColor)
                }
                (statsContainer.findViewWithTag<ProgressBar>("planner_progress_bar"))?.let {
                    it.progress = progressPct
                    it.progressTintList = android.content.res.ColorStateList.valueOf(lineProgressColor)
                }

                val greenColor = 0xFF22C55E.toInt()
                val redColor = 0xFFEF4444.toInt()

                for (goal in goalsList) {
                    val targetMins = goal.targetMinutes
                    val prog = progressMap[goal.id]
                    val actualMins = prog?.actualMinutes ?: 0

                    if (targetMins > 0) {
                        val isDone = actualMins >= targetMins
                        val progressText = if (isDone) {
                            "${actualMins}/${targetMins}m"
                        } else if (goal.completed) {
                            val deficit = targetMins - actualMins
                            "${actualMins}/${targetMins}m (${deficit}m left)"
                        } else {
                            "${actualMins}/${targetMins}m"
                        }
                        val chipColor = if (isDone) greenColor else if (goal.completed) redColor else themeCoordinator.primaryColor

                        (statsContainer.findViewWithTag<TextView>("goal_chip_${goal.id}"))?.let { chip ->
                            chip.text = progressText
                            chip.setTextColor(chipColor)
                            chip.background = themeCoordinator.createGlassChip(tintedColor(chipColor, 100), 10f)
                        }

                        (statsContainer.findViewWithTag<TextView>("goal_check_${goal.id}"))?.let { checkBtn ->
                            val isChecked = isDone || goal.completed
                            val isDeficit = !isDone && goal.completed
                            checkBtn.text = if (isChecked) (if (isDeficit) "\u2715" else "\u2713") else ""
                            checkBtn.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(if (isChecked) (if (isDeficit) redColor else greenColor) else Color.TRANSPARENT)
                                setStroke(dp(2), if (isChecked) (if (isDeficit) redColor else greenColor) else themeCoordinator.textColor)
                            }
                        }
                    }
                }
            }
            AppStatsTab.OVERVIEW -> {
                val todayFocus = snap.todayFocus
                val todayH = todayFocus / 3600
                val todayM = (todayFocus % 3600) / 60
                val todayBH = snap.todayBreak / 3600
                val todayBM = (snap.todayBreak % 3600) / 60
                val heroGoalSecs = snap.heroGoalSecs
                val heroGoalPctRaw = if (heroGoalSecs > 0) todayFocus.toFloat() / heroGoalSecs.toFloat() * 100f else 0f
                val goalReached = todayFocus >= heroGoalSecs && heroGoalSecs > 0

                (statsContainer.findViewWithTag<TextView>("overview_today_focus_time"))?.let {
                    it.text = getString(R.string.duration_h_m, todayH, todayM)
                }
                (statsContainer.findViewWithTag<TextView>("overview_target_subtext"))?.let {
                    it.text = "Target: ${formatGoalLabel(heroGoalSecs)} · ${(heroGoalPctRaw).toInt()}% completed"
                }
                (statsContainer.findViewWithTag<TextView>("overview_ring_center_text"))?.let {
                    it.text = if (goalReached) "✓" else "${heroGoalPctRaw.toInt()}%"
                }
                val remainingSecs = (heroGoalSecs - todayFocus).coerceAtLeast(0L)
                val remainingLabel = if (goalReached) "Goal Reached!" else "${formatGoalLabel(remainingSecs)} left"
                (statsContainer.findViewWithTag<TextView>("overview_remaining_badge"))?.let {
                    it.text = remainingLabel
                }
                val breakLabel = if (todayBH > 0) "${todayBH}h ${todayBM}m break" else "${todayBM}m break"
                (statsContainer.findViewWithTag<TextView>("overview_break_badge"))?.let {
                    it.text = breakLabel
                }
            }
            AppStatsTab.TIMELINE -> {
                val exams = ExamCountdownManager.getExams(this)
                for (exam in exams) {
                    val breakdown = ExamCountdownManager.getCountdownBreakdown(exam.targetTimestampMs)
                    (statsContainer.findViewWithTag<TextView>("exam_main_countdown_${exam.id}"))?.let {
                        it.text = breakdown.mainHeadline
                    }
                    (statsContainer.findViewWithTag<TextView>("exam_sub_countdown_${exam.id}"))?.let {
                        it.text = if (breakdown.readableSubtitle.isNotBlank()) breakdown.readableSubtitle else "Scheduled event"
                    }
                }
            }
        }
    }

    internal fun resolveLectureCountdownSecs(context: Context): Long {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }
        if (items.isEmpty()) return 3600L

        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val endMins = parseTimeToMinutes(item.endTime) ?: continue

            if (currentMins in startMins until endMins) {
                return ((endMins - currentMins) * 60).toLong()
            } else if (startMins > currentMins) {
                return ((endMins - startMins) * 60).toLong()
            }
        }
        val first = items.firstOrNull()
        if (first != null) {
            val startMins = parseTimeToMinutes(first.startTime) ?: 0
            val endMins = parseTimeToMinutes(first.endTime) ?: 60
            return ((endMins - startMins) * 60).toLong()
        }
        return 3600L
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


    private fun updatePauseBlink() {
        val animationsOff = try {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) == 0f
        } catch (_: Exception) { false }
        val shouldBlink = currentTimerState == TimerState.PAUSED && !animationsOff
        if (shouldBlink && pauseBlinkAnimator == null) {
            val target = if (prePauseState == TimerState.BREAK) breakTimerDisplay else studyTimerDisplay
            pauseBlinkAnimator = ValueAnimator.ofFloat(1f, 0.25f).apply {
                duration = 560
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { a ->
                    val alpha = a.animatedValue as Float
                    target.alpha = alpha
                    statusBadge.alpha = alpha
                }
                start()
            }
        } else if (!shouldBlink) {
            pauseBlinkAnimator?.cancel()
            pauseBlinkAnimator = null
            studyTimerDisplay.alpha = 1f
            breakTimerDisplay.alpha = 1f
            statusBadge.alpha = 1f
        }
    }

    private fun playToggleFeedback() {
        try {
            rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    private fun playStopFeedback() {
        try {
            rootLayout.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
    }

    private var pendingNotificationAction: (() -> Unit)? = null

    internal fun showNotificationRationaleDialog(onGranted: (() -> Unit)? = null) = BreakAndLectureDialogHelper(this).showNotificationRationaleDialog(onGranted)

    internal fun handleStateToggle() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        if (currentTimerState == TimerState.IDLE && Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val askedCount = prefs.getInt("post_notif_rationale_shown", 0)
                if (askedCount < 2) {
                    prefs.edit().putInt("post_notif_rationale_shown", askedCount + 1).apply()
                    showNotificationRationaleDialog {
                        executeStateToggle()
                    }
                    return
                }
            }
        }
        executeStateToggle()
    }

    private fun executeStateToggle() {
        if (!canExecuteAction()) return
        val prefs = appPrefs
        val isFreedomMode = prefs.getBoolean("pomodoro_freedom_mode", false)
        if (currentTimerState == TimerState.STUDYING && timerMode == "COUNTDOWN" && !isFreedomMode) {
            // Save current remaining focus countdown before break so returning to focus resumes from this position
            prefs.edit()
                .putLong("focus_remaining_secs", focusRemainingSecs)
                .putLong("focusRemainingSecs", focusRemainingSecs)
                .apply()
            showBreakDurationDialog()
            return
        }
        if (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK) {
            flipMainButton()
        }
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_TOGGLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    internal fun handlePause() {
        if (!canExecuteAction()) return
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    internal fun handleStartBreakOnly(breakSecs: Long = 300L) {
        if (!canExecuteAction()) return
        statsDirty = true
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_BREAK
            putExtra("BREAK_SECS", breakSecs)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        StudyWidgetProvider.refresh(this)
    }

    internal fun showBreakDurationDialog() = BreakAndLectureDialogHelper(this).showBreakDurationDialog()

    private fun updateTimerRing(showCountdown: Boolean) {
        if (!::timerRing.isInitialized || timerRing.visibility != View.VISIBLE) return
        if (timerMode != "COUNTDOWN") return
        val isPomoWhite = isPomodoroPureWhiteActive()
        val base = if (isPomoWhite) 0xFF000000.toInt() else themeCoordinator.textColor
        val track = if (isPomoWhite) 0xFFE2E8F0.toInt() else ((base and 0x00FFFFFF) or 0x1F000000)
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
        val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

        val studying = currentTimerState == TimerState.STUDYING ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
        val breaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)

        val primaryRingColor = if (isPomoWhite) 0xFF000000.toInt() else themeCoordinator.primaryColor
        val breakRingColor = if (isPomoWhite) 0xFF475569.toInt() else themeCoordinator.secondaryColor

        when {
            studying && showCountdown && focusCountdownSecs > 0 -> {
                timerRing.setProgress(focusRemainingSecs.toFloat() / focusCountdownSecs, primaryRingColor, track)
            }
            breaking && timerMode == "COUNTDOWN" && breakCountdownSecs > 0L -> {
                timerRing.setProgress((breakRemainingSecs.toFloat() / breakCountdownSecs).coerceIn(0f, 1f), breakRingColor, track)
            }
            breaking -> {
                timerRing.setProgress(1f, breakRingColor, track)
            }
            else -> {
                timerRing.setProgress(0f, primaryRingColor, track)
            }
        }
    }

    internal fun formatCountdown(totalSeconds: Long): String {
        val s = max(0L, totalSeconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
    }

    private fun flipMainButton() {
        if (frontFlipAnim?.isRunning == true || backFlipAnim?.isRunning == true) return
        mainBtn.scaleY = 1f
        val goingToBreak = currentTimerState != TimerState.BREAK
        frontFlipAnim = ValueAnimator.ofFloat(1f, 0f)
        frontFlipAnim!!.duration = 280
        frontFlipAnim!!.interpolator = android.view.animation.AccelerateInterpolator()
        frontFlipAnim!!.addUpdateListener { mainBtn.scaleY = it.animatedValue as Float }
        frontFlipAnim!!.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (animation !== frontFlipAnim) return
                if (goingToBreak) {
                    mainBtn.text = getString(R.string.resume_deep_focus)
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                } else {
                    mainBtn.text = getString(R.string.take_a_break)
                    mainBtn.background = rippleBackground(themeCoordinator.secondaryColor)
                }
                mainBtn.setTextColor(if (!themeCoordinator.isDarkMode() || themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.bgColor)
                backFlipAnim = ValueAnimator.ofFloat(0f, 1f)
                backFlipAnim!!.duration = 280
                backFlipAnim!!.interpolator = android.view.animation.DecelerateInterpolator()
                backFlipAnim!!.addUpdateListener { mainBtn.scaleY = it.animatedValue as Float }
                backFlipAnim!!.start()
            }
        })
        frontFlipAnim!!.start()
    }

    internal fun handleStopSession(silent: Boolean = false) {
        val savedStudy = accumulatedStudy
        val intent = Intent(this, TimerService::class.java).apply {
            action = if (silent) TimerService.ACTION_STOP_SILENT else TimerService.ACTION_STOP
        }
        startService(intent)

        currentTimerState = TimerState.IDLE
        accumulatedStudy = 0L
        currentBreakSeconds = 0L
        focusRemainingSecs = 0L

        getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).edit()
            .putLong("break_countdown_secs", 0L)
            .putLong("break_remaining_secs", 0L)
            .apply()

        statsDirty = true
        Thread {
            backupManager.runSilentAutoBackup()
            if (AuthManager.isLoggedIn(this@MainActivity)) {
                kotlinx.coroutines.runBlocking {
                    CloudSyncManager.syncDataToCloud(this@MainActivity)
                }
            }
        }.start()
        recalculateStreak(todayExtra = if (silent) 0L else savedStudy)

        if (silent) {
            try { rootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
        } else {
            playStopFeedback()
        }

        if (currentPanel == AppPanel.FOCUS) {
            updateVisualStyles()
            studyTimerDisplay.text = "00:00:00"
            breakTimerDisplay.text = getString(R.string.break_prefix, "00:00:00")
        }
        StudyWidgetProvider.refresh(this)
        checkCelebration()
    }

    internal fun updateKeepScreenOn() {
        val enabled = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).getBoolean("keep_screen_on", true)
        val shouldKeep = enabled && currentTimerState == TimerState.STUDYING
        val newFlag = if (shouldKeep) 1 else 0
        if (newFlag == lastKeepScreenOn) return
        lastKeepScreenOn = newFlag
        if (shouldKeep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private var lectureEndDialog: Dialog? = null

    internal fun checkAndShowLecturePrompt() {
        if (isFinishing || isDestroyed) return
        if (currentTimerState == TimerState.LECTURE_ENDED) {
            if (lectureEndDialog?.isShowing == true) return
            showLectureEndDialog()
        } else {
            if (lectureEndDialog?.isShowing == true) {
                lectureEndDialog?.dismiss()
                lectureEndDialog = null
            }
        }
    }

    internal fun showLectureEndDialog() = BreakAndLectureDialogHelper(this).showLectureEndDialog()

    internal fun handleStartBreak() {
        statsDirty = true
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.BREAK
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putString("timerState", "BREAK")
            .putBoolean("lecture_mode_enabled", false)
            .putLong("focus_remaining_secs", 0L)
            .putLong("lastTimestamp", now - 1L)
            .putLong("lecture_prompt_timestamp", 0L)
            .apply()

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_BREAK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        if (::studyTimerDisplay.isInitialized) {
            studyTimerDisplay.text = "00:00:00"
        }
        StudyWidgetProvider.refresh(this)
    }


    internal fun handleExtendLecture(secs: Long = 300L) {
        statsDirty = true
        val now = System.currentTimeMillis() / 1000
        currentTimerState = TimerState.STUDYING
        focusRemainingSecs = secs
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
            .putString("timerState", "STUDYING")
            .putLong("focus_remaining_secs", secs)
            .putLong("lastTimestamp", now - 1L)
            .putLong("lecture_prompt_timestamp", 0L)
            .apply()

        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_EXTEND_LECTURE
            putExtra("EXTEND_SECS", secs)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        playToggleFeedback()
        if (::studyTimerDisplay.isInitialized) {
            studyTimerDisplay.text = formatCountdown(secs)
        }
        StudyWidgetProvider.refresh(this)
    }




    internal fun updateVisualStyles() {
        val isPomoWhite = isPomodoroPureWhiteActive()
        if (currentPanel == AppPanel.FOCUS) {
            if (isPomoWhite) {
                rootLayout.setBackgroundColor(0xFFFFFFFF.toInt())
            } else {
                rootLayout.background = themeCoordinator.createBackgroundDrawable()
            }
        }

        val timerColor = if (isPomoWhite) {
            0xFF000000.toInt()
        } else if (pureWhiteTimerEnabled()) {
            0xFFFFFFFF.toInt()
        } else {
            themeCoordinator.primaryColor
        }

        val mainBtnTextColor = if (isPomoWhite) {
            0xFFFFFFFF.toInt()
        } else if (themeCoordinator.isBubbleStyle() || !themeCoordinator.isDarkMode()) {
            0xFFFFFFFF.toInt()
        } else {
            themeCoordinator.bgColor
        }

        if (isPomoWhite) {
            studyTimerDisplay.setShadowLayer(0f, 0f, 0f, 0)
        }

        when (currentTimerState) {
            TimerState.IDLE -> {
                statusBadge.text = getString(R.string.ready_to_track)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                }
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = if (isPomoWhite) View.VISIBLE else View.GONE
                if (timerMode == "COUNTDOWN") {
                    mainBtn.text = getString(R.string.start_focus)
                    mainBtn.setTextColor(mainBtnTextColor)
                    if (isPomoWhite) {
                        mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(24).toFloat()
                            setColor(0xFF000000.toInt())
                        }
                    } else {
                        mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                    }
                    pauseBtn.visibility = View.VISIBLE
                    pauseBtn.text = "☕ START BREAK"
                    if (isPomoWhite) {
                        pauseBtn.setTextColor(0xFF0F172A.toInt())
                        pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                            cornerRadius = dp(24).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                        }
                    } else {
                        pauseBtn.setTextColor(themeCoordinator.textColor)
                        pauseBtn.background = outlinedButtonBackground()
                    }
                    pauseBtn.setOnClickListener { showBreakDurationDialog() }
                    stopBtn.visibility = View.GONE
                } else {
                    mainBtn.text = getString(R.string.start_focus)
                    mainBtn.setTextColor(mainBtnTextColor)
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                    pauseBtn.visibility = View.GONE
                    stopBtn.visibility = View.GONE 
                }
            }
            TimerState.STUDYING -> {
                statusBadge.text = if (timerMode == "LECTURE") "LECTURE IN PROGRESS" else getString(R.string.learning_time)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                }
                studyTimerDisplay.setTextColor(timerColor)
                if (isPomoWhite) {
                    breakTimerDisplay.setTextColor(0xFF64748B.toInt())
                } else {
                    breakTimerDisplay.setTextColor(themeCoordinator.textColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE

                mainBtn.text = if (timerMode == "LECTURE") "Pause Lec & Start Break" else getString(R.string.take_a_break)
                mainBtn.setTextColor(mainBtnTextColor)
                if (isPomoWhite) {
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFF1E293B.toInt())
                    }
                } else {
                    mainBtn.background = rippleBackground(themeCoordinator.secondaryColor)
                }

                pauseBtn.visibility = pauseButtonVisibility()
                pauseBtn.text = getString(R.string.btn_pause)
                pauseBtn.setOnClickListener { handlePause() }
                if (isPomoWhite) {
                    pauseBtn.setTextColor(0xFF0F172A.toInt())
                    pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    pauseBtn.setTextColor(themeCoordinator.textColor)
                    pauseBtn.background = outlinedButtonBackground()
                }

                stopBtn.visibility = View.VISIBLE
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = themeCoordinator.primaryColor
                    stopBtn.setTextColor(themeCoordinator.textColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
            TimerState.LECTURE_ENDED -> {
                statusBadge.text = "LECTURE ENDED"
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                mainBtn.text = "Start Break Now"
                mainBtn.setTextColor(mainBtnTextColor)
                mainBtn.background = rippleBackground(themeCoordinator.secondaryColor)
                pauseBtn.visibility = View.GONE
                stopBtn.visibility = View.VISIBLE
                stopBtn.ringColor = themeCoordinator.primaryColor
                checkAndShowLecturePrompt()
            }
            TimerState.BREAK -> {
                statusBadge.text = getString(R.string.break_in_progress)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                    studyTimerDisplay.setTextColor(0xFF64748B.toInt())
                    breakTimerDisplay.setTextColor(0xFF000000.toInt())
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                    studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                    breakTimerDisplay.setTextColor(themeCoordinator.secondaryColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE

                mainBtn.text = getString(R.string.resume_deep_focus)
                mainBtn.setTextColor(mainBtnTextColor)
                if (isPomoWhite) {
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFF000000.toInt())
                    }
                } else {
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                }

                pauseBtn.visibility = pauseButtonVisibility()
                pauseBtn.text = getString(R.string.btn_pause)
                pauseBtn.setOnClickListener { handlePause() }
                if (isPomoWhite) {
                    pauseBtn.setTextColor(0xFF0F172A.toInt())
                    pauseBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    pauseBtn.setTextColor(themeCoordinator.textColor)
                    pauseBtn.background = outlinedButtonBackground()
                }

                stopBtn.visibility = View.VISIBLE
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = themeCoordinator.primaryColor
                    stopBtn.setTextColor(themeCoordinator.textColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
            TimerState.PAUSED -> {
                statusBadge.text = getString(R.string.paused)
                if (isPomoWhite) {
                    statusBadge.setTextColor(0xFF0F172A.toInt())
                    statusBadge.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                    studyTimerDisplay.setTextColor(0xFF000000.toInt())
                    breakTimerDisplay.setTextColor(0xFF64748B.toInt())
                } else {
                    statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                    statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                    studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                    breakTimerDisplay.setTextColor(themeCoordinator.textColor)
                }
                if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE

                mainBtn.text = getString(R.string.resume)
                mainBtn.setTextColor(mainBtnTextColor)
                if (isPomoWhite) {
                    mainBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFF000000.toInt())
                    }
                } else {
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                }

                pauseBtn.visibility = View.GONE
                stopBtn.visibility = View.VISIBLE
                if (isPomoWhite) {
                    stopBtn.ringColor = 0xFF000000.toInt()
                    stopBtn.setTextColor(0xFF0F172A.toInt())
                    stopBtn.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(24).toFloat()
                        setColor(0xFFF1F5F9.toInt())
                    }
                } else {
                    stopBtn.ringColor = themeCoordinator.primaryColor
                    stopBtn.setTextColor(themeCoordinator.textColor)
                    stopBtn.background = outlinedButtonBackground()
                }
            }
        }
        if (isPortraitFullscreenActive) {
            applyPortraitFullscreenLayout()
        }
        updateStatusBarIcons()
    }

    internal fun updateStatusBarIcons() {
        try {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            val isPomoWhiteOnFocus = (currentPanel == AppPanel.FOCUS && isPomodoroPureWhiteActive())
            val effectiveBgColor = if (isPomoWhiteOnFocus) 0xFFFFFFFF.toInt() else themeCoordinator.bgColor
            val isLight = isPomoWhiteOnFocus || themeCoordinator.activeBgMode == "LIGHT"

            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(effectiveBgColor))
            window.statusBarColor = effectiveBgColor
            window.navigationBarColor = effectiveBgColor
            val decor = window.peekDecorView() ?: window.decorView
            androidx.core.view.WindowCompat.getInsetsController(window, decor).let { controller ->
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
        } catch (_: Exception) {
            try {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                val isPomoWhiteOnFocus = (currentPanel == AppPanel.FOCUS && isPomodoroPureWhiteActive())
                val effectiveBgColor = if (isPomoWhiteOnFocus) 0xFFFFFFFF.toInt() else themeCoordinator.bgColor
                val isLight = isPomoWhiteOnFocus || themeCoordinator.activeBgMode == "LIGHT"

                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(effectiveBgColor))
                window.statusBarColor = effectiveBgColor
                window.navigationBarColor = effectiveBgColor
                @Suppress("DEPRECATION")
                var flags = window.decorView.systemUiVisibility
                flags = if (isLight) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = flags
            } catch (_: Exception) {}
        }
    }


    private fun maybeShowOnboarding() {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_seen_app_guide", false)) return
        prefs.edit().putBoolean("has_seen_app_guide", true).apply()
        showAppGuideDialog()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Study Timer Control", NotificationManager.IMPORTANCE_LOW).apply { 
                description = "Persistent tray utilities for active sessions"
                setShowBadge(false) 
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    internal fun ensureExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                } catch (_: Exception) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
            handler.post(updateRunnable)
        }
        val currentOrientation = resources.configuration.orientation
        if (lastConfigOrientation != currentOrientation) {
            lastConfigOrientation = currentOrientation
            isPortraitFullscreenActive = false
            buildCurrentPanel()
            updateVisualStyles()
        }
        applyImmersiveModeForLandscape()
        StudyWidgetProvider.refresh(this)
        checkOngoingScheduledLecturePrompt()
        checkCelebration()
        triggerAutoSyncIfEligible()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            ProfileSyncService.refreshProfileStatus(this@MainActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::updateRunnable.isInitialized) {
            handler.removeCallbacks(updateRunnable)
        }
    }



    internal fun checkCelebration() {
        if (isFinishing || isDestroyed) return
        val todayStr = dateKeyFmt.format(Date())
        val dailyGoal = resolveGoalFor(todayStr)
        if (dailyGoal <= 0L) return

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val storedTodayFocus = sharedPrefs.getLong("${todayStr}_focus_total", 0L)
        val runningSessionSecs = if (currentTimerState == TimerState.STUDYING) accumulatedStudy else 0L
        val totalTodayFocus = storedTodayFocus + runningSessionSecs

        if (totalTodayFocus >= dailyGoal) {
            val streak = sharedPrefs.safeInt("current_streak", 1)
            CelebrationEngine.checkAndCelebrate(
                activity = this,
                sessionSecs = runningSessionSecs,
                todayTotalSecs = totalTodayFocus,
                dailyGoalSecs = dailyGoal,
                currentStreak = streak
            )
        }
    }

    private var ongoingLectureDialogShowing = false

    private fun checkOngoingScheduledLecturePrompt() {
        if (ongoingLectureDialogShowing || currentTimerState != TimerState.IDLE) return

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("lecture_schedules_json", "[]") ?: "[]"
        val items = loadLectureSchedulesFromJson(jsonStr).filter { it.enabled }
        if (items.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val currentSecsInMin = cal.get(Calendar.SECOND)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        for (item in items) {
            val startMins = parseTimeToMinutes(item.startTime) ?: continue
            val endMins = parseTimeToMinutes(item.endTime) ?: continue

            if (currentMins in startMins until endMins) {
                val skipKey = "skipped_lecture_${todayStr}_${item.title}_${item.startTime}"
                if (sharedPrefs.getBoolean(skipKey, false)) {
                    continue
                }

                val remainingSecs = ((endMins - currentMins) * 60 - currentSecsInMin).toLong()
                if (remainingSecs <= 30L) continue

                showScheduledLecturePromptDialog(item, remainingSecs, skipKey)
                break
            }
        }
    }

    internal fun showScheduledLecturePromptDialog(item: LectureScheduleItem, remainingSecs: Long, skipKey: String) = BreakAndLectureDialogHelper(this).showScheduledLecturePromptDialog(item, remainingSecs, skipKey)

    private var inactivityCheckDialogShowing = false
    private var inactivityCheckDialog: Dialog? = null

    internal fun showInactivityCheckDialog() = BreakAndLectureDialogHelper(this).showInactivityCheckDialog()

    override fun onStart() {
        super.onStart()
        handler.postDelayed({ checkForUpdates(manual = false) }, 4000)
        handler.postDelayed({ maybePromptBatteryOptimization() }, 1200)
    }

    override fun onDestroy() {
        try {
            if (updateDialogRef?.isShowing == true) updateDialogRef?.dismiss()
            if (batteryOptDialogRef?.isShowing == true) batteryOptDialogRef?.dismiss()
        } catch (_: Exception) {}
        handler.removeCallbacks(holdToEndRunnable)
        pauseBlinkAnimator?.cancel()
        pauseBlinkAnimator = null
        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    internal fun toggleLandscapeControls() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return
        val sharedPrefs = appPrefs
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        if (!isLandscapeEnabled) return

        if (!::controlActionContainer.isInitialized) return
        if (controlActionContainer.visibility == View.GONE) {
            if (::navHeader.isInitialized) navHeader.visibility = View.VISIBLE
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.VISIBLE
            if (::breakTimerDisplay.isInitialized) breakTimerDisplay.visibility = View.VISIBLE
            controlActionContainer.visibility = View.VISIBLE
            if (::studyTimerDisplay.isInitialized) studyTimerDisplay.textSize = 64f
        } else {
            if (::navHeader.isInitialized) navHeader.visibility = View.GONE
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.GONE
            if (::breakTimerDisplay.isInitialized) breakTimerDisplay.visibility = View.GONE
            controlActionContainer.visibility = View.GONE
            if (::studyTimerDisplay.isInitialized) studyTimerDisplay.textSize = 110f
        }
    }

    internal fun applyTrueFullscreenMode() {
        val sharedPrefs = appPrefs
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled

        if (!isLandscape) {
            isZenModeActive = false
            if (isPortraitFullscreenActive) {
                // Keep portrait fullscreen handler
            } else {
                panelContainer.setOnClickListener(null)
            }
            return
        }

        if (isLandscapeEnabled) {
            isZenModeActive = true
            if (::navHeader.isInitialized) navHeader.visibility = View.GONE
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.GONE
            if (::breakTimerDisplay.isInitialized) breakTimerDisplay.visibility = View.GONE
            if (::controlActionContainer.isInitialized) controlActionContainer.visibility = View.GONE
            if (::studyTimerDisplay.isInitialized) studyTimerDisplay.textSize = 110f

            panelContainer.setOnClickListener {
                toggleLandscapeControls()
            }
            rootLayout.setOnClickListener {
                toggleLandscapeControls()
            }
        } else {
            isZenModeActive = false
            if (::navHeader.isInitialized) navHeader.visibility = View.VISIBLE
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.VISIBLE
            if (::breakTimerDisplay.isInitialized) breakTimerDisplay.visibility = View.VISIBLE
            if (::controlActionContainer.isInitialized) controlActionContainer.visibility = View.VISIBLE
            if (::studyTimerDisplay.isInitialized) studyTimerDisplay.textSize = 64f
            panelContainer.setOnClickListener(null)
            rootLayout.setOnClickListener(null)
        }
    }

    internal fun applyImmersiveModeForLandscape() {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isLandscapeEnabled = sharedPrefs.getBoolean("is_landscape_mode_enabled", sharedPrefs.getBoolean("true_fullscreen_landscape", true))
        val isLandscape = (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) && isLandscapeEnabled
        if (!isLandscape) {
            showSystemUI()
            return
        }
        if (isLandscapeEnabled) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    internal fun togglePortraitFullscreenMode() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFullscreenToggleTime < 350L) return
        lastFullscreenToggleTime = now

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) return
        if (currentPanel != AppPanel.FOCUS) return
        if (isPortraitFullscreenActive) {
            exitPortraitFullscreenMode()
        } else {
            enterPortraitFullscreenMode()
        }
    }

    internal fun enterPortraitFullscreenMode() {
        isPortraitFullscreenActive = true
        hideSystemUI()
        if (::navHeader.isInitialized) navHeader.visibility = View.GONE
        if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.GONE
        extraControlsContainer?.visibility = View.GONE
        if (::controlActionContainer.isInitialized) controlActionContainer.visibility = View.GONE
        statsFloatingIcon?.visibility = View.GONE
        applyPortraitFullscreenLayout()
        panelContainer.setOnClickListener {
            if (isPortraitFullscreenActive) {
                togglePortraitFullscreenMode()
            }
        }
        rootLayout.setOnClickListener {
            if (isPortraitFullscreenActive) {
                togglePortraitFullscreenMode()
            }
        }
        try { window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }

    internal fun exitPortraitFullscreenMode() {
        isPortraitFullscreenActive = false
        showSystemUI()
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            if (::navHeader.isInitialized) navHeader.visibility = View.VISIBLE
            if (::statusBadgeContainer.isInitialized) statusBadgeContainer.visibility = View.VISIBLE
            val timerModeSetting = sharedPrefs.getString("timer_mode", "SUBJECT") ?: "SUBJECT"
            val showSubjectTagging = sharedPrefs.getBoolean("enable_subject_tagging", true) && timerModeSetting != "STOPWATCH"
            if (showSubjectTagging) {
                extraControlsContainer?.visibility = View.VISIBLE
            }
            if (::controlActionContainer.isInitialized) controlActionContainer.visibility = View.VISIBLE
            statsFloatingIcon?.visibility = View.VISIBLE
            applyPortraitFullscreenLayout()
            panelContainer.setOnClickListener(null)
            rootLayout.setOnClickListener(null)
        }
        try { window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }

    internal fun applyPortraitFullscreenLayout() {
        if (!::studyTimerDisplay.isInitialized || !::breakTimerDisplay.isInitialized) return
        val isBreaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
        val hasRing = (timerMode == "COUNTDOWN" || timerMode == "LECTURE")

        if (::timerRing.isInitialized) {
            timerRing.isFullscreen = (!isLandscape && isPortraitFullscreenActive)
        }

        if (!isLandscape && isPortraitFullscreenActive) {
            val centerShift = -dp(32).toFloat()
            studyTimerDisplay.translationY = centerShift
            breakTimerDisplay.translationY = centerShift
            if (::timerRing.isInitialized) timerRing.translationY = centerShift

            val activeText = if (isBreaking) breakTimerDisplay.text.toString() else studyTimerDisplay.text.toString()
            val fullscreenTextSize = when {
                hasRing && activeText.length <= 5 -> 62f
                hasRing -> 50f
                activeText.length <= 5 -> 72f
                else -> 66f
            }

            if (isBreaking) {
                breakTimerDisplay.visibility = View.VISIBLE
                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                breakTimerDisplay.textSize = fullscreenTextSize
                breakTimerDisplay.setPadding(0, 0, 0, 0)

                studyTimerDisplay.visibility = if (currentTimerState == TimerState.PAUSED) View.VISIBLE else View.GONE
                if (currentTimerState == TimerState.PAUSED) {
                    studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                    studyTimerDisplay.textSize = 20f
                    studyTimerDisplay.setPadding(0, 0, 0, 40)
                }
            } else {
                studyTimerDisplay.visibility = View.VISIBLE
                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                studyTimerDisplay.textSize = fullscreenTextSize
                studyTimerDisplay.setPadding(0, 0, 0, 0)

                breakTimerDisplay.visibility = if (currentTimerState == TimerState.PAUSED) View.VISIBLE else View.GONE
                if (currentTimerState == TimerState.PAUSED) {
                    breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                    breakTimerDisplay.textSize = 20f
                    breakTimerDisplay.setPadding(0, 0, 0, 40)
                }
            }
        } else if (!isLandscape) {
            studyTimerDisplay.translationY = 0f
            breakTimerDisplay.translationY = 0f
            if (::timerRing.isInitialized) timerRing.translationY = 0f

            studyTimerDisplay.visibility = View.VISIBLE
            breakTimerDisplay.visibility = View.VISIBLE
            if (isBreaking) {
                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                breakTimerDisplay.textSize = 54f
                breakTimerDisplay.setPadding(0, 0, 0, 5)

                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                studyTimerDisplay.textSize = 20f
                studyTimerDisplay.setPadding(0, 0, 0, 40)
            } else {
                studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                studyTimerDisplay.textSize = 54f
                studyTimerDisplay.setPadding(0, 0, 0, 5)

                breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                breakTimerDisplay.textSize = 20f
                breakTimerDisplay.setPadding(0, 0, 0, 40)
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        updateStatusBarIcons()
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        updateStatusBarIcons()
    }

    fun showSubjectPickerDialog() {
        SubjectDialogHelper(this).showSubjectPickerDialog()
    }

    internal fun showDeleteSubjectConfirmDialog(subj: SubjectTag) {
        SubjectDialogHelper(this).showDeleteSubjectConfirmDialog(subj)
    }

    fun showAddCustomSubjectDialog(onSubjectCreated: ((SubjectTag) -> Unit)? = null) {
        SubjectDialogHelper(this).showAddCustomSubjectDialog(onSubjectCreated)
    }

    internal fun showPieChartDetailsModal(initialDateKey: String = SubjectTagManager.getTodayKey()) {
        SubjectDialogHelper(this).showPieChartDetailsModal(initialDateKey)
    }

    private var lastForegroundSyncCheckTime = 0L

    internal fun triggerAutoSyncIfEligible(force: Boolean = false) {
        if (!AuthManager.isLoggedIn(this)) return
        val now = SystemClock.elapsedRealtime()
        if (!force && (now - lastForegroundSyncCheckTime < 30_000L)) return
        lastForegroundSyncCheckTime = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (remoteMeta, rawRecord) = CloudSyncManager.fetchRemoteMetadata(this@MainActivity)
                if (remoteMeta != null && rawRecord != null) {
                    val localTs = BackupManager(this@MainActivity).getLastModifiedTimestamp()
                    val cloudTs = maxOf(remoteMeta.lastModifiedTimestamp, remoteMeta.updatedAt)
                    if (cloudTs > localTs + 1500L) {
                        android.util.Log.i("MainActivity", "Live auto-sync: Cloud data ($cloudTs) is newer than local ($localTs). Merging in background...")
                        val merged = CloudSyncManager.mergeCloudAndLocalData(this@MainActivity, rawRecord)
                        if (merged) {
                            withContext(Dispatchers.Main) {
                                if (!isDestroyed && !isFinishing) {
                                    statsDirty = true
                                    statsSnapshotCache = null
                                    tabPageCache.clear()
                                    themeCoordinator.applyThemeCoordinates()
                                    navigateToPanel(currentPanel)
                                    StudyWidgetProvider.refresh(this@MainActivity)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Auto-sync check non-fatal exception", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        hasPlayedStatsEntranceAnimation = false
    }

}




