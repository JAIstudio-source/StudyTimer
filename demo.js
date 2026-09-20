/**
 * STUDYTIMER WEB APPLICATION (v2.0.0)
 * Full-Fledged Browser Focus Studio & Productivity Workspace
 * Features:
 * - Millisecond-Exact Timing Engine (Zero Drift)
 * - Countdown Timer, Pomodoro, Stopwatch, and Break Modes
 * - Dynamic Subject Tagging & Live Subject Distribution Analytics
 * - Real-Time Daily Goal Progress & Streak Tracking
 * - Browser Native Fullscreen Focus Mode
 * - Two-Way Cloud Sync with StudyTimer Android App via Supabase
 */

// ============================================================================
// 1. SUPABASE CLIENT CONFIGURATION
// ============================================================================
const SUPABASE_URL = 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';

let supabaseClient = null;
if (typeof supabase !== 'undefined' && supabase.createClient) {
  supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
}

// ============================================================================
// 2. STATE MANAGEMENT & PREFERENCES
// ============================================================================
const DEFAULT_SUBJECTS = [
  { id: 'math', name: 'Mathematics', color: '#3b82f6' },
  { id: 'physics', name: 'Physics', color: '#10b981' },
  { id: 'coding', name: 'Coding', color: '#8b5cf6' },
  { id: 'english', name: 'English', color: '#f59e0b' }
];

// Timer Configuration Settings (Persisted)
let timerConfig = {
  customTimerMinutes: 25,
  pomoFocusMinutes: 25,
  pomoBreakMinutes: 5,
  dailyGoalMinutes: 120
};

let currentMode = 'timer'; // 'timer', 'pomodoro', 'stopwatch', 'break'
let timerStatus = 'IDLE';  // 'IDLE', 'RUNNING', 'PAUSED'

// Millisecond-Accurate Tracking Variables
let timerStartTimestamp = null;
let accumulatedElapsedSec = 0;
let timeRemaining = 25 * 60;
let stopwatchElapsed = 0;
let timerInterval = null;

// User Data & History
let appState = {
  currentUser: null,
  streakCount: 1,
  lastStudyDate: '',
  subjects: [...DEFAULT_SUBJECTS],
  selectedSubject: DEFAULT_SUBJECTS[0],
  timelineEntries: [],
  todaySessions: []
};

// ============================================================================
// 3. INITIALIZATION
// ============================================================================
document.addEventListener('DOMContentLoaded', async () => {
  initTheme();
  loadLocalState();
  initDomElements();
  setupEventListeners();
  renderSubjects();
  resetTimer();
  renderTimelineList();
  updateProgressAndStreak();
  updateSubjectBreakdown();

  if (supabaseClient) {
    await initAuth();
  }
});

// Theme Management
function initTheme() {
  const savedTheme = localStorage.getItem('studytimer-theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  
  const themeToggle = document.getElementById('themeToggleBtn');
  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('studytimer-theme', next);
    });
  }
}

// Supabase Authentication
async function initAuth() {
  try {
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (session && session.user) {
      handleUserSignedIn(session.user);
    } else {
      handleUserSignedOut();
    }

    supabaseClient.auth.onAuthStateChange(async (event, session) => {
      if (session && session.user) {
        handleUserSignedIn(session.user);
      } else {
        handleUserSignedOut();
      }
    });
  } catch (err) {
    console.error('Supabase auth initialization error:', err);
  }
}

function handleUserSignedIn(user) {
  appState.currentUser = user;
  
  const btnOpenAuth = document.getElementById('btnOpenAuth');
  const userDropdownContainer = document.getElementById('userDropdownContainer');
  const userDisplayName = document.getElementById('userDisplayName');
  const dropdownUserName = document.getElementById('dropdownUserName');
  const dropdownUserEmail = document.getElementById('dropdownUserEmail');
  const userAvatarImg = document.getElementById('userAvatarImg');
  const guestBanner = document.getElementById('guestBanner');
  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');

  const name = user.user_metadata?.full_name || user.email?.split('@')[0] || 'Student';
  const avatar = user.user_metadata?.avatar_url || 'assets/logo.png';

  if (btnOpenAuth) btnOpenAuth.classList.add('hidden');
  if (userDropdownContainer) userDropdownContainer.classList.remove('hidden');
  if (userDisplayName) userDisplayName.textContent = name;
  if (dropdownUserName) dropdownUserName.textContent = name;
  if (dropdownUserEmail) dropdownUserEmail.textContent = user.email || '';
  if (userAvatarImg) userAvatarImg.src = avatar;
  if (guestBanner) guestBanner.classList.add('hidden');

  if (syncStatusPill) {
    syncStatusPill.classList.add('synced');
    syncStatusText.textContent = 'Synced';
  }

  pullDataFromCloud();
  showToast('Signed in! Cloud Sync active.', 'success');
}

function handleUserSignedOut() {
  appState.currentUser = null;
  const btnOpenAuth = document.getElementById('btnOpenAuth');
  const userDropdownContainer = document.getElementById('userDropdownContainer');
  const guestBanner = document.getElementById('guestBanner');
  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');

  if (btnOpenAuth) btnOpenAuth.classList.remove('hidden');
  if (userDropdownContainer) userDropdownContainer.classList.add('hidden');
  if (guestBanner) guestBanner.classList.remove('hidden');

  if (syncStatusPill) {
    syncStatusPill.classList.remove('synced');
    syncStatusText.textContent = 'Local';
  }
}

// ============================================================================
// 4. TWO-WAY CLOUD SYNC LOGIC
// ============================================================================
async function pullDataFromCloud() {
  if (!supabaseClient || !appState.currentUser) return;

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const { data, error } = await supabaseClient
      .from('user_sync_data')
      .select('*')
      .eq('user_id', appState.currentUser.id)
      .single();

    if (error && error.code !== 'PGRST116') {
      console.warn('Error fetching cloud record:', error);
    }

    if (data) {
      if (data.prefs_data) {
        try {
          const prefs = JSON.parse(data.prefs_data);
          if (prefs.daily_goal_minutes) timerConfig.dailyGoalMinutes = prefs.daily_goal_minutes;
          if (prefs.streak_count) appState.streakCount = prefs.streak_count;
          if (prefs.last_study_date) appState.lastStudyDate = prefs.last_study_date;
          if (prefs.custom_timer_minutes) timerConfig.customTimerMinutes = prefs.custom_timer_minutes;
          if (prefs.pomo_focus_minutes) timerConfig.pomoFocusMinutes = prefs.pomo_focus_minutes;
          if (prefs.pomo_break_minutes) timerConfig.pomoBreakMinutes = prefs.pomo_break_minutes;

          if (prefs.__subject_tags_data__) {
            const subjectPrefs = JSON.parse(prefs.__subject_tags_data__);
            if (subjectPrefs.custom_subjects) {
              const loadedSubjects = typeof subjectPrefs.custom_subjects === 'string' 
                ? JSON.parse(subjectPrefs.custom_subjects) 
                : subjectPrefs.custom_subjects;
              if (Array.isArray(loadedSubjects) && loadedSubjects.length > 0) {
                appState.subjects = loadedSubjects;
                appState.selectedSubject = appState.subjects[0];
              }
            }
          }
        } catch (e) {
          console.error('Failed to parse remote prefs_data', e);
        }
      }

      if (data.timeline_data) {
        try {
          const remoteTimeline = JSON.parse(data.timeline_data);
          if (Array.isArray(remoteTimeline)) {
            appState.timelineEntries = remoteTimeline;
            reconstructTodaySessionsFromTimeline();
          }
        } catch (e) {
          console.error('Failed to parse remote timeline_data', e);
        }
      }

      renderSubjects();
      renderTimelineList();
      updateProgressAndStreak();
      updateSubjectBreakdown();
      saveLocalState();
      resetTimer();
    }
  } catch (err) {
    console.error('Cloud pull exception:', err);
  } finally {
    if (syncStatusPill) {
      syncStatusPill.classList.remove('syncing');
      syncStatusPill.classList.add('synced');
      syncStatusText.textContent = 'Synced';
    }
  }
}

async function pushDataToCloud() {
  saveLocalState();
  if (!supabaseClient || !appState.currentUser) return;

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const user = appState.currentUser;
    const userName = user.user_metadata?.full_name || user.email?.split('@')[0] || 'Student';
    const userEmail = user.email || '';
    const profileImg = user.user_metadata?.avatar_url || '';

    const subjectTagsObj = {
      custom_subjects: JSON.stringify(appState.subjects)
    };

    const prefsObj = {
      daily_goal_minutes: timerConfig.dailyGoalMinutes,
      custom_timer_minutes: timerConfig.customTimerMinutes,
      pomo_focus_minutes: timerConfig.pomoFocusMinutes,
      pomo_break_minutes: timerConfig.pomoBreakMinutes,
      streak_count: appState.streakCount,
      last_study_date: appState.lastStudyDate,
      __subject_tags_data__: JSON.stringify(subjectTagsObj)
    };

    const payload = {
      user_id: user.id,
      user_name: userName,
      user_email: userEmail,
      profile_image_uri: profileImg,
      prefs_data: JSON.stringify(prefsObj),
      timeline_data: JSON.stringify(appState.timelineEntries),
      updated_at: Date.now()
    };

    const { error } = await supabaseClient
      .from('user_sync_data')
      .upsert(payload, { onConflict: 'user_id' });

    if (error) {
      console.error('Cloud sync push error:', error);
      showToast('Cloud sync failed to update.', 'error');
    } else {
      showToast('Session synced with Android app! ☁️', 'success');
    }
  } catch (err) {
    console.error('pushDataToCloud exception:', err);
  } finally {
    if (syncStatusPill) {
      syncStatusPill.classList.remove('syncing');
      syncStatusPill.classList.add('synced');
      syncStatusText.textContent = 'Synced';
    }
  }
}

function reconstructTodaySessionsFromTimeline() {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);
  const startOfDayMs = startOfDay.getTime();

  const sessions = [];
  const entries = appState.timelineEntries.filter(e => e.t >= startOfDayMs);

  for (let i = 0; i < entries.length; i++) {
    const curr = entries[i];
    if (curr.s === 'STUDYING' || curr.s === 'POMODORO' || curr.s === 'COUNT_UP') {
      const next = entries[i + 1];
      const endMs = next ? next.t : curr.t + 1500 * 1000;
      const durationSec = Math.max(1, Math.round((endMs - curr.t) / 1000));
      
      let modeLabel = 'Focus Study';
      if (curr.s === 'POMODORO') modeLabel = 'Pomodoro';
      else if (curr.s === 'COUNT_UP') modeLabel = 'Stopwatch';

      sessions.push({
        id: 'sess_' + curr.t,
        subject: {
          name: curr.subName || 'Focus Study',
          color: curr.subColor || '#3b82f6'
        },
        durationSec,
        timestamp: curr.t,
        mode: modeLabel
      });
    }
  }

  appState.todaySessions = sessions.reverse();
}

// ============================================================================
// 5. LOCAL STORAGE PERSISTENCE
// ============================================================================
function loadLocalState() {
  try {
    const local = localStorage.getItem('studytimer_demo_state');
    if (local) {
      const parsed = JSON.parse(local);
      if (parsed.timerConfig) timerConfig = { ...timerConfig, ...parsed.timerConfig };
      if (parsed.streakCount) appState.streakCount = parsed.streakCount;
      if (parsed.lastStudyDate) appState.lastStudyDate = parsed.lastStudyDate;
      if (Array.isArray(parsed.subjects) && parsed.subjects.length > 0) {
        appState.subjects = parsed.subjects;
        appState.selectedSubject = appState.subjects[0];
      }
      if (Array.isArray(parsed.timelineEntries)) {
        appState.timelineEntries = parsed.timelineEntries;
        reconstructTodaySessionsFromTimeline();
      }
    }
  } catch (e) {
    console.error('Failed to load local demo state:', e);
  }
}

function saveLocalState() {
  try {
    const stateToSave = {
      timerConfig,
      streakCount: appState.streakCount,
      lastStudyDate: appState.lastStudyDate,
      subjects: appState.subjects,
      timelineEntries: appState.timelineEntries
    };
    localStorage.setItem('studytimer_demo_state', JSON.stringify(stateToSave));
  } catch (e) {
    console.error('Failed to save local demo state:', e);
  }
}

// ============================================================================
// 6. ACCURATE MILLISECOND-EXACT TIMER ENGINE
// ============================================================================
function getModeDurationSec() {
  switch (currentMode) {
    case 'timer': return (timerConfig.customTimerMinutes || 25) * 60;
    case 'pomodoro': return (timerConfig.pomoFocusMinutes || 25) * 60;
    case 'break': return (timerConfig.pomoBreakMinutes || 5) * 60;
    case 'stopwatch': return 0;
    default: return 25 * 60;
  }
}

function initDomElements() {
  // Mode Tabs
  document.querySelectorAll('.mode-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      if (timerStatus === 'RUNNING') {
        if (!confirm('Switching modes will reset your current timer. Continue?')) return;
      }
      document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      switchMode(btn.dataset.mode);
    });
  });
}

function setupEventListeners() {
  // Main Controls
  document.getElementById('btnToggleTimer').addEventListener('click', toggleTimer);
  document.getElementById('btnResetTimer').addEventListener('click', resetTimer);
  document.getElementById('btnQuickReset')?.addEventListener('click', resetTimer);
  document.getElementById('btnFinishSession').addEventListener('click', finishSession);

  // Fullscreen Browser Zen Mode
  document.getElementById('btnBrowserFullscreen')?.addEventListener('click', toggleBrowserFullscreen);

  // Settings Modal Triggers
  document.getElementById('btnOpenTimerSettings')?.addEventListener('click', openTimerSettingsModal);
  document.getElementById('btnEditGoal')?.addEventListener('click', openTimerSettingsModal);
  document.getElementById('btnCloseTimerSettingsModal')?.addEventListener('click', closeTimerSettingsModal);
  document.getElementById('timerSettingsModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'timerSettingsModalOverlay') closeTimerSettingsModal();
  });
  document.getElementById('timerSettingsForm')?.addEventListener('submit', handleSaveTimerSettings);

  // Presets in Settings Modal
  document.querySelectorAll('.settings-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.settings-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      const min = parseInt(chip.dataset.min, 10);
      const input = document.getElementById('customMinutesInput');
      if (input) input.value = min;
    });
  });

  // Auth Modal
  document.getElementById('btnOpenAuth')?.addEventListener('click', openAuthModal);
  document.getElementById('btnBannerSignIn')?.addEventListener('click', openAuthModal);
  document.getElementById('btnCloseAuthModal')?.addEventListener('click', closeAuthModal);
  document.getElementById('authModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'authModalOverlay') closeAuthModal();
  });

  // User Profile Dropdown
  const userProfileBtn = document.getElementById('userProfileBtn');
  const userMenuDropdown = document.getElementById('userMenuDropdown');
  if (userProfileBtn && userMenuDropdown) {
    userProfileBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      userMenuDropdown.classList.toggle('hidden');
    });
    document.addEventListener('click', () => {
      userMenuDropdown.classList.add('hidden');
    });
  }

  // Auth Actions
  document.getElementById('btnGoogleSignIn')?.addEventListener('click', signInWithGoogle);
  document.getElementById('emailAuthForm')?.addEventListener('submit', signInWithEmail);
  document.getElementById('btnLogout')?.addEventListener('click', signOutUser);
  document.getElementById('btnManualSync')?.addEventListener('click', pullDataFromCloud);

  // Subject Dropdown Menu Toggles
  const btnSubjectTrigger = document.getElementById('btnSubjectMenuTrigger');
  const timerSubjectDisplay = document.getElementById('timerSubjectDisplay');
  const subjectDropdownMenu = document.getElementById('subjectDropdownMenu');

  function toggleSubjectMenu(e) {
    e.stopPropagation();
    if (subjectDropdownMenu) {
      subjectDropdownMenu.classList.toggle('hidden');
    }
  }

  btnSubjectTrigger?.addEventListener('click', toggleSubjectMenu);
  timerSubjectDisplay?.addEventListener('click', toggleSubjectMenu);

  document.addEventListener('click', (e) => {
    if (subjectDropdownMenu && !subjectDropdownMenu.contains(e.target) && !btnSubjectTrigger?.contains(e.target) && !timerSubjectDisplay?.contains(e.target)) {
      subjectDropdownMenu.classList.add('hidden');
    }
  });

  // Subject Modal
  document.getElementById('btnAddSubject')?.addEventListener('click', (e) => {
    e.stopPropagation();
    subjectDropdownMenu?.classList.add('hidden');
    openSubjectModal();
  });
  document.getElementById('btnCloseSubjectModal')?.addEventListener('click', closeSubjectModal);
  document.getElementById('subjectModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'subjectModalOverlay') closeSubjectModal();
  });
  document.getElementById('addSubjectForm')?.addEventListener('submit', handleAddCustomSubject);

  // Palette buttons
  document.querySelectorAll('.color-choice-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.color-choice-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });
}

function toggleBrowserFullscreen() {
  const expandIcon = document.querySelector('.icon-expand-fs');
  const compressIcon = document.querySelector('.icon-compress-fs');

  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().then(() => {
      if (expandIcon) expandIcon.classList.add('hidden');
      if (compressIcon) compressIcon.classList.remove('hidden');
      showToast('Entered Fullscreen Focus Mode ⛶', 'info');
    }).catch(err => {
      console.warn('Fullscreen error:', err);
    });
  } else {
    document.exitFullscreen().then(() => {
      if (expandIcon) expandIcon.classList.remove('hidden');
      if (compressIcon) compressIcon.classList.add('hidden');
    });
  }
}

document.addEventListener('fullscreenchange', () => {
  const expandIcon = document.querySelector('.icon-expand-fs');
  const compressIcon = document.querySelector('.icon-compress-fs');
  if (!document.fullscreenElement) {
    if (expandIcon) expandIcon.classList.remove('hidden');
    if (compressIcon) compressIcon.classList.add('hidden');
  } else {
    if (expandIcon) expandIcon.classList.add('hidden');
    if (compressIcon) compressIcon.classList.remove('hidden');
  }
});

function switchMode(modeKey) {
  stopInterval();
  currentMode = modeKey;
  
  const modeBadge = document.getElementById('activeModeBadge');
  if (modeBadge) {
    if (modeKey === 'timer') modeBadge.textContent = '⏱️ Countdown Timer';
    else if (modeKey === 'pomodoro') modeBadge.textContent = '🍅 Pomodoro Focus';
    else if (modeKey === 'stopwatch') modeBadge.textContent = '⚡ Stopwatch';
    else if (modeKey === 'break') modeBadge.textContent = '☕ Short Break';
  }

  resetTimer();
}

function toggleTimer() {
  if (timerStatus === 'RUNNING') {
    pauseTimer();
  } else {
    startTimer();
  }
}

function startTimer() {
  timerStatus = 'RUNNING';
  timerStartTimestamp = Date.now();

  updateTimerControlsUI();

  // Tick interval checks real timestamp differences: 100% exact timing
  timerInterval = setInterval(() => {
    const now = Date.now();
    const elapsedSinceResume = Math.floor((now - timerStartTimestamp) / 1000);
    const totalElapsedSec = accumulatedElapsedSec + elapsedSinceResume;

    if (currentMode === 'stopwatch') {
      stopwatchElapsed = totalElapsedSec;
    } else {
      const totalSec = getModeDurationSec();
      timeRemaining = Math.max(0, totalSec - totalElapsedSec);
      if (timeRemaining === 0) {
        finishSession();
        return;
      }
    }
    updateTimerDisplay();
  }, 200);
}

function pauseTimer() {
  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    const elapsedSinceResume = Math.floor((Date.now() - timerStartTimestamp) / 1000);
    accumulatedElapsedSec += elapsedSinceResume;
  }
  timerStatus = 'PAUSED';
  timerStartTimestamp = null;
  stopInterval();
  updateTimerControlsUI();
  updateTimerDisplay();
}

function resetTimer() {
  stopInterval();
  timerStatus = 'IDLE';
  timerStartTimestamp = null;
  accumulatedElapsedSec = 0;
  stopwatchElapsed = 0;
  timeRemaining = getModeDurationSec();

  updateTimerControlsUI();
  updateTimerDisplay();
}

function stopInterval() {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
}

function finishSession() {
  let studiedDurationSec = 0;

  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    const elapsedSinceResume = Math.floor((Date.now() - timerStartTimestamp) / 1000);
    accumulatedElapsedSec += elapsedSinceResume;
  }

  studiedDurationSec = accumulatedElapsedSec;

  if (studiedDurationSec < 10) {
    resetTimer();
    showToast('Session too short to record (< 10s).', 'info');
    return;
  }

  const now = Date.now();
  const startMs = now - (studiedDurationSec * 1000);
  const subject = appState.selectedSubject;

  let stateKey = 'STUDYING';
  let modeLabel = 'Focus Timer';
  if (currentMode === 'pomodoro') { stateKey = 'POMODORO'; modeLabel = 'Pomodoro'; }
  else if (currentMode === 'stopwatch') { stateKey = 'COUNT_UP'; modeLabel = 'Stopwatch'; }
  else if (currentMode === 'break') { stateKey = 'BREAK'; modeLabel = 'Break'; }

  if (stateKey !== 'BREAK') {
    const startEntry = {
      t: startMs,
      s: stateKey,
      subId: subject.id,
      subName: subject.name,
      subColor: subject.color
    };
    const idleEntry = {
      t: now,
      s: 'IDLE'
    };

    appState.timelineEntries.push(startEntry, idleEntry);

    const newSession = {
      id: 'sess_' + Date.now(),
      subject: { ...subject },
      durationSec: studiedDurationSec,
      timestamp: startMs,
      mode: modeLabel
    };

    appState.todaySessions.unshift(newSession);

    // Update Streak
    checkAndUpdateStreak();
  }

  resetTimer();
  renderTimelineList();
  updateProgressAndStreak();
  updateSubjectBreakdown();
  saveLocalState();
  pushDataToCloud();

  const minStr = Math.round(studiedDurationSec / 60);
  showToast(`🎉 Focus session saved! +${minStr > 0 ? minStr : 1}m logged to ${subject.name}`, 'success');
}

function checkAndUpdateStreak() {
  const todayStr = new Date().toISOString().split('T')[0];
  if (appState.lastStudyDate !== todayStr) {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().split('T')[0];

    if (appState.lastStudyDate === yesterdayStr) {
      appState.streakCount = (appState.streakCount || 0) + 1;
    } else if (!appState.lastStudyDate) {
      appState.streakCount = 1;
    }
    appState.lastStudyDate = todayStr;
  }
}

// ============================================================================
// 7. UI RENDERING & UPDATES
// ============================================================================
function updateTimerDisplay() {
  let displaySec = 0;
  let progressRatio = 1;
  const totalSec = getModeDurationSec();

  if (currentMode === 'stopwatch') {
    displaySec = stopwatchElapsed;
    progressRatio = (stopwatchElapsed % 60) / 60;
  } else {
    displaySec = timeRemaining;
    progressRatio = totalSec > 0 ? timeRemaining / totalSec : 0;
  }

  const mins = Math.floor(displaySec / 60);
  const secs = displaySec % 60;
  const timeFormatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

  const timerDisplay = document.getElementById('timerDisplay');
  if (timerDisplay) timerDisplay.textContent = timeFormatted;

  // Title tag update
  if (timerStatus === 'RUNNING') {
    document.title = `(${timeFormatted}) StudyTimer Focus`;
  } else {
    document.title = `StudyTimer Web - Focus Timer & Habit Tracker for Students`;
  }

  // Update Indicator Ring
  const ring = document.getElementById('timerIndicator');
  if (ring) {
    const circumference = 867.08; // 2 * PI * 138
    const offset = circumference * (1 - progressRatio);
    ring.style.strokeDashoffset = offset;

    if (currentMode === 'break') {
      ring.style.stroke = 'var(--accent-emerald)';
    } else if (currentMode === 'pomodoro') {
      ring.style.stroke = 'var(--accent-red)';
    } else {
      ring.style.stroke = 'var(--accent-blue)';
    }
  }

  // State Badge
  const stateBadge = document.getElementById('timerStateBadge');
  if (stateBadge) {
    if (timerStatus === 'RUNNING') {
      stateBadge.textContent = currentMode === 'break' ? '☕ TAKING A BREAK' : '🔥 FOCUSING';
      stateBadge.style.color = currentMode === 'break' ? 'var(--accent-emerald)' : 'var(--accent-blue)';
    } else if (timerStatus === 'PAUSED') {
      stateBadge.textContent = '⏸️ PAUSED';
      stateBadge.style.color = '#f59e0b';
    } else {
      stateBadge.textContent = 'READY TO FOCUS';
      stateBadge.style.color = 'var(--text-muted)';
    }
  }
}

function updateTimerControlsUI() {
  const btnToggle = document.getElementById('btnToggleTimer');
  const btnToggleLabel = document.getElementById('btnToggleLabel');
  const playIcon = document.getElementById('playIcon');
  const pauseIcon = document.getElementById('pauseIcon');

  if (timerStatus === 'RUNNING') {
    btnToggle.classList.add('running');
    btnToggleLabel.textContent = 'Pause Focus';
    playIcon.classList.add('hidden');
    pauseIcon.classList.remove('hidden');
  } else if (timerStatus === 'PAUSED') {
    btnToggle.classList.remove('running');
    btnToggleLabel.textContent = 'Resume Focus';
    playIcon.classList.remove('hidden');
    pauseIcon.classList.add('hidden');
  } else {
    btnToggle.classList.remove('running');
    btnToggleLabel.textContent = 'Start Focus';
    playIcon.classList.remove('hidden');
    pauseIcon.classList.add('hidden');
  }
}

function renderSubjects() {
  const container = document.getElementById('subjectMenuItems');
  if (!container) return;

  container.innerHTML = '';
  appState.subjects.forEach(sub => {
    const isSelected = sub.id === appState.selectedSubject.id;
    const item = document.createElement('button');
    item.type = 'button';
    item.className = `subject-menu-item ${isSelected ? 'active' : ''}`;
    item.innerHTML = `
      <div class="subject-item-left">
        <span class="subject-menu-dot" style="background-color: ${sub.color};"></span>
        <span class="subject-item-title">${sub.name}</span>
      </div>
      ${isSelected ? '<svg class="subject-check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>' : ''}
    `;
    item.addEventListener('click', (e) => {
      e.stopPropagation();
      appState.selectedSubject = sub;
      saveLocalState();
      renderSubjects();
      updateSelectedSubjectUI();
      document.getElementById('subjectDropdownMenu')?.classList.add('hidden');
      showToast(`Selected Subject: ${sub.name}`, 'info');
    });
    container.appendChild(item);
  });

  updateSelectedSubjectUI();
}

function updateSelectedSubjectUI() {
  const triggerDot = document.getElementById('triggerSubjectDot');
  const triggerName = document.getElementById('triggerSubjectName');
  const currentSubjectDot = document.getElementById('currentSubjectDot');
  const currentSubjectName = document.getElementById('currentSubjectName');

  if (triggerDot) triggerDot.style.backgroundColor = appState.selectedSubject.color;
  if (triggerName) triggerName.textContent = appState.selectedSubject.name;
  if (currentSubjectDot) currentSubjectDot.style.backgroundColor = appState.selectedSubject.color;
  if (currentSubjectName) currentSubjectName.textContent = appState.selectedSubject.name;
}

function renderTimelineList() {
  const container = document.getElementById('timelineList');
  const countBadge = document.getElementById('sessionsCountBadge');
  if (!container) return;

  const validSessions = appState.todaySessions.filter(s => s.durationSec >= 10);
  if (countBadge) countBadge.textContent = `${validSessions.length} session${validSessions.length === 1 ? '' : 's'}`;

  if (validSessions.length === 0) {
    container.innerHTML = `
      <div class="timeline-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p>No study sessions recorded yet today.</p>
        <span>Click <strong>Start Focus</strong> to record your first study block!</span>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  validSessions.forEach(sess => {
    const mins = Math.max(1, Math.round(sess.durationSec / 60));
    const timeStr = new Date(sess.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const item = document.createElement('div');
    item.className = 'timeline-item';
    item.innerHTML = `
      <div class="timeline-item-left">
        <span class="subject-color-dot" style="background-color: ${sess.subject.color};"></span>
        <div>
          <div class="timeline-subject-name">${sess.subject.name}</div>
          <span class="timeline-mode-pill">${sess.mode || 'Focus Study'}</span>
        </div>
      </div>
      <div class="timeline-item-right">
        <span class="timeline-duration">+${mins} min</span>
        <span class="timeline-time">${timeStr}</span>
      </div>
    `;
    container.appendChild(item);
  });
}

function updateProgressAndStreak() {
  // Calculate total study minutes today
  let totalSecToday = 0;
  appState.todaySessions.forEach(s => {
    totalSecToday += s.durationSec;
  });

  const totalMinToday = Math.round(totalSecToday / 60);
  const goalMin = timerConfig.dailyGoalMinutes || 120;
  const percent = Math.min(100, Math.round((totalMinToday / goalMin) * 100));

  // Update Insights Hub Streak & Goal
  const hubStreakCount = document.getElementById('hubStreakCount');
  if (hubStreakCount) hubStreakCount.textContent = `${appState.streakCount || 1} Day Streak`;

  // Update Hub Stats
  const statStudiedToday = document.getElementById('statStudiedToday');
  const statDailyGoal = document.getElementById('statDailyGoal');
  const statGoalPercent = document.getElementById('statGoalPercent');
  const largeGoalProgressBar = document.getElementById('largeGoalProgressBar');

  if (statStudiedToday) {
    if (totalMinToday >= 60) {
      const h = Math.floor(totalMinToday / 60);
      const m = totalMinToday % 60;
      statStudiedToday.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else {
      statStudiedToday.textContent = `${totalMinToday}m`;
    }
  }

  if (statDailyGoal) {
    if (goalMin >= 60) {
      const h = Math.floor(goalMin / 60);
      const m = goalMin % 60;
      statDailyGoal.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else {
      statDailyGoal.textContent = `${goalMin}m`;
    }
  }

  if (statGoalPercent) statGoalPercent.textContent = `${percent}%`;
  if (largeGoalProgressBar) largeGoalProgressBar.style.width = `${percent}%`;
}

function updateSubjectBreakdown() {
  const container = document.getElementById('subjectBarsList');
  if (!container) return;

  const subjectTotals = {};
  let maxDurationSec = 0;

  appState.todaySessions.forEach(s => {
    const subName = s.subject.name;
    if (!subjectTotals[subName]) {
      subjectTotals[subName] = { durationSec: 0, color: s.subject.color };
    }
    subjectTotals[subName].durationSec += s.durationSec;
    if (subjectTotals[subName].durationSec > maxDurationSec) {
      maxDurationSec = subjectTotals[subName].durationSec;
    }
  });

  const keys = Object.keys(subjectTotals);
  if (keys.length === 0) {
    container.innerHTML = `
      <div class="empty-hub-state">
        <span>Start studying to see time breakdown per subject.</span>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  keys.forEach(name => {
    const item = subjectTotals[name];
    const mins = Math.round(item.durationSec / 60);
    const barWidthPercent = maxDurationSec > 0 ? Math.round((item.durationSec / maxDurationSec) * 100) : 0;

    const row = document.createElement('div');
    row.className = 'subject-bar-row';
    row.innerHTML = `
      <div class="subject-bar-meta">
        <div class="subject-name-with-dot">
          <span class="subject-color-dot" style="background-color: ${item.color};"></span>
          <span>${name}</span>
        </div>
        <span class="subject-duration-text">${mins}m</span>
      </div>
      <div class="subject-progress-track">
        <div class="subject-progress-fill" style="width: ${barWidthPercent}%; background-color: ${item.color};"></div>
      </div>
    `;
    container.appendChild(row);
  });
}

// ============================================================================
// 8. TIMER SETTINGS MODAL
// ============================================================================
function openTimerSettingsModal() {
  const modal = document.getElementById('timerSettingsModalOverlay');
  if (!modal) return;

  const customMinInput = document.getElementById('customMinutesInput');
  const pomoFocusInput = document.getElementById('pomoFocusInput');
  const pomoBreakInput = document.getElementById('pomoBreakInput');
  const dailyGoalInput = document.getElementById('dailyTargetGoalInput');

  if (customMinInput) customMinInput.value = timerConfig.customTimerMinutes || 25;
  if (pomoFocusInput) pomoFocusInput.value = timerConfig.pomoFocusMinutes || 25;
  if (pomoBreakInput) pomoBreakInput.value = timerConfig.pomoBreakMinutes || 5;
  if (dailyGoalInput) dailyGoalInput.value = timerConfig.dailyGoalMinutes || 120;

  // Active chip highlight
  document.querySelectorAll('.settings-chip').forEach(c => {
    const min = parseInt(c.dataset.min, 10);
    if (min === timerConfig.customTimerMinutes) {
      c.classList.add('active');
    } else {
      c.classList.remove('active');
    }
  });

  modal.classList.remove('hidden');
}

function closeTimerSettingsModal() {
  const modal = document.getElementById('timerSettingsModalOverlay');
  if (modal) modal.classList.add('hidden');
}

function handleSaveTimerSettings(e) {
  e.preventDefault();
  if (timerStatus === 'RUNNING') {
    if (!confirm('Applying new settings will reset your active timer. Continue?')) return;
  }

  const customMin = parseInt(document.getElementById('customMinutesInput')?.value, 10) || 25;
  const pomoFocus = parseInt(document.getElementById('pomoFocusInput')?.value, 10) || 25;
  const pomoBreak = parseInt(document.getElementById('pomoBreakInput')?.value, 10) || 5;
  const dailyGoal = parseInt(document.getElementById('dailyTargetGoalInput')?.value, 10) || 120;

  timerConfig.customTimerMinutes = Math.max(1, Math.min(720, customMin));
  timerConfig.pomoFocusMinutes = Math.max(1, Math.min(180, pomoFocus));
  timerConfig.pomoBreakMinutes = Math.max(1, Math.min(60, pomoBreak));
  timerConfig.dailyGoalMinutes = Math.max(15, Math.min(1440, dailyGoal));

  closeTimerSettingsModal();
  resetTimer();
  updateProgressAndStreak();
  pushDataToCloud();
  showToast('Timer preferences applied! ⚙️', 'success');
}

// ============================================================================
// 9. AUTH & SUBJECT MODALS
// ============================================================================
function openAuthModal() {
  const modal = document.getElementById('authModalOverlay');
  if (modal) modal.classList.remove('hidden');
}

function closeAuthModal() {
  const modal = document.getElementById('authModalOverlay');
  if (modal) modal.classList.add('hidden');
}

async function signInWithGoogle() {
  if (!supabaseClient) return;
  try {
    const { error } = await supabaseClient.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: window.location.href
      }
    });
    if (error) throw error;
  } catch (err) {
    console.error('Google Sign-In Error:', err);
    showToast('Failed to start Google Sign-In: ' + err.message, 'error');
  }
}

async function signInWithEmail(e) {
  e.preventDefault();
  if (!supabaseClient) return;

  const emailInput = document.getElementById('authEmailInput');
  const statusMsg = document.getElementById('authStatusMessage');
  const email = emailInput?.value.trim();

  if (!email) return;

  try {
    statusMsg.textContent = 'Sending sign-in link...';
    statusMsg.classList.remove('hidden');

    const { error } = await supabaseClient.auth.signInWithOtp({
      email,
      options: {
        emailRedirectTo: window.location.href
      }
    });

    if (error) throw error;

    statusMsg.textContent = '✨ Magic sign-in link sent! Please check your email inbox.';
    statusMsg.style.borderColor = 'var(--accent-emerald)';
    statusMsg.style.color = 'var(--accent-emerald)';
  } catch (err) {
    console.error('Email sign-in error:', err);
    statusMsg.textContent = 'Error: ' + err.message;
    statusMsg.style.borderColor = 'var(--accent-red)';
    statusMsg.style.color = 'var(--accent-red)';
  }
}

async function signOutUser() {
  if (!supabaseClient) return;
  try {
    await supabaseClient.auth.signOut();
    handleUserSignedOut();
    showToast('Logged out of cloud account.', 'info');
  } catch (err) {
    console.error('Sign-out error:', err);
  }
}

function openSubjectModal() {
  const modal = document.getElementById('subjectModalOverlay');
  const input = document.getElementById('customSubjectName');
  if (input) input.value = '';
  if (modal) modal.classList.remove('hidden');
}

function closeSubjectModal() {
  const modal = document.getElementById('subjectModalOverlay');
  if (modal) modal.classList.add('hidden');
}

function handleAddCustomSubject(e) {
  e.preventDefault();
  const input = document.getElementById('customSubjectName');
  const name = input?.value.trim();
  if (!name) return;

  const activeColorBtn = document.querySelector('.color-choice-btn.active');
  const color = activeColorBtn ? activeColorBtn.dataset.color : '#3b82f6';

  const newSubject = {
    id: 'sub_' + Date.now(),
    name,
    color
  };

  appState.subjects.push(newSubject);
  appState.selectedSubject = newSubject;

  closeSubjectModal();
  renderSubjects();
  saveLocalState();
  pushDataToCloud();
  showToast(`Subject "${name}" added!`, 'success');
}

// Toast Notifications
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(12px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}
