package com.madeby.JAI

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeaderboardPanelBuilder(private val host: MainActivity) {

    private val density = host.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    private var selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.DAILY
    private var isLoading = false
    private var hasLoadedOnce = false
    private var currentEntries: List<LeaderboardEntry> = emptyList()
    private var pollJob: kotlinx.coroutines.Job? = null

    fun build(target: android.view.ViewGroup = host.panelContainer) {
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(16), dp(12), dp(16), 0)
        }

        renderLeaderboardUI(root)
        target.addView(root)

        loadLeaderboardData(root, forceRefresh = false)
        startLivePolling(root)
    }

    private fun startLivePolling(root: LinearLayout) {
        pollJob?.cancel()
        pollJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                kotlinx.coroutines.delay(15_000L)
                if (!root.isAttachedToWindow) break
                if (!isLoading) {
                    loadLeaderboardData(root, forceRefresh = true, isSilent = true)
                }
            }
        }
    }

    private fun renderLeaderboardUI(root: LinearLayout) {
        root.removeAllViews()

        // 1. TOP HEADER (Back Button + Title + Live Count + Refresh Button)
        val header = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val backBtn = ImageView(host).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(host.themeCoordinator.textColor)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = host.themeCoordinator.createGlassIconBackground(
                host.tintedColor(host.themeCoordinator.textColor, 25)
            )
            contentDescription = "Back to Timer"
            setOnClickListener {
                host.navigateToPanel(AppPanel.FOCUS)
            }
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                setMargins(0, 0, dp(10), 0)
            }
        }
        header.addView(backBtn)

        val titleCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(host).apply {
            text = "Leaderboard"
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(host.themeCoordinator.textColor)
        }
        titleCol.addView(title)

        val subtitle = TextView(host).apply {
            text = "Global Study Community"
            textSize = 11.5f
            alpha = 0.65f
            setTextColor(host.themeCoordinator.textColor)
        }
        titleCol.addView(subtitle)
        header.addView(titleCol)

        // Live Studying Pill
        val liveCountPill = TextView(host).apply {
            val activeCount = currentEntries.count { it.isStudying }
            text = if (activeCount > 0) "🟢 $activeCount Live" else "⚡ Global"
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (activeCount > 0) Color.parseColor("#4ADE80") else host.themeCoordinator.primaryColor)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (activeCount > 0) Color.argb(40, 74, 222, 128) else host.tintedColor(host.themeCoordinator.primaryColor, 35))
                setStroke(dp(1), if (activeCount > 0) Color.argb(90, 74, 222, 128) else host.tintedColor(host.themeCoordinator.primaryColor, 80))
            }
            setPadding(dp(10), dp(5), dp(10), dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, dp(8), 0) }
        }
        header.addView(liveCountPill)

        val refreshBtn = ImageView(host).apply {
            setImageResource(R.drawable.ic_clock)
            setColorFilter(host.themeCoordinator.primaryColor)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = host.themeCoordinator.createGlassIconBackground(
                host.tintedColor(host.themeCoordinator.primaryColor, 30)
            )
            contentDescription = "Refresh Leaderboard"
            setOnClickListener {
                loadLeaderboardData(root, forceRefresh = true)
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }
        header.addView(refreshBtn)

        val settingsBtn = ImageView(host).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(host.themeCoordinator.textColor)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = host.themeCoordinator.createGlassIconBackground(
                host.tintedColor(host.themeCoordinator.textColor, 25)
            )
            contentDescription = "Leaderboard Settings"
            setOnClickListener {
                showLeaderboardSettingsDialog(root)
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                setMargins(dp(6), 0, 0, 0)
            }
        }
        header.addView(settingsBtn)
        root.addView(header)

        // 2. PERIOD SELECTOR TABS (Today | This Week | This Month)
        val periodContainer = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = host.themeCoordinator.createCardBackground(24f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                setMargins(0, 0, 0, dp(14))
            }
        }

        val periods = listOf(
            LeaderboardPeriod.DAILY to "Today",
            LeaderboardPeriod.WEEKLY to "This Week",
            LeaderboardPeriod.MONTHLY to "This Month"
        )

        periods.forEach { (p, label) ->
            val isSelected = (p == selectedPeriod)
            val tabView = TextView(host).apply {
                text = label
                textSize = 12.5f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (isSelected) Color.WHITE else host.themeCoordinator.textColor)
                alpha = if (isSelected) 1f else 0.7f
                background = if (isSelected) {
                    GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(host.themeCoordinator.primaryColor)
                    }
                } else null

                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener {
                    if (selectedPeriod != p) {
                        selectedPeriod = p
                        loadLeaderboardData(root, forceRefresh = false)
                    }
                }
            }
            periodContainer.addView(tabView)
        }
        root.addView(periodContainer)

        // 3. SCROLLABLE CONTENT (Podium + Rankings List)
        val scrollWrapper = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        if (isLoading) {
            val loadingBox = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            val progress = ProgressBar(host)
            val loadText = TextView(host).apply {
                text = "Fetching live rankings..."
                textSize = 13f
                alpha = 0.7f
                setTextColor(host.themeCoordinator.textColor)
                setPadding(0, dp(10), 0, 0)
            }
            loadingBox.addView(progress)
            loadingBox.addView(loadText)
            scrollWrapper.addView(loadingBox)
        } else {
            val contentScroll = ScrollView(host).apply {
                isVerticalScrollBarEnabled = false
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            val scrollContent = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, dp(16))
            }

            // Optional: Show paused participation banner if user has opted out
            if (!LeaderboardManager.isParticipating(host)) {
                val pausedCard = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(Color.argb(30, 239, 68, 68))
                        setStroke(dp(1), Color.argb(90, 239, 68, 68))
                    }
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, dp(12)) }
                }
                val pauseIcon = TextView(host).apply {
                    text = "⏸️"
                    textSize = 18f
                    setPadding(0, 0, dp(10), 0)
                }
                val pauseTextCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                pauseTextCol.addView(TextView(host).apply {
                    text = "Leaderboard Participation Paused"
                    setTextColor(Color.parseColor("#EF4444"))
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                })
                pauseTextCol.addView(TextView(host).apply {
                    text = "Your study hours are not being shared to global rankings."
                    setTextColor(host.themeCoordinator.textColor)
                    alpha = 0.7f
                    textSize = 11.5f
                    setPadding(0, 2, 0, 0)
                })
                val resumeBtn = TextView(host).apply {
                    text = "Resume"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(Color.parseColor("#10B981"))
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        LeaderboardManager.setParticipating(host, true)
                        android.widget.Toast.makeText(host, "Leaderboard participation enabled! 🚀", android.widget.Toast.LENGTH_SHORT).show()
                        loadLeaderboardData(root, forceRefresh = true)
                    }
                }
                pausedCard.addView(pauseIcon)
                pausedCard.addView(pauseTextCol)
                pausedCard.addView(resumeBtn)
                scrollContent.addView(pausedCard)
            }

            // A. Top 3 Podium
            val podiumView = buildPodiumView(currentEntries)
            scrollContent.addView(podiumView)

            // B. Rankings List (Ranks 4-50)
            val listHeader = TextView(host).apply {
                text = "TOP RANKS"
                textSize = 11f
                letterSpacing = 0.08f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(host.themeCoordinator.primaryColor)
                setPadding(dp(4), dp(16), 0, dp(8))
            }
            scrollContent.addView(listHeader)

            val remainingRanks = currentEntries.filter { it.rank > 3 }
            if (remainingRanks.isEmpty() && currentEntries.size <= 3) {
                if (currentEntries.isEmpty()) {
                    val emptyBox = LinearLayout(host).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = host.themeCoordinator.createCardBackground(18f)
                        setPadding(dp(20), dp(24), dp(20), dp(24))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, dp(8), 0, dp(8)) }
                    }
                    val emptyTitle = TextView(host).apply {
                        text = "🏆 No Sessions Yet"
                        textSize = 16f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(host.themeCoordinator.textColor)
                    }
                    val emptySubtitle = TextView(host).apply {
                        text = "Start a focus timer to claim the #1 spot on the leaderboard!"
                        textSize = 12f
                        alpha = 0.7f
                        gravity = Gravity.CENTER
                        setTextColor(host.themeCoordinator.textColor)
                        setPadding(0, dp(6), 0, 0)
                    }
                    emptyBox.addView(emptyTitle)
                    emptyBox.addView(emptySubtitle)
                    scrollContent.addView(emptyBox)
                }
            } else {
                remainingRanks.forEach { entry ->
                    scrollContent.addView(buildRankRow(entry))
                }
            }

            contentScroll.addView(scrollContent)
            scrollWrapper.addView(contentScroll)
        }
        root.addView(scrollWrapper)

        // 4. STICKY BOTTOM BAR (Personal User Card or Guest CTA)
        val bottomBar = buildBottomBar()
        root.addView(bottomBar)
    }

    private fun loadLeaderboardData(root: LinearLayout, forceRefresh: Boolean, isSilent: Boolean = false) {
        if (!isSilent) {
            isLoading = true
            renderLeaderboardUI(root)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                LeaderboardManager.fetchLeaderboard(host, selectedPeriod, forceRefresh)
            }
            isLoading = false
            hasLoadedOnce = true
            if (result.isSuccess) {
                currentEntries = result.getOrDefault(emptyList())
            } else if (!isSilent) {
                Toast.makeText(host, "Could not refresh leaderboard. Check internet connection.", Toast.LENGTH_SHORT).show()
            }
            renderLeaderboardUI(root)
        }
    }

    private fun buildPodiumView(entries: List<LeaderboardEntry>): LinearLayout {
        val podiumContainer = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(8), 0, dp(12))
            }
        }

        val top1 = entries.find { it.rank == 1 }
        val top2 = entries.find { it.rank == 2 }
        val top3 = entries.find { it.rank == 3 }

        // Rank 2 (Left, Silver)
        podiumContainer.addView(buildPodiumPedestal(top2, 2, Color.parseColor("#94A3B8"), dp(160)))
        // Rank 1 (Center, Gold - Tallest)
        podiumContainer.addView(buildPodiumPedestal(top1, 1, Color.parseColor("#F59E0B"), dp(185)))
        // Rank 3 (Right, Bronze)
        podiumContainer.addView(buildPodiumPedestal(top3, 3, Color.parseColor("#D97706"), dp(148)))

        return podiumContainer
    }

    private fun createAvatarView(entry: LeaderboardEntry?, sizePx: Int, accentColor: Int = host.themeCoordinator.primaryColor): View {
        val isCurrent = entry != null && LeaderboardManager.isCurrentUser(entry, host)
        if (isCurrent && LocalAvatarManager.hasCustomAvatar(host)) {
            val customBitmap = LocalAvatarManager.getCircularAvatarBitmap(host, sizePx)
            if (customBitmap != null) {
                return ImageView(host).apply {
                    setImageBitmap(customBitmap)
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(1), accentColor)
                    }
                }
            }
        }

        val initial = entry?.userName?.trim()?.take(1)?.uppercase() ?: "-"
        val label = if (entry != null && entry.avatarUrl.isNotBlank() && entry.avatarUrl.length <= 4) {
            entry.avatarUrl
        } else initial

        return TextView(host).apply {
            text = label
            textSize = (sizePx / density * 0.38f).coerceAtLeast(11f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
        }
    }

    private fun showStudentProfileDialog(entry: LeaderboardEntry) {
        val dialog = android.app.Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isCurrent = LeaderboardManager.isCurrentUser(entry, host)
        val dialogRoot = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = host.themeCoordinator.createDialogBackground(26f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Centered Avatar Frame
        val avatarBox = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
        val avatarView = createAvatarView(entry, dp(72), host.themeCoordinator.primaryColor)
        avatarBox.addView(avatarView)
        dialogRoot.addView(avatarBox)

        // Student Display Name
        val nameView = TextView(host).apply {
            text = if (isCurrent) "${entry.userName} (You)" else entry.userName
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(host.themeCoordinator.textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        dialogRoot.addView(nameView)

        // Target Exam / Focus Track Badge
        val targetExam = if (isCurrent) {
            ProfileManager.getProfile(host).targetExam.ifBlank { "Self-Study" }
        } else {
            entry.examTarget.ifBlank { "Self-Study" }
        }

        val trackPill = TextView(host).apply {
            text = "🎯 $targetExam"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(host.themeCoordinator.primaryColor)
            background = host.themeCoordinator.createGlassChip(host.tintedColor(host.themeCoordinator.primaryColor, 35), 12f)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        dialogRoot.addView(trackPill)

        // Student Bio / Motto (if present)
        val bioText = if (isCurrent) {
            ProfileManager.getProfile(host).bio
        } else {
            entry.bio
        }
        if (bioText.isNotBlank()) {
            val mottoView = TextView(host).apply {
                text = "\"$bioText\""
                textSize = 12.5f
                alpha = 0.8f
                setTextColor(host.themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            }
            dialogRoot.addView(mottoView)
        }

        // Stats Summary Grid
        val statsCard = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            background = host.themeCoordinator.createGlassChip(host.tintedColor(host.themeCoordinator.textColor, 20), 14f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }

        val colRank = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        colRank.addView(TextView(host).apply {
            text = "RANK"
            textSize = 10f
            alpha = 0.6f
            setTextColor(host.themeCoordinator.textColor)
        })
        colRank.addView(TextView(host).apply {
            text = "#${entry.rank}"
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(host.themeCoordinator.primaryColor)
        })
        statsCard.addView(colRank)

        val colTime = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        colTime.addView(TextView(host).apply {
            text = "STUDY TIME"
            textSize = 10f
            alpha = 0.6f
            setTextColor(host.themeCoordinator.textColor)
        })
        colTime.addView(TextView(host).apply {
            text = LeaderboardManager.formatDuration(entry.totalSeconds)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(host.themeCoordinator.textColor)
        })
        statsCard.addView(colTime)

        val colStatus = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        colStatus.addView(TextView(host).apply {
            text = "STATUS"
            textSize = 10f
            alpha = 0.6f
            setTextColor(host.themeCoordinator.textColor)
        })
        colStatus.addView(TextView(host).apply {
            text = if (entry.isStudying) {
                if (entry.currentSubject.isNotBlank()) "🟢 ${entry.currentSubject}" else "🟢 Live"
            } else "Resting"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (entry.isStudying) Color.parseColor("#4ADE80") else host.themeCoordinator.textColor)
        })
        statsCard.addView(colStatus)
        dialogRoot.addView(statsCard)

        // Dismiss Button
        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = host.themeCoordinator.createButtonBackground(host.themeCoordinator.primaryColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
            setOnClickListener { dialog.dismiss() }
        }
        dialogRoot.addView(closeBtn)

        dialog.setContentView(dialogRoot)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun buildPodiumPedestal(entry: LeaderboardEntry?, rank: Int, accentColor: Int, minHeightPx: Int): View {
        val isCurrent = entry != null && LeaderboardManager.isCurrentUser(entry, host)

        val pedestal = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                val bgCol = if (host.themeCoordinator.isDarkMode()) {
                    if (isCurrent) Color.argb(40, Color.red(host.themeCoordinator.primaryColor), Color.green(host.themeCoordinator.primaryColor), Color.blue(host.themeCoordinator.primaryColor))
                    else Color.parseColor("#14161F")
                } else {
                    if (isCurrent) Color.argb(30, 99, 102, 241) else Color.parseColor("#FFFFFF")
                }
                setColor(bgCol)
                setStroke(
                    dp(if (rank == 1 || isCurrent) 2 else 1),
                    if (isCurrent) host.themeCoordinator.primaryColor else Color.argb(120, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                )
            }
            setPadding(dp(8), dp(12), dp(8), dp(12))
            minimumHeight = minHeightPx
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            if (entry != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { showStudentProfileDialog(entry) }
            }
        }

        // Crown on Rank 1
        if (rank == 1) {
            val crown = TextView(host).apply {
                text = "👑"
                textSize = 18f
                gravity = Gravity.CENTER
            }
            pedestal.addView(crown)
        } else {
            val medal = TextView(host).apply {
                text = if (rank == 2) "🥈" else "🥉"
                textSize = 14f
                gravity = Gravity.CENTER
            }
            pedestal.addView(medal)
        }

        // Avatar Frame
        val avatarSize = if (rank == 1) dp(46) else dp(40)
        val avatarFrame = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                setMargins(0, dp(4), 0, dp(6))
            }
        }

        val avatarView = createAvatarView(entry, avatarSize, if (entry != null) accentColor else Color.DKGRAY)
        avatarFrame.addView(avatarView)

        // Rank Pill Badge
        val rankPill = TextView(host).apply {
            text = "#$rank"
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(accentColor)
            }
            setPadding(dp(5), dp(1), dp(5), dp(1))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        }
        avatarFrame.addView(rankPill)
        pedestal.addView(avatarFrame)

        // Name
        val nameText = TextView(host).apply {
            text = if (entry != null) {
                if (isCurrent) "You" else entry.userName
            } else "Open Spot"
            textSize = 11.5f
            maxLines = 1
            typeface = Typeface.create("sans-serif-medium", if (isCurrent) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (isCurrent) host.themeCoordinator.primaryColor else host.themeCoordinator.textColor)
            alpha = if (entry != null) 1f else 0.5f
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        pedestal.addView(nameText)

        // Time Duration
        val timeText = TextView(host).apply {
            text = if (entry != null) LeaderboardManager.formatDuration(entry.totalSeconds) else "--"
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(if (entry != null) accentColor else host.themeCoordinator.textColor)
            alpha = if (entry != null) 1f else 0.4f
            gravity = Gravity.CENTER
        }
        pedestal.addView(timeText)

        // Live Subject / State Chip
        if (entry != null) {
            val statusChip = TextView(host).apply {
                text = if (entry.isStudying) {
                    if (entry.currentSubject.isNotBlank()) "🟢 ${entry.currentSubject}" else "🟢 Studying"
                } else "Resting"
                textSize = 9f
                maxLines = 1
                setTextColor(if (entry.isStudying) Color.parseColor("#4ADE80") else host.themeCoordinator.textColor)
                alpha = if (entry.isStudying) 1f else 0.5f
                setPadding(dp(4), dp(2), dp(4), dp(2))
                gravity = Gravity.CENTER
            }
            pedestal.addView(statusChip)
        }

        return pedestal
    }

    private fun buildRankRow(entry: LeaderboardEntry): View {
        val isCurrent = LeaderboardManager.isCurrentUser(entry, host)

        val row = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                val bgCol = if (host.themeCoordinator.isDarkMode()) {
                    if (isCurrent) Color.argb(45, Color.red(host.themeCoordinator.primaryColor), Color.green(host.themeCoordinator.primaryColor), Color.blue(host.themeCoordinator.primaryColor))
                    else Color.parseColor("#12141C")
                } else {
                    if (isCurrent) Color.argb(35, 99, 102, 241) else Color.parseColor("#FFFFFF")
                }
                setColor(bgCol)
                setStroke(
                    dp(1),
                    if (isCurrent) host.themeCoordinator.primaryColor else Color.argb(40, 255, 255, 255)
                )
            }
            setPadding(dp(12), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showStudentProfileDialog(entry) }
        }

        // Rank Number
        val rankView = TextView(host).apply {
            text = "#${entry.rank}"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isCurrent) host.themeCoordinator.primaryColor else host.themeCoordinator.textColor)
            alpha = if (isCurrent) 1f else 0.7f
            setPadding(0, 0, dp(10), 0)
        }
        row.addView(rankView)

        // Avatar Frame
        val avatarSize = dp(36)
        val avatarFrame = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                setMargins(0, 0, dp(12), 0)
            }
        }
        val avatarView = createAvatarView(entry, avatarSize, host.themeCoordinator.primaryColor)
        avatarFrame.addView(avatarView)
        row.addView(avatarFrame)

        // User Info (Name + Subject + Target)
        val userCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameView = TextView(host).apply {
            text = entry.userName
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", if (isCurrent) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(host.themeCoordinator.textColor)
        }
        nameRow.addView(nameView)

        if (isCurrent) {
            val youTag = TextView(host).apply {
                text = "YOU"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(host.themeCoordinator.primaryColor)
                }
                setPadding(dp(5), dp(1), dp(5), dp(1))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(6), 0, 0, 0) }
            }
            nameRow.addView(youTag)
        }

        if (entry.examTarget.isNotBlank() && entry.examTarget != "Self-Study") {
            val examTag = TextView(host).apply {
                text = "🎯 ${entry.examTarget}"
                textSize = 9f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(host.themeCoordinator.primaryColor)
                background = host.themeCoordinator.createGlassChip(host.tintedColor(host.themeCoordinator.primaryColor, 25), 6f)
                setPadding(dp(5), dp(1), dp(5), dp(1))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(6), 0, 0, 0) }
            }
            nameRow.addView(examTag)
        }

        userCol.addView(nameRow)

        val statusView = TextView(host).apply {
            text = if (entry.isStudying) {
                if (entry.currentSubject.isNotBlank()) "🟢 Studying ${entry.currentSubject}" else "🟢 Studying Now"
            } else "Resting"
            textSize = 10.5f
            setTextColor(if (entry.isStudying) Color.parseColor("#4ADE80") else host.themeCoordinator.textColor)
            alpha = if (entry.isStudying) 1f else 0.55f
        }
        userCol.addView(statusView)
        row.addView(userCol)

        // Study Duration
        val durationView = TextView(host).apply {
            text = LeaderboardManager.formatDuration(entry.totalSeconds)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (isCurrent) host.themeCoordinator.primaryColor else host.themeCoordinator.textColor)
        }
        row.addView(durationView)

        return row
    }

    private fun buildBottomBar(): View {
        val isGuest = AuthManager.isGuest(host)

        if (isGuest) {
            // GUEST CTA: Log in to join the leaderboard
            val ctaCard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.argb(220, 79, 70, 229),
                        Color.argb(220, 147, 51, 234)
                    )
                ).apply {
                    cornerRadius = dp(18).toFloat()
                }
                setPadding(dp(16), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(8), 0, dp(12))
                }
            }

            val iconView = TextView(host).apply {
                text = "🏆"
                textSize = 22f
                setPadding(0, 0, dp(12), 0)
            }
            ctaCard.addView(iconView)

            val textCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val ctaTitle = TextView(host).apply {
                text = "Join the Leaderboard"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
            }
            textCol.addView(ctaTitle)

            val ctaDesc = TextView(host).apply {
                text = "Log in to compete & track your rank"
                textSize = 11f
                setTextColor(Color.WHITE)
                alpha = 0.85f
            }
            textCol.addView(ctaDesc)
            ctaCard.addView(textCol)

            val loginBtn = Button(host).apply {
                text = "LOG IN"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.parseColor("#4F46E5"))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.WHITE)
                }
                setPadding(dp(14), dp(4), dp(14), dp(4))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(36)
                )
                setOnClickListener {
                    host.startActivity(Intent(host, LoginActivity::class.java))
                }
            }
            ctaCard.addView(loginBtn)

            return ctaCard
        } else {
            // LOGGED-IN: Sticky Personal Ranking Card
            val myEntry = currentEntries.find { LeaderboardManager.isCurrentUser(it, host) }
            val currentProfile = ProfileManager.getProfile(host)
            val effectiveName = ProfileManager.getEffectiveDisplayName(host)

            val todayFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val todayRealSecs = LeaderboardManager.getRealTimerFocusSecondsForDate(host, todayFmt).toInt()
            val finalDurationSecs = if (myEntry != null && myEntry.totalSeconds > 0) myEntry.totalSeconds else todayRealSecs

            val myCard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = host.themeCoordinator.createCardBackground(18f)
                setPadding(dp(12), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dp(8), 0, dp(12))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (myEntry != null) {
                        showStudentProfileDialog(myEntry)
                    } else {
                        val dummyEntry = LeaderboardEntry(
                            rank = 0,
                            userId = AuthManager.getUserId(host) ?: "",
                            userName = effectiveName,
                            avatarUrl = ProfileManager.getEffectiveAvatarUrl(host),
                            totalSeconds = finalDurationSecs,
                            isStudying = false,
                            examTarget = currentProfile.targetExam,
                            bio = currentProfile.bio
                        )
                        showStudentProfileDialog(dummyEntry)
                    }
                }
            }

            // Avatar Frame
            val avatarSize = dp(38)
            val avatarFrame = FrameLayout(host).apply {
                layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                    setMargins(0, 0, dp(10), 0)
                }
            }
            val avatarView = createAvatarView(myEntry, avatarSize, host.themeCoordinator.primaryColor)
            avatarFrame.addView(avatarView)
            myCard.addView(avatarFrame)

            val myInfoCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val myName = TextView(host).apply {
                text = effectiveName
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(host.themeCoordinator.textColor)
            }
            nameRow.addView(myName)

            if (currentProfile.targetExam.isNotBlank() && currentProfile.targetExam != "Self-Study") {
                val targetBadge = TextView(host).apply {
                    text = "🎯 ${currentProfile.targetExam}"
                    textSize = 9f
                    setTextColor(host.themeCoordinator.primaryColor)
                    background = host.themeCoordinator.createGlassChip(host.tintedColor(host.themeCoordinator.primaryColor, 25), 6f)
                    setPadding(dp(5), dp(1), dp(5), dp(1))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(dp(6), 0, 0, 0) }
                }
                nameRow.addView(targetBadge)
            }
            myInfoCol.addView(nameRow)

            val myStatus = TextView(host).apply {
                text = if (myEntry != null) "Ranked #${myEntry.rank} • Tap to view profile" else "Active today • Tap to view profile"
                textSize = 11f
                setTextColor(host.themeCoordinator.textColor)
                alpha = 0.65f
            }
            myInfoCol.addView(myStatus)
            myCard.addView(myInfoCol)

            val myTime = TextView(host).apply {
                text = LeaderboardManager.formatDuration(finalDurationSecs)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(host.themeCoordinator.primaryColor)
            }
            myCard.addView(myTime)

            return myCard
        }
    }

    private fun showLeaderboardSettingsDialog(root: LinearLayout) {
        val dialog = android.app.Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val sharedPrefs = host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)

        val dialogRoot = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = host.themeCoordinator.createDialogBackground(28f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        val titleView = TextView(host).apply {
            text = "Leaderboard Settings"
            setTextColor(host.themeCoordinator.primaryColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }
        val subView = TextView(host).apply {
            text = "Manage your global leaderboard participation and live visibility."
            setTextColor(host.themeCoordinator.textColor)
            alpha = 0.7f
            textSize = 12.5f
            setPadding(0, 0, 0, dp(16))
        }
        dialogRoot.addView(titleView)
        dialogRoot.addView(subView)

        val isParticipating = sharedPrefs.getBoolean("leaderboard_participate", true)
        val isShareLive = sharedPrefs.getBoolean("leaderboard_share_live_status", true)

        var participateSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null

        fun promptTurnOffLeaderboard() {
            DeveloperToolsHelper.showThemedConfirmDialog(
                activity = host,
                themeCoordinator = host.themeCoordinator,
                title = "Pause Leaderboard Participation?",
                message = "Turning this off stops your focus sessions from syncing to global student rankings and removes your live study presence.\n\nYour personal statistics, study logs, and streaks remain completely safe on your device.\n\nAre you sure you want to stop participating?",
                confirmText = "Pause Participation",
                isDestructive = true,
                onCancel = {
                    participateSwitchRef?.isChecked = true
                }
            ) {
                sharedPrefs.edit().putBoolean("leaderboard_participate", false).apply()
                participateSwitchRef?.isChecked = false
                CoroutineScope(Dispatchers.IO).launch {
                    LeaderboardManager.updateStudyPresence(host, false)
                }
                Toast.makeText(host, "Leaderboard participation paused", Toast.LENGTH_SHORT).show()
                loadLeaderboardData(root, forceRefresh = true)
            }
        }

        // Row 1: Participate
        val partSwitch = com.google.android.material.switchmaterial.SwitchMaterial(host).apply {
            isChecked = isParticipating
            setOnClickListener {
                if (!isChecked) {
                    isChecked = true
                    promptTurnOffLeaderboard()
                } else {
                    sharedPrefs.edit().putBoolean("leaderboard_participate", true).apply()
                    Toast.makeText(host, "Participating in Leaderboard! 🚀", Toast.LENGTH_SHORT).show()
                    loadLeaderboardData(root, forceRefresh = true)
                }
            }
        }
        participateSwitchRef = partSwitch

        val partRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                val cur = sharedPrefs.getBoolean("leaderboard_participate", true)
                if (cur) {
                    promptTurnOffLeaderboard()
                } else {
                    sharedPrefs.edit().putBoolean("leaderboard_participate", true).apply()
                    partSwitch.isChecked = true
                    Toast.makeText(host, "Participating in Leaderboard! 🚀", Toast.LENGTH_SHORT).show()
                    loadLeaderboardData(root, forceRefresh = true)
                }
            }
        }
        val partIcon = TextView(host).apply { text = "🏆"; textSize = 20f; setPadding(0, 0, dp(12), 0) }
        val partTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        partTextCol.addView(TextView(host).apply {
            text = "Participate in Leaderboard"
            setTextColor(host.themeCoordinator.textColor)
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        partTextCol.addView(TextView(host).apply {
            text = "Sync focus hours and appear on global rankings (On by default)"
            setTextColor(host.themeCoordinator.textColor)
            alpha = 0.55f
            textSize = 11.5f
            setPadding(0, 2, 0, 0)
        })
        partRow.addView(partIcon)
        partRow.addView(partTextCol)
        partRow.addView(partSwitch)
        dialogRoot.addView(partRow)

        // Divider
        dialogRoot.addView(View(host).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(6), 0, dp(6))
            }
            background = GradientDrawable().apply {
                setColor(if (host.themeCoordinator.isDarkMode()) Color.parseColor("#22232B") else host.tintedColor(host.themeCoordinator.textColor, 25))
            }
        })

        // Row 2: Live Status
        val liveSwitch = com.google.android.material.switchmaterial.SwitchMaterial(host).apply {
            isChecked = isShareLive
            setOnClickListener {
                val newState = isChecked
                sharedPrefs.edit().putBoolean("leaderboard_share_live_status", newState).apply()
                if (!newState) {
                    CoroutineScope(Dispatchers.IO).launch {
                        LeaderboardManager.updateStudyPresence(host, false)
                    }
                    Toast.makeText(host, "Live study status hidden", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(host, "Live study status visible", Toast.LENGTH_SHORT).show()
                }
                loadLeaderboardData(root, forceRefresh = true)
            }
        }
        val liveRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                val cur = sharedPrefs.getBoolean("leaderboard_share_live_status", true)
                val next = !cur
                sharedPrefs.edit().putBoolean("leaderboard_share_live_status", next).apply()
                liveSwitch.isChecked = next
                if (!next) {
                    CoroutineScope(Dispatchers.IO).launch {
                        LeaderboardManager.updateStudyPresence(host, false)
                    }
                    Toast.makeText(host, "Live study status hidden", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(host, "Live study status visible", Toast.LENGTH_SHORT).show()
                }
                loadLeaderboardData(root, forceRefresh = true)
            }
        }
        val liveIcon = TextView(host).apply { text = "🟢"; textSize = 20f; setPadding(0, 0, dp(12), 0) }
        val liveTextCol = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        liveTextCol.addView(TextView(host).apply {
            text = "Share Live Study Status"
            setTextColor(host.themeCoordinator.textColor)
            textSize = 14.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        liveTextCol.addView(TextView(host).apply {
            text = "Show green live indicator & active subject to others (On by default)"
            setTextColor(host.themeCoordinator.textColor)
            alpha = 0.55f
            textSize = 11.5f
            setPadding(0, 2, 0, 0)
        })
        liveRow.addView(liveIcon)
        liveRow.addView(liveTextCol)
        liveRow.addView(liveSwitch)
        dialogRoot.addView(liveRow)

        // Close button
        val closeBtn = Button(host).apply {
            text = "Done"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(host.themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(0, dp(16), 0, 0)
            }
            setOnClickListener { dialog.dismiss() }
        }
        dialogRoot.addView(closeBtn)

        dialog.setContentView(dialogRoot)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout((host.resources.displayMetrics.widthPixels * 0.90f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }
}
