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

private val dailyQuotes = listOf(
    "\"The secret of getting ahead is getting started.\" \u2014 Mark Twain",
    "\"Success is the sum of small efforts, repeated day in and day out.\" \u2014 Robert Collier",
    "\"The future depends on what you do today.\" \u2014 Mahatma Gandhi",
    "\"It always seems impossible until it's done.\" \u2014 Nelson Mandela",
    "\"Education is the most powerful weapon which you can use to change the world.\" \u2014 Nelson Mandela",
    "\"The expert in anything was once a beginner.\" \u2014 Helen Hayes",
    "\"Well done is better than well said.\" \u2014 Benjamin Franklin",
    "\"Quality is not an act, it is a habit.\" \u2014 Aristotle",
    "\"Our greatest glory is not in never falling, but in rising every time we fall.\" \u2014 Confucius",
    "\"He who has a why to live can bear almost any how.\" \u2014 Friedrich Nietzsche",
    "\"Do what you can, with what you have, where you are.\" \u2014 Theodore Roosevelt",
    "\"The beautiful thing about learning is that no one can take it away from you.\" \u2014 B. B. King",
    "\"An investment in knowledge pays the best interest.\" \u2014 Benjamin Franklin",
    "\"The roots of education are bitter, but the fruit is sweet.\" \u2014 Aristotle",
    "\"Act as if what you do makes a difference. It does.\" \u2014 William James",
    "\"Do not wait to strike till the iron is hot, but make it hot by striking.\" \u2014 William Butler Yeats",
    "\"Perseverance is not a long race; it is many short races one after another.\" \u2014 Walter Elliot",
    "\"Knowing is not enough; we must apply. Willing is not enough; we must do.\" \u2014 Johann Wolfgang von Goethe",
    "\"The best way to predict the future is to create it.\" \u2014 Peter Drucker",
    "\"Don't watch the clock; do what it does. Keep going.\" \u2014 Sam Levenson",
    "\"Knowing yourself is the beginning of wisdom.\" \u2014 Aristotle",
    "\"We become what we repeatedly do.\" \u2014 Aristotle",
    "\"To improve is to change; to be perfect is to change often.\" \u2014 Winston Churchill",
    "\"The greatest victory is over yourself.\" \u2014 Plato",
    "\"No man ever steps in the same river twice.\" \u2014 Heraclitus",
    "\"Freedom is the power to choose our own chains.\" \u2014 Jean-Jacques Rousseau",
    "\"The unexamined life is not worth living.\" \u2014 Socrates",
    "\"The mind is not a vessel to be filled but a fire to be kindled.\" \u2014 Plutarch",
    "\"Patience is power.\" \u2014 Rumi",
    "\"Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.\" \u2014 Rumi",
    "\"Respond intelligently even to unintelligent treatment.\" \u2014 Lao Tzu",
    "\"Nature does not hurry, yet everything is accomplished.\" \u2014 Lao Tzu",
    "\"Be the change that you wish to see in the world.\" \u2014 Mahatma Gandhi",
    "\"Strength does not come from physical capacity. It comes from an indomitable will.\" \u2014 Mahatma Gandhi",
    "\"Live as if you were to die tomorrow. Learn as if you were to live forever.\" \u2014 Mahatma Gandhi",
    "\"Arise, awake, and stop not till the goal is reached.\" \u2014 Swami Vivekananda",
    "\"Take risks in your life. If you win, you can lead; if you lose, you can guide.\" \u2014 Swami Vivekananda",
    "\"All power is within you; you can do anything.\" \u2014 Swami Vivekananda",
    "\"Dream is not that which you see while sleeping; it is something that does not let you sleep.\" \u2014 A. P. J. Abdul Kalam",
    "\"Excellence happens not by accident. It is a process.\" \u2014 A. P. J. Abdul Kalam",
    "\"If you want to shine like a sun, first burn like a sun.\" \u2014 A. P. J. Abdul Kalam",
    "\"Failure will never overtake me if my determination to succeed is strong enough.\" \u2014 Og Mandino",
    "\"The best revenge is to be unlike him who performed the injury.\" \u2014 Marcus Aurelius",
    "\"Waste no time on what you cannot control.\" \u2014 Epictetus",
    "\"Difficulties are things that show what people are.\" \u2014 Epictetus",
    "\"The willing are led by fate; the unwilling are dragged.\" \u2014 Cleanthes",
    "\"To be everywhere is to be nowhere.\" \u2014 Seneca",
    "\"Every new beginning comes from some other beginning's end.\" \u2014 Seneca",
    "\"Without continual growth and progress, such words as improvement and success have no meaning.\" \u2014 Benjamin Franklin",
    "\"The journey itself is my home.\" \u2014 Matsuo Bash\u014D",
    "\"The only source of knowledge is experience.\" \u2014 Albert Einstein",
    "\"Logic will get you from A to B. Imagination will take you everywhere.\" \u2014 Albert Einstein",
    "\"Anyone who has never made a mistake has never tried anything new.\" \u2014 Albert Einstein",
    "\"Do not pray for an easy life; pray for the strength to endure a difficult one.\" \u2014 Bruce Lee",
    "\"Absorb what is useful, discard what is not.\" \u2014 Bruce Lee",
    "\"The successful warrior is the average man, with laser-like focus.\" \u2014 Bruce Lee",
    "\"The only true failure is the failure to learn.\" \u2014 John C. Maxwell",
    "\"Success is achieved and maintained by those who try and keep trying.\" \u2014 W. Clement Stone",
    "\"There is no substitute for hard work.\" \u2014 Thomas A. Edison",
    "\"The only limit to our realization of tomorrow is our doubts of today.\" \u2014 Franklin D. Roosevelt",
    "\"It does not matter how slowly you go as long as you do not stop.\" \u2014 Confucius",
    "\"Happiness depends upon ourselves.\" \u2014 Aristotle",
    "\"The mind is everything. What you think you become.\" \u2014 Gautama Buddha",
    "\"The best preparation for tomorrow is doing your best today.\" \u2014 H. Jackson Brown Jr.",
    "\"Energy and persistence conquer all things.\" \u2014 Benjamin Franklin",
    "\"Life is really simple, but we insist on making it complicated.\" \u2014 Confucius",
    "\"The greatest wealth is to live content with little.\" \u2014 Plato",
    "\"Do not let what you cannot do interfere with what you can do.\" \u2014 John Wooden",
    "\"If you can dream it, you can do it.\" \u2014 Walt Disney",
    "\"The way to get started is to quit talking and begin doing.\" \u2014 Walt Disney",
    "\"Success usually comes to those who are too busy to be looking for it.\" \u2014 Henry David Thoreau",
    "\"What lies behind us and what lies before us are tiny matters compared to what lies within us.\" \u2014 Ralph Waldo Emerson",
    "\"Fortune favors the bold.\" \u2014 Virgil",
    "\"The harder you work, the luckier you get.\" \u2014 Gary Player",
    "\"A journey is best measured in friends, rather than miles.\" \u2014 Tim Cahill",
    "\"What you do speaks so loudly that I cannot hear what you say.\" \u2014 Ralph Waldo Emerson",
    "\"Success is where preparation and opportunity meet.\" \u2014 Bobby Unser",
    "\"No act of kindness, no matter how small, is ever wasted.\" \u2014 Aesop",
    "\"The future belongs to those who prepare for it today.\" \u2014 Malcolm X",
    "\"The purpose of our lives is to be happy.\" \u2014 Dalai Lama",
    "\"Difficulties strengthen the mind, as labor does the body.\" \u2014 Seneca",
    "\"Waste no more time arguing what a good person should be. Be one.\" \u2014 Marcus Aurelius",
    "\"The soul becomes dyed with the color of its thoughts.\" \u2014 Marcus Aurelius",
    "\"He who learns but does not think is lost.\" \u2014 Confucius",
    "\"Knowledge speaks, but wisdom listens.\" \u2014 Jimi Hendrix",
    "\"The only impossible journey is the one you never begin.\" \u2014 Tony Robbins",
)

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

    private var accumulatedStudy: Long = 0
    private var currentBreakSeconds: Long = 0

    internal var timerMode: String = "STOPWATCH"
    private var focusCountdownSecs: Long = 1500L
    private var focusRemainingSecs: Long = 0L
    private var prePauseState: TimerState = TimerState.STUDYING

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

    internal var statsSnapshotCache: StatsSnapshot? = null
    internal var statsSnapshotGen = 0
    internal var statsDirty = true
    internal var statsInternalRefresh = false
    private var lastStyleKey = ""
    private var lastTickTimerState: TimerState? = null
    private var lastIsBreakingState: Boolean? = null
    private var lastZenModeState: Boolean? = null
    private var lastShowPauseState: Boolean? = null
    private var lastDayBucket: Long = -1L
    private var cachedTodayStr = ""

    internal lateinit var themeCoordinator: ThemeCoordinator
    private lateinit var backupManager: BackupManager
    internal val statsEngine by lazy { StatsEngine(this) }
    internal val dateKeyFmt by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    private lateinit var rootLayout: LinearLayout
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
    private var isZenModeActive = false
    private var isNavigatingBack = false
    internal var calendarYear = 0
    internal var calendarMonth = 0
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    internal val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> showRestoreConfirmDialog(uri) }
        }
    }

    internal val avatarImagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                Thread {
                    kotlinx.coroutines.runBlocking {
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            if (originalBitmap != null) {
                                val maxDim = 400
                                val scale = maxDim.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)
                                val scaledBitmap = if (scale < 1f) {
                                    android.graphics.Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        (originalBitmap.width * scale).toInt(),
                                        (originalBitmap.height * scale).toInt(),
                                        true
                                    )
                                } else originalBitmap
                                val baos = java.io.ByteArrayOutputStream()
                                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, baos)
                                val imageBytes = baos.toByteArray()

                                val base64Str = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                                val localDataUri = "data:image/jpeg;base64,$base64Str"
                                AuthManager.saveProfileImageUri(this@MainActivity, localDataUri)

                                val publicUrl = CloudSyncManager.uploadProfileImageToStorage(this@MainActivity, imageBytes)
                                if (!publicUrl.isNullOrEmpty()) {
                                    AuthManager.saveProfileImageUri(this@MainActivity, publicUrl)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to upload profile image", e)
                        }
                        runOnUiThread {
                            tabPageCache.remove(settingsTabKey(AppSettingsTab.PROFILE))
                            navigateToPanel(AppPanel.SETTINGS)
                        }
                        CloudSyncManager.syncDataToCloud(this@MainActivity)
                    }
                }.start()
            }
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
        val profileImageUriStr = AuthManager.getProfileImageUri(this)
        val userName = AuthManager.getUserName(this)
        val userEmail = AuthManager.getUserEmail(this)

        val frame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        var avatarBitmap: android.graphics.Bitmap? = null
        if (!profileImageUriStr.isNullOrEmpty()) {
            try {
                if (profileImageUriStr.startsWith("http://") || profileImageUriStr.startsWith("https://")) {
                    Thread {
                        try {
                            val url = java.net.URL(profileImageUriStr)
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                            if (bmp != null) {
                                runOnUiThread {
                                    val avatarImgView = frame.findViewById<android.widget.ImageView>(1002)
                                    if (avatarImgView != null) {
                                        avatarImgView.setImageBitmap(bmp)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }.start()
                } else if (profileImageUriStr.startsWith("data:image")) {
                    val base64Data = profileImageUriStr.substringAfter("base64,")
                    val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    avatarBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                } else {
                    val uri = android.net.Uri.parse(profileImageUriStr)
                    val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.createSource(contentResolver, uri)
                    } else null
                    avatarBitmap = if (source != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                }
            } catch (_: Exception) {}
        }

        if (avatarBitmap != null || (profileImageUriStr != null && (profileImageUriStr.startsWith("http://") || profileImageUriStr.startsWith("https://")))) {
            val imgView = android.widget.ImageView(this).apply {
                id = 1002
                if (avatarBitmap != null) setImageBitmap(avatarBitmap)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeCoordinator.primaryColor)
                }
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
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

    internal fun showSignOutConfirmDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(this).apply {
            text = "ACCOUNT"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Sign Out"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "Are you sure you want to sign out of your account?"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13.5f
            setPadding(0, dp(6), 0, 0)
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
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val signOutBtn = Button(this).apply {
            text = "SIGN OUT"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#E53E3E"))
            }
            setOnClickListener {
                dialog.dismiss()
                AuthManager.logout(this@MainActivity)
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(signOutBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun showDeleteAccountConfirmDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(18))
        }

        content.addView(TextView(this).apply {
            text = "DANGER ZONE"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Delete Account & Cloud Data"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "This will permanently delete your cloud backup, habit logs, synced profile, and local study history. This action cannot be undone."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 13.5f
            setPadding(0, dp(6), 0, 0)
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
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        val deleteBtn = Button(this).apply {
            text = "HOLD TO DELETE"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#DC2626"))
            }
            setOnClickListener {
                android.widget.Toast.makeText(this@MainActivity, "Hold button for 1.5s to confirm wipe", android.widget.Toast.LENGTH_SHORT).show()
            }
            setOnLongClickListener {
                try {
                    rootLayout.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (_: Exception) {}
                dialog.dismiss()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.deleteUserCloudData(this@MainActivity)
                    }
                    runOnUiThread {
                        AuthManager.deleteLocalUserData(this@MainActivity)
                        android.widget.Toast.makeText(this@MainActivity, "Account & all data permanently deleted.", android.widget.Toast.LENGTH_LONG).show()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }.start()
                true
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(8), 0, 0, 0) }
        }
        buttonRow.addView(cancelBtn)
        buttonRow.addView(deleteBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
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
                openUpdateUrl(info.url)
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
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().remove("notification_perm_prompt_count").apply()
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
            val fill = if (themeCoordinator.isDarkMode()) 0x1AFFFFFF.toInt() else 0xE6FFFFFF.toInt()
            val stroke = if (themeCoordinator.isDarkMode()) 0x33FFFFFF.toInt() else 0x1A0F172A.toInt()
            return GradientDrawable().apply {
                this.cornerRadius = 80f * density
                setColor(fill)
                setStroke((1.5f * density).toInt(), stroke)
            }
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
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    (v.background as? Soft3DBubbleDrawable)?.isPressed = false
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220).setInterpolator(android.view.animation.OvershootInterpolator(2.0f)).start()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("activeBgMode")) {
            sharedPrefs.edit()
                .putString("activeBgMode", "OLED")
                .putString("ui_style", "BUBBLE")
                .putInt("customHue", 234)
                .putInt("customPrimary", Color.HSVToColor(floatArrayOf(234f, 0.65f, 0.95f)))
                .putInt("customSecondaryHue", 1)
                .putInt("customSecondary", Color.HSVToColor(floatArrayOf(1f, 0.65f, 0.95f)))
                .putString("timer_mode", "COUNTDOWN")
                .putLong("focus_countdown_secs", 3600L)
                .putInt("reminder_hour", 20)
                .putInt("reminder_minute", 0)
                .putBoolean("reminder_enabled", true)
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
        
        AppAnalytics.init(this)
        CrashReporter.init(this)

        backupManager.triggerAutoRestoreIfPresent()
        themeCoordinator.applyThemeCoordinates()

        createNotificationChannel()
        GoalReminderScheduler.schedule(this)

        requestNotificationPermissionIfNeeded()

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createBackgroundDrawable()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        panelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        panelHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        panelHost.addView(panelContainer)
        rootLayout.addView(panelHost)
        setContentView(rootLayout)

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
        migrateHistoricalDailyGoals(this)
        checkAndResetGoalsForNewDay()


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private var lastBackTime = 0L
            override fun handleOnBackPressed() {
                if (currentPanel == AppPanel.HEATMAP) {
                    navigateToPanel(AppPanel.STATS)
                } else if (currentPanel == AppPanel.SETTINGS && currentSettingsTab != AppSettingsTab.HUB) {
                    // Nested Settings sub-screen: pop back one level to the main Settings Hub Dashboard
                    currentSettingsTab = AppSettingsTab.HUB
                    navigateToPanel(AppPanel.SETTINGS)
                } else {
                    // Root Top-Level Destinations (Settings Hub, Stats, Focus): exit cleanly via standard double-tap or finish
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
        navigateToPanel(currentPanel)
        applyImmersiveModeForLandscape()
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
        if (currentPanel != AppPanel.STATS) {
            tabDragArmed = false
            tabDragActive = false
            return false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (tabDragSettling) finalizeSettle(tabDragSettleIsCommit)
                if (panelHost.childCount > 1) return false
                if (swipeStartedOnHorizontalScroll(ev.rawX, ev.rawY)) return false
                tabDragArmed = true
                tabDragActive = false
                tabDragDownX = ev.x
                tabDragDownY = ev.y
                tabDragLastX = ev.x
                tabDragLastT = SystemClock.uptimeMillis()
                tabDragVelocityX = 0f
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (tabDragSettling) return true
                if (!tabDragArmed) return false
                val now = SystemClock.uptimeMillis()
                if (now - tabDragLastT > 0) {
                    tabDragVelocityX = (ev.x - tabDragLastX) / (now - tabDragLastT) * 1000f
                }
                tabDragLastX = ev.x
                tabDragLastT = now
                if (!tabDragActive) {
                    val dx = ev.x - tabDragDownX
                    val dy = ev.y - tabDragDownY
                    if (Math.abs(dy) > tabDragSlop && Math.abs(dy) >= Math.abs(dx) * 1.1f) {
                        tabDragArmed = false
                        return false
                    }
                    if (Math.abs(dx) > tabDragSlop && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                        val side = if (dx < 0) 1 else -1
                        if (tabDragNeighborExists(side) && beginTabDrag(side)) {
                            tabDragActive = true
                            panelContainer.cancelPendingInputEvents()
                        } else {
                            tabDragArmed = false
                            return false
                        }
                    } else {
                        return false
                    }
                }
                val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
                val delta = Math.max(-width.toFloat(), Math.min(width.toFloat(), ev.x - tabDragDownX))
                panelContainer.translationX = delta
                tabDragOverlay?.translationX = (tabDragSide * width) + delta
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (tabDragSettling) return true
                if (!tabDragActive) {
                    tabDragArmed = false
                    return false
                }
                val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
                val delta = panelContainer.translationX
                val commit = when {
                    tabDragSide == 1 -> delta < -width * 0.22f || tabDragVelocityX < -550f
                    else -> delta > width * 0.22f || tabDragVelocityX > 550f
                }
                tabDragArmed = false
                tabDragActive = false
                if (commit) finishTabDrag() else cancelTabDrag()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (tabDragSettling) return true
                if (!tabDragActive) {
                    tabDragArmed = false
                    return false
                }
                tabDragArmed = false
                tabDragActive = false
                cancelTabDrag()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                if (tabDragActive || tabDragSettling) return true
                return false
            }
        }
        return false
    }

    private fun tabDragNeighborExists(side: Int): Boolean {
        return when (currentPanel) {
            AppPanel.STATS -> if (side == 1) currentStatsTab != AppStatsTab.PLANNER else currentStatsTab != AppStatsTab.OVERVIEW
            AppPanel.SETTINGS -> if (side == 1) currentSettingsTab != AppSettingsTab.PROFILE else currentSettingsTab != AppSettingsTab.SIMPLE
            else -> false
        }
    }

    private fun applyCardStyle(v: View) {
        val r = dp(22)
        val bg = android.graphics.drawable.GradientDrawable().apply {
            setColor(themeCoordinator.bgColor)
            cornerRadius = r.toFloat()
        }
        v.background = bg
        v.clipToOutline = true
        v.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        v.elevation = dp(14).toFloat()
    }

    private fun clearCardStyle(v: View) {
        v.background = null
        v.clipToOutline = false
        v.elevation = 0f
    }

    private fun beginTabDrag(side: Int): Boolean {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val height = panelContainer.height.takeIf { it > 0 } ?: dp(240)
        tabDragSide = side
        val key = when (currentPanel) {
            AppPanel.STATS -> {
                tabDragCommitStatsTab = if (side == 1) {
                    if (currentStatsTab == AppStatsTab.OVERVIEW) AppStatsTab.TIMELINE else AppStatsTab.PLANNER
                } else {
                    if (currentStatsTab == AppStatsTab.PLANNER) AppStatsTab.TIMELINE else AppStatsTab.OVERVIEW
                }
                statsTabKey(tabDragCommitStatsTab)
            }


            AppPanel.SETTINGS -> {
                tabDragCommitSettingsTab = if (side == 1) {
                    if (currentSettingsTab == AppSettingsTab.SIMPLE) AppSettingsTab.THEME else AppSettingsTab.PROFILE
                } else {
                    if (currentSettingsTab == AppSettingsTab.PROFILE) AppSettingsTab.THEME else AppSettingsTab.SIMPLE
                }
                settingsTabKey(tabDragCommitSettingsTab)
            }
            else -> return false
        }
        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(width, height)
        }
        applyCardStyle(panelContainer)
        overlay.addView(getOrBuildTabPage(key))
        applyCardStyle(overlay)
        overlay.translationX = (side * width).toFloat()
        panelHost.addView(overlay)
        tabDragOverlay = overlay
        return true
    }

    private fun settleAnimDuration(remaining: Float): Long {
        val dist = Math.abs(remaining)
        val vel = Math.abs(tabDragVelocityX)
        val base = if (vel > 120f) (dist / vel * 1000f).toLong() else 340L
        return Math.max(150L, Math.min(340L, base))
    }

    private fun settleInterpolator() = android.view.animation.PathInterpolator(0.25f, 0.9f, 0.25f, 1f)

    private fun finishTabDrag() {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val overlay = tabDragOverlay
        val token = ++tabDragSettleToken
        tabDragSettling = true
        tabDragSettleIsCommit = true
        val targetX = (-tabDragSide * width).toFloat()
        val remaining = targetX - panelContainer.translationX
        val dur = settleAnimDuration(remaining)
        val interp = settleInterpolator()
        panelContainer.animate().translationX(targetX)
            .setDuration(dur)
            .setInterpolator(interp)
            .withLayer().start()
        overlay?.animate()?.translationX(0f)
            ?.setDuration(dur)
            ?.setInterpolator(interp)
            ?.withLayer()?.start()
        handler.postDelayed({ if (token == tabDragSettleToken) finalizeSettle(true) }, dur + 80L)
    }

    private fun cancelTabDrag() {
        val width = panelContainer.width.takeIf { it > 0 } ?: dp(160)
        val overlay = tabDragOverlay
        val token = ++tabDragSettleToken
        tabDragSettling = true
        tabDragSettleIsCommit = false
        val remaining = 0f - panelContainer.translationX
        val dur = settleAnimDuration(remaining)
        val interp = settleInterpolator()
        panelContainer.animate().translationX(0f)
            .setDuration(dur)
            .setInterpolator(interp)
            .withLayer().start()
        overlay?.animate()?.translationX((tabDragSide * width).toFloat())
            ?.setDuration(dur)
            ?.setInterpolator(interp)
            ?.withLayer()?.start()
        handler.postDelayed({ if (token == tabDragSettleToken) finalizeSettle(false) }, dur + 80L)
    }

    private fun finalizeSettle(commit: Boolean) {
        if (!tabDragSettling) return
        tabDragSettling = false
        tabDragSettleToken++
        val overlay = tabDragOverlay
        panelContainer.animate().cancel()
        panelContainer.translationX = 0f
        overlay?.animate()?.cancel()
        clearCardStyle(panelContainer)
        var incomingPage: View? = null
        if (overlay != null) {
            if (overlay.parent != null) panelHost.removeView(overlay)
            if (overlay.childCount > 0) {
                incomingPage = overlay.getChildAt(0)
                overlay.removeView(incomingPage)
            }
        }
        tabDragOverlay = null
        if (commit) {
            when (currentPanel) {
                AppPanel.STATS -> {
                    currentStatsTab = tabDragCommitStatsTab
                }
                AppPanel.SETTINGS -> currentSettingsTab = tabDragCommitSettingsTab
                else -> {}
            }
            if (incomingPage != null) {
                swapPanelContent(incomingPage)
            } else {
                navigateToPanel(currentPanel)
            }
        }
        prewarmTabPages()
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

    private fun buildCurrentPanel() {
        panelContainer.removeAllViews()
        rootLayout.background = themeCoordinator.createBackgroundDrawable()
        when (currentPanel) {
            AppPanel.FOCUS -> buildFocusPanel()
            AppPanel.STATS -> buildStatsPanel()
            AppPanel.SETTINGS -> buildSettingsPanel()
            AppPanel.HEATMAP -> buildHeatmapFullscreenPanel()
        }
        prewarmTabPages()
    }

    internal fun navigateToPanel(targetPanel: AppPanel) {
        isNavigatingBack = false

        if (targetPanel == AppPanel.STATS && currentPanel != AppPanel.STATS) {
            currentStatsTab = AppStatsTab.OVERVIEW
        }
        statsInternalRefresh = (targetPanel == AppPanel.STATS && currentPanel == AppPanel.STATS)

        if (currentPanel == AppPanel.STATS && targetPanel != AppPanel.STATS && targetPanel != AppPanel.HEATMAP) {
            statsDirty = true
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

        val fastOutSlowIn = androidx.core.view.animation.PathInterpolatorCompat.create(0.2f, 0.0f, 0.0f, 1.0f)
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
            .setDuration(320L)
            .setInterpolator(fastOutSlowIn)
            .withLayer()
            .start()

        for (j in 0 until panelContainer.childCount) {
            panelContainer.getChildAt(j).animate()
                .translationX(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(320L)
                .setInterpolator(fastOutSlowIn)
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

    internal fun computeStatsSnapshot(): StatsSnapshot = statsEngine.computeStatsSnapshot(currentBreakSeconds)

    private fun buildStatsPanel(target: android.view.ViewGroup = panelContainer) {
        StatsPanelBuilder(this).build(target)
    }

    private fun buildHeatmapData(): Map<String, Long> = statsEngine.buildHeatmapData()

    private fun buildHeatmapFullscreenPanel() {
        val heatmapData = statsSnapshotCache?.heatmapData ?: buildHeatmapData()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, dp(10)) }
        }
        val headerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerCol.addView(TextView(this).apply {
            text = getString(R.string.heatmap_fullscreen_title)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            letterSpacing = 0.16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        headerCol.addView(TextView(this).apply {
            text = getString(R.string.heatmap_hint)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(0, dp(3), 0, 0)
        })
        headerRow.addView(headerCol)
        headerRow.addView(TextView(this).apply {
            text = getString(R.string.btn_done)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
            setOnClickListener { navigateToPanel(AppPanel.STATS) }
        })
        root.addView(headerRow)

        val heatmapScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        heatmapScroll.addView(HeatmapView(this).apply {
            forcedCellSize = dp(32).toFloat()
            setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
            onDayTap = { dateStr ->
                val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                this@MainActivity.showDayDialog(dateStr, lbl)
            }
        })
        heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
        root.addView(heatmapScroll)

        panelContainer.addView(root)
    }

    internal fun showDeleteGoalDialog(dateStr: String, dateLabel: String) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val focusSecs = sharedPrefs.getLong("day_focus_$dateStr", 0L)
        val dialog = Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(24f)
        }

        root.addView(TextView(this).apply {
            text = "Delete Goal & Habit History?"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val formattedFocus = if (focusSecs >= 3600) "${focusSecs / 3600}h ${(focusSecs % 3600) / 60}m" else "${(focusSecs % 3600) / 60}m"
        root.addView(TextView(this).apply {
            text = "Date: $dateLabel\nRecorded Focus: $formattedFocus\n\nWould you like to keep or delete this goal history entry?"
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
            text = "Keep"
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener { dialog.dismiss() }
        })

        btnRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })

        btnRow.addView(TextView(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(Color.argb(40, 255, 82, 82), 16f)
            setOnClickListener {
                dialog.dismiss()
                sharedPrefs.edit()
                    .remove("day_focus_$dateStr")
                    .remove("daily_goal_sec_$dateStr")
                    .apply()
                TimelineLogger.deleteDay(this@MainActivity, dateStr)
                SubjectTagManager.clearTodaySubjectDurations(this@MainActivity, dateStr)
                statsSnapshotCache = null
                tabPageCache.remove(statsTabKey(AppStatsTab.OVERVIEW))
                tabPageCache.remove(statsTabKey(AppStatsTab.TIMELINE))
                if (currentPanel == AppPanel.STATS || currentPanel == AppPanel.HEATMAP) {
                    navigateToPanel(currentPanel)
                }
                Toast.makeText(this@MainActivity, "Goal history deleted for $dateLabel", Toast.LENGTH_SHORT).show()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(this@MainActivity)
                    }
                }.start()
            }
        })

        root.addView(btnRow)
        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    internal fun renderStatsContent(statsRoot: FrameLayout, snap: StatsSnapshot, tab: AppStatsTab = currentStatsTab) {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f); isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(96)) }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = getString(R.string.insights_title)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 22f
            setPadding(dp(6), 0, dp(6), 0)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayQuote = dailyQuotes[dayOfYear % dailyQuotes.size]
        val quoteCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
        }
        quoteCard.addView(TextView(this).apply {
            text = getString(R.string.thought_of_the_day)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.5f
            textSize = 10f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.15f
        })
        quoteCard.addView(TextView(this).apply {
            text = todayQuote
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            setPadding(0, dp(4), 0, 0)
        })
        content.addView(quoteCard)

        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }

        fun tabBtn(title: String, tab: AppStatsTab): TextView {
            return TextView(this).apply {
                text = title
                textSize = 14f
                setPadding(dp(18), dp(10), dp(18), dp(10))
                typeface = Typeface.create("sans-serif-medium", if (currentStatsTab == tab) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (currentStatsTab == tab) themeCoordinator.primaryColor else themeCoordinator.textColor)
                background = if (currentStatsTab == tab) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f) else null
                setOnClickListener {
                    currentStatsTab = tab
                    tabPageCache.clear()
                    navigateToPanel(AppPanel.STATS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4), 0, dp(4), 0) }
            }
        }
        tabContainer.addView(tabBtn(getString(R.string.tab_overview), AppStatsTab.OVERVIEW))
        tabContainer.addView(tabBtn(getString(R.string.tab_history), AppStatsTab.TIMELINE))
        tabContainer.addView(tabBtn("Planner", AppStatsTab.PLANNER))
        content.addView(tabContainer)



        val todayFocus = snap.todayFocus
        val todayBreak = snap.todayBreak
        val todayH = todayFocus / 3600
        val todayM = (todayFocus % 3600) / 60
        val todayBH = todayBreak / 3600
        val todayBM = (todayBreak % 3600) / 60
        val streak = snap.streak

        val avg7 = snap.avg7
        val avgH = avg7 / 3600
        val avgM = (avg7 % 3600) / 60

        val chartCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val heroCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(2), 0, dp(8)) }
        }

        val heroTopRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        heroTopRow.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.today_label)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.2f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        heroTopRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        val streakChip = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { cornerRadius = 14f; setColor(tintedColor(themeCoordinator.primaryColor, 22)) }
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
        streakChip.addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_flame)
            setColorFilter(themeCoordinator.primaryColor)
            contentDescription = getString(R.string.content_desc_streak)
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14))
        })
        streakChip.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.streak_days, streak)
            setTextColor(themeCoordinator.textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(5), 0, 0, 0)
        })
        heroTopRow.addView(streakChip)
        heroCard.addView(heroTopRow)

        val heroMainRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, 0) }
        heroMainRow.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.duration_h_m, todayH, todayM)
            setTextColor(themeCoordinator.primaryColor)
            textSize = 40f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        heroMainRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(12), 0) })
        val trendChip = TextView(this@MainActivity).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(9), dp(5), dp(9), dp(5))
        }
        val yesterdaySecs = snap.yesterdaySecs
        val trendPct = if (yesterdaySecs > 0L) {
            (((todayFocus - yesterdaySecs).toFloat() / yesterdaySecs.toFloat()) * 100f).toInt()
        } else if (todayFocus > 0L) {
            100
        } else {
            0
        }
        when {
            yesterdaySecs == 0L && todayFocus == 0L -> {
                trendChip.text = getString(R.string.trend_no_sessions)
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.5f
            }
            trendPct > 0 -> {
                trendChip.text = getString(R.string.trend_up, trendPct)
                trendChip.setTextColor(themeCoordinator.primaryColor)
                trendChip.background = GradientDrawable().apply { cornerRadius = 12f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            }
            trendPct < 0 -> {
                trendChip.text = getString(R.string.trend_down, -trendPct)
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.6f
            }
            else -> {
                trendChip.text = getString(R.string.trend_same)
                trendChip.setTextColor(themeCoordinator.textColor)
                trendChip.alpha = 0.6f
            }
        }
        heroMainRow.addView(trendChip)
        heroMainRow.addView(LinearLayout(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        heroCard.addView(heroMainRow)

        val heroGoalSecs = snap.heroGoalSecs
        val heroGoalPctRaw = todayFocus.toFloat() / heroGoalSecs.toFloat() * 100f
        val heroGoalPct = heroGoalPctRaw.coerceIn(0f, 100f)
        val goalReached = todayFocus >= heroGoalSecs
        val goalRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(12), 0, 0) }
        val goalTextCol = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        goalTextCol.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.daily_goal_label, formatGoalLabel(heroGoalSecs))
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        goalTextCol.addView(TextView(this@MainActivity).apply {
            text = if (goalReached) getString(R.string.goal_reached_yes) else {
                val remaining = heroGoalSecs - todayFocus
                getString(R.string.x_to_go, formatGoalLabel(remaining))
            }
            setTextColor(if (goalReached) 0xFF4CAF50.toInt() else themeCoordinator.textColor)
            alpha = if (goalReached) 1f else 0.5f
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        })
        goalRow.addView(goalTextCol)

        val goalRingColor = if (goalReached) 0xFF4CAF50.toInt() else themeCoordinator.primaryColor
        val goalRingWrap = FrameLayout(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
        }
        goalRingWrap.addView(SegmentRing(
            listOf(heroGoalPct / 100f to goalRingColor),
            themeCoordinator.bgColor,
            dp(6),
            Pair(goalRingColor, lightenColor(goalRingColor, 0.35f))
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        goalRingWrap.addView(TextView(this@MainActivity).apply {
            text = if (goalReached) "\u2713" else "${heroGoalPctRaw.toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(goalRingColor)
            textSize = if (goalReached) 20f else 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        goalRow.addView(goalRingWrap)
        heroCard.addView(goalRow)

        val breakRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        breakRow.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.break_h_m, todayBH, todayBM)
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(tintedColor(themeCoordinator.secondaryColor, 22)) }
            setPadding(dp(12), dp(7), dp(12), dp(7))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        breakRow.addView(TextView(this@MainActivity).apply {
            text = getString(R.string.avg7_label, avgH, avgM)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.55f
            textSize = 12f
            setPadding(dp(10), 0, 0, 0)
        })
        heroCard.addView(breakRow)

        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }
        val filterLabels = listOf(getString(R.string.filter_7d), getString(R.string.filter_30d), getString(R.string.filter_all))
        val filterValues = listOf(7, 30, -1)
        for (idx in filterLabels.indices) {
            val isActive = selectedDaysFilter == filterValues[idx]
            chipRow.addView(TextView(this).apply {
                text = filterLabels[idx]
                textSize = 13f
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setTextColor(if (isActive) themeCoordinator.primaryColor else themeCoordinator.textColor)
                alpha = if (isActive) 1f else 0.5f
                typeface = Typeface.create("sans-serif-medium", if (isActive) Typeface.BOLD else Typeface.NORMAL)
                background = if (isActive) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f) else null
                setOnClickListener {
                    selectedDaysFilter = filterValues[idx]
                    sharedPrefs.edit().putInt("selected_days_filter", selectedDaysFilter).apply()
                    navigateToPanel(AppPanel.STATS)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(8), 0) }
            })
        }
        chartCard.addView(chipRow)

        chartCard.addView(TextView(this).apply {
            text = getString(R.string.goal_mark, formatGoalLabel(dailyGoalSecs()))
            setTextColor(themeCoordinator.textColor)
            alpha = 0.45f
            textSize = 11f
            setPadding(dp(4), 0, dp(4), dp(6))
        })

        val chartContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        chartCard.addView(chartContainer)

        fun buildChart(daysLimit: Int) {
            chartContainer.removeAllViews()
            var maxDayFocusFound = 1L

            if (daysLimit == 7) {
                val displaySdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val cal = Calendar.getInstance()
                val mondayOffset = WeekHelper.mondayOffset(cal)
                val weekData = ArrayList<Pair<String, String>>()

                for (i in 0..6) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -mondayOffset + i)
                    val dStr = sdf.format(c.time)
                    weekData.add(Pair(dStr, "${weekdayNames[i]} ${displaySdf.format(c.time)}"))
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L

                for ((dStr, label) in weekData) {
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val dayGoalSecs = resolveGoalFor(dStr)
                    val dayGoalRatio = (dayGoalSecs.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(this).apply {
                        text = label
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = dayGoalRatio,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = isToday,
                        barHeight = dp(16)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(16), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(this).apply {
                        text = getString(R.string.duration_h_m, f / 3600, (f % 3600) / 60)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 13f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, label) }
                    chartContainer.addView(row)
                }
            } else if (daysLimit == 30) {
                val labelSdf = SimpleDateFormat("d MMM", Locale.getDefault())
                for (i in 0 until 30) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    if (f > maxDayFocusFound) maxDayFocusFound = f
                }
                val scale = if (maxDayFocusFound <= 0L) 3600L else Math.ceil(maxDayFocusFound.toDouble() / 3600.0).toLong() * 3600L

                for (i in 29 downTo 0) {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = sdf.format(c.time)
                    val f = snap.dayFocus[dStr] ?: 0L
                    val isToday = dStr == todayStr
                    val dayGoalSecs = resolveGoalFor(dStr)
                    val dayGoalRatio = (dayGoalSecs.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(2)) }
                    row.addView(TextView(this).apply {
                        text = labelSdf.format(c.time)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", if (isToday) Typeface.BOLD else Typeface.NORMAL)
                        layoutParams = LinearLayout.LayoutParams(dp(55), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) f.toFloat() / scale.toFloat() else 0f,
                        goalRatio = dayGoalRatio,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = isToday,
                        barHeight = dp(13)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(13), 1f).apply { setMargins(dp(4), 0, dp(6), 0) } })
                    row.addView(TextView(this).apply {
                        text = getString(R.string.duration_h_m, f / 3600, (f % 3600) / 60)
                        setTextColor(if (isToday) themeCoordinator.primaryColor else themeCoordinator.textColor)
                        textSize = 12f
                        typeface = Typeface.MONOSPACE
                        alpha = if (isToday) 1f else 0.85f
                    })
                    row.setOnClickListener { showDayDialog(dStr, labelSdf.format(c.time)) }
                    chartContainer.addView(row)
                }
            } else {
                var maxM = 1L
                for (mb in snap.monthBuckets) if (mb.focus > maxM) maxM = mb.focus
                val scale = if (maxM <= 0L) 3600L else Math.ceil(maxM.toDouble() / 3600.0).toLong() * 3600L

                for (mb in snap.monthBuckets) {
                    val mSecs = mb.focus
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3)) }
                    row.addView(TextView(this).apply { text = mb.label; setTextColor(themeCoordinator.textColor); textSize = 13f; layoutParams = LinearLayout.LayoutParams(dp(75), LinearLayout.LayoutParams.WRAP_CONTENT) })
                    row.addView(BarTrackView(
                        ratio = if (scale > 0L) mSecs.toFloat() / scale.toFloat() else 0f,
                        goalRatio = -1f,
                        trackColor = tintedColor(themeCoordinator.textColor, 26),
                        fillStart = themeCoordinator.primaryColor,
                        fillEnd = darkenColor(themeCoordinator.primaryColor, 0.15f),
                        isToday = false,
                        barHeight = dp(14)
                    ).apply { layoutParams = LinearLayout.LayoutParams(0, dp(14), 1f).apply { setMargins(dp(6), 0, dp(8), 0) } })
                    row.addView(TextView(this).apply { text = getString(R.string.duration_h_m, mSecs / 3600, (mSecs % 3600) / 60); setTextColor(themeCoordinator.textColor); textSize = 13f; typeface = Typeface.MONOSPACE; alpha = 0.85f })
                    row.setOnClickListener { showMonthDialog(mb.label, mSecs, mb.breakSecs) }
                    chartContainer.addView(row)
                }
            }
        }

        buildChart(selectedDaysFilter)

        // DUAL-MODE PIE CHART CARD (Placed strictly BELOW Focus Pattern Chart)
        val pieCard = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
        }

        val pieHeaderRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        pieHeaderRow.addView(TextView(this@MainActivity).apply {
            text = "📊 ANALYTICS DISTRIBUTION"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val tapDetailsBtn = TextView(this@MainActivity).apply {
            text = "Tap chart for details ↗"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 12f)
        }

        pieHeaderRow.addView(tapDetailsBtn)
        pieCard.addView(pieHeaderRow)

        val pieModeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }

        var currentPieMode = sharedPrefs.safeInt("pie_chart_mode", 0) // 0: Subject Sessions, 1: Focus Depth & Quality Ratio

        fun updatePieChartContent(mode: Int, container: LinearLayout) {
            container.removeAllViews()
            val pieView = SubjectPieChartView(this@MainActivity).apply {
                primaryColor = themeCoordinator.primaryColor
                textColor = themeCoordinator.textColor
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(360))
                setOnClickListener { showPieChartDetailsModal() }
            }

            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()

            if (mode == 0) {
                // Mode 1: Subject Sessions Breakdown derived from unified daily session logs
                val todayStr = dateKeyFmt.format(Date())
                val (allDaySessions, _) = dayBlocks(todayStr)
                val allSubjects = SubjectTagManager.getAllSubjects(this@MainActivity)

                // Group unified sessions by subject — EXCLUDING untagged null sessions
                val durationBySubject = HashMap<String, Long>()
                for (s in allDaySessions) {
                    if (s.subjectId != null) {
                        durationBySubject[s.subjectId] = (durationBySubject[s.subjectId] ?: 0L) + s.secs
                    }
                }

                // If no unified timeline entries exist yet for today, fallback to SubjectTagManager durations for today specifically
                if (durationBySubject.isEmpty()) {
                    val legacyDurations = SubjectTagManager.getSubjectDurationsForDate(this@MainActivity, todayStr)
                    for ((subId, secs) in legacyDurations) {
                        if (allSubjects.any { it.id == subId }) {
                            durationBySubject[subId] = secs
                        }
                    }
                }

                val totalSecsAll = durationBySubject.values.sum()

                if (durationBySubject.isEmpty() || totalSecsAll < 60L) {
                    val emptyBox = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    }
                    emptyBox.addView(TextView(this@MainActivity).apply {
                        text = "📊 Not Enough Data"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                    })
                    emptyBox.addView(TextView(this@MainActivity).apply {
                        text = "Log at least 1 minute of focus time to track your subject breakdown."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 12.5f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                    container.addView(emptyBox)
                } else {
                    val sortedSubjects = allSubjects
                        .map { it to (durationBySubject[it.id] ?: 0L) }
                        .filter { it.second >= 60L }
                        .sortedByDescending { it.second }

                    for ((subj, secs) in sortedSubjects) {
                        slicesList.add(SubjectPieChartView.PieSlice(subj.name, subj.iconEmoji, secs.toDouble(), subj.colorHex))
                    }

                    if (slicesList.isEmpty()) {
                        val emptyBox = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            setPadding(0, dp(24), 0, dp(24))
                        }
                        emptyBox.addView(TextView(this@MainActivity).apply {
                            text = "📊 Not Enough Data"
                            setTextColor(themeCoordinator.textColor)
                            textSize = 15f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            gravity = Gravity.CENTER
                        })
                        container.addView(emptyBox)
                    } else {
                        pieView.setData(slicesList)
                        container.addView(pieView)
                    }
                }
            } else {
                // Mode 2: Session Quality & Focus Depth Ratio (Deep Focus vs Standard Focus vs Light Focus)
                val totalFocusSecs = snap.todayFocus
                if (totalFocusSecs < 60L) {
                    val emptyBox = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(0, dp(24), 0, dp(24))
                    }
                    emptyBox.addView(TextView(this@MainActivity).apply {
                        text = "📊 Not Enough Data"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 15f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                    })
                    emptyBox.addView(TextView(this@MainActivity).apply {
                        text = "Log at least 1 minute of focus time to analyze your session quality and focus depth."
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.55f
                        textSize = 12.5f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                    container.addView(emptyBox)
                } else {
                    // Compute Focus Depth Distribution strictly from real session block durations
                    val todayStr = dateKeyFmt.format(Date())
                    val (daySessions, _) = statsEngine.dayBlocks(todayStr)
                    var deepSecs = 0L
                    var stdSecs = 0L
                    var lightSecs = 0L

                    for (b in daySessions) {
                        val durationSecs = b.secs
                        when {
                            durationSecs >= 2400L -> deepSecs += durationSecs // >= 40 mins (Deep Focus)
                            durationSecs >= 900L -> stdSecs += durationSecs   // 15 to 40 mins (Standard Focus)
                            else -> lightSecs += durationSecs                 // < 15 mins (Light Focus)
                        }
                    }

                    val isCurrentlyStudying = (currentTimerState == TimerState.STUDYING)
                    if (isCurrentlyStudying && accumulatedStudy > 0L) {
                        when {
                            accumulatedStudy >= 2400L -> deepSecs += accumulatedStudy
                            accumulatedStudy >= 900L -> stdSecs += accumulatedStudy
                            else -> lightSecs += accumulatedStudy
                        }
                    }

                    val sumCalc = deepSecs + stdSecs + lightSecs
                    if (sumCalc < totalFocusSecs && totalFocusSecs > 0L) {
                        val diff = totalFocusSecs - sumCalc
                        if (sumCalc > 0L) {
                            deepSecs += (diff * (deepSecs.toDouble() / sumCalc)).toLong()
                            stdSecs += (diff * (stdSecs.toDouble() / sumCalc)).toLong()
                            lightSecs = max(0L, totalFocusSecs - deepSecs - stdSecs)
                        } else {
                            if (totalFocusSecs >= 2400L) deepSecs = totalFocusSecs
                            else if (totalFocusSecs >= 900L) stdSecs = totalFocusSecs
                            else lightSecs = totalFocusSecs
                        }
                    }

                    if (deepSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Deep Focus (≥40m)", "🔥", deepSecs.toDouble(), "#10B981"))
                    }
                    if (stdSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Standard Focus (15-40m)", "⚡", stdSecs.toDouble(), "#3B82F6"))
                    }
                    if (lightSecs > 0L) {
                        slicesList.add(SubjectPieChartView.PieSlice("Light Focus (<15m)", "☕", lightSecs.toDouble(), "#F59E0B"))
                    }

                    pieView.setData(slicesList)
                    container.addView(pieView)
                }
            }
        }

        val pieContentContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }

        val m1Btn = TextView(this@MainActivity).apply {
            text = "🏷️ Subject Sessions"
            textSize = 12f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val m2Btn = TextView(this@MainActivity).apply {
            text = "🔥 Focus Depth & Quality"
            textSize = 12f
            setPadding(dp(12), dp(6), dp(12), dp(6))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        fun refreshModeButtons() {
            m1Btn.setTextColor(if (currentPieMode == 0) themeCoordinator.primaryColor else themeCoordinator.textColor)
            m1Btn.alpha = if (currentPieMode == 0) 1f else 0.5f
            m1Btn.background = if (currentPieMode == 0) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 100), 14f) else null

            m2Btn.setTextColor(if (currentPieMode == 1) themeCoordinator.primaryColor else themeCoordinator.textColor)
            m2Btn.alpha = if (currentPieMode == 1) 1f else 0.5f
            m2Btn.background = if (currentPieMode == 1) themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 100), 14f) else null
        }

        m1Btn.setOnClickListener {
            currentPieMode = 0
            sharedPrefs.edit().putInt("pie_chart_mode", 0).apply()
            refreshModeButtons()
            updatePieChartContent(0, pieContentContainer)
        }

        m2Btn.setOnClickListener {
            currentPieMode = 1
            sharedPrefs.edit().putInt("pie_chart_mode", 1).apply()
            refreshModeButtons()
            updatePieChartContent(1, pieContentContainer)
        }

        refreshModeButtons()
        pieModeRow.addView(m1Btn)
        pieModeRow.addView(m2Btn)
        pieCard.addView(pieModeRow)
        pieCard.addView(pieContentContainer)
        updatePieChartContent(currentPieMode, pieContentContainer)
        val totalLifeFocus = snap.totalLifeFocus
        val totalLifeBreak = snap.totalLifeBreak
        val totalLife = snap.totalLife

        val lifetimeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }

        val lifetimeTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lifetimeLeftCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val lifetimeFocusLabel = TextView(this).apply { text = getString(R.string.lifetime_focus); setTextColor(themeCoordinator.primaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeFocusValue = TextView(this).apply { setTextColor(themeCoordinator.primaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, dp(10)) }
        val lifetimeBreakLabel = TextView(this).apply { text = getString(R.string.lifetime_breaks); setTextColor(themeCoordinator.secondaryColor); alpha = 0.7f; textSize = 12f; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
        val lifetimeBreakValue = TextView(this).apply { setTextColor(themeCoordinator.secondaryColor); textSize = 26f; typeface = Typeface.MONOSPACE; setPadding(0, dp(2), 0, 0) }
        lifetimeFocusValue.text = getString(R.string.duration_h_m, totalLifeFocus / 3600, (totalLifeFocus % 3600) / 60)
        lifetimeBreakValue.text = getString(R.string.duration_h_m, totalLifeBreak / 3600, (totalLifeBreak % 3600) / 60)
        lifetimeLeftCol.addView(lifetimeFocusLabel)
        lifetimeLeftCol.addView(lifetimeFocusValue)
        lifetimeLeftCol.addView(lifetimeBreakLabel)
        lifetimeLeftCol.addView(lifetimeBreakValue)
        lifetimeTopRow.addView(lifetimeLeftCol)

        val focusFrac = if (totalLife > 0L) totalLifeFocus.toFloat() / totalLife.toFloat() else 0f
        val breakFrac = if (totalLife > 0L) totalLifeBreak.toFloat() / totalLife.toFloat() else 0f
        val lifetimeDonutWrap = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }
        lifetimeDonutWrap.addView(SegmentRing(
            listOf(focusFrac to themeCoordinator.primaryColor, breakFrac to themeCoordinator.secondaryColor),
            themeCoordinator.bgColor,
            dp(10),
            null
        ), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        lifetimeDonutWrap.addView(TextView(this).apply {
            text = "${(focusFrac * 100f).toInt()}%"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })
        lifetimeTopRow.addView(lifetimeDonutWrap)
        lifetimeCard.addView(lifetimeTopRow)

        val longestStreak = snap.longestStreak
        val activeDays = snap.activeDays
        val bestWeekdaySecs = snap.bestWeekdaySecs
        val bestWeekdayName = snap.bestWeekdayName
        val bestWeekSecs = snap.bestWeekSecs
        val bestWeekLabel = snap.bestWeekLabel

        val thisWeek = snap.thisWeek
        val prevWeek = snap.prevWeek

        val goalHits = snap.goalHits

        val insightsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var insightCount = 0

        if (thisWeek > 0L || prevWeek > 0L) {
            val diff = if (prevWeek > 0L) ((thisWeek - prevWeek).toFloat() / prevWeek.toFloat() * 100f).toInt() else 100
            val statement = when {
                diff > 0 -> "$diff% more focus than last week"
                diff < 0 -> "${-diff}% less focus than last week"
                else -> "same pace as last week"
            }
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_trending, themeCoordinator.primaryColor,
                "TREND", statement, "This week \u00B7 ${thisWeek / 3600}h ${(thisWeek % 3600) / 60}m"
            ))
            insightCount++
        }

        if (longestStreak > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_flame, themeCoordinator.primaryColor,
                "LONGEST STREAK",
                "$longestStreak-day streak",
                "Consecutive daily goal+ focus days"
            ))
            insightCount++
        }

        if (activeDays > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_book, themeCoordinator.secondaryColor,
                "TOTAL SESSIONS",
                "$activeDays active day${if (activeDays == 1) "" else "s"}",
                "Days with logged focus time"
            ))
            insightCount++
        }

        if (bestWeekdaySecs > 0L) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_trending, themeCoordinator.primaryColor,
                "BEST WEEKDAY",
                bestWeekdayName,
                getString(R.string.best_weekday_subtitle, bestWeekdayName)
            ))
            insightCount++
        }

        if (bestWeekSecs > 0L) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_medal, themeCoordinator.secondaryColor,
                "BEST WEEK",
                "Week of $bestWeekLabel",
                "${bestWeekSecs / 3600}h ${(bestWeekSecs % 3600) / 60}m of focus"
            ))
            insightCount++
        }

        if (goalHits > 0) {
            insightsContainer.addView(createInsightCard(
                R.drawable.ic_target, themeCoordinator.secondaryColor,
                "CONSISTENCY",
                "Hit daily goal on $goalHits of the last 14 days",
                ""
            ))
            insightCount++
        }

        if (tab == AppStatsTab.OVERVIEW) {
            val hasAnySessions = snap.hasAnySessions
            if (!hasAnySessions) {
                content.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(28), dp(20), dp(28))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
                    addView(TextView(this@MainActivity).apply {
                        text = "\uD83C\uDFA7\uFE0F"
                        textSize = 36f
                        gravity = Gravity.CENTER
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = getString(R.string.no_sessions_yet)
                        setTextColor(themeCoordinator.textColor)
                        textSize = 16f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(10), 0, 0)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = getString(R.string.insights_empty_hint)
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setPadding(0, dp(4), 0, 0)
                    })
                })
            } else {
            (heroCard.parent as? android.view.ViewGroup)?.removeView(heroCard)
            (chartCard.parent as? android.view.ViewGroup)?.removeView(chartCard)
            content.addView(heroCard)
            content.addView(chartCard)
            buildChart(selectedDaysFilter)

            val showHeatmap = snap.showHeatmap

            if (showHeatmap) {
                val heatmapData = snap.heatmapData

                val heatmapCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val heatmapHeaderRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val heatmapHeaderCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                heatmapHeaderCol.addView(TextView(this).apply {
                    text = getString(R.string.focus_heatmap)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                heatmapHeaderCol.addView(TextView(this).apply {
                    text = getString(R.string.heatmap_hint)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.45f
                    textSize = 11f
                    setPadding(0, dp(2), 0, 0)
                })
                heatmapHeaderRow.addView(heatmapHeaderCol)
                heatmapHeaderRow.addView(TextView(this).apply {
                    text = getString(R.string.btn_fullscreen)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18).toFloat()
                        setStroke(dp(1).toInt(), tintedColor(themeCoordinator.primaryColor, 180))
                        setColor(tintedColor(themeCoordinator.primaryColor, 25))
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener { navigateToPanel(AppPanel.HEATMAP) }
                })
                heatmapCard.addView(heatmapHeaderRow)
                val heatmapScroll = HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isFillViewport = true
                }
                heatmapScroll.addView(HeatmapView(this).apply {
                    setData(heatmapData, themeCoordinator.primaryColor, themeCoordinator.textColor, { resolveGoalFor(it) })
                    onDayTap = { dateStr ->
                        val d = try { sdf.parse(dateStr) } catch (_: Exception) { null }
                        val lbl = if (d != null) SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(d) else dateStr
                        this@MainActivity.showDayDialog(dateStr, lbl)
                    }
                })
                heatmapScroll.post { heatmapScroll.fullScroll(View.FOCUS_RIGHT) }
                heatmapCard.addView(heatmapScroll)

                val legendRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, 0)
                }
                legendRow.addView(TextView(this).apply { text = getString(R.string.legend_less); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 10f })
                fun addLegendLevel(alpha: Int) {
                    legendRow.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { setMargins(dp(4), 0, dp(4), 0) }
                        background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.argb(alpha, Color.red(themeCoordinator.primaryColor), Color.green(themeCoordinator.primaryColor), Color.blue(themeCoordinator.primaryColor))) }
                    })
                }
                addLegendLevel(30); addLegendLevel(60); addLegendLevel(110); addLegendLevel(170)
                legendRow.addView(TextView(this).apply { text = getString(R.string.legend_more); setTextColor(themeCoordinator.textColor); alpha = 0.5f; textSize = 10f })
                heatmapCard.addView(legendRow)

                content.addView(heatmapCard)
            }

            var using7 = true

            val patternCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
            }
            val patternHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            patternHeader.addView(TextView(this).apply {
                text = getString(R.string.focus_pattern)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 11f
                letterSpacing = 0.18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val segWrap = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(tintedColor(themeCoordinator.textColor, 16)) }
                setPadding(dp(3), dp(2), dp(3), dp(2))
            }
            val seg7 = TextView(this).apply {
                text = getString(R.string.week_7d)
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            val seg30 = TextView(this).apply {
                text = getString(R.string.week_30d)
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(10), dp(3), dp(10), dp(3))
            }
            fun styleSeg(btn: TextView, selected: Boolean) {
                btn.setTextColor(if (selected) themeCoordinator.bgColor else themeCoordinator.textColor)
                btn.alpha = if (selected) 1f else 0.6f
                btn.background = if (selected) GradientDrawable().apply { cornerRadius = dp(11).toFloat(); setColor(themeCoordinator.primaryColor) } else null
            }
            segWrap.addView(seg7); segWrap.addView(seg30)
            patternHeader.addView(segWrap)
            patternCard.addView(patternHeader)

            val blockLabels = focusBlockLabels()
            val startLabels = focusBlockStartLabels()
            val cellsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val cellsScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            cellsScroll.addView(cellsRow)
            patternCard.addView(cellsScroll)

            val footerText = TextView(this).apply {
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            }
            patternCard.addView(footerText)

            fun showBlockDialog(b: Int, secs: Long, total: Long, winLabel: String) {
                val pct = if (total > 0L) (secs * 100.0 / total) else 0.0
                val dialog = android.app.Dialog(this)
                dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                val dialogContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createDialogBackground(28f)
                    setPadding(dp(20), dp(20), dp(20), dp(18))
                }
                dialogContent.addView(TextView(this).apply {
                    text = focusBlockRangeLabel(b)
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 11f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                dialogContent.addView(TextView(this).apply {
                    text = getString(R.string.block_focus_total, formatDuration(secs))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(8), 0, 0)
                })
                dialogContent.addView(TextView(this).apply {
                    text = getString(R.string.block_fraction, String.format(java.util.Locale.getDefault(), "%.0f%%", pct), winLabel)
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 12f
                    setPadding(0, dp(4), 0, 0)
                })
                dialogContent.addView(TextView(this).apply {
                    text = getString(R.string.btn_close)
                    gravity = Gravity.CENTER
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 12f
                    letterSpacing = 0.18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
                    setOnClickListener { dialog.dismiss() }
                })
                dialog.setContentView(dialogContent)
                dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.show()
            }

            fun rebuildCells() {
                val blockSecs = if (using7) snap.blockSecs7 else snap.blockSecs30
                val maxBlock = if (using7) snap.maxBlock7 else snap.maxBlock30
                val total = if (using7) snap.patternTotal7 else snap.patternTotal30
                val winLabel = if (using7) "7-day" else "30-day"
                cellsRow.removeAllViews()
                for (b in 0 until 12) {
                    val v = blockSecs[b]
                    val intensity = if (maxBlock > 0L) v.toFloat() / maxBlock.toFloat() else 0f
                    val col = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(0, dp(8), 0, 0)
                    }
                    val tile = FrameLayout(this).apply {
                        background = if (v > 0L) GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setColor(tintedColor(themeCoordinator.primaryColor, 40 + (185 * intensity).toInt()))
                        } else GradientDrawable().apply {
                            cornerRadius = dp(8).toFloat()
                            setColor(tintedColor(themeCoordinator.textColor, 16))
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                        setOnClickListener {
                            showBlockDialog(b, v, total, winLabel)
                        }
                    }
                    col.addView(tile)
                    col.addView(TextView(this).apply {
                        text = startLabels[b]
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.45f
                        textSize = 8f
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                        setPadding(0, dp(4), 0, 0)
                    })
                    col.addView(TextView(this).apply {
                        text = if (v > 0L && total > 0L) "${(v * 100.0 / total).toInt()}%" else ""
                        setTextColor(themeCoordinator.primaryColor)
                        alpha = if (v > 0L) 0.85f else 0.3f
                        textSize = 9f
                        typeface = Typeface.MONOSPACE
                        gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                    col.setOnClickListener {
                        showBlockDialog(b, v, total, winLabel)
                    }
                    cellsRow.addView(col)
                }
                if (total > 0L) {
                    var maxBlockVal = 0L
                    for (b in 0 until 12) if (blockSecs[b] > maxBlockVal) maxBlockVal = blockSecs[b]

                    val peakBlocks = mutableListOf<Int>()
                    if (maxBlockVal > 0L) {
                        for (b in 0 until 12) {
                            if (blockSecs[b] >= (maxBlockVal * 0.70f).toLong()) {
                                peakBlocks.add(b)
                            }
                        }
                    }

                    val ranges = mutableListOf<String>()
                    var idx = 0
                    while (idx < peakBlocks.size) {
                        val start = peakBlocks[idx]
                        var end = start
                        while (idx + 1 < peakBlocks.size && peakBlocks[idx + 1] == end + 1) {
                            end = peakBlocks[idx + 1]
                            idx++
                        }
                        val startLabel = blockLabels[start].split("-", "\u2013").first().trim()
                        val endLabel = blockLabels[end].split("-", "\u2013").last().trim()
                        ranges.add("$startLabel \u2013 $endLabel")
                        idx++
                    }

                    val rangeStr = if (ranges.isNotEmpty()) ranges.joinToString(" & ") else blockLabels[0].replace("-", " \u2013 ")
                    footerText.text = getString(R.string.most_focused, rangeStr)
                    footerText.setAlpha(0.7f)
                    cellsScroll.post {
                        val colW = dp(42)
                        val firstPeak = peakBlocks.firstOrNull() ?: 0
                        val target = (firstPeak * colW - (cellsScroll.width - colW) / 2).coerceAtLeast(0)
                        cellsScroll.smoothScrollTo(target, 0)
                    }
                } else {
                    footerText.text = getString(R.string.no_focus_recent, if (using7) "7" else "30")
                    footerText.setAlpha(0.5f)
                }
            }

            styleSeg(seg7, true); styleSeg(seg30, false)
            seg7.setOnClickListener {
                using7 = true
                styleSeg(seg7, true); styleSeg(seg30, false)
                rebuildCells()
            }
            seg30.setOnClickListener {
                using7 = false
                styleSeg(seg7, false); styleSeg(seg30, true)
                rebuildCells()
            }
            rebuildCells()
            (patternCard.parent as? android.view.ViewGroup)?.removeView(patternCard)
            content.addView(patternCard)

            val shouldShowPieCard = true

            if (shouldShowPieCard) {
                (pieCard.parent as? android.view.ViewGroup)?.removeView(pieCard)
                content.addView(pieCard)
            }
            }

            content.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)) })
            if (insightCount > 0) {
                content.addView(createSectionLabel("Highlights"))
                content.addView(insightsContainer)
            }
            content.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)) })
            content.addView(lifetimeCard)

            val exportCardBtn = TextView(this).apply {
                text = getString(R.string.export_summary_card)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 30f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(12)) }
                setOnClickListener {
                    showSummaryCardPreview()
                }
            }
            content.addView(exportCardBtn)
        } else if (tab == AppStatsTab.PLANNER) {
            renderPlannerTabContent(content, snap)
        } else {


            CalendarTimeline(this).build(content, snap, todayStr)
        }


        layout.addView(scroll)

        statsRoot.addView(layout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val backFab = TextView(this).apply {
            text = "\u2190"
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.bgColor)
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(themeCoordinator.primaryColor) }
            elevation = dp(8).toFloat()
            setOnClickListener { navigateToPanel(AppPanel.FOCUS) }
            layoutParams = FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(20), dp(20)) }
        }
        statsRoot.addView(backFab)
    }

    internal fun refreshStatsPanel() {
        statsDirty = true
        tabPageCache.remove(statsTabKey(AppStatsTab.PLANNER))
        if (currentPanel == AppPanel.STATS) {
            navigateToPanel(AppPanel.STATS)
        }
    }

    internal fun checkAndResetGoalsForNewDay() {
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = dateKeyFmt.format(Date())
        val lastResetDate = sharedPrefs.getString("last_planner_reset_date", "") ?: ""

        if (lastResetDate != todayStr) {
            val goals = loadSessionGoalsFromJson(sharedPrefs.getString("session_goals_json", "[]") ?: "[]")
            if (goals.isNotEmpty()) {
                if (lastResetDate.isNotEmpty()) {
                    PlannerHistoryManager.snapshotForDate(this, lastResetDate, goals)
                } else {
                    PlannerHistoryManager.snapshotToday(this, goals)
                }
                val resetGoals = goals.map { it.copy(completed = false, checkedAt = 0L) }
                saveSessionGoalsToJson(resetGoals)
            }
            sharedPrefs.edit().putString("last_planner_reset_date", todayStr).apply()
            refreshStatsPanel()
        }
    }


    internal fun loadSessionGoalsFromJson(jsonStr: String): List<SessionGoal> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<SessionGoal>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(SessionGoal(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    note = obj.optString("note", ""),
                    targetMinutes = obj.optInt("targetMinutes", 0),
                    completed = obj.optBoolean("completed", false),
                    checkedAt = obj.optLong("checkedAt", 0L),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveSessionGoalsToJson(goals: List<SessionGoal>) {
        val array = org.json.JSONArray()
        for (goal in goals) {
            val obj = org.json.JSONObject().apply {
                put("id", goal.id)
                put("title", goal.title)
                put("note", goal.note)
                put("targetMinutes", goal.targetMinutes)
                put("completed", goal.completed)
                put("checkedAt", goal.checkedAt)
                put("createdAt", goal.createdAt)
            }
            array.put(obj)
        }
        getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putString("session_goals_json", array.toString()).apply()
    }

    internal fun resolvePlannerColors(): Pair<Int, Int> {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        return when (prefs.getString("planner_theme_preset", "DEFAULT")) {
            "EMERALD" -> Pair(0xFF10B981.toInt(), 0xFFF59E0B.toInt()) // Emerald Green & Amber Gold
            "VIOLET" -> Pair(0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt())  // Violet Purple & Cyan Blue
            "OCEAN" -> Pair(0xFF06B6D4.toInt(), 0xFFFF6B6B.toInt())   // Ocean Cyan & Coral Red
            "SUNSET" -> Pair(0xFFF97316.toInt(), 0xFF10B981.toInt())  // Sunset Orange & Neon Mint
            "MIDNIGHT" -> Pair(0xFFEAB308.toInt(), 0xFFEC4899.toInt())// Obsidian Gold & Electric Pink
            else -> Pair(themeCoordinator.accentColor, themeCoordinator.secondaryColor)
        }
    }

    internal fun resolvePlannerAccentColor(): Int = resolvePlannerColors().first

    internal fun showPlannerThemePickerDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (currentP, _) = resolvePlannerColors()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "🎨 PLANNER DUAL THEME SYSTEM"
            setTextColor(currentP)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Choose a dual-color theme (Buttons/Text + Values/Graphs):"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(4), 0, dp(14))
        })

        val themes = listOf(
            Triple("🌿 Emerald & Amber Gold", "EMERALD", Pair(0xFF10B981.toInt(), 0xFFF59E0B.toInt())),
            Triple("🍇 Violet & Cyan", "VIOLET", Pair(0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt())),
            Triple("🌊 Ocean & Coral Red", "OCEAN", Pair(0xFF06B6D4.toInt(), 0xFFFF6B6B.toInt())),
            Triple("🌅 Sunset & Neon Mint", "SUNSET", Pair(0xFFF97316.toInt(), 0xFF10B981.toInt())),
            Triple("👑 Midnight & Pink", "MIDNIGHT", Pair(0xFFEAB308.toInt(), 0xFFEC4899.toInt())),
            Triple("🎨 Global Theme Pair", "DEFAULT", Pair(themeCoordinator.accentColor, themeCoordinator.secondaryColor))
        )

        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val currentPreset = prefs.getString("planner_theme_preset", "DEFAULT") ?: "DEFAULT"

        for ((label, key, colorPair) in themes) {
            val (c1, c2) = colorPair
            val isSel = currentPreset == key
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isSel) themeCoordinator.createGlassChip(tintedColor(c1, 120), 16f) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 16f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                setOnClickListener {
                    prefs.edit().putString("planner_theme_preset", key).apply()
                    dialog.dismiss()
                    navigateToPanel(AppPanel.STATS)
                }
            }

            val dot1 = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c1)
                }
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(4), 0) }
            }
            val dot2 = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c2)
                }
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(10), 0) }
            }
            row.addView(dot1)
            row.addView(dot2)

            row.addView(TextView(this).apply {
                text = label
                setTextColor(if (isSel) c1 else themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", if (isSel) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            if (isSel) {
                row.addView(TextView(this).apply {
                    text = "✓"
                    setTextColor(c1)
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
            }
            content.addView(row)
        }

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return if (result > 0) result else dp(24)
    }

    internal fun renderPlannerTabContent(parent: LinearLayout, snap: StatsSnapshot) {
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val goalsJson = prefs.getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val todayFocusMins = (snap.todayFocus / 60).toInt()
        val sortedGoals = goalsList.sortedBy { it.targetMinutes }
        val checkedGoals = sortedGoals.filter { it.completed }.sortedBy { it.checkedAt }
        var pool = todayFocusMins
        val trulyAchievedIds = mutableSetOf<String>()

        for (goal in checkedGoals) {
            val targetMins = goal.targetMinutes
            if (targetMins > 0) {
                if (pool >= targetMins) {
                    pool -= targetMins
                    trulyAchievedIds.add(goal.id)
                }
            } else {
                trulyAchievedIds.add(goal.id)
            }
        }

        val completedCount = trulyAchievedIds.size
        val totalCount = goalsList.size
        val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
        }

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(this).apply {
            text = "DAILY PLANNER & GOALS"
            setTextColor(plannerPrimary)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.15f
        })
        topRow.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })

        val addBtn = TextView(this).apply {
            text = "+ Add Goal"
            setTextColor(plannerPrimary)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 12f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { showAddSessionGoalDialog() }
        }
        topRow.addView(addBtn)
        summaryCard.addView(topRow)

        val isGoalReached = progressPct >= 100 && totalCount > 0
        val lineProgressColor = if (isGoalReached) 0xFF22C55E.toInt() else 0xFFEF4444.toInt()

        val progressRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(4)) }
        progressRow.addView(TextView(this).apply {
            text = "$completedCount of $totalCount completed"
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        progressRow.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        progressRow.addView(TextView(this).apply {
            text = "$progressPct%"
            setTextColor(lineProgressColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        summaryCard.addView(progressRow)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = progressPct
            progressTintList = android.content.res.ColorStateList.valueOf(lineProgressColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(tintedColor(themeCoordinator.textColor, 30))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { setMargins(0, dp(4), 0, 0) }
        }
        summaryCard.addView(progressBar)
        parent.addView(summaryCard)

        if (goalsList.isEmpty()) {
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = themeCoordinator.createCardBackground()
                setPadding(dp(20), dp(32), dp(20), dp(32))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(8)) }
            }
            emptyCard.addView(TextView(this).apply { text = "\uD83D\uDCCB"; textSize = 36f; gravity = Gravity.CENTER })
            emptyCard.addView(TextView(this).apply {
                text = "No Planner Goals Yet"
                setTextColor(themeCoordinator.textColor)
                textSize = 16f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(4))
            })

            emptyCard.addView(TextView(this).apply {
                text = "Tap '+ Add Goal' above to create custom planner items like 'Physics Lecture' or 'Math Problems'."
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 12f
                gravity = Gravity.CENTER
            })
            parent.addView(emptyCard)
        } else {
            val goalsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val uncheckedGoals = sortedGoals.filter { !it.completed }

            class GoalDisplayInfo(
                val isChecked: Boolean,
                val progressText: String,
                val chipColor: Int,
                val isDeficit: Boolean
            )

            val infoMap = mutableMapOf<String, GoalDisplayInfo>()
            var focusPool = todayFocusMins

            val greenColor = 0xFF22C55E.toInt()
            val redColor = 0xFFEF4444.toInt()

            for (goal in checkedGoals) {
                val targetMins = goal.targetMinutes
                if (targetMins > 0) {
                    if (focusPool >= targetMins) {
                        focusPool -= targetMins
                        infoMap[goal.id] = GoalDisplayInfo(
                            isChecked = true,
                            progressText = "\u2713 ${targetMins}m Goal Reached",
                            chipColor = greenColor,
                            isDeficit = false
                        )
                    } else {
                        val deficit = targetMins - focusPool
                        focusPool = 0
                        infoMap[goal.id] = GoalDisplayInfo(
                            isChecked = true,
                            progressText = "${deficit}m remaining",
                            chipColor = redColor,
                            isDeficit = true
                        )
                    }
                } else {
                    infoMap[goal.id] = GoalDisplayInfo(
                        isChecked = true,
                        progressText = "\u2713 Goal Reached",
                        chipColor = greenColor,
                        isDeficit = false
                    )
                }
            }

            for (goal in uncheckedGoals) {
                val targetMins = goal.targetMinutes
                if (targetMins > 0) {
                    val alloc = minOf(focusPool, targetMins)
                    focusPool = (focusPool - alloc).coerceAtLeast(0)
                    infoMap[goal.id] = GoalDisplayInfo(
                        isChecked = false,
                        progressText = "${alloc}/${targetMins}m",
                        chipColor = themeCoordinator.primaryColor,
                        isDeficit = false
                    )
                } else {
                    infoMap[goal.id] = GoalDisplayInfo(
                        isChecked = false,
                        progressText = "0/0m",
                        chipColor = themeCoordinator.primaryColor,
                        isDeficit = false
                    )
                }
            }

            for (goal in goalsList) {
                val info = infoMap[goal.id] ?: continue
                val isChecked = info.isChecked
                val targetMins = goal.targetMinutes

                val goalCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }

                val checkBtn = TextView(this).apply {
                    text = if (isChecked) (if (info.isDeficit) "\u2715" else "\u2713") else ""
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isChecked) (if (info.isDeficit) redColor else greenColor) else Color.TRANSPARENT)
                        setStroke(dp(2), if (isChecked) (if (info.isDeficit) redColor else greenColor) else themeCoordinator.textColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { setMargins(0, 0, dp(12), 0) }
                    setOnClickListener {
                        val now = System.currentTimeMillis()
                        val updated = goalsList.map {
                            if (it.id == goal.id) it.copy(completed = !it.completed, checkedAt = if (!it.completed) now else 0L) else it
                        }
                        saveSessionGoalsToJson(updated)
                        PlannerHistoryManager.snapshotToday(this@MainActivity, updated)
                        refreshStatsPanel()
                    }
                }
                goalCard.addView(checkBtn)

                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { showGoalHistoryDialog(goal) }
                }
                textCol.addView(TextView(this).apply {
                    text = goal.title
                    setTextColor(if (isChecked) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    if (isChecked) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                })
                if (goal.note.isNotBlank()) {
                    textCol.addView(TextView(this).apply {
                        text = goal.note
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.5f
                        textSize = 12f
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                goalCard.addView(textCol)

                if (targetMins > 0) {
                    val chipView = TextView(this).apply {
                        text = info.progressText
                        setTextColor(info.chipColor)
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = themeCoordinator.createGlassChip(tintedColor(info.chipColor, 100), 10f)
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6), 0, dp(6), 0) }
                        setOnClickListener { showGoalHistoryDialog(goal) }
                    }
                    goalCard.addView(chipView)
                }

                val deleteBtn = TextView(this).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 16f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        showConfirmDialog(
                            title = "Delete Goal?",
                            message = "Are you sure you want to remove '${goal.title}' from your planner?"
                        ) {
                            val updated = goalsList.filter { it.id != goal.id }
                            saveSessionGoalsToJson(updated)
                            refreshStatsPanel()
                        }
                    }
                }
                goalCard.addView(deleteBtn)
                goalsContainer.addView(goalCard)
            }
            parent.addView(goalsContainer)
        }

        // Planner Insights Section (always rendered whether goals exist or not)
        val overallInsights = PlannerHistoryManager.computeOverallPlannerInsights(this, goalsList)
        parent.addView(createSectionLabel("Planner Insights"))

        val insightsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(4), 0, dp(12)) }
        }

        fun addInsightRow(icon: String, title: String, value: String, sub: String) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 20f
                setPadding(0, 0, dp(12), 0)
            })
            val textCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            textCol.addView(TextView(this@MainActivity).apply {
                text = sub
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                textSize = 12f
                setPadding(0, dp(2), 0, 0)
            })
            row.addView(textCol)
            row.addView(TextView(this@MainActivity).apply {
                text = value
                setTextColor(plannerSecondary)
                textSize = 15f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            insightsCard.addView(row)
        }

        addInsightRow("🔥", "Best Streak", if (overallInsights.bestStreakDays > 0) "${overallInsights.bestStreakDays}d" else "0d", if (overallInsights.bestStreakDays > 0) overallInsights.bestStreakGoalTitle else "No streak yet")
        insightsCard.addView(createDivider())
        addInsightRow("📈", "Most Consistent", if (overallInsights.mostConsistentPct > 0) "${overallInsights.mostConsistentPct}%" else "0%", if (overallInsights.mostConsistentPct > 0) overallInsights.mostConsistentGoalTitle else "No track yet")
        insightsCard.addView(createDivider())

        val matrixBtn = TextView(this).apply {
            text = "📊 Goal & Habit Grid"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 14f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            setOnClickListener { showPlannerMatrixDialog() }
        }
        insightsCard.addView(matrixBtn)

        val themeBtn = TextView(this).apply {
            text = "🎨 Planner Theme"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 120), 14f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(8), 0, 0) }
            setOnClickListener { showPlannerThemePickerDialog() }
        }
        insightsCard.addView(themeBtn)

        parent.addView(insightsCard)
    }

    internal fun showGoalHistoryDialog(goal: PlannerGoal, displayMonthOffset: Int = 0) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()
        val historyDetailedMap = PlannerHistoryManager.loadGoalHistoryDetailed(this, goal.id)
        val insights = PlannerHistoryManager.computeGoalInsights(this, goal.id)
        var currentOffset = displayMonthOffset

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // 1. Scrollable Header Box for Goal Title and Optional Note ONLY
        val maxHeaderHeight = dp(110)
        val headerScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(4))
            }
            isVerticalScrollBarEnabled = true
        }

        val headerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        headerBox.addView(TextView(this).apply {
            text = "📊 ${goal.title}"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        if (goal.note.isNotBlank()) {
            headerBox.addView(TextView(this).apply {
                text = goal.note
                setTextColor(themeCoordinator.textColor)
                alpha = 0.8f
                textSize = 13f
                setLineSpacing(dp(2).toFloat(), 1.1f)
                setPadding(0, dp(4), 0, 0)
            })
        }

        headerScroll.addView(headerBox)

        // Restrict headerScroll height to maxHeaderHeight if content exceeds it
        headerScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                headerScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (headerScroll.height > maxHeaderHeight) {
                    headerScroll.layoutParams = (headerScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxHeaderHeight
                    }
                }
            }
        })
        content.addView(headerScroll)

        // 2. FIXED Stats Summary Chips Row
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(10))
        }
        fun makeChip(icon: String, textVal: String) {
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(tintedColor(plannerPrimary, 100), 14f)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
            }
            chip.addView(TextView(this@MainActivity).apply { text = icon; textSize = 13f; setPadding(0, 0, dp(4), 0) })
            chip.addView(TextView(this@MainActivity).apply {
                text = textVal
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            statsRow.addView(chip)
        }
        makeChip("🔥", "${insights.currentStreak}d streak")
        makeChip("🎯", "${(insights.completionRate * 100).toInt()}% rate")
        makeChip("📅", "${insights.completedDays}/${insights.totalDaysTracked} days")
        content.addView(statsRow)

        content.addView(createDivider())

        // 3. FIXED Calendar Grid View Container
        val calendarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateKeys = historyDetailedMap.keys.mapNotNull { runCatching { sdf.parse(it) }.getOrNull() }
        val minCal = Calendar.getInstance()
        if (dateKeys.isNotEmpty()) {
            minCal.time = dateKeys.minOrNull()!!
        } else {
            minCal.timeInMillis = goal.createdAt
        }
        val todayCal = Calendar.getInstance()
        val minOffset = (minCal.get(Calendar.YEAR) - todayCal.get(Calendar.YEAR)) * 12 + (minCal.get(Calendar.MONTH) - todayCal.get(Calendar.MONTH))
        val maxOffset = 0

        fun updateCalendarView(offset: Int) {
            calendarContainer.removeAllViews()
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, offset)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthLabelSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            // Calendar header with navigation
            val calNavRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(6))
            }
            calNavRow.addView(TextView(this@MainActivity).apply {
                text = "‹"
                textSize = 24f
                setTextColor(plannerPrimary)
                alpha = if (currentOffset <= minOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset > minOffset) {
                        currentOffset--
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calNavRow.addView(TextView(this@MainActivity).apply {
                text = monthLabelSdf.format(cal.time)
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            calNavRow.addView(TextView(this@MainActivity).apply {
                text = "›"
                textSize = 24f
                setTextColor(plannerPrimary)
                alpha = if (currentOffset >= maxOffset) 0.3f else 1f
                setPadding(dp(14), dp(4), dp(14), dp(4))
                setOnClickListener {
                    if (currentOffset < maxOffset) {
                        currentOffset++
                        updateCalendarView(currentOffset)
                    }
                }
            })
            calendarContainer.addView(calNavRow)

            // Day of week labels
            val weekdaysRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(6)) }
            val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
            for (day in daysOfWeek) {
                weekdaysRow.addView(TextView(this@MainActivity).apply {
                    text = day
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 12f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            calendarContainer.addView(weekdaysRow)

            // Days Grid
            val gridCal = cal.clone() as Calendar
            gridCal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) - 1
            val maxDaysInMonth = gridCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val todayStr = sdf.format(Date())

            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > maxDaysInMonth) break
                val weekRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(3), 0, dp(3)) }
                for (col in 0..6) {
                    if ((week == 0 && col < firstDayOfWeek) || dayCounter > maxDaysInMonth) {
                        weekRow.addView(View(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f) })
                    } else {
                        val currCal = cal.clone() as Calendar
                        currCal.set(Calendar.DAY_OF_MONTH, dayCounter)
                        val dateKey = sdf.format(currCal.time)
                        val status = historyDetailedMap[dateKey]
                        val isToday = dateKey == todayStr

                        val dayCell = FrameLayout(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
                            val circle = View(this@MainActivity).apply {
                                layoutParams = FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER)
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.OVAL
                                    when (status) {
                                        GoalHistoryStatus.ACHIEVED -> setColor(0xFF22C55E.toInt())
                                        GoalHistoryStatus.DEFICIT -> setColor(0xFFEF4444.toInt())
                                        GoalHistoryStatus.NOT_COMPLETED -> setColor(0x33EF4444.toInt())
                                        null -> {
                                            if (isToday) {
                                                setColor(tintedColor(plannerSecondary, 40))
                                                setStroke(dp(2), plannerSecondary)
                                            } else {
                                                setColor(Color.TRANSPARENT)
                                            }
                                        }
                                    }
                                }
                            }
                            addView(circle)
                            addView(TextView(this@MainActivity).apply {
                                text = if (status == GoalHistoryStatus.DEFICIT) "\u2715" else dayCounter.toString()
                                textSize = 12f
                                gravity = Gravity.CENTER
                                setTextColor(if (status == GoalHistoryStatus.ACHIEVED || status == GoalHistoryStatus.DEFICIT) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            })
                        }
                        weekRow.addView(dayCell)
                        dayCounter++
                    }
                }
                calendarContainer.addView(weekRow)
            }
        }

        updateCalendarView(currentOffset)
        content.addView(calendarContainer)

        // 4. FIXED Legend Row
        val legendRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(8))
        }
        legendRow.addView(TextView(this).apply {
            text = "● Achieved   ✕ Deficit   ○ Missed"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 11f
        })
        content.addView(legendRow)

        // 5. FIXED Action Buttons Row at Bottom (Edit Goal + Close)
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }

        val editBtn = TextView(this).apply {
            text = "✏️ Edit Goal"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(plannerPrimary, 20f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener {
                dialog.dismiss()
                showEditSessionGoalDialog(goal)
            }
        }
        buttonRow.addView(editBtn)

        val closeBtn = TextView(this).apply {
            text = "CLOSE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonRow.addView(closeBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun migrateHistoricalDailyGoals(context: Context) {
        val prefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val globalGoal = (prefs.all["daily_goal_secs"] as? Number)?.toLong() ?: 2700L
        val editor = prefs.edit()
        var modified = false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (key in prefs.all.keys) {
            if (key.endsWith("_focus_total")) {
                val dStr = key.removeSuffix("_focus_total")
                val goalKey = "${dStr}_goal_secs"
                if (!prefs.contains(goalKey)) {
                    editor.putLong(goalKey, globalGoal)
                    modified = true
                }
            }
        }
        if (!prefs.contains("${todayStr}_goal_secs")) {
            editor.putLong("${todayStr}_goal_secs", globalGoal)
            modified = true
        }
        if (modified) editor.apply()
    }

    internal data class MatrixGoalItem(val id: String, val title: String, val note: String = "", val targetMinutes: Int = 0, val isDeleted: Boolean = false)

    internal fun showPlannerMatrixDialog(startFullscreen: Boolean = false) {
        val isFullscreen = startFullscreen
        val dialog = Dialog(
            this,
            if (isFullscreen) android.R.style.Theme_Black_NoTitleBar_Fullscreen else android.R.style.Theme_Dialog
        )
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val goalsJson = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        var selectedRangeDays = 14

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = if (isFullscreen) {
                android.graphics.drawable.ColorDrawable(themeCoordinator.bgColor)
            } else {
                themeCoordinator.createDialogBackground(28f)
            }
            setPadding(
                dp(16),
                if (isFullscreen) getStatusBarHeight() + dp(12) else dp(18),
                dp(16),
                if (isFullscreen) dp(16) else dp(18)
            )
        }

        val topHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topHeaderRow.addView(TextView(this).apply {
            text = "📊 GOAL & HABIT GRID"
            setTextColor(plannerPrimary)
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val fsBtn = TextView(this).apply {
            text = if (isFullscreen) "⛶ Exit Fullscreen" else "⛶ Fullscreen"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(plannerPrimary, 12f)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener {
                dialog.dismiss()
                showPlannerMatrixDialog(startFullscreen = !isFullscreen)
            }
        }
        topHeaderRow.addView(fsBtn)
        content.addView(topHeaderRow)

        content.addView(TextView(this).apply {
            text = "Full date-by-date habit completion status for all goals:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(10))
        })

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHeaderFmt = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Collect all goals (both active and historically deleted)
        val allGoalItemsMap = mutableMapOf<String, MatrixGoalItem>()

        // Add current active goals first
        for (g in goalsList) {
            allGoalItemsMap[g.id] = MatrixGoalItem(g.id, g.title, note = g.note, targetMinutes = g.targetMinutes, isDeleted = false)
        }

        // Add past deleted goals from daily snapshots
        val prefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        for (key in prefs.all.keys) {
            if (key.endsWith("_planner_snapshot")) {
                if (!prefs.contains(key)) continue
                val snapshots = PlannerHistoryManager.loadDaySnapshot(this, key.removeSuffix("_planner_snapshot"))
                for (s in snapshots) {
                    if (s.goalId.isNotBlank() && !allGoalItemsMap.containsKey(s.goalId)) {
                        allGoalItemsMap[s.goalId] = MatrixGoalItem(s.goalId, if (s.title.isNotBlank()) s.title else "Deleted Goal", isDeleted = true)
                    }
                }
            }
        }

        val allGoalItems = allGoalItemsMap.values.toList()
        val allDates = mutableSetOf<String>()

        val goalHistories = allGoalItems.associate { item ->
            val h = PlannerHistoryManager.loadGoalHistoryDetailed(this, item.id)
            allDates.addAll(h.keys)
            item.id to h
        }

        val sortedDatesAll = allDates.mapNotNull { runCatching { sdf.parse(it) }.getOrNull() }.sortedDescending()

        val matrixContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun renderMatrix(rangeDays: Int) {
            matrixContainer.removeAllViews()
            val calendarList = mutableListOf<Date>()
            val futureOffset = 3
            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, futureOffset) }
            val count = if (rangeDays > 0) rangeDays else maxOf(14, sortedDatesAll.size + futureOffset)
            for (i in 0 until count) {
                val c = startCal.clone() as Calendar
                c.add(Calendar.DAY_OF_YEAR, -i)
                calendarList.add(c.time)
            }
            val filteredDates = calendarList

            if (filteredDates.isEmpty()) {
                matrixContainer.addView(TextView(this).apply {
                    text = "No history recorded yet. Complete goals across days to populate the matrix."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(20), 0, dp(20))
                })
                return
            }

            val splitLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // Left Fixed Column for Goal Titles
            val leftColumn = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            leftColumn.addView(TextView(this@MainActivity).apply {
                text = "Goal Title"
                setTextColor(plannerPrimary)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(dp(110), dp(36))
            })
            leftColumn.addView(createDivider())

            for (item in allGoalItems) {
                leftColumn.addView(TextView(this@MainActivity).apply {
                    text = if (item.isDeleted) "${item.title} (Old)" else item.title
                    setTextColor(if (item.isDeleted) tintedColor(themeCoordinator.textColor, 120) else themeCoordinator.textColor)
                    textSize = 13f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(110), dp(38))
                    setOnClickListener {
                        showGoalDetailFloatingCard(item)
                    }
                    setOnLongClickListener {
                        showDeletePlannerGoalMatrixDialog(item.id, item.title) {
                            dialog.dismiss()
                            showPlannerMatrixDialog(startFullscreen = isFullscreen)
                        }
                        true
                    }
                })
                leftColumn.addView(createDivider())
            }
            splitLayout.addView(leftColumn)

            // Right Scrollable Dates Container
            val rightScroll = HorizontalScrollView(this@MainActivity).apply {
                isHorizontalScrollBarEnabled = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val rightTable = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val todayStr = sdf.format(Date())

            val allGoalsTodayCompleted = allGoalItems.isNotEmpty() && allGoalItems.all { item ->
                val history = goalHistories[item.id] ?: emptyMap()
                history[todayStr] == GoalHistoryStatus.ACHIEVED
            }

            val todayAccent = if (allGoalsTodayCompleted) 0xFF22C55E.toInt() else plannerSecondary
            val todayBgColor = if (allGoalsTodayCompleted) Color.argb(35, 34, 197, 94) else Color.argb(22, Color.red(plannerSecondary), Color.green(plannerSecondary), Color.blue(plannerSecondary))

            val headerRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36))
            }
            for (d in filteredDates) {
                val dateKey = sdf.format(d)
                val isToday = (dateKey == todayStr)
                headerRow.addView(TextView(this@MainActivity).apply {
                    text = if (isToday) "TODAY\n${dateHeaderFmt.format(d)}" else dateHeaderFmt.format(d)
                    setTextColor(if (isToday) 0xFFFFFFFF.toInt() else plannerPrimary)
                    textSize = if (isToday) 10f else 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    if (isToday) {
                        background = themeCoordinator.createGlassChip(todayAccent, 10f)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
            rightTable.addView(headerRow)
            rightTable.addView(createDivider())

            val greenColor = 0xFF22C55E.toInt()
            val redColor = 0xFFEF4444.toInt()

            for (item in allGoalItems) {
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38))
                }
                val history = goalHistories[item.id] ?: emptyMap()
                for (d in filteredDates) {
                    val dateKey = sdf.format(d)
                    val status = history[dateKey]
                    val isToday = (dateKey == todayStr)
                    row.addView(TextView(this@MainActivity).apply {
                        text = when (status) {
                            GoalHistoryStatus.ACHIEVED -> "\u2713"
                            GoalHistoryStatus.DEFICIT -> "\u2715"
                            GoalHistoryStatus.NOT_COMPLETED, null -> "-"
                        }
                        setTextColor(when (status) {
                            GoalHistoryStatus.ACHIEVED -> greenColor
                            GoalHistoryStatus.DEFICIT -> redColor
                            GoalHistoryStatus.NOT_COMPLETED, null -> if (isToday) 0xFFFFFFFF.toInt() else tintedColor(themeCoordinator.textColor, 80)
                        })
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        gravity = Gravity.CENTER
                        if (isToday) {
                            background = GradientDrawable().apply {
                                setColor(todayBgColor)
                                setStroke(dp(1), todayAccent)
                                cornerRadius = dp(6).toFloat()
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.MATCH_PARENT)
                    })
                }
                rightTable.addView(row)
                rightTable.addView(createDivider())
            }
            rightScroll.addView(rightTable)

            val todayIndex = filteredDates.indexOfFirst { sdf.format(it) == todayStr }
            if (todayIndex >= 0) {
                val targetScrollX = todayIndex * dp(54)
                rightScroll.visibility = View.INVISIBLE
                rightScroll.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        rightScroll.viewTreeObserver.removeOnPreDrawListener(this)
                        rightScroll.scrollTo(targetScrollX, 0)
                        rightScroll.visibility = View.VISIBLE
                        return true
                    }
                })
            }

            splitLayout.addView(rightScroll)

            matrixContainer.addView(splitLayout)
        }

        // Filter Bar (14 Days | 30 Days | All Time)
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(10))
        }

        fun makeFilterBtn(label: String, daysVal: Int): TextView {
            val isSel = selectedRangeDays == daysVal
            return TextView(this).apply {
                text = label
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", if (isSel) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (isSel) Color.WHITE else tintedColor(themeCoordinator.textColor, 180))
                background = if (isSel)
                    themeCoordinator.createGlassChip(plannerPrimary, 20f)
                else
                    themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                setPadding(dp(14), dp(6), dp(14), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, dp(6), 0) }
                setOnClickListener {
                    selectedRangeDays = daysVal
                    for (i in 0 until filterRow.childCount) {
                        val child = filterRow.getChildAt(i) as? TextView ?: continue
                        val sel = (child.tag as? Int) == selectedRangeDays
                        child.typeface = Typeface.create("sans-serif-medium", if (sel) Typeface.BOLD else Typeface.NORMAL)
                        child.setTextColor(if (sel) Color.WHITE else tintedColor(themeCoordinator.textColor, 180))
                        child.background = if (sel)
                            themeCoordinator.createGlassChip(plannerPrimary, 20f)
                        else
                            themeCoordinator.createGlassChip(Color.argb(40, 255, 255, 255), 20f)
                    }
                    renderMatrix(selectedRangeDays)
                }
                tag = daysVal
            }
        }
        filterRow.addView(makeFilterBtn("14 Days", 14))
        filterRow.addView(makeFilterBtn("30 Days", 30))
        filterRow.addView(makeFilterBtn("All Time", 0))

        content.addView(filterRow)
        renderMatrix(selectedRangeDays)
        content.addView(matrixContainer)

        val closeBtn = Button(this).apply {
            text = "CLOSE"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(plannerPrimary, 24f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { setMargins(0, dp(14), 0, 0) }
        }
        content.addView(closeBtn)

        dialog.setContentView(content)

        if (isFullscreen) {
            dialog.window?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setDecorFitsSystemWindows(false)
                }
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.TRANSPARENT
                navigationBarColor = Color.TRANSPARENT
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(themeCoordinator.bgColor))
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        } else {
            dialog.window?.apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setLayout((resources.displayMetrics.widthPixels * 0.95f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        dialog.show()
    }

    internal fun showFeedbackReportDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        // Header Title
        val titleText = TextView(this).apply {
            text = "💬 Report a Problem & Feedback"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        content.addView(titleText)

        val subtitleText = TextView(this).apply {
            text = "Help us improve StudyTimer! Report bugs, suggest features, or share thoughts."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.65f
            textSize = 12.5f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(subtitleText)

        // Category Selector Chips
        val categories = listOf("Bug Report", "Feature Request", "General Feedback")
        var selectedCategory = "Bug Report"

        val chipContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }

        val chipViews = ArrayList<TextView>()
        fun updateChipStyles() {
            chipViews.forEachIndexed { index, chip ->
                val isSelected = categories[index] == selectedCategory
                chip.setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                chip.background = if (isSelected) {
                    themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 18f)
                } else {
                    themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 18f)
                }
            }
        }

        categories.forEach { cat ->
            val chip = TextView(this).apply {
                text = when (cat) {
                    "Bug Report" -> "🐛 Bug"
                    "Feature Request" -> "💡 Feature"
                    else -> "💭 Feedback"
                }
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
                setOnClickListener {
                    selectedCategory = cat
                    updateChipStyles()
                }
            }
            chipViews.add(chip)
            chipContainer.addView(chip)
        }
        updateChipStyles()
        content.addView(chipContainer)

        // Description Text Field
        val inputField = android.widget.EditText(this).apply {
            hint = "Describe what happened or what you'd like to see..."
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13.5f
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 7
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        content.addView(inputField)

        // Contact Email Input Field (Optional)
        val contactField = android.widget.EditText(this).apply {
            hint = "Your email for reply (optional)..."
            val userEmail = AuthManager.getUserEmail(this@MainActivity)
            if (!userEmail.isNullOrBlank()) setText(userEmail)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 13f
            isSingleLine = true
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (themeCoordinator.isDarkMode()) 0xFF141414.toInt() else tintedColor(themeCoordinator.textColor, 18))
                setStroke(dp(1), tintedColor(themeCoordinator.textColor, 35))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        content.addView(contactField)

        // Anonymous Diagnostic Info Toggle
        var includeDiagnostics = true
        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(12))
            isClickable = true
            isFocusable = true
        }

        val checkIcon = TextView(this).apply {
            text = "☑️"
            textSize = 15f
            setPadding(0, 0, dp(8), 0)
        }
        toggleRow.addView(checkIcon)

        val toggleLabel = TextView(this).apply {
            text = "Include anonymous diagnostic info\n(App version, OS, Device model, Sync state)"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            textSize = 11.5f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleRow.addView(toggleLabel)

        toggleRow.setOnClickListener {
            includeDiagnostics = !includeDiagnostics
            checkIcon.text = if (includeDiagnostics) "☑️" else "⬜"
        }
        content.addView(toggleRow)

        val feedbackPrefs = getSharedPreferences("studytimer_feedback_prefs", Context.MODE_PRIVATE)
        val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes cooldown
        val lastSubmissionEpoch = feedbackPrefs.getLong("last_feedback_submission_epoch", 0L)
        val isDevBypass = BuildConfig.DEBUG || isDevModeUnlocked

        // Cooldown Warning & Live Countdown View
        val cooldownWarningText = TextView(this).apply {
            setTextColor(0xFFFF7043.toInt()) // Soft coral warning
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(6))
            visibility = View.GONE
        }

        // Urgent Help Fallback Action
        val urgentEmailBtn = TextView(this).apply {
            text = "Need urgent help? [Email studytimer737@gmail.com]"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:studytimer737@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "[StudyTimer Urgent Help]")
                }
                try {
                    startActivity(Intent.createChooser(emailIntent, "Email Support..."))
                } catch (_: Exception) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Support Email", "studytimer737@gmail.com"))
                    Toast.makeText(this@MainActivity, "Email copied to clipboard: studytimer737@gmail.com", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Submit Feedback Action Button
        val sendBtn = Button(this).apply {
            text = "🚀 Submit Report"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, dp(4), 0, dp(6))
            }
        }

        // Countdown Timer Handler
        val countdownHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSubmissionEpoch
                val remaining = COOLDOWN_MS - elapsed

                if (!isDevBypass && remaining > 0) {
                    val mins = (remaining / 1000) / 60
                    val secs = (remaining / 1000) % 60
                    sendBtn.isEnabled = false
                    sendBtn.alpha = 0.5f
                    cooldownWarningText.visibility = View.VISIBLE
                    cooldownWarningText.text = "⏳ You recently sent a report. Please wait ${mins}m ${secs}s before submitting another."
                    urgentEmailBtn.visibility = View.VISIBLE
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    sendBtn.isEnabled = true
                    sendBtn.alpha = 1.0f
                    cooldownWarningText.visibility = View.GONE
                    urgentEmailBtn.visibility = View.GONE
                }
            }
        }

        countdownHandler.post(countdownRunnable)
        dialog.setOnDismissListener {
            countdownHandler.removeCallbacks(countdownRunnable)
        }

        sendBtn.setOnClickListener {
            val now = System.currentTimeMillis()
            val elapsed = now - lastSubmissionEpoch
            val remaining = COOLDOWN_MS - elapsed

            if (!isDevBypass && remaining > 0) {
                val mins = (remaining / 1000) / 60
                val secs = (remaining / 1000) % 60
                Toast.makeText(this@MainActivity, "Please wait ${mins}m ${secs}s before submitting another report.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userFeedbackText = inputField.text.toString().trim()
            if (userFeedbackText.length < 5) {
                Toast.makeText(this@MainActivity, "Please enter at least 5 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userContactText = contactField.text.toString().trim()
            sendBtn.isEnabled = false
            sendBtn.text = "⏳ Submitting..."

            // Build Diagnostic Object
            val diagJson = org.json.JSONObject().apply {
                put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                put("android_os", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                put("timer_mode", timerMode)
                put("sync_status", if (AuthManager.isLoggedIn(this@MainActivity)) "ONLINE_SYNCED" else "GUEST_OFFLINE")
                put("timestamp_epoch", System.currentTimeMillis() / 1000)
            }

            val typeEnum = when (selectedCategory) {
                "Bug Report" -> "BUG_REPORT"
                "Feature Request" -> "FEATURE_REQUEST"
                else -> "GENERAL_FEEDBACK"
            }

            // Execute Real HTTP POST in background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                var isSuccess = false
                var errorMessage = ""
                var statusCode = 0

                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

                val targetEndpoint = if (supabaseUrl.isNotBlank()) {
                    "$supabaseUrl/rest/v1/feedback_reports"
                } else ""

                val payload = org.json.JSONObject().apply {
                    put("type", typeEnum)
                    put("status", "NEW")
                    if (userContactText.isNotBlank()) put("user_contact", userContactText)
                    put("message", userFeedbackText)
                    put("diagnostics", if (includeDiagnostics) diagJson else org.json.JSONObject())
                }

                android.util.Log.d("FeedbackSubmission", "========================================")
                android.util.Log.d("FeedbackSubmission", "Sending to: $targetEndpoint")
                android.util.Log.d("FeedbackSubmission", "Payload: ${payload.toString()}")
                android.util.Log.d("FeedbackSubmission", "========================================")

                if (targetEndpoint.isNotBlank()) {
                    try {
                        val url = java.net.URL(targetEndpoint)
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", supabaseAnonKey)
                        conn.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Prefer", "return=representation")
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doOutput = true

                        conn.outputStream.use { os ->
                            os.write(payload.toString().toByteArray(Charsets.UTF_8))
                        }

                        statusCode = conn.responseCode
                        val responseBody = try {
                            if (statusCode in 200..299) {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                            }
                        } catch (_: Exception) { "Could not read response" }

                        android.util.Log.d("FeedbackSubmission", "Server Response HTTP $statusCode: $responseBody")

                        if (statusCode in 200..299) {
                            isSuccess = true
                        } else {
                            errorMessage = "Server HTTP $statusCode: $responseBody"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FeedbackSubmission", "Network error during feedback post", e)
                        errorMessage = e.localizedMessage ?: "Network connection error"
                    }
                } else {
                    errorMessage = "No backend endpoint configured."
                }

                // Handle on Main Thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (isSuccess) {
                        feedbackPrefs.edit().putLong("last_feedback_submission_epoch", System.currentTimeMillis()).apply()
                        Toast.makeText(this@MainActivity, "🎉 Report submitted successfully to database!", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        sendBtn.isEnabled = true
                        sendBtn.text = "🚀 Submit Report"
                        android.util.Log.w("FeedbackSubmission", "HTTP Failed: $errorMessage. Triggering email fallback.")

                        val devEmail = "studytimer737@gmail.com"
                        val emailSubject = "[StudyTimer Feedback - $selectedCategory] v${BuildConfig.VERSION_NAME}"
                        val diagnosticBlock = if (includeDiagnostics) {
                            """
                            
                            -------------------------------------
                            Diagnostic Info (Helps us fix bugs faster):
                            • App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                            • Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                            • Device: ${Build.MANUFACTURER} ${Build.MODEL}
                            • Timer Mode Active: $timerMode
                            • Sync Status: ${if (AuthManager.isLoggedIn(this@MainActivity)) "Online" else "Guest"}
                            -------------------------------------
                            """.trimIndent()
                        } else ""

                        val emailBody = "$userFeedbackText\n$diagnosticBlock"

                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(devEmail))
                            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                            putExtra(Intent.EXTRA_TEXT, emailBody)
                        }

                        try {
                            Toast.makeText(this@MainActivity, "Database unavailable ($errorMessage). Opening email client...", Toast.LENGTH_LONG).show()
                            startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email..."))
                            dialog.dismiss()
                        } catch (_: Exception) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("StudyTimer Feedback", "To: $devEmail\nSubject: $emailSubject\n\n$emailBody"))
                            Toast.makeText(this@MainActivity, "Feedback copied to clipboard! (Server unavailable & no email app)", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        content.addView(sendBtn)
        content.addView(cooldownWarningText)
        content.addView(urgentEmailBtn)

        // Optional GitHub Issues Link Button
        val githubBtn = TextView(this).apply {
            text = "🐙 Open GitHub Issues (Web)"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val githubUrl = "https://github.com/issues"
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                    startActivity(browserIntent)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Could not open browser", Toast.LENGTH_SHORT).show()
                }
            }
        }
        content.addView(githubBtn)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    internal fun showAppGuideDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        val titleText = TextView(this).apply {
            text = "⚡ StudyTimer Quick Guide"
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        content.addView(titleText)

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun addGuideSection(icon: String, title: String, description: String) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(tintedColor(themeCoordinator.primaryColor, 18))
                }
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }
            val iconView = TextView(this).apply {
                text = icon
                textSize = 22f
                setPadding(0, 0, dp(12), 0)
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sectionTitle = TextView(this).apply {
                text = title
                setTextColor(themeCoordinator.primaryColor)
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            val sectionDesc = TextView(this).apply {
                text = description
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 12.5f
                setPadding(0, dp(2), 0, 0)
            }
            textCol.addView(sectionTitle)
            textCol.addView(sectionDesc)
            card.addView(iconView)
            card.addView(textCol)
            list.addView(card)
        }

        addGuideSection("⏱️", "1-Hour Pomodoro & Modes", "Default countdown timer is set to 1 hour (Pomodoro mode). Switch between Pomodoro Countdown, Stopwatch, and Class Lecture timetable modes in Settings.")
        addGuideSection("🎨", "3D Look & Custom Accents", "Pre-configured with AMOLED Black & 3D look buttons. Customize Focus (#234) and Break (#1) color hues in Appearance tab.")
        addGuideSection("🎯", "Goals & Full Screen Grid", "Track subject goals on the main page. Tap the expand button on the Goal & Habit grid to open the full-screen interactive habit tracker.")
        addGuideSection("📅", "Class Timetable & 8 PM Reminder", "Tap 'Class Timetable' on the main timer screen to organize lecture slots. Daily study reminders are active at 8:00 PM.")
        addGuideSection("📊", "Heatmap & Spacious CSV Export", "View daily productivity heatmaps in Insights. Export clean, formatted logs to Excel/Sheets with formatted summary sections.")
        addGuideSection("☁️", "Cloud Sync & Multi-Device Restore", "Sign in with your account to automatically back up and restore your custom themes, goals, and focus history on any device.")

        scrollView.addView(list)
        content.addView(scrollView)

        val closeBtn = Button(this).apply {
            text = "GOT IT! LET'S STUDY"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 24f)
            setOnClickListener {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
                dialog.dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, dp(14), 0, 0)
            }
        }
        content.addView(closeBtn)

        dialog.setOnDismissListener {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean("has_seen_app_guide", true).apply()
        }
        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), (resources.displayMetrics.heightPixels * 0.75f).toInt())
        }
        dialog.show()
    }

    private fun showGoalDetailFloatingCard(item: Any) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, plannerSecondary) = resolvePlannerColors()

        val itemId: String
        val itemTitle: String
        val itemNote: String
        val itemTargetMins: Int
        val itemIsDeleted: Boolean

        when (item) {
            is MatrixGoalItem -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = item.isDeleted
            }
            is PlannerGoal -> {
                itemId = item.id
                itemTitle = item.title
                itemNote = item.note
                itemTargetMins = item.targetMinutes
                itemIsDeleted = false
            }
            else -> {
                itemId = runCatching { item.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getId").invoke(item) as String }.getOrNull() ?: ""
                itemTitle = runCatching { item.javaClass.getDeclaredField("title").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTitle").invoke(item) as String }.getOrNull() ?: ""
                itemNote = runCatching { item.javaClass.getDeclaredField("note").apply { isAccessible = true }.get(item) as String }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getNote").invoke(item) as String }.getOrNull() ?: ""
                itemTargetMins = runCatching { item.javaClass.getDeclaredField("targetMinutes").apply { isAccessible = true }.get(item) as Int }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("getTargetMinutes").invoke(item) as Int }.getOrNull() ?: 0
                itemIsDeleted = runCatching { item.javaClass.getDeclaredField("isDeleted").apply { isAccessible = true }.get(item) as Boolean }.getOrNull()
                    ?: runCatching { item.javaClass.getMethod("isDeleted").invoke(item) as Boolean }.getOrNull() ?: false
            }
        }

        val goalsJson = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]"
        val goalsList = loadSessionGoalsFromJson(goalsJson)
        val activeGoal = goalsList.find { it.id == itemId }
        val noteText = if (activeGoal != null && activeGoal.note.isNotBlank()) activeGoal.note else itemNote
        val targetMins = if (activeGoal != null && activeGoal.targetMinutes > 0) activeGoal.targetMinutes else itemTargetMins

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        // 1. FIXED Header Tag
        content.addView(TextView(this).apply {
            text = if (itemIsDeleted) "ARCHIVED GOAL" else "GOAL DETAILS"
            setTextColor(plannerSecondary)
            textSize = 11f
            letterSpacing = 0.15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // 2. Scrollable Body Box (Title + Target + Notes)
        val maxCardBodyHeight = dp(240)
        val bodyScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
            isVerticalScrollBarEnabled = true
        }

        val bodyBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        bodyBox.addView(TextView(this).apply {
            text = if (itemTitle.isNotBlank()) itemTitle else "Untitled Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })

        if (targetMins > 0) {
            bodyBox.addView(TextView(this).apply {
                text = "🎯 Target: $targetMins mins / day"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.85f
                textSize = 13f
                setPadding(0, 0, 0, dp(6))
            })
        }

        if (noteText.isNotBlank()) {
            bodyBox.addView(createDivider())
            bodyBox.addView(TextView(this).apply {
                text = "NOTES & DETAILS"
                setTextColor(tintedColor(themeCoordinator.textColor, 140))
                textSize = 10f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(4))
            })

            val noteBox = TextView(this).apply {
                text = noteText
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                setLineSpacing(dp(3).toFloat(), 1.1f)
                background = themeCoordinator.createGlassChip(Color.argb(30, 255, 255, 255), 14f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }
            bodyBox.addView(noteBox)
        }

        bodyScroll.addView(bodyBox)

        bodyScroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bodyScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (bodyScroll.height > maxCardBodyHeight) {
                    bodyScroll.layoutParams = (bodyScroll.layoutParams as LinearLayout.LayoutParams).apply {
                        height = maxCardBodyHeight
                    }
                }
            }
        })
        content.addView(bodyScroll)

        // 3. FIXED Action Buttons Row at Bottom
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        if (activeGoal != null) {
            val historyBtn = TextView(this).apply {
                text = "📊 Calendar Stats"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(plannerPrimary, 20f)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(8), 0) }
                setOnClickListener {
                    dialog.dismiss()
                    showGoalHistoryDialog(activeGoal)
                }
            }
            buttonRow.addView(historyBtn)
        }

        val closeBtn = TextView(this).apply {
            text = "CLOSE"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(if (activeGoal != null) Color.argb(40, 255, 255, 255) else plannerPrimary, 20f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(if (activeGoal != null) 0 else LinearLayout.LayoutParams.MATCH_PARENT, dp(44), 1f)
            setOnClickListener { dialog.dismiss() }
        }
        buttonRow.addView(closeBtn)
        content.addView(buttonRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    internal fun showDeletePlannerGoalMatrixDialog(goalId: String, goalTitle: String, onDeleted: () -> Unit) {
        val dialog = Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(24f)
        }

        root.addView(TextView(this).apply {
            text = "Delete Goal from History?"
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Goal: $goalTitle\n\nWould you like to keep or delete this goal from your Goal & Habit Grid history?"
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
            text = "Keep"
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener { dialog.dismiss() }
        })

        btnRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })

        btnRow.addView(TextView(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(Color.argb(40, 255, 82, 82), 16f)
            setOnClickListener {
                dialog.dismiss()
                val prefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
                val activeJson = prefs.getString("session_goals_json", "[]") ?: "[]"
                val activeList = loadSessionGoalsFromJson(activeJson).filterNot { it.id == goalId }
                saveSessionGoalsToJson(activeList)

                PlannerHistoryManager.deleteGoalHistory(this@MainActivity, goalId)

                Toast.makeText(this@MainActivity, "Deleted '$goalTitle' from grid history", Toast.LENGTH_SHORT).show()
                onDeleted()
                Thread {
                    kotlinx.coroutines.runBlocking {
                        CloudSyncManager.syncDataToCloud(this@MainActivity)
                    }
                }.start()
            }
        })

        root.addView(btnRow)
        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    internal fun showAddSessionGoalDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "New Planner Goal"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box to ensure buttons never go offscreen
        val scrollContainer = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isVerticalScrollBarEnabled = true
        }

        val inputsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(this).apply {
            hint = "Goal title (e.g., Physics Lecture)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(this).apply {
            hint = "Optional note (e.g., Chapter 4 practice)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(this).apply {
            hint = "Target minutes (max 1440m / 24h)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(durationInput)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(this).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(this).apply {
            text = "Save Goal"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val titleText = titleInput.text.toString().trim()
                if (titleText.isNotBlank()) {
                    val noteText = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val targetMins = rawMins.coerceAtMost(1440)
                    val currentGoals = loadSessionGoalsFromJson(getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    currentGoals.add(PlannerGoal(title = titleText, note = noteText, targetMinutes = targetMins))
                    saveSessionGoalsToJson(currentGoals)
                    refreshStatsPanel()
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), (resources.displayMetrics.heightPixels * 0.80f).toInt().coerceAtMost(android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.show()
    }

    internal fun showEditSessionGoalDialog(goal: PlannerGoal) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val (plannerPrimary, _) = resolvePlannerColors()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "✏️ Edit Goal"
            setTextColor(plannerPrimary)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Scrollable input fields box
        val scrollContainer = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            isVerticalScrollBarEnabled = true
        }

        val inputsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleInput = android.widget.EditText(this).apply {
            hint = "Goal title"
            setText(goal.title)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, dp(10)) }
        }
        inputsBox.addView(titleInput)

        val noteInput = android.widget.EditText(this).apply {
            hint = "Optional note"
            setText(goal.note)
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            minLines = 2
            maxLines = 5
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(noteInput)

        val durationInput = android.widget.EditText(this).apply {
            hint = "Target minutes (max 1440m / 24h)"
            setText(if (goal.targetMinutes > 0) goal.targetMinutes.toString() else "")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createCardBackground()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) }
        }
        inputsBox.addView(durationInput)

        scrollContainer.addView(inputsBox)
        content.addView(scrollContainer)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        btnRow.addView(Button(this).apply {
            text = "Cancel"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        })
        btnRow.addView(Button(this).apply {
            text = "Save Changes"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(plannerPrimary)
            setOnClickListener {
                val newTitle = titleInput.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    val newNote = noteInput.text.toString().trim()
                    val rawMins = durationInput.text.toString().toIntOrNull() ?: 0
                    val newTargetMins = rawMins.coerceAtMost(1440)

                    val currentGoals = loadSessionGoalsFromJson(getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("session_goals_json", "[]") ?: "[]").toMutableList()
                    val idx = currentGoals.indexOfFirst { it.id == goal.id }
                    if (idx >= 0) {
                        val oldG = currentGoals[idx]
                        currentGoals[idx] = PlannerGoal(id = oldG.id, title = newTitle, note = newNote, targetMinutes = newTargetMins, completed = oldG.completed, checkedAt = oldG.checkedAt, createdAt = oldG.createdAt)
                        saveSessionGoalsToJson(currentGoals)
                        refreshStatsPanel()
                    }
                }
                dialog.dismiss()
            }
        })
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), (resources.displayMetrics.heightPixels * 0.80f).toInt().coerceAtMost(android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.show()
    }

    internal fun loadLectureSchedulesFromJson(jsonStr: String): List<LectureScheduleItem> {
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<LectureScheduleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(LectureScheduleItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Lecture"),
                    startTime = obj.optString("startTime", "09:00"),
                    endTime = obj.optString("endTime", "10:00"),
                    enabled = obj.optBoolean("enabled", true),
                    subjectId = obj.optString("subjectId", "general")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun saveLectureSchedulesToJson(items: List<LectureScheduleItem>) {
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("enabled", item.enabled)
                put("subjectId", item.subjectId)
            }
            array.put(obj)
        }
        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val hasEnabled = items.any { it.enabled }
        prefs.edit()
            .putString("lecture_schedules_json", array.toString())
            .putBoolean("lecture_mode_enabled", hasEnabled)
            .apply()

        if (hasEnabled) {
            val serviceIntent = Intent(this, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    internal fun showLectureScheduleManagerDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val contentWidth = (resources.displayMetrics.widthPixels * 0.82).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(this).apply {
            text = "\uD83C\uDF93 Class Timetable"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val addBtn = TextView(this).apply {
            text = "+ Add Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 12f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                dialog.dismiss()
                showAddLectureScheduleDialog()
            }
        }
        topRow.addView(addBtn)
        content.addView(topRow)

        val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val items = loadLectureSchedulesFromJson(prefs.getString("lecture_schedules_json", "[]") ?: "[]")

        fun formatScheduleTime(timeStr: String): String {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return TimeFormat.formatHourMinute(this@MainActivity, h, m)
            }
            return timeStr
        }

        if (items.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "No scheduled classes yet.\nTap '+ Add Class' to set start and end times."
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
            })
        } else {
            val listContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }
            for (item in items) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val allSubjects = SubjectTagManager.getAllSubjects(this@MainActivity)
                val itemSub = allSubjects.find { it.id == item.subjectId } ?: SubjectTagManager.DEFAULT_SUBJECTS[0]
                textCol.addView(TextView(this).apply {
                    text = "${itemSub.iconEmoji} ${item.title}"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                textCol.addView(TextView(this).apply {
                    text = "${formatScheduleTime(item.startTime)} - ${formatScheduleTime(item.endTime)}"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                })
                row.addView(textCol)

                val toggleSwitch = SwitchMaterial(this).apply {
                    isChecked = item.enabled
                    scaleX = 0.75f
                    scaleY = 0.75f
                    setOnCheckedChangeListener { _, isChecked ->
                        val updated = items.map { if (it.id == item.id) it.copy(enabled = isChecked) else it }
                        saveLectureSchedulesToJson(updated)
                    }
                }

                row.addView(toggleSwitch)

                val editBtn = TextView(this).apply {
                    text = "\u270F\uFE0F"
                    textSize = 14f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        dialog.dismiss()
                        showAddLectureScheduleDialog(editItem = item)
                    }
                }
                row.addView(editBtn)

                val delBtn = TextView(this).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.5f
                    textSize = 16f
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    setOnClickListener {
                        showConfirmDialog("Delete Class?", "Are you sure you want to remove '${item.title}' from your timetable?") {
                            val updated = items.filter { it.id != item.id }
                            saveLectureSchedulesToJson(updated)
                            dialog.dismiss()
                            showLectureScheduleManagerDialog()
                        }
                    }
                }
                row.addView(delBtn)
                listContainer.addView(row)
            }
            content.addView(listContainer)
        }

        val closeBtn = Button(this).apply {
            text = "Close"
            setTextColor(themeCoordinator.textColor)
            background = null
            setOnClickListener { dialog.dismiss() }
        }
        content.addView(closeBtn)
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    internal fun showAddLectureScheduleDialog(editItem: LectureScheduleItem? = null) {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val contentWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(18), dp(20), dp(18))
            }

            content.addView(TextView(this).apply {
                text = if (editItem != null) "Edit Class Schedule" else "Add Class Schedule"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, 0, 0, dp(14))
            })

            val titleInput = android.widget.EditText(this).apply {
                hint = "Class Title (e.g. Physics 101)"
                if (editItem != null) setText(editItem.title)
                filters = arrayOf(android.text.InputFilter.LengthFilter(100))
                setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                background = themeCoordinator.createCardBackground()
                setPadding(dp(14), dp(11), dp(14), dp(11))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }
            content.addView(titleInput)

            fun parseIsPm(time24: String?): Boolean {
                if (time24.isNullOrBlank()) return false
                val parts = time24.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 0
                    return h >= 12
                }
                return false
            }

            var startIsPm = parseIsPm(editItem?.startTime)
            var endIsPm = parseIsPm(editItem?.endTime)

            fun createTimeInputGroup(initTime24: String?): Triple<LinearLayout, android.widget.EditText, android.widget.EditText> {
                val group = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                var hInit = ""
                var mInit = ""
                if (!initTime24.isNullOrBlank()) {
                    val parts = initTime24.split(":")
                    if (parts.size == 2) {
                        val h24 = parts[0].toIntOrNull() ?: 0
                        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
                        hInit = String.format(Locale.US, "%02d", h12)
                        mInit = parts[1]
                    }
                }

                val hourEdit = android.widget.EditText(this).apply {
                    hint = "10"
                    if (hInit.isNotBlank()) setText(hInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val colonLabel = TextView(this).apply {
                    text = ":"
                    setTextColor(themeCoordinator.primaryColor)
                    textSize = 18f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(4), 0, dp(4), 0)
                }

                val minEdit = android.widget.EditText(this).apply {
                    hint = "00"
                    if (mInit.isNotBlank()) setText(mInit)
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(2))
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(8), dp(9), dp(8), dp(9))
                    layoutParams = LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                hourEdit.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (s?.length == 2) {
                            minEdit.requestFocus()
                        }
                    }
                })

                group.addView(hourEdit)
                group.addView(colonLabel)
                group.addView(minEdit)
                return Triple(group, hourEdit, minEdit)
            }

            content.addView(TextView(this).apply {
                text = "Start Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val startRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (startGroup, startHEdit, startMEdit) = createTimeInputGroup(editItem?.startTime)
            startRow.addView(startGroup)

            val startAmBtn = TextView(this).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val startPmBtn = TextView(this).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            startAmBtn.setOnClickListener { startIsPm = false; startAmBtn.setTextColor(themeCoordinator.bgColor); startAmBtn.background = rippleBackground(themeCoordinator.primaryColor); startPmBtn.setTextColor(themeCoordinator.textColor); startPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            startPmBtn.setOnClickListener { startIsPm = true; startPmBtn.setTextColor(themeCoordinator.bgColor); startPmBtn.background = rippleBackground(themeCoordinator.primaryColor); startAmBtn.setTextColor(themeCoordinator.textColor); startAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            startAmBtn.setTextColor(if (!startIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            startAmBtn.background = if (!startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            startPmBtn.setTextColor(if (startIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            startPmBtn.background = if (startIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val startToggleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            startToggleRow.addView(startAmBtn)
            startToggleRow.addView(startPmBtn)
            startRow.addView(startToggleRow)
            content.addView(startRow)

            content.addView(TextView(this).apply {
                text = "End Time"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })
            val endRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
            }
            val (endGroup, endHEdit, endMEdit) = createTimeInputGroup(editItem?.endTime)
            endRow.addView(endGroup)

            val endAmBtn = TextView(this).apply {
                text = "AM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            val endPmBtn = TextView(this).apply {
                text = "PM"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
            }
            endAmBtn.setOnClickListener { endIsPm = false; endAmBtn.setTextColor(themeCoordinator.bgColor); endAmBtn.background = rippleBackground(themeCoordinator.primaryColor); endPmBtn.setTextColor(themeCoordinator.textColor); endPmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }
            endPmBtn.setOnClickListener { endIsPm = true; endPmBtn.setTextColor(themeCoordinator.bgColor); endPmBtn.background = rippleBackground(themeCoordinator.primaryColor); endAmBtn.setTextColor(themeCoordinator.textColor); endAmBtn.background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f) }

            endAmBtn.setTextColor(if (!endIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            endAmBtn.background = if (!endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)
            endPmBtn.setTextColor(if (endIsPm) themeCoordinator.bgColor else themeCoordinator.textColor)
            endPmBtn.background = if (endIsPm) rippleBackground(themeCoordinator.primaryColor) else themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 20), 10f)

            val endToggleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(14), 0, 0, 0) }
            }
            endToggleRow.addView(endAmBtn)
            endToggleRow.addView(endPmBtn)
            endRow.addView(endToggleRow)
            content.addView(endRow)

            content.addView(TextView(this).apply {
                text = "Subject Tag"
                setTextColor(themeCoordinator.textColor)
                textSize = 12f
                setPadding(0, 0, 0, dp(4))
            })

            val availableSubjects = SubjectTagManager.getAllSubjects(this).toMutableList()
            val initSubId = editItem?.subjectId ?: SubjectTagManager.getSelectedSubject(this).id
            var chosenSubIdx = availableSubjects.indexOfFirst { it.id == initSubId }.coerceAtLeast(0)

            val subjectRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            }

            val subjectSelectBtn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(12), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val subjectLabelTv = TextView(this).apply {
                val cur = availableSubjects[chosenSubIdx]
                text = "${cur.iconEmoji} ${cur.name}"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setSingleLine(true)
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val subjectArrowTv = TextView(this).apply {
                text = " ▾"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            subjectSelectBtn.addView(subjectLabelTv)
            subjectSelectBtn.addView(subjectArrowTv)

            val addQuickSubBtn = TextView(this).apply {
                text = "➕ Add"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 10f)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
            }

            fun openSubjectPickerMenu() {
                val popup = android.widget.PopupMenu(this, subjectSelectBtn)
                for (i in availableSubjects.indices) {
                    val s = availableSubjects[i]
                    popup.menu.add(0, i, i, "${s.iconEmoji} ${s.name}")
                }
                popup.menu.add(0, 9999, 9999, "➕ Add New Subject...")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.itemId == 9999) {
                        showAddCustomSubjectDialog { newSub ->
                            availableSubjects.clear()
                            availableSubjects.addAll(SubjectTagManager.getAllSubjects(this@MainActivity))
                            chosenSubIdx = availableSubjects.indexOfFirst { it.id == newSub.id }.coerceAtLeast(0)
                            subjectLabelTv.text = "${newSub.iconEmoji} ${newSub.name}"
                        }
                    } else {
                        chosenSubIdx = menuItem.itemId
                        val sel = availableSubjects[chosenSubIdx]
                        subjectLabelTv.text = "${sel.iconEmoji} ${sel.name}"
                    }
                    true
                }
                popup.show()
            }

            subjectSelectBtn.setOnClickListener { openSubjectPickerMenu() }
            addQuickSubBtn.setOnClickListener {
                showAddCustomSubjectDialog { newSub ->
                    availableSubjects.clear()
                    availableSubjects.addAll(SubjectTagManager.getAllSubjects(this@MainActivity))
                    chosenSubIdx = availableSubjects.indexOfFirst { it.id == newSub.id }.coerceAtLeast(0)
                    subjectLabelTv.text = "${newSub.iconEmoji} ${newSub.name}"
                }
            }

            subjectRow.addView(subjectSelectBtn)
            subjectRow.addView(addQuickSubBtn)
            content.addView(subjectRow)

            fun convertTo24h(hEdit: android.widget.EditText, mEdit: android.widget.EditText, isPm: Boolean): String {
                val rawH = hEdit.text.toString().trim().toIntOrNull() ?: return ""
                val rawM = mEdit.text.toString().trim().toIntOrNull() ?: 0
                val h12 = rawH.coerceIn(1, 12)
                val m = rawM.coerceIn(0, 59)
                val h24 = when {
                    isPm && h12 < 12 -> h12 + 12
                    !isPm && h12 == 12 -> 0
                    else -> h12
                }
                return String.format(Locale.US, "%02d:%02d", h24, m)
            }

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(10), 0, 0)
            }
            btnRow.addView(Button(this).apply {
                text = "Cancel"
                setTextColor(themeCoordinator.textColor)
                background = null
                setOnClickListener {
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            btnRow.addView(Button(this).apply {
                text = if (editItem != null) "Update Class" else "Save Class"
                setTextColor(themeCoordinator.bgColor)
                background = rippleBackground(themeCoordinator.primaryColor)
                setOnClickListener {
                    val titleText = titleInput.text.toString().trim().take(100)
                    val startText = convertTo24h(startHEdit, startMEdit, startIsPm)
                    val endText = convertTo24h(endHEdit, endMEdit, endIsPm)
                    val selectedSubjectId = availableSubjects[chosenSubIdx].id

                    if (titleText.isNotBlank() && startText.isNotBlank() && endText.isNotBlank()) {
                        val currentSchedules = loadLectureSchedulesFromJson(getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getString("lecture_schedules_json", "[]") ?: "[]").toMutableList()
                        if (editItem != null) {
                            val idx = currentSchedules.indexOfFirst { it.id == editItem.id }
                            if (idx != -1) {
                                currentSchedules[idx] = editItem.copy(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId)
                            }
                        } else {
                            currentSchedules.add(LectureScheduleItem(title = titleText, startTime = startText, endTime = endText, subjectId = selectedSubjectId))
                        }
                        saveLectureSchedulesToJson(currentSchedules)

                        val serviceIntent = Intent(this@MainActivity, TimerService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                    dialog.dismiss()
                    showLectureScheduleManagerDialog()
                }
            })
            content.addView(btnRow)

            dialog.setContentView(content)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
            dialog.window?.setLayout(contentWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        } catch (e: Exception) {
            android.util.Log.e("StudyTimer", "Error in showAddLectureScheduleDialog", e)
        }
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
            textSize = 12f
            letterSpacing = 0.18f
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
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f
        private val maxFillRatio = ratio.coerceIn(0f, 1f)

        init {
            tickPaint.style = Paint.Style.FILL
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = 700
            anim.interpolator = android.view.animation.DecelerateInterpolator()
            anim.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            anim.start()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(barHeight, MeasureSpec.EXACTLY))
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = h / 2f

            paint.style = Paint.Style.FILL
            paint.color = trackColor
            canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)

            val fillW = w * maxFillRatio * progress
            if (fillW > 0f) {
                fillPaint.shader = LinearGradient(0f, 0f, w, 0f, fillStart, fillEnd, Shader.TileMode.CLAMP)
                fillPaint.setShadowLayer(
                    dp(7).toFloat(), 0f, 0f,
                    Color.argb(150, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
                )
                if (fillW >= radius * 2f) {
                    canvas.drawRoundRect(RectF(0f, 0f, fillW, h), radius, radius, fillPaint)
                } else {
                    canvas.save()
                    canvas.clipRect(0f, 0f, fillW, h)
                    canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, fillPaint)
                    canvas.restore()
                }
                fillPaint.setShadowLayer(0f, 0f, 0f, 0)
            }

            if (goalRatio > 0f) {
                val gx = w * goalRatio
                tickPaint.color = Color.argb(120, 255, 255, 255)
                canvas.drawRect(gx - dp(1).toFloat(), 0f, gx + dp(1).toFloat(), h, tickPaint)
            }

            if (isToday) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2) * 0.75f
                paint.color = Color.argb(180, Color.red(fillStart), Color.green(fillStart), Color.blue(fillStart))
                canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    internal inner class SegmentRing(
        private val segments: List<Pair<Float, Int>>,
        private val trackColor: Int,
        private val strokeWidth: Int,
        private val gradient: Pair<Int, Int>?
    ) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.strokeCap = if (segments.size <= 1) Paint.Cap.ROUND else Paint.Cap.BUTT
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = 800
            anim.interpolator = android.view.animation.DecelerateInterpolator()
            anim.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            anim.start()
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

            paint.shader = null
            paint.color = trackColor
            canvas.drawCircle(cx, cy, r, paint)

            var startAngle = -90f
            for ((frac, color) in segments) {
                val f = frac.coerceIn(0f, 1f)
                if (f <= 0f) continue
                val sweep = f * 360f * progress
                if (gradient != null && segments.size == 1) {
                    paint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, gradient.first, gradient.second, Shader.TileMode.CLAMP)
                } else {
                    paint.shader = null
                    paint.color = color
                }
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), startAngle, sweep, false, paint)
                startAngle += sweep
                if (segments.size > 1) startAngle += 3f
            }
        }
    }


    internal fun dayBlocks(dateStr: String): Pair<List<BlockInfo>, List<BlockInfo>> = statsEngine.dayBlocks(dateStr)

    private fun reconcileDayTotals(dateStr: String) {
        statsEngine.reconcileDayTotals(dateStr)
    }

    internal fun msForDateAndTime(dateStr: String, h: Int, m: Int): Long = statsEngine.msForDateAndTime(dateStr, h, m)

    internal fun applyBlockEdit(dateStr: String, block: BlockInfo, isBreak: Boolean, newStartMs: Long, newEndMs: Long) {
        statsEngine.applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
        statsDirty = true
    }

    private fun showBlockEditDialog(dateStr: String, block: BlockInfo, isBreak: Boolean, onApplied: (() -> Unit)? = null) {
        val kind = if (isBreak) "break" else "focus"
        val is24 = TimeFormat.is24Hour(this)
        val startCal = Calendar.getInstance().apply { timeInMillis = block.startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = block.endMs }
        android.app.TimePickerDialog(
            this,
            { _, h, m ->
                val newStartMs = msForDateAndTime(dateStr, h, m)
                android.app.TimePickerDialog(
                    this,
                    { _, h2, m2 ->
                        val newEndMs = msForDateAndTime(dateStr, h2, m2)
                        if (newEndMs <= newStartMs) {
                            Toast.makeText(this, getString(R.string.toast_end_after_start), Toast.LENGTH_SHORT).show()
                        } else {
                            applyBlockEdit(dateStr, block, isBreak, newStartMs, newEndMs)
                            Toast.makeText(this, getString(R.string.toast_kind_block_updated, kind), Toast.LENGTH_SHORT).show()
                            onApplied?.invoke() ?: navigateToPanel(AppPanel.STATS)
                        }
                    },
                    endCal.get(Calendar.HOUR_OF_DAY),
                    endCal.get(Calendar.MINUTE),
                    is24
                ).apply { setTitle(getString(R.string.block_end)) }.show()
            },
            startCal.get(Calendar.HOUR_OF_DAY),
            startCal.get(Calendar.MINUTE),
            is24
        ).apply { setTitle(getString(R.string.block_start)) }.show()
    }

    internal fun showDevTimelineEditor() {
        DeveloperToolsHelper.showFullDevConsoleDialog(this, themeCoordinator)
    }


    private fun formatBlockRow(startMs: Long, endMs: Long, secs: Long): String {
        val dur = when {
            secs >= 3600 -> getString(R.string.duration_h_m, secs / 3600, (secs % 3600) / 60)
            secs >= 60 -> getString(R.string.duration_m, secs / 60)
            else -> getString(R.string.duration_s, secs)
        }
        return "${TimeFormat.formatWallClock(this, startMs)} \u2013 ${TimeFormat.formatWallClock(this, endMs)} \u00B7 $dur"
    }

    private fun formatDuration(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        return when {
            h > 0 && m > 0 -> getString(R.string.duration_h_m, h, m)
            h > 0 -> getString(R.string.duration_h, h)
            m > 0 -> getString(R.string.duration_m, m)
            else -> getString(R.string.duration_m, 0)
        }
    }

    private fun focusBlockLabels(): Array<String> {
        if (TimeFormat.is24Hour(this)) {
            return Array(12) { i -> "${i * 2}-${i * 2 + 2}" }
        }
        fun short12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "a" else "p"}"
        }
        fun full12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "am" else "pm"}"
        }
        return Array(12) { i -> "${short12(i * 2)}-${full12(i * 2 + 2)}" }
    }

    private fun focusBlockStartLabels(): Array<String> {
        if (TimeFormat.is24Hour(this)) {
            return Array(12) { i -> "${i * 2}" }
        }
        fun short12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "a" else "p"}"
        }
        return Array(12) { i -> short12(i * 2) }
    }

    private fun focusBlockRangeLabel(b: Int): String {
        val s = b * 2
        val e = s + 2
        if (TimeFormat.is24Hour(this)) return "$s:00\u2013$e:00"
        fun h12(h: Int): String {
            val hh = if (h % 12 == 0) 12 else h % 12
            return "$hh${if (h < 12) "am" else "pm"}"
        }
        return "${h12(s)} \u2013 ${h12(e)}"
    }

    private fun fillBlockRows(container: LinearLayout, sessions: List<BlockInfo>, breaks: List<BlockInfo>, onDelete: ((BlockInfo, Boolean) -> Unit)? = null) {
        val rows = ArrayList<Pair<BlockInfo, Boolean>>()
        for (s in sessions) rows.add(Pair(s, false))
        for (b in breaks) rows.add(Pair(b, true))
        rows.sortBy { it.first.startMs }
        var prevWasBreak = false
        val allSubs = SubjectTagManager.getAllSubjects(this)
        for ((b, isBreak) in rows) {
            val blockLabel: String
            val blockColor: Int
            if (isBreak) {
                blockLabel = if (b.manual) getString(R.string.block_manual) else getString(R.string.block_break)
                blockColor = themeCoordinator.secondaryColor
            } else {
                val matchedSub = if (b.subjectId != null) {
                    allSubs.find { it.id == b.subjectId } ?: SubjectTag(b.subjectId, b.subjectName ?: "General", "📖", b.subjectColor ?: "#6366F1")
                } else if (b.subjectName != null && b.subjectName.isNotBlank()) {
                    allSubs.find { it.name.equals(b.subjectName, ignoreCase = true) } ?: SubjectTag("custom_${b.subjectName}", b.subjectName, "📖", b.subjectColor ?: "#6366F1")
                } else null

                blockLabel = if (matchedSub != null) {
                    "${matchedSub.iconEmoji} ${matchedSub.name}"
                } else {
                    if (b.manual) getString(R.string.block_manual) else "⏱ Focus"
                }

                blockColor = try {
                    if (matchedSub != null && matchedSub.colorHex.isNotEmpty()) Color.parseColor(matchedSub.colorHex)
                    else themeCoordinator.primaryColor
                } catch (_: Exception) {
                    themeCoordinator.primaryColor
                }
            }

            if (onDelete == null || b.running) {
                container.addView(TextView(this).apply {
                    text = blockLabel + "  " + formatBlockRow(b.startMs, b.endMs, b.secs)
                    setTextColor(blockColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.95f
                    setPadding(0, if (prevWasBreak) dp(12) else dp(3), 0, 0)
                })
            } else {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, if (prevWasBreak) dp(12) else dp(3), 0, 0)
                }
                row.addView(TextView(this).apply {
                    text = blockLabel + "  " + formatBlockRow(b.startMs, b.endMs, b.secs)
                    setTextColor(blockColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.95f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "\u2715"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    alpha = 0.55f
                    setPadding(dp(10), 0, 0, 0)
                    setOnClickListener { onDelete(b, isBreak) }
                })
                container.addView(row)
            }
            prevWasBreak = isBreak
        }
    }

    private fun confirmDeleteBlock(dateStr: String, block: BlockInfo, isBreak: Boolean, onDone: () -> Unit = {}) {
        val kind = if (isBreak) "break" else "focus"
        showConfirmDialog(
            getString(R.string.confirm_delete_block, kind),
            getString(R.string.confirm_delete_block_msg, formatBlockRow(block.startMs, block.endMs, block.secs))
        ) {
            TimelineLogger.deleteBlock(this, block.startMs, block.endMs)
            statsEngine.forceReconcileDayTotals(dateStr)
            if (!isBreak) {
                val s = SubjectTagManager.getSelectedSubject(this)
                val curSubjMap = SubjectTagManager.getSubjectDurationsForDate(this, dateStr)
                val curSubjSecs = curSubjMap[s.id] ?: 0L
                val deduct = Math.min(curSubjSecs, block.secs)
                if (deduct > 0) SubjectTagManager.recordSubjectStudyTime(this, s.id, -deduct, dateStr)
            }
            statsDirty = true
            statsSnapshotCache = null
            tabPageCache.clear()
            recalculateStreak()
            checkCelebration()
            StudyWidgetProvider.refresh(this)
            Toast.makeText(this, getString(R.string.toast_kind_block_deleted, kind), Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    internal fun confirmDeleteDay(dateStr: String, label: String) {
        showConfirmDialog(
            getString(R.string.confirm_delete_day, label),
            getString(R.string.confirm_delete_day_msg)
        ) {
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().apply {
                remove("${dateStr}_focus_total")
                remove("${dateStr}_break_total")
                remove("${dateStr}_focus_manual")
                remove("${dateStr}_break_manual")
            }.apply()
            TimelineLogger.deleteDay(this, dateStr)
            SubjectTagManager.clearTodaySubjectDurations(this, dateStr)
            Toast.makeText(this, getString(R.string.toast_day_deleted), Toast.LENGTH_SHORT).show()
            recalculateStreak()
            navigateToPanel(AppPanel.STATS)
        }
    }

    internal fun showDayDialog(dateStr: String, label: String) {
        val shared = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val todayStr = dateKeyFmt.format(Date())
        val focusSecs = shared.getLong("${dateStr}_focus_total", 0L) + (if (dateStr == todayStr) shared.getLong("accumulatedStudy", 0L) else 0L)
        val breakSecs = shared.getLong("${dateStr}_break_total", 0L) + (if (dateStr == todayStr) currentBreakSeconds else 0L)
        val (allSessions, allBreaks) = dayBlocks(dateStr)
        val sessions = allSessions.filter { it.secs >= 60L }
        val breaks = allBreaks.filter { it.secs >= 60L }
        val longest = sessions.maxOfOrNull { it.secs } ?: 0L
        val goal = resolveGoalFor(dateStr)
        val goalReached = goal > 0L && focusSecs >= goal
        val goalColor = if (goalReached) 0xFF43D36E.toInt() else 0xFFFF4D4D.toInt()

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        content.addView(TextView(this).apply {
            text = label
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            letterSpacing = 0.1f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = if (goalReached) {
                getString(R.string.goal_reached_yes)
            } else {
                val pct = if (goal > 0L) (focusSecs.toFloat() / goal.toFloat() * 100f).toInt() else 0
                val remaining = max(0L, goal - focusSecs)
                val achieved = getString(R.string.cal_goal_pct, pct)
                val toGo = getString(R.string.x_to_go, formatGoalLabel(remaining))
                android.text.SpannableStringBuilder()
                    .append(achieved, android.text.style.ForegroundColorSpan(0xFF43D36E.toInt()), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append("  \u00B7  ", android.text.style.ForegroundColorSpan(goalColor), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append(toGo, android.text.style.ForegroundColorSpan(goalColor), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            setTextColor(goalColor)
            alpha = 1f
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
        })
        if (goal > 0L) {
            val barPct = (focusSecs.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
            val bar = FrameLayout(this).apply {
                background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(tintedColor(themeCoordinator.textColor, 26)) }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { setMargins(0, dp(7), 0, 0) }
            }
            val fill = View(this).apply {
                background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(goalColor) }
            }
            bar.addView(fill, FrameLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.START))
            bar.post {
                val target = (bar.width * barPct).toInt()
                if (target > 0) {
                    val lp = fill.layoutParams as FrameLayout.LayoutParams
                    lp.width = target.coerceAtLeast(dp(6))
                    fill.layoutParams = lp
                }
            }
            content.addView(bar)
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.focus_break_summary, focusSecs / 3600, (focusSecs % 3600) / 60, breakSecs / 3600, (breakSecs % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.sessions_summary, sessions.size, longest / 3600, (longest % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 12f
            setPadding(0, dp(2), 0, dp(2))
        })
        if (sessions.isEmpty() && breaks.isEmpty()) {
            content.addView(TextView(this).apply {
                text = getString(R.string.no_session_log_day)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(10), 0, 0)
            })
        } else {
            val logsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, 0)
            }
            val logsChevron = TextView(this).apply { text = "\u25BE"; textSize = 13f; setTextColor(themeCoordinator.primaryColor) }
            logsRow.addView(TextView(this).apply {
                text = getString(R.string.cal_see_logs)
                setTextColor(themeCoordinator.primaryColor)
                textSize = 14f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            })
            logsRow.addView(LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(dp(6), 0) })
            logsRow.addView(logsChevron)
            val logsBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(0, dp(4), 0, 0)
            }
            fillBlockRows(logsBox, sessions, breaks, onDelete = { b, isBrk -> confirmDeleteBlock(dateStr, b, isBrk) { dialog.dismiss() } })
            content.addView(logsRow)
            content.addView(logsBox)
            logsRow.setOnClickListener {
                val show = logsBox.visibility != View.VISIBLE
                logsBox.visibility = if (show) View.VISIBLE else View.GONE
                logsChevron.text = if (show) "\u25B4" else "\u25BE"
            }
        }

        val seePieChartBtn = TextView(this).apply {
            text = "See Full Chart 📊"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 16f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener {
                dialog.dismiss()
                showPieChartDetailsModal(dateStr)
            }
        }
        content.addView(seePieChartBtn)
        content.addView(TextView(this).apply {
            text = getString(R.string.btn_close)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showMonthDialog(mName: String, focusSecs: Long, breakSecs: Long) {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        content.addView(TextView(this).apply {
            text = mName
            setTextColor(themeCoordinator.primaryColor)
            textSize = 11f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.focus_break_summary, focusSecs / 3600, (focusSecs % 3600) / 60, breakSecs / 3600, (breakSecs % 3600) / 60)
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(8), 0, 0)
        })

        val shared = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val daySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val todayStr = daySdf.format(Date())
        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val days = shared.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { key ->
                val dStr = key.removeSuffix("_focus_total")
                val parsed = try { daySdf.parse(dStr) } catch (_: Exception) { null }
                if (parsed != null && monthSdf.format(parsed) == mName) dStr to parsed else null
            }
            .sortedBy { it.second.time }

        val dayList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, 0) }
        if (days.isEmpty()) {
            dayList.addView(TextView(this).apply {
                text = getString(R.string.no_days_this_month)
                setTextColor(themeCoordinator.textColor)
                alpha = 0.45f
                textSize = 12f
                setPadding(0, dp(8), 0, 0)
            })
        } else {
            for ((dStr, parsed) in days) {
                var f = shared.getLong("${dStr}_focus_total", 0L)
                var b = shared.getLong("${dStr}_break_total", 0L)
                if (dStr == todayStr) { f += accumulatedStudy; b += currentBreakSeconds }
                if (f <= 0L && b <= 0L) continue
                dayList.addView(TextView(this).apply {
                    text = getString(R.string.day_row_summary, dayLabelSdf.format(parsed), f / 3600, (f % 3600) / 60, b / 3600, (b % 3600) / 60)
                    setTextColor(themeCoordinator.textColor)
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.85f
                    setPadding(0, dp(4), 0, 0)
                })
            }
        }
        val dayScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
            isVerticalScrollBarEnabled = false
        }
        dayScroll.addView(dayList)
        content.addView(dayScroll)

        content.addView(TextView(this).apply {
            text = getString(R.string.btn_close)
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.primaryColor)
            textSize = 12f
            letterSpacing = 0.18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(tintedColor(themeCoordinator.primaryColor, 26)) }
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showSummaryCardPreview() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.window?.attributes?.windowAnimations = R.style.DialogScaleAnimation

        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val currentSessionSecs = sharedPrefs.getLong("accumulatedStudy", 0L)
        val currentBreakSecs = sharedPrefs.getLong("currentBreakSeconds", 0L)
        val dayNameSdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val streakGoalBased = sharedPrefs.getBoolean("streak_uses_daily_goal", false)
        val dateRangeFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Fixed output aspect for the shareable card (9:16 portrait)
        val CARD_ASPECT = 0.5625f

        // Compute stats for a given week (0 = this week, -1 = last week, etc.)
        fun computeWeekStats(weekOffset: Int): WeekStats {
            val mo = WeekHelper.mondayOffset(Calendar.getInstance())
            var totalSecs = 0L
            var breakSecs = 0L
            var bestSecs = 0L
            var bestName = "N/A"
            var bestDate = ""
            var sessionCount = 0
            val daySecs = LongArray(7)
            val dayGoals = LongArray(7)
            val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + weekOffset * 7) }
                val dateStr = sdf.format(c.time)
                var dayFocus = sharedPrefs.getLong("${dateStr}_focus_total", 0L)
                var dayBreak = sharedPrefs.getLong("${dateStr}_break_total", 0L)
                if (weekOffset == 0 && dateStr == todayStr) {
                    dayFocus += currentSessionSecs
                    dayBreak += currentBreakSecs
                }
                totalSecs += dayFocus
                breakSecs += dayBreak
                if (dayFocus > 0L) sessionCount++
                daySecs[i] = dayFocus
                dayGoals[i] = resolveGoalFor(dateStr)
                if (dayFocus > bestSecs) {
                    bestSecs = dayFocus
                    bestName = dayNameSdf.format(c.time)
                    bestDate = dateRangeFormat.format(c.time)
                }
            }

            var prevSecs = 0L
            for (i in 0..6) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + i + (weekOffset - 1) * 7) }
                prevSecs += sharedPrefs.getLong("${sdf.format(c.time)}_focus_total", 0L)
            }

            val vsPrev = if (prevSecs == 0L) {
                getString(R.string.no_previous_data)
            } else {
                val diff = (((totalSecs - prevSecs).toFloat() / prevSecs.toFloat()) * 100).toInt()
                if (diff >= 0) "+$diff%" else "$diff%"
            }

            val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + weekOffset * 7) }
            val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -mo + 6 + weekOffset * 7) }
            val dateRange = "${dateRangeFormat.format(startCal.time)} – ${dateRangeFormat.format(endCal.time)}".uppercase()

            // Streak as of the week's end (capped at today), counting back goal-qualifying days
            var streakEnd = endCal.clone() as Calendar
            val todayCal = Calendar.getInstance()
            if (streakEnd.after(todayCal)) streakEnd = todayCal
            var streak = 0
            var streakCal = streakEnd.clone() as Calendar
            while (streak < 365) {
                val dStr = sdf.format(streakCal.time)
                val dSecs = sharedPrefs.getLong("${dStr}_focus_total", 0L)
                val streakThreshold = if (streakGoalBased) resolveGoalFor(dStr) else 2700L
                if (dSecs >= streakThreshold) streak++ else break
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            return WeekStats(totalSecs, bestSecs, bestName, bestDate, vsPrev, dateRange, breakSecs, streak, totalSecs > 0L || breakSecs > 0L, daySecs, dayGoals, dayLabels, sessionCount)
        }

        // Render card with given stats at the fixed 9:16 aspect
        var currentBitmap: android.graphics.Bitmap? = null
        fun renderCard(stats: WeekStats) {
            val days = (0..6).map { i -> WeeklyCardView.Day(stats.dayLabels[i], stats.daySecs[i], stats.dayGoals[i]) }
            val card = WeeklyCardView(this).apply {
                setData(
                    WeeklyCardView.CardData(
                        dateRange = stats.dateRange,
                        totalSecs = stats.totalSecs,
                        breakSecs = stats.breakSecs,
                        bestName = stats.bestName,
                        bestSecs = stats.bestSecs,
                        streak = stats.streak,
                        vsPrev = stats.vsPrev,
                        sessionCount = stats.sessionCount,
                        days = days,
                        hasData = stats.hasData
                    )
                )
            }
            currentBitmap = renderWeeklyCardBitmap(card, CARD_ASPECT)
        }

        // Compute min/max week bounds
        val allDates = sharedPrefs.all.keys
            .filter { it.endsWith("_focus_total") }
            .mapNotNull { try { sdf.parse(it.removeSuffix("_focus_total")) } catch (_: Exception) { null } }
        val minDate = allDates.minOrNull()
        val minWeekOffset = if (minDate != null) {
            val minCal = Calendar.getInstance().apply { time = minDate }
            val daysFromToday = ((System.currentTimeMillis() - minCal.timeInMillis) / 86400000L).toInt()
            -(daysFromToday / 7) - 1
        } else {
            -52
        }
        val maxWeekOffset = 0 // don't show future weeks

        // Default to last week
        var weekOffset = -1
        var currentStats = computeWeekStats(weekOffset)
        renderCard(currentStats)

        // Scaled preview — fits the screen without scrolling
        val dm = resources.displayMetrics
        val maxW = (dm.widthPixels - dp(64)).coerceAtMost(dp(330))
        val reservedH = dp(60 + 16 + 28 + 24 + 48 + 8 + 56)
        val maxH = (dm.heightPixels - reservedH).coerceAtLeast(dp(280))
        var previewWidth = maxW
        var previewHeight = (previewWidth / CARD_ASPECT).toInt()
        if (previewHeight > maxH) {
            previewHeight = maxH
            previewWidth = (previewHeight * CARD_ASPECT).toInt()
        }

        val dialogRoot = FrameLayout(this)

        val scrollView = android.widget.ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(60), dp(16), dp(16))
        }
        scrollView.addView(contentColumn)

        val previewImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(previewWidth, previewHeight).apply { setMargins(0, 0, 0, dp(28)) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.cd_weekly_card_preview)
            setImageBitmap(currentBitmap)
        }

        contentColumn.addView(previewImageView)

        // Week navigation row
        val dateRangeText = TextView(this).apply {
            text = currentStats.dateRange
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateNavButtons(prev: Button, next: Button) {
            prev.alpha = if (weekOffset <= minWeekOffset) 0.3f else 1f
            next.alpha = if (weekOffset >= maxWeekOffset) 0.3f else 1f
        }

        val prevWeekBtn = Button(this).apply {
            text = "\u25C0"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val nextWeekBtn = Button(this).apply {
            text = "\u25B6"
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        prevWeekBtn.setOnClickListener {
            if (weekOffset > minWeekOffset) {
                weekOffset--
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        nextWeekBtn.setOnClickListener {
            if (weekOffset < maxWeekOffset) {
                weekOffset++
                currentStats = computeWeekStats(weekOffset)
                renderCard(currentStats)
                dateRangeText.text = currentStats.dateRange
                previewImageView.setImageBitmap(currentBitmap)
            }
            updateNavButtons(prevWeekBtn, nextWeekBtn)
        }

        // Set initial button states
        updateNavButtons(prevWeekBtn, nextWeekBtn)

        val weekNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(24))
            }
        }
        weekNavRow.addView(prevWeekBtn)
        weekNavRow.addView(dateRangeText)
        weekNavRow.addView(nextWeekBtn)
        contentColumn.addView(weekNavRow)

        // Action buttons
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(previewWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Loading overlay indicator
        val loadingOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setBackgroundColor(tintedColor(themeCoordinator.bgColor, 220))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        loadingOverlay.addView(android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
        })
        loadingOverlay.addView(TextView(this).apply {
            text = getString(R.string.saving)
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            setPadding(0, dp(12), 0, 0)
        })

        fun getSafeBitmap() = currentBitmap

        val closeBtn = Button(this).apply {
            text = getString(R.string.btn_close_lower)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 40), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val saveBtn = Button(this).apply {
            text = getString(R.string.btn_save)
            setTextColor(themeCoordinator.bgColor)
            background = themeCoordinator.createButtonBackground(themeCoordinator.primaryColor)
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    if (bmp != null) saveBitmapToMediaStore(bmp)
                    handler.post { loadingOverlay.visibility = View.GONE; dialog.dismiss() }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(0, 0, dp(8), 0)
            }
        }

        val shareBtn = Button(this).apply {
            text = getString(R.string.btn_share)
            setTextColor(themeCoordinator.bgColor)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(themeCoordinator.primaryColor)
            }
            setOnClickListener {
                val bmp = getSafeBitmap()
                loadingOverlay.visibility = View.VISIBLE
                Thread {
                    var shared = false
                    if (bmp != null) {
                        val uri = writeBitmapToCache(bmp)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(shareIntent, getString(R.string.share_chooser_title))
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            handler.post {
                                loadingOverlay.visibility = View.GONE
                                startActivity(chooser)
                            }
                            shared = true
                        }
                    }
                    if (!shared) {
                        handler.post {
                            loadingOverlay.visibility = View.GONE
                            Toast.makeText(this@MainActivity, getString(R.string.toast_share_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }

        actionRow.addView(closeBtn)
        actionRow.addView(saveBtn)
        actionRow.addView(shareBtn)
        contentColumn.addView(actionRow)

        dialogRoot.addView(scrollView)
        dialogRoot.addView(loadingOverlay)

        dialog.setContentView(dialogRoot)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.show()
    }

    data class WeekStats(
        val totalSecs: Long,
        val bestSecs: Long,
        val bestName: String,
        val bestDate: String,
        val vsPrev: String,
        val dateRange: String,
        val breakSecs: Long,
        val streak: Int,
        val hasData: Boolean,
        val daySecs: LongArray,
        val dayGoals: LongArray,
        val dayLabels: Array<String>,
        val sessionCount: Int
    )

    private fun renderViewToBitmap(view: View): android.graphics.Bitmap {
        val w = dp(360)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val h = view.measuredHeight
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun saveBitmapToMediaStore(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val filename = "StudySummary_${System.currentTimeMillis()}.png"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/StudyTimer")
                }
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    private fun renderWeeklyCardBitmap(view: WeeklyCardView, aspect: Float): android.graphics.Bitmap {
        val w = 1080
        val h = (w / aspect).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        return bitmap
    }

    private fun writeBitmapToCache(bitmap: android.graphics.Bitmap): Uri? {
        return try {
            val dir = java.io.File(cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "StudySummary_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            }
            androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (_: Exception) {
            null
        }
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
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        textContainer.addView(TextView(this).apply {
            text = subtitle
            setTextColor(themeCoordinator.textColor)
            alpha = 0.5f
            textSize = 12f
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
            val sharedPrefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            accumulatedStudy = sharedPrefs.getLong("accumulatedStudy", 0L)
            currentBreakSeconds = sharedPrefs.getLong("currentBreakSeconds", 0L)
            currentTimerState = TimerState.valueOf(sharedPrefs.getString("timerState", "IDLE") ?: "IDLE")
            timerMode = sharedPrefs.getString("timer_mode", "STOPWATCH") ?: "STOPWATCH"
            focusCountdownSecs = sharedPrefs.getLong("focus_countdown_secs", 1500L)
            focusRemainingSecs = sharedPrefs.getLong("focus_remaining_secs", 0L)
            prePauseState = runCatching { TimerState.valueOf(sharedPrefs.getString("pre_pause_state", "STUDYING") ?: "STUDYING") }.getOrDefault(TimerState.STUDYING)

            updateKeepScreenOn()

            val dayBucket = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86400000L
            if (dayBucket != lastDayBucket) {
                lastDayBucket = dayBucket
                cachedTodayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
            }

            maybeFireForegroundGoalPing(cachedTodayStr)

            // Check if TimerService stored a pending lecture-switch request
            if (!ongoingLectureDialogShowing
                && sharedPrefs.getBoolean("pending_switch_to_lecture", false)
                && (currentTimerState == TimerState.STUDYING || currentTimerState == TimerState.BREAK)) {
                sharedPrefs.edit().remove("pending_switch_to_lecture").apply()
                showSwitchToLectureDialog()
            }


            if (currentPanel == AppPanel.FOCUS) {
                val isLectureModeActive = sharedPrefs.getBoolean("lecture_mode_enabled", false)
                val isStudying = currentTimerState == TimerState.STUDYING ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
                val isBreaking = currentTimerState == TimerState.BREAK ||
                    (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
                val showCountdown = (timerMode == "COUNTDOWN" || isLectureModeActive) && isStudying

                studyTimerDisplay.text = when {
                    showCountdown -> formatCountdown(focusRemainingSecs)
                    else -> formatTime(accumulatedStudy)
                }

                val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
                val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

                breakTimerDisplay.text = when {
                    isBreaking && timerMode == "COUNTDOWN" && breakCountdownSecs > 0L -> formatCountdown(breakRemainingSecs)
                    else -> formatTime(currentBreakSeconds)
                }

                if (lastIsBreakingState != isBreaking) {
                    lastIsBreakingState = isBreaking
                    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    if (isBreaking) {
                        breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                        breakTimerDisplay.textSize = if (isLandscape) 96f else 54f
                        breakTimerDisplay.setPadding(0, 0, 0, 5)

                        studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                        studyTimerDisplay.textSize = if (isLandscape) 24f else 20f
                        studyTimerDisplay.setPadding(0, 0, 0, if (isLandscape) 20 else 40)
                    } else {
                        studyTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                        studyTimerDisplay.textSize = if (isLandscape) 96f else 54f
                        studyTimerDisplay.setPadding(0, 0, 0, 5)

                        breakTimerDisplay.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                        breakTimerDisplay.textSize = if (isLandscape) 24f else 20f
                        breakTimerDisplay.setPadding(0, 0, 0, if (isLandscape) 20 else 40)
                    }
                }

                updateTimerRing(showCountdown)

                val showPause = sharedPrefs.getBoolean("show_pause_button", true)
                if (timerStateChanged || isZenModeActive != lastZenModeState || showPause != lastShowPauseState) {
                    lastZenModeState = isZenModeActive
                    lastShowPauseState = showPause
                    updateVisualStyles()
                }
                updatePauseBlink()
                if (currentTimerState == TimerState.STUDYING && accumulatedStudy > 0L) {
                    checkCelebration()
                }
            }
            handler.postDelayed(updateRunnable, 500)
        }
        handler.post(updateRunnable)
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

    internal fun handleStateToggle() {
        if (currentTimerState == TimerState.STUDYING && timerMode == "COUNTDOWN") {
            // Save current remaining focus countdown before break so returning to focus resumes from this position
            getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit()
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

    internal fun showBreakDurationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val prefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val prevRemaining = prefs.getLong("break_remaining_secs", 0L)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(this).apply {
            text = "☕ Choose Break Duration"
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Select duration for your resting break session:"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 13f
            setPadding(0, dp(4), 0, dp(14))
        })

        if (prevRemaining > 0L) {
            val resumeBtn = TextView(this).apply {
                text = "▶ Continue Previous Break (${formatCountdown(prevRemaining)} left)"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createButtonBackground(themeCoordinator.secondaryColor)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
                setOnClickListener {
                    dialog.dismiss()
                    handleStartBreakOnly(prevRemaining)
                }
            }
            content.addView(resumeBtn)
        }

        val options = listOf(
            "5 Min" to 300L,
            "10 Min" to 600L,
            "15 Min" to 900L,
            "20 Min" to 1200L,
            "30 Min" to 1800L
        )

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (opt in options) {
            val row = TextView(this).apply {
                text = opt.first
                setTextColor(themeCoordinator.textColor)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.secondaryColor, 80), 16f)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                setOnClickListener {
                    dialog.dismiss()
                    prefs.edit()
                        .putLong("break_countdown_secs", opt.second)
                        .putLong("break_remaining_secs", opt.second)
                        .apply()
                    handleStartBreakOnly(opt.second)
                }
            }
            grid.addView(row)
        }
        content.addView(grid)

        val cancelBtn = Button(this).apply {
            text = getString(R.string.btn_cancel_upper)
            setTextColor(themeCoordinator.textColor)
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 50f)
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
        }
        content.addView(cancelBtn)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun updateTimerRing(showCountdown: Boolean) {
        if (!::timerRing.isInitialized || timerRing.visibility != View.VISIBLE) return
        if (timerMode != "COUNTDOWN") return
        val base = themeCoordinator.textColor
        val track = (base and 0x00FFFFFF) or 0x1F000000
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val breakCountdownSecs = sharedPrefs.getLong("break_countdown_secs", 300L)
        val breakRemainingSecs = sharedPrefs.getLong("break_remaining_secs", 0L)

        val studying = currentTimerState == TimerState.STUDYING ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.STUDYING)
        val breaking = currentTimerState == TimerState.BREAK ||
            (currentTimerState == TimerState.PAUSED && prePauseState == TimerState.BREAK)
        when {
            studying && showCountdown && focusCountdownSecs > 0 -> {
                timerRing.setProgress(focusRemainingSecs.toFloat() / focusCountdownSecs, themeCoordinator.primaryColor, track)
            }
            breaking && timerMode == "COUNTDOWN" && breakCountdownSecs > 0L -> {
                timerRing.setProgress((breakRemainingSecs.toFloat() / breakCountdownSecs).coerceIn(0f, 1f), themeCoordinator.secondaryColor, track)
            }
            breaking -> {
                timerRing.setProgress(1f, themeCoordinator.secondaryColor, track)
            }
            else -> {
                timerRing.setProgress(0f, themeCoordinator.primaryColor, track)
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
                mainBtn.setTextColor(themeCoordinator.bgColor)
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
        Thread { backupManager.runSilentAutoBackup() }.start()
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

    internal fun showLectureEndDialog() {
        if (isFinishing || isDestroyed) return
        lectureEndDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        lectureEndDialog = dialog

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
        }

        val icon = TextView(this).apply {
            text = "\uD83C\uDF93"
            textSize = 40f
            gravity = Gravity.CENTER
        }
        root.addView(icon)

        val title = TextView(this).apply {
            text = "Has the lecture ended?"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(title)

        val sub = TextView(this).apply {
            text = "Start your break now, or extend the duration if class is running over."
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(sub)

        val autoBreakText = TextView(this).apply {
            text = "Auto-starting break in 15s..."
            setTextColor(themeCoordinator.secondaryColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(6))
        }
        root.addView(autoBreakText)

        val autoBreakProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 15
            progress = 15
            progressTintList = android.content.res.ColorStateList.valueOf(themeCoordinator.secondaryColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { setMargins(0, 0, 0, dp(16)) }
        }
        root.addView(autoBreakProgress)

        val startBreakBtn = Button(this).apply {
            text = "Start Break Now"
            setTextColor(themeCoordinator.bgColor)
            background = rippleBackground(themeCoordinator.secondaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setOnClickListener {
                dialog.dismiss()
                handleStartBreak()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
        }
        root.addView(startBreakBtn)

        val extendRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val extend5Btn = Button(this).apply {
            text = "+5 Mins"
            setTextColor(themeCoordinator.primaryColor)
            background = outlinedButtonBackground()
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setOnClickListener {
                dialog.dismiss()
                handleExtendLecture(300L)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(4), 0) }
        }
        extendRow.addView(extend5Btn)

        val extend10Btn = Button(this).apply {
            text = "+10 Mins"
            setTextColor(themeCoordinator.primaryColor)
            background = outlinedButtonBackground()
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setOnClickListener {
                dialog.dismiss()
                handleExtendLecture(600L)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, 0, 0) }
        }
        extendRow.addView(extend10Btn)
        root.addView(extendRow)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val startTime = System.currentTimeMillis() / 1000
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (!dialog.isShowing) return
                val elapsed = ((System.currentTimeMillis() / 1000) - startTime).toInt()
                val remaining = (15 - elapsed).coerceAtLeast(0)
                autoBreakProgress.progress = remaining
                autoBreakText.text = "Auto-starting break in ${remaining}s..."
                if (remaining <= 0) {
                    dialog.dismiss()
                    handleStartBreak()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

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
        val timerColor = if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor
        val mainBtnTextColor = if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.bgColor
        when (currentTimerState) {
            TimerState.IDLE -> {
                statusBadge.text = getString(R.string.ready_to_track)
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                if (timerMode == "COUNTDOWN") {
                    mainBtn.text = getString(R.string.start_focus)
                    mainBtn.setTextColor(mainBtnTextColor)
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor)
                    pauseBtn.visibility = View.VISIBLE
                    pauseBtn.text = "☕ START BREAK"
                    pauseBtn.setTextColor(themeCoordinator.textColor)
                    pauseBtn.background = outlinedButtonBackground()
                    pauseBtn.setOnClickListener { showBreakDurationDialog() }
                    stopBtn.visibility = View.GONE
                } else {
                    mainBtn.text = getString(R.string.start_focus); mainBtn.setTextColor(mainBtnTextColor)
                    mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = View.GONE; stopBtn.visibility = View.GONE 
                }
            }
            TimerState.STUDYING -> {
                statusBadge.text = if (timerMode == "LECTURE") "LECTURE IN PROGRESS" else getString(R.string.learning_time)
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.primaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                studyTimerDisplay.setTextColor(timerColor)
                breakTimerDisplay.setTextColor(themeCoordinator.textColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE
                mainBtn.text = if (timerMode == "LECTURE") "End Lecture & Start Break" else getString(R.string.take_a_break)
                mainBtn.setTextColor(mainBtnTextColor)
                mainBtn.background = rippleBackground(themeCoordinator.secondaryColor); pauseBtn.visibility = pauseButtonVisibility(); pauseBtn.text = getString(R.string.btn_pause); pauseBtn.setOnClickListener { handlePause() }; pauseBtn.setTextColor(themeCoordinator.textColor); pauseBtn.background = outlinedButtonBackground(); stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
            TimerState.LECTURE_ENDED -> {
                statusBadge.text = "LECTURE ENDED"
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                studyTimerDisplay.setTextColor(timerColor)
                if (!isZenModeActive) breakTimerDisplay.visibility = View.GONE
                mainBtn.text = "Start Break Now"; mainBtn.setTextColor(mainBtnTextColor)
                mainBtn.background = rippleBackground(themeCoordinator.secondaryColor); pauseBtn.visibility = View.GONE; stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
                checkAndShowLecturePrompt()
            }
            TimerState.BREAK -> {
                statusBadge.text = getString(R.string.break_in_progress)
                statusBadge.setTextColor(if (themeCoordinator.isBubbleStyle()) 0xFFFFFFFF.toInt() else themeCoordinator.secondaryColor)
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.secondaryColor, 30f)
                studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor) 
                breakTimerDisplay.setTextColor(themeCoordinator.secondaryColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE; mainBtn.text = getString(R.string.resume_deep_focus); mainBtn.setTextColor(mainBtnTextColor)
                mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = pauseButtonVisibility(); pauseBtn.text = getString(R.string.btn_pause); pauseBtn.setOnClickListener { handlePause() }; pauseBtn.setTextColor(themeCoordinator.textColor); pauseBtn.background = outlinedButtonBackground(); stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
            TimerState.PAUSED -> {
                statusBadge.text = getString(R.string.paused)
                statusBadge.setTextColor(0xFFFFFFFF.toInt())
                statusBadge.background = themeCoordinator.createGlassChip(themeCoordinator.primaryColor, 30f)
                studyTimerDisplay.setTextColor(if (pureWhiteTimerEnabled()) 0xFFFFFFFF.toInt() else themeCoordinator.textColor)
                breakTimerDisplay.setTextColor(themeCoordinator.textColor); if (!isZenModeActive) breakTimerDisplay.visibility = View.VISIBLE; mainBtn.text = getString(R.string.resume); mainBtn.setTextColor(mainBtnTextColor)
                mainBtn.background = rippleBackground(themeCoordinator.primaryColor); pauseBtn.visibility = View.GONE; stopBtn.visibility = View.VISIBLE; stopBtn.ringColor = themeCoordinator.primaryColor
            }
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
        applyImmersiveModeForLandscape()
        StudyWidgetProvider.refresh(this)
        checkOngoingScheduledLecturePrompt()
        checkCelebration()
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

    private fun showScheduledLecturePromptDialog(item: LectureScheduleItem, remainingSecs: Long, skipKey: String) {
        ongoingLectureDialogShowing = true
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = themeCoordinator.createDialogBackground(28f)
        }

        root.addView(TextView(this).apply {
            text = "🎓 Scheduled Class Ongoing"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        val remainingStr = if (remainingSecs >= 3600) "${remainingSecs / 3600}h ${(remainingSecs % 3600) / 60}m" else "${(remainingSecs % 3600) / 60}m"
        root.addView(TextView(this).apply {
            text = "Class: ${item.title}\nSchedule: ${item.startTime} - ${item.endTime}\nRemaining: $remainingStr\n\nThe class has already started. Would you like to continue the class or skip?"
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
            text = "Skip"
            setTextColor(themeCoordinator.textColor)
            alpha = 0.6f
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener {
                getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).edit().putBoolean(skipKey, true).apply()
                ongoingLectureDialogShowing = false
                dialog.dismiss()
            }
        })

        btnRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })

        btnRow.addView(TextView(this).apply {
            text = "Continue Class"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 16f)
            setOnClickListener {
                ongoingLectureDialogShowing = false
                dialog.dismiss()

                val prefs = getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
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
                    .apply()

                currentTimerState = TimerState.STUDYING
                timerMode = "LECTURE"
                focusRemainingSecs = remainingSecs
                focusCountdownSecs = remainingSecs

                val intent = Intent(this@MainActivity, TimerService::class.java).apply {
                    action = TimerService.ACTION_TOGGLE
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

    internal fun applyTrueFullscreenMode() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val isZenModeEnabled = sharedPrefs.getBoolean("true_fullscreen_landscape", false)

        if (!isLandscape) {
            isZenModeActive = false
            panelContainer.setOnClickListener(null)
            return
        }

        if (isZenModeEnabled) {
            isZenModeActive = true
            navHeader.visibility = View.GONE
            statusBadgeContainer.visibility = View.GONE
            breakTimerDisplay.visibility = View.GONE
            controlActionContainer.visibility = View.GONE
            studyTimerDisplay.textSize = 110f

            panelContainer.setOnClickListener {
                if (controlActionContainer.visibility == View.GONE) {
                    navHeader.visibility = View.VISIBLE
                    statusBadgeContainer.visibility = View.VISIBLE
                    breakTimerDisplay.visibility = View.VISIBLE
                    controlActionContainer.visibility = View.VISIBLE
                    studyTimerDisplay.textSize = 64f
                } else {
                    navHeader.visibility = View.GONE
                    statusBadgeContainer.visibility = View.GONE
                    breakTimerDisplay.visibility = View.GONE
                    controlActionContainer.visibility = View.GONE
                    studyTimerDisplay.textSize = 110f
                }
            }
        } else {
            isZenModeActive = false
            navHeader.visibility = View.VISIBLE
            statusBadgeContainer.visibility = View.VISIBLE
            breakTimerDisplay.visibility = View.VISIBLE
            controlActionContainer.visibility = View.VISIBLE
            studyTimerDisplay.textSize = 64f
            panelContainer.setOnClickListener(null)
        }
    }

    internal fun applyImmersiveModeForLandscape() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            showSystemUI()
            return
        }
        val sharedPrefs = getSharedPreferences("StudyTimerPrefs", MODE_PRIVATE)
        val immersiveEnabled = sharedPrefs.getBoolean("true_fullscreen_landscape", false)
        if (immersiveEnabled) {
            hideSystemUI()
        } else {
            showSystemUI()
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
    }

    val customAudioPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            AmbientSoundEngine.selectPreset(this, AmbientSoundEngine.Preset.CUSTOM, uri)
            buildFocusPanel()
        }
    }

    fun showSubjectPickerDialog() {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(22), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(420)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(this).apply {
                text = "🏷️ Select Study Subject"
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(title)

            val subtitle = TextView(this).apply {
                text = "Tap to select · Hold any subject to delete"
                textSize = 12f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.5f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(subtitle)

            val scrollContainer = android.widget.ScrollView(this).apply {
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(280)
                )
            }

            val subjectsListContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val currentSelectedSubj = SubjectTagManager.getSelectedSubject(this)
            val subjectsList = SubjectTagManager.getAllSubjects(this)
            for (subj in subjectsList) {
                val isSelected = subj.id == currentSelectedSubj.id
                val btn = Button(this).apply {
                    text = if (isSelected) "✓  ${subj.iconEmoji} ${subj.name}" else "${subj.iconEmoji} ${subj.name}"
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        val baseColor = try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }
                        setColor(baseColor)
                        cornerRadius = dp(14).toFloat()
                        if (isSelected) {
                            setStroke(dp(2), Color.WHITE)
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                    setOnClickListener {
                        SubjectTagManager.setSelectedSubject(this@MainActivity, subj.id)
                        dialog.dismiss()
                        buildFocusPanel()
                    }
                    setOnLongClickListener {
                        if (subj.id != "general") {
                            dialog.dismiss()
                            showDeleteSubjectConfirmDialog(subj)
                        } else {
                            Toast.makeText(this@MainActivity, "General subject cannot be deleted", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                }
                subjectsListContainer.addView(btn)
            }

            scrollContainer.addView(subjectsListContainer)
            container.addView(scrollContainer)

            val addBtn = Button(this).apply {
                text = "➕ Add Custom Subject"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.primaryColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 40), 14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(14), 0, 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showAddCustomSubjectDialog()
                }
            }
            container.addView(addBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    private fun showDeleteSubjectConfirmDialog(subj: SubjectTag) {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(22), dp(22), dp(22), dp(22))
            }

            val title = TextView(this).apply {
                text = "Delete '${subj.name}' Subject?"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(8))
            }
            container.addView(title)

            val msg = TextView(this).apply {
                text = "Are you sure you want to remove ${subj.iconEmoji} ${subj.name}? Existing recorded stats for this subject will remain saved."
                textSize = 13f
                setTextColor(themeCoordinator.textColor)
                alpha = 0.7f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(18))
            }
            container.addView(msg)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val cancelBtn = Button(this).apply {
                text = "Cancel"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, dp(6), 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    showSubjectPickerDialog()
                }
            }

            val delBtn = Button(this).apply {
                text = "Delete"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EF4444"))
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(6), 0, 0, 0)
                }
                setOnClickListener {
                    SubjectTagManager.removeSubject(this@MainActivity, subj.id)
                    dialog.dismiss()
                    buildFocusPanel()
                    showSubjectPickerDialog()
                }
            }

            btnRow.addView(cancelBtn)
            btnRow.addView(delBtn)
            container.addView(btnRow)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showAddCustomSubjectDialog(onSubjectCreated: ((SubjectTag) -> Unit)? = null) {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(24f)
                setPadding(dp(20), dp(20), dp(20), dp(20))
            }

            val title = TextView(this).apply {
                text = "➕ Add New Subject"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val input = android.widget.EditText(this).apply {
                hint = "Subject Name (max 25 chars)"
                setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                setTextColor(themeCoordinator.textColor)
                textSize = 14f
                filters = arrayOf(android.text.InputFilter.LengthFilter(25))
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            container.addView(input)

            var selectedColorHex = SubjectTagManager.generateUniqueColor(this)

            val previewRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(14), 0, dp(8))
            }
            val colorPreviewCircle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    setMargins(0, 0, dp(10), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(selectedColorHex))
                }
            }
            previewRow.addView(colorPreviewCircle)

            val colorLabel = TextView(this).apply {
                text = "Subject Color: $selectedColorHex"
                setTextColor(themeCoordinator.textColor)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }
            previewRow.addView(colorLabel)
            container.addView(previewRow)

            // Preset Swatches Horizontal Row
            val swatchesScroll = android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(0, 0, 0, dp(10))
            }
            val swatchesLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val presetColors = listOf("#6366F1", "#EC4899", "#F97316", "#06B6D4", "#10B981", "#EAB308", "#8B5CF6", "#EF4444", "#3B82F6", "#14B8A6")
            for (hex in presetColors) {
                val swatch = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        setStroke(dp(2), if (hex.equals(selectedColorHex, true)) Color.WHITE else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        selectedColorHex = hex
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(Color.parseColor(hex))
                        colorLabel.text = "Subject Color: $selectedColorHex"
                    }
                }
                swatchesLayout.addView(swatch)
            }
            swatchesScroll.addView(swatchesLayout)
            container.addView(swatchesScroll)

            // Custom Hue Color Slider Bar
            val hueLabel = TextView(this).apply {
                text = "🎨 Custom Hue Bar"
                setTextColor(themeCoordinator.textColor)
                alpha = 0.6f
                textSize = 11.5f
                setPadding(0, 0, 0, dp(4))
            }
            container.addView(hueLabel)

            val hueSeekBar = android.widget.SeekBar(this).apply {
                max = 360
                progress = 240
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, prog: Int, fromUser: Boolean) {
                        val colorInt = Color.HSVToColor(floatArrayOf(prog.toFloat(), 0.85f, 0.90f))
                        selectedColorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                        (colorPreviewCircle.background as? GradientDrawable)?.setColor(colorInt)
                        colorLabel.text = "Subject Color: $selectedColorHex"
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            container.addView(hueSeekBar)

            val saveBtn = Button(this).apply {
                text = "Save Subject"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(themeCoordinator.primaryColor)
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(16), 0, 0)
                }
                setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val created = SubjectTagManager.addCustomSubject(this@MainActivity, name, "📚", selectedColorHex)
                        dialog.dismiss()
                        buildFocusPanel()
                        onSubjectCreated?.invoke(created)
                    }
                }
            }
            container.addView(saveBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    fun showAmbientSoundDialog(context: Context = this) {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(22), dp(22), dp(20))
                layoutParams = LinearLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(420)),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val title = TextView(this).apply {
                text = "🎧 Ambient Focus Soundscapes"
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(14))
            }
            container.addView(title)

            val scrollContainer = android.widget.ScrollView(this).apply {
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(280)
                )
            }

            val soundsListContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            for (preset in AmbientSoundEngine.Preset.values()) {
                if (preset == AmbientSoundEngine.Preset.CUSTOM) continue
                val isSelected = AmbientSoundEngine.getActivePreset() == preset
                val btn = Button(this).apply {
                    text = preset.displayName
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else themeCoordinator.textColor)
                    background = if (isSelected) {
                        GradientDrawable().apply {
                            setColor(themeCoordinator.primaryColor)
                            cornerRadius = dp(14).toFloat()
                        }
                    } else {
                        themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 14f)
                    }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                    setOnClickListener {
                        AmbientSoundEngine.selectPreset(this@MainActivity, preset)
                        dialog.dismiss()
                        buildFocusPanel()
                    }
                }
                soundsListContainer.addView(btn)
            }

            scrollContainer.addView(soundsListContainer)
            container.addView(scrollContainer)

            val importBtn = Button(this).apply {
                text = "📁 Import Custom Audio File (.mp3/.wav)"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(themeCoordinator.accentColor)
                background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.accentColor, 40), 14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dp(14), 0, 0)
                }
                setOnClickListener {
                    dialog.dismiss()
                    customAudioPickerLauncher.launch("audio/*")
                }
            }
            container.addView(importBtn)

            dialog.setContentView(container)
            dialog.show()
        } catch (_: Exception) {}
    }

    private fun showPieChartDetailsModal(initialDateKey: String = SubjectTagManager.getTodayKey()) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        var currentDateKey = initialDateKey
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val todayKey = SubjectTagManager.getTodayKey()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090C15"))
            setPadding(dp(20), dp(36), dp(20), dp(20))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        headerRow.addView(TextView(this).apply {
            text = "📊 Detailed Session Analytics"
            setTextColor(themeCoordinator.textColor)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val closeBtn = TextView(this).apply {
            text = "✕ Close"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 110), 20f)
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(closeBtn)
        rootLayout.addView(headerRow)

        // Date Navigation Row (< 📅 16 Aug 2026 >)
        val dateNavRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        val prevBtn = TextView(this).apply {
            text = "◄"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        val dateTitleBtn = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(themeCoordinator.textColor)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 25), 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8), 0, dp(8), 0) }
        }

        val nextBtn = TextView(this).apply {
            text = "►"
            setTextColor(themeCoordinator.primaryColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.primaryColor, 50), 16f)
        }

        dateNavRow.addView(prevBtn)
        dateNavRow.addView(dateTitleBtn)
        dateNavRow.addView(nextBtn)
        rootLayout.addView(dateNavRow)

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        scrollView.addView(scrollContent)
        rootLayout.addView(scrollView)

        fun updateDateView() {
            val parsedDate = try { sdfKey.parse(currentDateKey) } catch (_: Exception) { Date() } ?: Date()
            val formattedTitle = if (currentDateKey == todayKey) "📅 Today (${sdfDisplay.format(parsedDate)})" else "📅 ${sdfDisplay.format(parsedDate)}"
            dateTitleBtn.text = formattedTitle

            val isToday = (currentDateKey == todayKey)
            nextBtn.alpha = if (isToday) 0.3f else 1.0f
            nextBtn.isEnabled = !isToday

            scrollContent.removeAllViews()

            val topChartCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(20)) }
            }

            val largePieView = SubjectPieChartView(this).apply {
                primaryColor = themeCoordinator.primaryColor
                textColor = themeCoordinator.textColor
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340))
            }

            val (daySessions, dayBreaks) = StatsEngine(this).dayBlocks(currentDateKey)
            val allSubjects = SubjectTagManager.getAllSubjects(this)
            val subjectDurations = HashMap<String, Long>()

            // Aggregate focus durations directly from unified session logs
            for (s in daySessions) {
                if (s.subjectId != null) {
                    subjectDurations[s.subjectId] = (subjectDurations[s.subjectId] ?: 0L) + s.secs
                }
            }

            // Fallback to legacy SubjectTagManager if daySessions is empty
            if (subjectDurations.isEmpty()) {
                val legacy = SubjectTagManager.getSubjectDurationsForDate(this, currentDateKey)
                for ((subId, secs) in legacy) {
                    if (allSubjects.any { it.id == subId }) {
                        subjectDurations[subId] = secs
                    }
                }
            }

            val subjectBreakDurations = SubjectTagManager.getSubjectBreakDurationsForDate(this, currentDateKey)

            val activeSubjectsSorted = allSubjects
                .map { it to (subjectDurations[it.id] ?: 0L) }
                .filter { it.second >= 60L }
                .sortedByDescending { it.second }

            val slicesList = ArrayList<SubjectPieChartView.PieSlice>()

            for ((subj, secs) in activeSubjectsSorted) {
                slicesList.add(SubjectPieChartView.PieSlice(subj.name, subj.iconEmoji, secs.toDouble(), subj.colorHex))
            }

            if (slicesList.isNotEmpty()) {
                largePieView.setData(slicesList)
                topChartCard.addView(largePieView)
            } else {
                topChartCard.addView(TextView(this).apply {
                    text = "📊 No Subject Session Data For This Date"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(40), 0, dp(40))
                })
            }

            scrollContent.addView(topChartCard)

            scrollContent.addView(TextView(this).apply {
                text = "⏱️ DETAILED SESSION LOGS ($currentDateKey)"
                setTextColor(themeCoordinator.primaryColor)
                textSize = 12f
                letterSpacing = 0.15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(4), 0, 0, dp(12))
            })

            val totalSecsAll = subjectDurations.values.sum()

            if (slicesList.isEmpty()) {
                val emptyCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(20), dp(24), dp(20), dp(24))
                    gravity = Gravity.CENTER
                }
                emptyCard.addView(TextView(this).apply {
                    text = "📖 No Active Subject Sessions"
                    setTextColor(themeCoordinator.textColor)
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                emptyCard.addView(TextView(this).apply {
                    text = "No focus activity with subject tags logged for this specific day."
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.6f
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, 0)
                })
                scrollContent.addView(emptyCard)
            } else {
                for (subjPair in activeSubjectsSorted) {
                    val (subj, secs) = subjPair
                    val matchedSessions = daySessions.filter {
                        (it.subjectId != null && it.subjectId == subj.id) ||
                        (it.subjectId == null && subj.id == "general")
                    }

                    val subjCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = themeCoordinator.createCardBackground()
                        setPadding(dp(18), dp(16), dp(18), dp(16))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(12)) }
                    }

                    val headerRowSubj = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val colorCircle = FrameLayout(this).apply {
                        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(try { Color.parseColor(subj.colorHex) } catch (_: Exception) { themeCoordinator.primaryColor }) }
                        layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(10), 0) }
                    }

                    headerRowSubj.addView(colorCircle)
                    headerRowSubj.addView(TextView(this).apply {
                        text = "${subj.iconEmoji} ${subj.name}"
                        setTextColor(themeCoordinator.textColor)
                        textSize = 17f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })

                    val pct = if (totalSecsAll > 0) ((secs.toDouble() / totalSecsAll.toDouble()) * 100).toInt() else 0
                    headerRowSubj.addView(TextView(this).apply {
                        text = "$pct% Share"
                        setTextColor(themeCoordinator.primaryColor)
                        textSize = 13f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    })

                    subjCard.addView(headerRowSubj)

                    val timingStr = if (matchedSessions.isNotEmpty()) {
                        val firstMs = matchedSessions.minOf { it.startMs }
                        val lastMs = matchedSessions.maxOf { it.endMs }
                        "${TimeFormat.formatWallClock(this, firstMs)} \u2013 ${TimeFormat.formatWallClock(this, lastMs)}"
                    } else if (daySessions.isNotEmpty()) {
                        val firstMs = daySessions.minOf { it.startMs }
                        val lastMs = daySessions.maxOf { it.endMs }
                        "${TimeFormat.formatWallClock(this, firstMs)} \u2013 ${TimeFormat.formatWallClock(this, lastMs)}"
                    } else {
                        "N/A"
                    }

                    val hrs = secs / 3600L
                    val mins = (secs % 3600L) / 60L
                    val remSecs = secs % 60L
                    val focusTimeStr = if (hrs > 0) "${hrs}h ${mins}m ${remSecs}s" else "${mins}m ${remSecs}s"

                    val bSecs = subjectBreakDurations[subj.id] ?: 0L
                    val bHrs = bSecs / 3600L
                    val bMins = (bSecs % 3600L) / 60L
                    val bRemSecs = bSecs % 60L
                    val breakTimeStr = if (bHrs > 0) "${bHrs}h ${bMins}m ${bRemSecs}s" else "${bMins}m ${bRemSecs}s"

                    val detailsText = TextView(this).apply {
                        text = "⏱️ Focus Duration: $focusTimeStr\n☕ Break Duration: $breakTimeStr\n⏰ Timing: $timingStr"
                        setTextColor(themeCoordinator.textColor)
                        alpha = 0.75f
                        textSize = 13.5f
                        setLineSpacing(dp(4).toFloat(), 1.1f)
                        setPadding(0, dp(10), 0, 0)
                    }
                    subjCard.addView(detailsText)

                    scrollContent.addView(subjCard)
                }
            }
        }

        prevBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDateKey = sdfKey.format(cal.time)
            updateDateView()
        }

        nextBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val newKey = sdfKey.format(cal.time)
            if (newKey <= todayKey) {
                currentDateKey = newKey
                updateDateView()
            }
        }

        dateTitleBtn.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = try { sdfKey.parse(currentDateKey) } catch(_: Exception) { Date() } ?: Date() }
            android.app.DatePickerDialog(this, { _, y: Int, m: Int, d: Int ->
                val selCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                val selKey = sdfKey.format(selCal.time)
                if (selKey <= todayKey) {
                    currentDateKey = selKey
                    updateDateView()
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        updateDateView()
        dialog.setContentView(rootLayout)
        dialog.show()
    }

}




