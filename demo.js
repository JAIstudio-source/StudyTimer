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
  pomoLongBreakMinutes: 15,
  pomoTotalCycles: 4,
  pomoAutoSwitchBreak: true,
  pomoAutoSwitchFocus: true,
  dailyGoalMinutes: 120
};

let pomoCurrentCycle = 1;
let isLongBreakActive = false;
let pendingGoalIdToDelete = null;

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
  userProfile: {
    displayName: 'Student',
    avatarPreset: '🐱',
    motto: '🎯 Deep focus & daily consistency',
    primarySubjectId: 'math',
    isPublicLeaderboard: true
  },
  subjects: [...DEFAULT_SUBJECTS],
  selectedSubject: DEFAULT_SUBJECTS[0],
  plannerGoals: [
    { id: 'goal_1', subjectId: 'math', dailyMinutes: 60 },
    { id: 'goal_2', subjectId: 'coding', dailyMinutes: 90 }
  ],
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
  initTimerWorker();
  initBackgroundSyncListeners();
  renderSubjects();
  resetTimer();
  updateProgressAndStreak();
  renderSubjectDonutChart();
  renderActivityHeatmap();
  renderMonthlyCalendar();
  renderPlannerGoals();

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
        if (!appState.currentUser || appState.currentUser.id !== session.user.id) {
          handleUserSignedIn(session.user);
        }
      } else if (!session) {
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

  // Refresh presence & leaderboard on login
  if (timerStatus === 'RUNNING' && currentMode !== 'break') {
    startPresenceHeartbeat();
  }
  leaderboardCache.timestamp = 0;
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }

  pullDataFromCloud();
  showToast('Signed in! Cloud Sync active.', 'success');
}

function handleUserSignedOut() {
  stopPresenceHeartbeat();
  appState.currentUser = null;
  leaderboardCache.timestamp = 0;
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }

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
          if (prefs.pomo_long_break_minutes) timerConfig.pomoLongBreakMinutes = prefs.pomo_long_break_minutes;
          if (prefs.pomo_total_cycles) timerConfig.pomoTotalCycles = prefs.pomo_total_cycles;
          if (typeof prefs.pomo_auto_switch_break === 'boolean') timerConfig.pomoAutoSwitchBreak = prefs.pomo_auto_switch_break;
          if (typeof prefs.pomo_auto_switch_focus === 'boolean') timerConfig.pomoAutoSwitchFocus = prefs.pomo_auto_switch_focus;

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

          if (prefs.__planner_goals_data__) {
            try {
              const loadedGoals = typeof prefs.__planner_goals_data__ === 'string'
                ? JSON.parse(prefs.__planner_goals_data__)
                : prefs.__planner_goals_data__;
              if (Array.isArray(loadedGoals)) {
                appState.plannerGoals = loadedGoals;
              }
            } catch (e) {
              console.error('Failed to parse remote planner_goals', e);
            }
          }

          if (prefs.__user_profile__) {
            try {
              const loadedProfile = typeof prefs.__user_profile__ === 'string'
                ? JSON.parse(prefs.__user_profile__)
                : prefs.__user_profile__;
              if (loadedProfile && typeof loadedProfile === 'object') {
                appState.userProfile = { ...appState.userProfile, ...loadedProfile };
              }
            } catch (e) {
              console.error('Failed to parse remote user_profile', e);
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
      renderUserProfileUI();
      updateProgressAndStreak();
      renderSubjectDonutChart();
      renderActivityHeatmap();
      renderMonthlyCalendar();
      renderPlannerGoals();
      saveLocalState();
      if (timerStatus === 'IDLE') {
        resetTimer();
      }
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

let lastCloudPushTime = 0;
let cloudPushDebounceTimer = null;

async function pushDataToCloud() {
  saveLocalState();
  if (!supabaseClient || !appState.currentUser) return;

  // Rate Limiting & Cooldown Protection (Minimum 3 seconds between cloud API calls)
  const now = Date.now();
  if (now - lastCloudPushTime < 3000) {
    if (cloudPushDebounceTimer) clearTimeout(cloudPushDebounceTimer);
    cloudPushDebounceTimer = setTimeout(() => {
      pushDataToCloud();
    }, 3000);
    return;
  }
  lastCloudPushTime = now;

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const user = appState.currentUser;
    const userName = (appState.userProfile?.displayName || user.user_metadata?.full_name || user.email?.split('@')[0] || 'Student').slice(0, 100);
    const userEmail = (user.email || '').slice(0, 150);
    const profileImg = (user.user_metadata?.avatar_url || '').slice(0, 500);

    // Payload sanitization & safety caps (prevent bot memory overflow)
    const sanitizedSubjects = Array.isArray(appState.subjects) ? appState.subjects.slice(0, 50) : [];
    const sanitizedTimeline = Array.isArray(appState.timelineEntries) ? appState.timelineEntries.slice(-500) : [];
    const sanitizedGoals = Array.isArray(appState.plannerGoals) ? appState.plannerGoals.slice(0, 50) : [];

    const subjectTagsObj = {
      custom_subjects: JSON.stringify(sanitizedSubjects)
    };

    const prefsObj = {
      daily_goal_minutes: Math.min(1440, Math.max(1, Number(timerConfig.dailyGoalMinutes) || 120)),
      custom_timer_minutes: Math.min(720, Math.max(1, Number(timerConfig.customTimerMinutes) || 45)),
      pomo_focus_minutes: Math.min(180, Math.max(1, Number(timerConfig.pomoFocusMinutes) || 25)),
      pomo_break_minutes: Math.min(60, Math.max(1, Number(timerConfig.pomoBreakMinutes) || 5)),
      pomo_long_break_minutes: Math.min(120, Math.max(1, Number(timerConfig.pomoLongBreakMinutes) || 15)),
      pomo_total_cycles: Math.min(12, Math.max(1, Number(timerConfig.pomoTotalCycles) || 4)),
      pomo_auto_switch_break: timerConfig.pomoAutoSwitchBreak !== false,
      pomo_auto_switch_focus: timerConfig.pomoAutoSwitchFocus !== false,
      streak_count: Math.max(0, Number(appState.streakCount) || 0),
      last_study_date: appState.lastStudyDate || '',
      __subject_tags_data__: JSON.stringify(subjectTagsObj),
      __planner_goals_data__: JSON.stringify(sanitizedGoals),
      __user_profile__: JSON.stringify(appState.userProfile)
    };

    const payload = {
      user_id: user.id,
      user_name: userName,
      user_email: userEmail,
      profile_image_uri: profileImg,
      prefs_data: JSON.stringify(prefsObj),
      timeline_data: JSON.stringify(sanitizedTimeline),
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
  const endOfDayMs = startOfDayMs + 86400000;

  const sessions = [];
  const entries = (appState.timelineEntries || []).filter(e => e && e.t >= startOfDayMs && e.t < endOfDayMs);

  for (let i = 0; i < entries.length; i++) {
    const curr = entries[i];
    if (curr && (curr.s === 'STUDYING' || curr.s === 'POMODORO' || curr.s === 'COUNT_UP')) {
      let durationSec = curr.durationSec;
      let sessionEnd = curr.endT;

      if (typeof durationSec !== 'number' || durationSec <= 0) {
        const next = entries[i + 1];
        if (next && next.t > curr.t) {
          const diff = Math.round((next.t - curr.t) / 1000);
          durationSec = (diff > 0 && diff <= 28800) ? diff : 60;
          sessionEnd = next.t;
        } else {
          durationSec = 60;
          sessionEnd = curr.t + 60000;
        }
      }

      let modeLabel = 'Focus Study';
      if (curr.s === 'POMODORO') modeLabel = 'Pomodoro';
      else if (curr.s === 'COUNT_UP') modeLabel = 'Stopwatch';

      sessions.push({
        id: 'sess_' + curr.t,
        subject: {
          name: curr.subName || 'Focus Study',
          color: curr.subColor || '#3b82f6',
          id: curr.subId || 'default'
        },
        durationSec,
        startTime: curr.t,
        endTime: sessionEnd || (curr.t + durationSec * 1000),
        timestamp: curr.t,
        mode: modeLabel
      });
    }
  }

  appState.todaySessions = sessions.reverse();
}

// Universal extractor for all completed sessions with zero duplicate or hardcoded inflations
function getAllValidatedSessions() {
  const sessions = [];
  const seenKeys = new Set();

  // 1. Add verified sessions from today's real-time state first
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      sessions.push(s);
      const key = `${Math.floor(s.timestamp / 1000)}_${s.durationSec}`;
      seenKeys.add(key);
    }
  });

  // 2. Parse historical timeline entries
  const entries = appState.timelineEntries || [];
  for (let i = 0; i < entries.length; i++) {
    const curr = entries[i];
    if (curr && (curr.s === 'STUDYING' || curr.s === 'POMODORO' || curr.s === 'COUNT_UP')) {
      let durationSec = curr.durationSec;
      let sessionEnd = curr.endT;

      if (typeof durationSec !== 'number' || durationSec <= 0) {
        const next = entries[i + 1];
        if (next && next.t > curr.t) {
          const diff = Math.round((next.t - curr.t) / 1000);
          durationSec = (diff > 0 && diff <= 28800) ? diff : 60;
          sessionEnd = next.t;
        } else {
          durationSec = 60;
          sessionEnd = curr.t + 60000;
        }
      }

      const key = `${Math.floor(curr.t / 1000)}_${durationSec}`;
      if (!seenKeys.has(key)) {
        seenKeys.add(key);
        let modeLabel = 'Focus Study';
        if (curr.s === 'POMODORO') modeLabel = 'Pomodoro';
        else if (curr.s === 'COUNT_UP') modeLabel = 'Stopwatch';

        sessions.push({
          id: 'sess_' + curr.t,
          subject: {
            id: curr.subId || 'default',
            name: curr.subName || 'Focus Study',
            color: curr.subColor || '#3b82f6'
          },
          durationSec,
          startTime: curr.t,
          endTime: sessionEnd || (curr.t + durationSec * 1000),
          timestamp: curr.t,
          mode: modeLabel
        });
      }
    }
  }

  return sessions;
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
      if (parsed.userProfile) {
        appState.userProfile = { ...appState.userProfile, ...parsed.userProfile };
      }
      if (Array.isArray(parsed.plannerGoals)) {
        appState.plannerGoals = parsed.plannerGoals;
      }
      if (Array.isArray(parsed.timelineEntries)) {
        appState.timelineEntries = parsed.timelineEntries;
      }

      const startOfDay = new Date();
      startOfDay.setHours(0, 0, 0, 0);
      const startOfDayMs = startOfDay.getTime();
      const endOfDayMs = startOfDayMs + 86400000;

      if (Array.isArray(parsed.todaySessions) && parsed.todaySessions.length > 0) {
        // Filter strictly for verified sessions that occurred TODAY
        appState.todaySessions = parsed.todaySessions.filter(s => 
          s && typeof s.durationSec === 'number' && s.durationSec > 0 &&
          s.timestamp >= startOfDayMs && s.timestamp < endOfDayMs
        );
      } else if (Array.isArray(appState.timelineEntries) && appState.timelineEntries.length > 0) {
        reconstructTodaySessionsFromTimeline();
      } else {
        appState.todaySessions = [];
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
      userProfile: appState.userProfile,
      subjects: appState.subjects,
      plannerGoals: appState.plannerGoals || [],
      timelineEntries: appState.timelineEntries || [],
      todaySessions: appState.todaySessions || []
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
    case 'break': return (isLongBreakActive ? (timerConfig.pomoLongBreakMinutes || 15) : (timerConfig.pomoBreakMinutes || 5)) * 60;
    case 'stopwatch': return 0;
    default: return 25 * 60;
  }
}

function initDomElements() {
  // Mode Tabs
  document.querySelectorAll('.mode-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (btn.dataset.mode === currentMode) return;
      if (timerStatus === 'RUNNING' || (timerStatus === 'PAUSED' && accumulatedElapsedSec > 0)) {
        const confirmed = await showCustomConfirmDialog({
          title: 'Switch Timer Mode?',
          subtitle: 'Active session warning',
          message: 'Switching modes will reset your current timer and unsaved progress. Continue?',
          confirmText: 'Switch Mode',
          cancelText: 'Stay Here',
          isDanger: true
        });
        if (!confirmed) return;
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
  document.getElementById('btnResetTimer').addEventListener('click', handleUserResetTimer);
  document.getElementById('btnQuickReset')?.addEventListener('click', handleUserResetTimer);
  document.getElementById('btnFinishSession').addEventListener('click', () => finishSession(false));

  // Fullscreen Browser Zen Mode
  document.getElementById('btnBrowserFullscreen')?.addEventListener('click', toggleBrowserFullscreen);

  // 3-Tab Pill Switcher Navbar
  document.getElementById('tabBtnOverview')?.addEventListener('click', () => switchInsightsTab('overview'));
  document.getElementById('tabBtnCalendar')?.addEventListener('click', () => switchInsightsTab('calendar'));
  document.getElementById('tabBtnPlanner')?.addEventListener('click', () => switchInsightsTab('planner'));

  // Dedicated Floating Leaderboard Modal Trigger
  document.getElementById('btnMobileLeaderboard')?.addEventListener('click', openLeaderboardModal);
  document.getElementById('btnMenuLeaderboard')?.addEventListener('click', () => {
    document.getElementById('userMenuDropdown')?.classList.add('hidden');
    openLeaderboardModal();
  });
  document.getElementById('btnCloseLeaderboardModal')?.addEventListener('click', closeLeaderboardModal);
  document.getElementById('leaderboardModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'leaderboardModalOverlay') closeLeaderboardModal();
  });
  document.getElementById('btnRefreshLeaderboard')?.addEventListener('click', () => fetchLeaderboard(true));

  // Profile Customization Modal Trigger (Available upon user login in dropdown)
  document.getElementById('btnOpenProfileModal')?.addEventListener('click', () => {
    document.getElementById('userMenuDropdown')?.classList.add('hidden');
    openProfileModal();
  });
  document.getElementById('btnCloseProfileModal')?.addEventListener('click', closeProfileModal);
  document.getElementById('btnCancelProfileModal')?.addEventListener('click', closeProfileModal);
  document.getElementById('profileModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'profileModalOverlay') closeProfileModal();
  });

  // Avatar Presets Picker
  document.querySelectorAll('.avatar-preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.avatar-preset-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedAvatarPreset = btn.dataset.avatar || '🐱';
      updateProfileLivePreview();
    });
  });

  // Live Input Preview
  document.getElementById('inputProfileDisplayName')?.addEventListener('input', updateProfileLivePreview);
  document.getElementById('inputProfileMotto')?.addEventListener('input', updateProfileLivePreview);
  document.getElementById('profileCustomizationForm')?.addEventListener('submit', handleSaveProfile);

  // Mobile Insights Bottom Sheet Drawer & Backdrop
  document.getElementById('btnMobileInsights')?.addEventListener('click', openInsightsDrawer);
  document.getElementById('btnCloseMobileInsights')?.addEventListener('click', closeInsightsDrawer);
  document.getElementById('insightsBackdrop')?.addEventListener('click', closeInsightsDrawer);

  // Calendar Month Navigation
  document.getElementById('btnPrevMonth')?.addEventListener('click', () => changeCalendarMonth(-1));
  document.getElementById('btnNextMonth')?.addEventListener('click', () => changeCalendarMonth(1));

  // Planner Goals Modal Triggers
  document.getElementById('btnOpenAddGoalModal')?.addEventListener('click', openAddGoalModal);
  document.getElementById('btnCloseGoalModal')?.addEventListener('click', closeAddGoalModal);
  document.getElementById('plannerGoalModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'plannerGoalModalOverlay') closeAddGoalModal();
  });
  document.getElementById('addGoalForm')?.addEventListener('submit', handleAddGoal);

  // Goal Delete Confirmation Modal
  document.getElementById('btnConfirmDeleteGoal')?.addEventListener('click', confirmDeletePlannerGoal);
  document.getElementById('btnCancelDeleteGoal')?.addEventListener('click', closeDeleteGoalModal);
  document.getElementById('btnCloseDeleteGoalModal')?.addEventListener('click', closeDeleteGoalModal);
  document.getElementById('deleteGoalModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'deleteGoalModalOverlay') closeDeleteGoalModal();
  });

  // Post-Session Complete & Subject Switch Modal
  document.getElementById('btnKeepSameSubject')?.addEventListener('click', closeSessionCompleteModal);
  document.getElementById('btnApplyNextSubject')?.addEventListener('click', applyNextSessionSubject);
  document.getElementById('btnCloseSessionCompleteModal')?.addEventListener('click', closeSessionCompleteModal);
  document.getElementById('sessionCompleteModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'sessionCompleteModalOverlay') closeSessionCompleteModal();
  });

  // Delete All Local Data Modal
  document.getElementById('btnOpenDeleteAllDataModal')?.addEventListener('click', openDeleteAllDataModal);
  document.getElementById('btnConfirmDeleteAllData')?.addEventListener('click', confirmDeleteAllData);
  document.getElementById('btnCancelDeleteAllData')?.addEventListener('click', closeDeleteAllDataModal);
  document.getElementById('btnCloseDeleteAllDataModal')?.addEventListener('click', closeDeleteAllDataModal);
  document.getElementById('deleteAllDataModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'deleteAllDataModalOverlay') closeDeleteAllDataModal();
  });

  // Custom Confirmation Dialog Modal
  document.getElementById('btnOkCustomConfirm')?.addEventListener('click', () => closeCustomConfirmDialog(true));
  document.getElementById('btnCancelCustomConfirm')?.addEventListener('click', () => closeCustomConfirmDialog(false));
  document.getElementById('btnCloseCustomConfirmModal')?.addEventListener('click', () => closeCustomConfirmDialog(false));
  document.getElementById('customConfirmModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'customConfirmModalOverlay') closeCustomConfirmDialog(false);
  });

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

  // Subject Dropdown Menu Toggles (Main & Full Screen)
  const btnSubjectTrigger = document.getElementById('btnSubjectMenuTrigger');
  const timerSubjectDisplay = document.getElementById('timerSubjectDisplay');
  const subjectDropdownMenu = document.getElementById('subjectDropdownMenu');

  const zenSubjectDisplay = document.getElementById('zenSubjectDisplay');
  const zenSubjectDropdownMenu = document.getElementById('zenSubjectDropdownMenu');

  function toggleMainSubjectMenu(e) {
    if (timerStatus === 'RUNNING') return;
    e.stopPropagation();
    if (subjectDropdownMenu) {
      subjectDropdownMenu.classList.toggle('hidden');
    }
  }

  function toggleZenSubjectMenu(e) {
    if (timerStatus === 'RUNNING') return;
    e.stopPropagation();
    if (zenSubjectDropdownMenu) {
      zenSubjectDropdownMenu.classList.toggle('hidden');
    }
  }

  btnSubjectTrigger?.addEventListener('click', toggleMainSubjectMenu);
  timerSubjectDisplay?.addEventListener('click', toggleMainSubjectMenu);
  zenSubjectDisplay?.addEventListener('click', toggleZenSubjectMenu);

  document.addEventListener('click', (e) => {
    if (subjectDropdownMenu && !subjectDropdownMenu.contains(e.target) && !btnSubjectTrigger?.contains(e.target) && !timerSubjectDisplay?.contains(e.target)) {
      subjectDropdownMenu.classList.add('hidden');
    }
    if (zenSubjectDropdownMenu && !zenSubjectDropdownMenu.contains(e.target) && !zenSubjectDisplay?.contains(e.target)) {
      zenSubjectDropdownMenu.classList.add('hidden');
    }
  });

  // Subject Modal Triggers
  document.getElementById('btnAddSubject')?.addEventListener('click', (e) => {
    e.stopPropagation();
    subjectDropdownMenu?.classList.add('hidden');
    openSubjectModal();
  });
  document.getElementById('btnZenAddSubject')?.addEventListener('click', (e) => {
    e.stopPropagation();
    zenSubjectDropdownMenu?.classList.add('hidden');
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

  // Full Screen Focus Mode
  document.getElementById('btnZenTimer')?.addEventListener('click', openZenMode);
  document.getElementById('btnExitZen')?.addEventListener('click', closeZenMode);
  document.getElementById('btnZenToggle')?.addEventListener('click', toggleTimer);
  document.getElementById('btnZenSave')?.addEventListener('click', () => {
    finishSession();
  });

  // Tap on full screen canvas (outside buttons & subject menu) to toggle play/pause
  document.getElementById('zenTimerCanvas')?.addEventListener('click', (e) => {
    if (!e.target.closest('.zen-controls-bar') && !e.target.closest('.zen-exit-btn') && !e.target.closest('#zenSubjectWrapper')) {
      toggleTimer();
    }
  });

  // Global keybindings
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      const zenOverlay = document.getElementById('zenTimerOverlay');
      if (zenOverlay && !zenOverlay.classList.contains('hidden')) {
        closeZenMode();
      }
      const lbModal = document.getElementById('leaderboardModalOverlay');
      if (lbModal && !lbModal.classList.contains('hidden')) {
        closeLeaderboardModal();
      }
      const profileModal = document.getElementById('profileModalOverlay');
      if (profileModal && !profileModal.classList.contains('hidden')) {
        closeProfileModal();
      }
    }
    // Spacebar to toggle timer when not in input
    if (e.code === 'Space' && e.target.tagName !== 'INPUT' && e.target.tagName !== 'TEXTAREA') {
      e.preventDefault();
      toggleTimer();
    }
  });
}

function openZenMode() {
  const zenOverlay = document.getElementById('zenTimerOverlay');
  if (!zenOverlay) return;

  zenOverlay.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
  document.documentElement.style.overflow = 'hidden';

  updateTimerDisplay();
  updateTimerControlsUI();

  // Try native fullscreen if available
  if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
    document.documentElement.requestFullscreen().catch(() => {});
  }
  showToast('Entered Full Screen Mode ⛶', 'info');
}

function closeZenMode() {
  const zenOverlay = document.getElementById('zenTimerOverlay');
  if (zenOverlay) zenOverlay.classList.add('hidden');

  document.body.style.overflow = '';
  document.documentElement.style.overflow = '';

  if (document.fullscreenElement && document.exitFullscreen) {
    document.exitFullscreen().catch(() => {});
  }
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
    if (modeKey === 'timer') {
      modeBadge.textContent = '⏱️ Countdown Timer';
    } else if (modeKey === 'pomodoro') {
      const total = timerConfig.pomoTotalCycles || 4;
      modeBadge.textContent = `🍅 Pomodoro Focus (Cycle ${pomoCurrentCycle}/${total})`;
    } else if (modeKey === 'stopwatch') {
      modeBadge.textContent = '⚡ Stopwatch';
    } else if (modeKey === 'break') {
      const dur = isLongBreakActive ? (timerConfig.pomoLongBreakMinutes || 15) : (timerConfig.pomoBreakMinutes || 5);
      modeBadge.textContent = `☕ ${isLongBreakActive ? 'Long Break' : 'Short Break'} (${dur}m)`;
    }
  }

  // Synchronize top mode buttons
  document.querySelectorAll('.mode-btn').forEach(btn => {
    if (btn.dataset.mode === modeKey) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  resetTimer();
}

function toggleTimer() {
  if (timerStatus === 'RUNNING') {
    pauseTimer();
  } else {
    startTimer();
  }
}

// ============================================================================
// BACKGROUND WORKER TICKER & DESKTOP/ANDROID BACKGROUND SYNC
// ============================================================================
let timerWorker = null;
let wakeLockSentinel = null;

function initTimerWorker() {
  try {
    const workerCode = `
      let interval = null;
      self.onmessage = function(e) {
        if (e.data === 'start') {
          if (interval) clearInterval(interval);
          interval = setInterval(() => {
            self.postMessage('tick');
          }, 250);
        } else if (e.data === 'stop') {
          if (interval) {
            clearInterval(interval);
            interval = null;
          }
        }
      };
    `;
    const blob = new Blob([workerCode], { type: 'application/javascript' });
    const workerUrl = URL.createObjectURL(blob);
    timerWorker = new Worker(workerUrl);
    timerWorker.onmessage = function(e) {
      if (e.data === 'tick') {
        tickTimer();
      }
    };
  } catch (err) {
    console.warn('Web Worker ticker unsupported, falling back to standard interval:', err);
    timerWorker = null;
  }
}

function initBackgroundSyncListeners() {
  // Seamless sync when returning from minimized window, locked screen, or other tabs
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      if (timerStatus === 'RUNNING') {
        tickTimer();
        requestWakeLock();
      }
    }
  });

  window.addEventListener('focus', () => {
    if (timerStatus === 'RUNNING') {
      tickTimer();
    }
  });

  window.addEventListener('pageshow', () => {
    if (timerStatus === 'RUNNING') {
      tickTimer();
    }
  });
}

// Request Screen Wake Lock (keeps screen on during active focus study)
async function requestWakeLock() {
  if ('wakeLock' in navigator) {
    try {
      if (!wakeLockSentinel) {
        wakeLockSentinel = await navigator.wakeLock.request('screen');
        wakeLockSentinel.addEventListener('release', () => {
          wakeLockSentinel = null;
        });
      }
    } catch (err) {
      // Ignored if device is low battery or tab is not active
    }
  }
}

function releaseWakeLock() {
  if (wakeLockSentinel) {
    wakeLockSentinel.release().catch(() => {});
    wakeLockSentinel = null;
  }
}

function tickTimer() {
  if (timerStatus !== 'RUNNING' || !timerStartTimestamp) return;

  const now = Date.now();
  const elapsedSinceResume = Math.floor((now - timerStartTimestamp) / 1000);
  const totalElapsedSec = accumulatedElapsedSec + elapsedSinceResume;

  if (currentMode === 'stopwatch') {
    stopwatchElapsed = totalElapsedSec;
  } else {
    const totalSec = getModeDurationSec();
    timeRemaining = Math.max(0, totalSec - totalElapsedSec);
    if (timeRemaining === 0) {
      finishSession(true);
      return;
    }
  }
  updateTimerDisplay();
}

function startTimer() {
  timerStatus = 'RUNNING';
  timerStartTimestamp = Date.now();

  updateTimerControlsUI();
  requestWakeLock();

  if (currentMode !== 'break') {
    startPresenceHeartbeat();
  }

  if (timerWorker) {
    timerWorker.postMessage('start');
  }

  if (timerInterval) clearInterval(timerInterval);
  timerInterval = setInterval(tickTimer, 250);
}

function pauseTimer() {
  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    const elapsedSinceResume = Math.floor((Date.now() - timerStartTimestamp) / 1000);
    accumulatedElapsedSec += elapsedSinceResume;
  }
  timerStatus = 'PAUSED';
  timerStartTimestamp = null;
  stopPresenceHeartbeat();
  stopInterval();
  updateTimerControlsUI();
  updateTimerDisplay();
}

async function handleUserResetTimer() {
  if (timerStatus === 'RUNNING' || timerStatus === 'PAUSED' || accumulatedElapsedSec > 0) {
    const confirmed = await showCustomConfirmDialog({
      title: 'Reset Active Timer?',
      subtitle: 'Unsaved focus progress warning',
      message: 'Are you sure you want to reset the timer? Current unsaved session progress will be lost.',
      confirmText: 'Reset Timer',
      cancelText: 'Keep Focus',
      isDanger: true
    });
    if (!confirmed) return;
  }
  resetTimer();
  showToast('Timer reset', 'info');
}

function resetTimer() {
  stopPresenceHeartbeat();
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
  if (timerWorker) {
    timerWorker.postMessage('stop');
  }
  releaseWakeLock();
}

function finishSession(isAutoFinished = false) {
  let currentElapsed = accumulatedElapsedSec;
  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    currentElapsed += Math.floor((Date.now() - timerStartTimestamp) / 1000);
  }

  if (currentElapsed < 10 && !isAutoFinished) {
    showToast('Session too short to save (< 10s). Focus a bit longer!', 'info');
    return;
  }

  let studiedDurationSec = Math.max(isAutoFinished ? 60 : 1, currentElapsed);
  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    const elapsedSinceResume = Math.floor((Date.now() - timerStartTimestamp) / 1000);
    accumulatedElapsedSec += elapsedSinceResume;
  }

  const now = Date.now();
  const startMs = now - (studiedDurationSec * 1000);
  const subject = appState.selectedSubject;
  const prevMode = currentMode;

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
      subColor: subject.color,
      durationSec: studiedDurationSec,
      endT: now
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
      startTime: startMs,
      endTime: now,
      timestamp: startMs,
      mode: modeLabel
    };

    appState.todaySessions.unshift(newSession);

    // Update Streak
    checkAndUpdateStreak();
  }

  resetTimer();
  updateProgressAndStreak();
  renderSubjectDonutChart();
  renderActivityHeatmap();
  renderMonthlyCalendar();
  renderPlannerGoals();
  saveLocalState();

  // Log to Supabase Cloud Leaderboard & sync
  if (stateKey !== 'BREAK' && studiedDurationSec >= 10) {
    logSessionToLeaderboard(studiedDurationSec, subject);
  }

  // Only push to cloud database if session is at least 1 minute (>= 60s) and not a break
  if (stateKey !== 'BREAK' && studiedDurationSec >= 60) {
    pushDataToCloud();
  }

  triggerSaveSuccessFeedback();

  if (stateKey === 'BREAK') {
    showToast('☕ Break finished! Ready to focus.', 'info');

    // Automation: Auto-switch back to Pomodoro Focus after break
    if (timerConfig.pomoAutoSwitchFocus !== false) {
      isLongBreakActive = false;
      setTimeout(() => {
        switchMode('pomodoro');
        const totalCycles = timerConfig.pomoTotalCycles || 4;
        showToast(`🍅 Ready for Pomodoro Focus (Cycle ${pomoCurrentCycle}/${totalCycles})`, 'info');
      }, 500);
    }
  } else {
    const minStr = Math.max(1, Math.round(studiedDurationSec / 60));
    if (studiedDurationSec >= 60) {
      showToast(`🎉 Focus session saved! +${minStr}m added to ${subject.name}`, 'success');
    } else {
      showToast(`✓ Session saved locally (+${studiedDurationSec}s). Cloud sync activates after 1 min.`, 'info');
    }

    // Give option to change subject after session ended
    openSessionCompleteModal(subject, minStr);

    // Pomodoro Automation: Auto-switch to break
    if (prevMode === 'pomodoro' && timerConfig.pomoAutoSwitchBreak !== false) {
      const totalCycles = timerConfig.pomoTotalCycles || 4;
      if (pomoCurrentCycle >= totalCycles) {
        isLongBreakActive = true;
        pomoCurrentCycle = 1;
      } else {
        isLongBreakActive = false;
        pomoCurrentCycle++;
      }

      setTimeout(() => {
        switchMode('break');
      }, 600);
    }
  }
}

let saveFeedbackTimeout = null;
function triggerSaveSuccessFeedback() {
  const btnMainSave = document.getElementById('btnFinishSession');
  const btnZenSave = document.getElementById('btnZenSave');

  if (saveFeedbackTimeout) clearTimeout(saveFeedbackTimeout);

  if (btnMainSave) {
    btnMainSave.classList.add('saved-success');
    btnMainSave.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>✓ Saved!</span>
    `;
  }

  if (btnZenSave) {
    btnZenSave.classList.add('saved-success');
    btnZenSave.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>✓ Saved!</span>
    `;
  }

  // Reset to original state after 1 minute (60,000ms)
  saveFeedbackTimeout = setTimeout(() => {
    resetSaveButtonState();
  }, 60000);
}

function resetSaveButtonState() {
  const isBreak = currentMode === 'break';
  const finishLabel = isBreak ? 'Finish Break' : 'Finish &amp; Save';
  const zenFinishLabel = isBreak ? 'End Break' : 'Save Session';

  const btnMainSave = document.getElementById('btnFinishSession');
  const btnZenSave = document.getElementById('btnZenSave');

  if (btnMainSave) {
    btnMainSave.classList.remove('saved-success');
    btnMainSave.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>${finishLabel}</span>
    `;
  }

  if (btnZenSave) {
    btnZenSave.classList.remove('saved-success');
    btnZenSave.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>${zenFinishLabel}</span>
    `;
  }
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

  const zenTimerDisplay = document.getElementById('zenTimerDisplay');
  if (zenTimerDisplay) zenTimerDisplay.textContent = timeFormatted;

  // Title tag update
  if (timerStatus === 'RUNNING') {
    const actionLabel = currentMode === 'break' ? 'Break' : 'Focus';
    document.title = `(${timeFormatted}) StudyTimer ${actionLabel}`;
  } else {
    document.title = `StudyTimer Web - Focus Timer &amp; Habit Tracker for Students`;
  }

  // Update Main Indicator Ring
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

  // Update Zen Indicator Ring
  const zenRing = document.getElementById('zenTimerIndicator');
  if (zenRing) {
    const zenCircumference = 1130.97; // 2 * PI * 180
    const zenOffset = zenCircumference * (1 - progressRatio);
    zenRing.style.strokeDashoffset = zenOffset;

    if (currentMode === 'break') {
      zenRing.style.stroke = 'var(--accent-emerald)';
    } else if (currentMode === 'pomodoro') {
      zenRing.style.stroke = 'var(--accent-red)';
    } else {
      zenRing.style.stroke = 'var(--accent-blue)';
    }
  }

  // State Badge Text and Colors
  const stateBadge = document.getElementById('timerStateBadge');
  const zenStateBadge = document.getElementById('zenStateBadge');
  let stateText = 'READY TO FOCUS';
  let stateColor = 'var(--text-muted)';

  if (currentMode === 'break') {
    if (timerStatus === 'RUNNING') {
      stateText = '☕ TAKING A BREAK';
      stateColor = 'var(--accent-emerald)';
    } else if (timerStatus === 'PAUSED') {
      stateText = '⏸️ BREAK PAUSED';
      stateColor = '#f59e0b';
    } else {
      stateText = '☕ READY FOR BREAK';
      stateColor = 'var(--accent-emerald)';
    }
  } else {
    if (timerStatus === 'RUNNING') {
      stateText = currentMode === 'pomodoro' ? '🔥 POMODORO FOCUS' : '🔥 FOCUSING';
      stateColor = currentMode === 'pomodoro' ? 'var(--accent-red)' : 'var(--accent-blue)';
    } else if (timerStatus === 'PAUSED') {
      stateText = '⏸️ PAUSED';
      stateColor = '#f59e0b';
    } else {
      stateText = 'READY TO FOCUS';
      stateColor = 'var(--text-muted)';
    }
  }

  if (stateBadge) {
    stateBadge.textContent = stateText;
    stateBadge.style.color = stateColor;
  }
  if (zenStateBadge) {
    let zenText = 'READY TO FOCUS';
    if (currentMode === 'break') {
      zenText = timerStatus === 'RUNNING' ? 'BREAK' : (timerStatus === 'PAUSED' ? 'BREAK PAUSED' : 'READY FOR BREAK');
    } else {
      zenText = timerStatus === 'RUNNING' ? 'FOCUSING' : (timerStatus === 'PAUSED' ? 'PAUSED' : 'READY TO FOCUS');
    }
    zenStateBadge.textContent = zenText;
    zenStateBadge.style.color = 'var(--text-muted)';
  }
}

function updateTimerControlsUI() {
  const isBreak = currentMode === 'break';
  const btnToggle = document.getElementById('btnToggleTimer');
  const btnToggleLabel = document.getElementById('btnToggleLabel');
  const playIcon = document.getElementById('playIcon');
  const pauseIcon = document.getElementById('pauseIcon');

  const btnZenToggle = document.getElementById('btnZenToggle');
  const zenToggleLabel = document.getElementById('zenToggleLabel');
  const zenPlayIcon = document.getElementById('zenPlayIcon');
  const zenPauseIcon = document.getElementById('zenPauseIcon');

  const btnFinishSession = document.getElementById('btnFinishSession');
  const btnZenSave = document.getElementById('btnZenSave');

  const subjectDropdownWrapper = document.getElementById('subjectDropdownWrapper');
  const timerSubjectDisplay = document.getElementById('timerSubjectDisplay');
  const zenSubjectDisplay = document.getElementById('zenSubjectDisplay');
  const subjectDropdownMenu = document.getElementById('subjectDropdownMenu');
  const zenSubjectDropdownMenu = document.getElementById('zenSubjectDropdownMenu');

  const startLabel = isBreak ? 'Start Break' : 'Start Focus';
  const pauseLabel = isBreak ? 'Pause Break' : 'Pause Focus';
  const resumeLabel = isBreak ? 'Resume Break' : 'Resume Focus';
  const finishLabel = isBreak ? 'Finish Break' : 'Finish &amp; Save';
  const zenFinishLabel = isBreak ? 'End Break' : 'Save Session';

  if (btnFinishSession && !btnFinishSession.classList.contains('saved-success')) {
    btnFinishSession.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>${finishLabel}</span>
    `;
  }

  if (btnZenSave && !btnZenSave.classList.contains('saved-success')) {
    btnZenSave.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
      <span>${zenFinishLabel}</span>
    `;
  }

  if (timerStatus === 'RUNNING') {
    btnToggle?.classList.add('running');
    if (btnToggleLabel) btnToggleLabel.textContent = pauseLabel;
    playIcon?.classList.add('hidden');
    pauseIcon?.classList.remove('hidden');

    btnZenToggle?.classList.add('running');
    if (zenToggleLabel) zenToggleLabel.textContent = pauseLabel;
    zenPlayIcon?.classList.add('hidden');
    zenPauseIcon?.classList.remove('hidden');

    // Lock subject changing mid-session
    subjectDropdownWrapper?.classList.add('session-running');
    timerSubjectDisplay?.classList.add('timer-subject-locked');
    zenSubjectDisplay?.classList.add('timer-subject-locked');
    subjectDropdownMenu?.classList.add('hidden');
    zenSubjectDropdownMenu?.classList.add('hidden');
  } else if (timerStatus === 'PAUSED') {
    btnToggle?.classList.remove('running');
    if (btnToggleLabel) btnToggleLabel.textContent = resumeLabel;
    playIcon?.classList.remove('hidden');
    pauseIcon?.classList.add('hidden');

    btnZenToggle?.classList.remove('running');
    if (zenToggleLabel) zenToggleLabel.textContent = resumeLabel;
    zenPlayIcon?.classList.remove('hidden');
    zenPauseIcon?.classList.add('hidden');

    subjectDropdownWrapper?.classList.remove('session-running');
    timerSubjectDisplay?.classList.remove('timer-subject-locked');
    zenSubjectDisplay?.classList.remove('timer-subject-locked');
  } else {
    btnToggle?.classList.remove('running');
    if (btnToggleLabel) btnToggleLabel.textContent = startLabel;
    playIcon?.classList.remove('hidden');
    pauseIcon?.classList.add('hidden');

    btnZenToggle?.classList.remove('running');
    if (zenToggleLabel) zenToggleLabel.textContent = startLabel;
    zenPlayIcon?.classList.remove('hidden');
    zenPauseIcon?.classList.add('hidden');

    subjectDropdownWrapper?.classList.remove('session-running');
    timerSubjectDisplay?.classList.remove('timer-subject-locked');
    zenSubjectDisplay?.classList.remove('timer-subject-locked');
  }

  updateSelectedSubjectUI();
}

function renderSubjects() {
  const mainContainer = document.getElementById('subjectMenuItems');
  const zenContainer = document.getElementById('zenSubjectMenuItems');

  function populateList(container) {
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
        if (timerStatus === 'RUNNING' || currentMode === 'break') return;
        e.stopPropagation();
        appState.selectedSubject = sub;
        saveLocalState();
        renderSubjects();
        updateSelectedSubjectUI();
        document.getElementById('subjectDropdownMenu')?.classList.add('hidden');
        document.getElementById('zenSubjectDropdownMenu')?.classList.add('hidden');
        showToast(`Selected Subject: ${sub.name}`, 'info');
      });
      container.appendChild(item);
    });
  }

  populateList(mainContainer);
  populateList(zenContainer);

  updateSelectedSubjectUI();
}

function updateSelectedSubjectUI() {
  const isBreak = currentMode === 'break';
  const triggerDot = document.getElementById('triggerSubjectDot');
  const triggerName = document.getElementById('triggerSubjectName');
  const currentSubjectDot = document.getElementById('currentSubjectDot');
  const currentSubjectName = document.getElementById('currentSubjectName');
  const zenSubjectDot = document.getElementById('zenSubjectDot');
  const zenSubjectName = document.getElementById('zenSubjectName');

  const subjectDropdownWrapper = document.getElementById('subjectDropdownWrapper');
  const timerSubjectDisplay = document.getElementById('timerSubjectDisplay');
  const zenSubjectDisplay = document.getElementById('zenSubjectDisplay');

  if (isBreak) {
    subjectDropdownWrapper?.classList.add('is-break-mode');
    timerSubjectDisplay?.classList.add('is-break-mode');
    zenSubjectDisplay?.classList.add('is-break-mode');

    if (currentSubjectName) currentSubjectName.textContent = '☕ Rest & Recharge';
    if (zenSubjectName) zenSubjectName.textContent = '☕ Rest & Recharge';
  } else {
    subjectDropdownWrapper?.classList.remove('is-break-mode');
    timerSubjectDisplay?.classList.remove('is-break-mode');
    zenSubjectDisplay?.classList.remove('is-break-mode');

    if (triggerDot) triggerDot.style.backgroundColor = appState.selectedSubject.color;
    if (triggerName) triggerName.textContent = appState.selectedSubject.name;
    if (currentSubjectDot) currentSubjectDot.style.backgroundColor = appState.selectedSubject.color;
    if (currentSubjectName) currentSubjectName.textContent = appState.selectedSubject.name;
    if (zenSubjectDot) zenSubjectDot.style.backgroundColor = appState.selectedSubject.color;
    if (zenSubjectName) zenSubjectName.textContent = appState.selectedSubject.name;
  }
}

// ============================================================================
// 7. INSIGHTS HUB (3-TAB ARCHITECTURE & ANALYTICS SUITE)
// ============================================================================

let currentInsightsTab = 'overview';

function switchInsightsTab(tabName) {
  currentInsightsTab = tabName;
  
  const tabs = ['overview', 'calendar', 'planner'];
  tabs.forEach(t => {
    const btn = document.getElementById(`tabBtn${t.charAt(0).toUpperCase() + t.slice(1)}`);
    const panel = document.getElementById(`panel${t.charAt(0).toUpperCase() + t.slice(1)}`);
    if (t === tabName) {
      btn?.classList.add('active');
      panel?.classList.remove('hidden');
    } else {
      btn?.classList.remove('active');
      panel?.classList.add('hidden');
    }
  });

  if (tabName === 'overview') {
    updateProgressAndStreak();
    renderSubjectDonutChart();
    renderActivityHeatmap();
  } else if (tabName === 'calendar') {
    renderMonthlyCalendar();
  } else if (tabName === 'planner') {
    renderPlannerGoals();
  }
}

// Mobile Bottom Sheet Drawer Controls
function openInsightsDrawer() {
  const hub = document.getElementById('insightsHub');
  const backdrop = document.getElementById('insightsBackdrop');
  hub?.classList.add('open');
  backdrop?.classList.add('open');
  backdrop?.classList.remove('hidden');
  document.body.style.overflow = 'hidden';

  // Trigger render of current active tab
  switchInsightsTab(currentInsightsTab);
}

function closeInsightsDrawer() {
  const hub = document.getElementById('insightsHub');
  const backdrop = document.getElementById('insightsBackdrop');
  hub?.classList.remove('open');
  backdrop?.classList.remove('open');
  setTimeout(() => {
    backdrop?.classList.add('hidden');
  }, 300);
  document.body.style.overflow = '';
}

// ----------------------------------------------------------------------------
// TAB 1: OVERVIEW (Daily Goal, Donut Chart, 52-Week Heatmap)
// ----------------------------------------------------------------------------

function updateProgressAndStreak() {
  let totalSecToday = 0;
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      totalSecToday += s.durationSec;
    }
  });

  const totalMinToday = Math.round(totalSecToday / 60);
  const goalMin = Math.max(1, timerConfig.dailyGoalMinutes || 120);
  const goalSec = goalMin * 60;
  const percent = Math.min(100, Math.round((totalSecToday / goalSec) * 100));
  const remainingSec = Math.max(0, goalSec - totalSecToday);
  const remainingMin = Math.ceil(remainingSec / 60);

  const hubStreakCount = document.getElementById('hubStreakCount');
  if (hubStreakCount) hubStreakCount.textContent = `${appState.streakCount || 1} Day Streak`;

  const statStudiedToday = document.getElementById('statStudiedToday');
  const statDailyGoal = document.getElementById('statDailyGoal');
  const statGoalRemaining = document.getElementById('statGoalRemaining');
  const statGoalPercent = document.getElementById('statGoalPercent');
  const largeGoalProgressBar = document.getElementById('largeGoalProgressBar');

  if (statStudiedToday) {
    if (totalSecToday === 0) {
      statStudiedToday.textContent = '0m';
    } else if (totalMinToday >= 60) {
      const h = Math.floor(totalMinToday / 60);
      const m = totalMinToday % 60;
      statStudiedToday.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMinToday === 0 && totalSecToday > 0) {
      statStudiedToday.textContent = `${totalSecToday}s`;
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

  if (statGoalRemaining) {
    if (totalSecToday >= goalSec) {
      statGoalRemaining.textContent = 'Goal Met! 🎉';
      statGoalRemaining.style.color = 'var(--accent-emerald)';
    } else {
      if (remainingMin >= 60) {
        const h = Math.floor(remainingMin / 60);
        const m = remainingMin % 60;
        statGoalRemaining.textContent = `${h}h ${m > 0 ? m + 'm' : ''} left`;
      } else {
        statGoalRemaining.textContent = `${remainingMin}m left`;
      }
      statGoalRemaining.style.color = '#f59e0b';
    }
  }

  if (statGoalPercent) statGoalPercent.textContent = `${percent}%`;
  if (largeGoalProgressBar) largeGoalProgressBar.style.width = `${percent}%`;
}

// Interactive Subject Distribution Donut / Pie Chart (SubjectPieChartView.kt)
let activeHighlightedSubjectId = null;

function renderSubjectDonutChart() {
  const svg = document.getElementById('subjectDonutSvg');
  const svgWrap = document.getElementById('donutSvgWrap');
  const totalBadge = document.getElementById('donutTotalBadge');
  const centerSub = document.getElementById('donutCenterSub');
  const centerVal = document.getElementById('donutCenterVal');
  const centerPct = document.getElementById('donutCenterPct');
  const legendList = document.getElementById('donutLegendList');
  if (!svg || !legendList) return;

  const subjectTotals = {};
  let totalSecToday = 0;

  (appState.todaySessions || []).forEach(s => {
    if (s && s.durationSec > 0) {
      const subId = s.subject?.id || s.subject?.name || 'default';
      if (!subjectTotals[subId]) {
        subjectTotals[subId] = {
          id: subId,
          name: s.subject?.name || 'Focus Study',
          color: s.subject?.color || '#3b82f6',
          durationSec: 0
        };
      }
      subjectTotals[subId].durationSec += s.durationSec;
      totalSecToday += s.durationSec;
    }
  });

  const totalMinToday = Math.round(totalSecToday / 60);
  let formattedTotal = '0m';
  if (totalSecToday > 0) {
    if (totalMinToday >= 60) {
      formattedTotal = `${Math.floor(totalMinToday / 60)}h ${totalMinToday % 60 > 0 ? (totalMinToday % 60) + 'm' : ''}`;
    } else if (totalMinToday === 0) {
      formattedTotal = `${totalSecToday}s`;
    } else {
      formattedTotal = `${totalMinToday}m`;
    }
  }

  if (totalBadge) totalBadge.textContent = `${formattedTotal} total`;
  if (centerSub) centerSub.textContent = 'Today';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  activeHighlightedSubjectId = null;

  const bgTrack = svg.querySelector('.donut-bg-track');
  svg.innerHTML = '';
  if (bgTrack) {
    svg.appendChild(bgTrack);
  } else {
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);
  }

  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals);
  if (entries.length === 0 || totalSecToday === 0) {
    legendList.innerHTML = `
      <div class="empty-hub-state">
        <span>No study sessions yet today. Start focusing to see subject breakdown.</span>
      </div>
    `;
    return;
  }

  const radius = 70;
  const circumference = 2 * Math.PI * radius; // ~439.82
  let accumulatedPercent = 0;

  entries.sort((a, b) => b.durationSec - a.durationSec);

  function highlightSubject(item) {
    activeHighlightedSubjectId = item.id;
    const itemPct = Math.round((item.durationSec / totalSecToday) * 100);
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    if (centerSub) centerSub.textContent = item.name;
    if (centerVal) centerVal.textContent = itemTimeStr;
    if (centerPct) {
      centerPct.textContent = `${itemPct}% of study`;
      centerPct.classList.remove('hidden');
    }

    svg.querySelectorAll('.donut-slice').forEach(s => {
      if (s.getAttribute('data-sub-id') === item.id) {
        s.classList.add('active');
        s.style.opacity = '1';
        s.style.strokeWidth = '26';
      } else {
        s.classList.remove('active');
        s.style.opacity = '0.35';
        s.style.strokeWidth = '22';
      }
    });

    legendList.querySelectorAll('.donut-legend-item').forEach(l => {
      if (l.getAttribute('data-sub-id') === item.id) {
        l.classList.add('active');
      } else {
        l.classList.remove('active');
      }
    });
  }

  function resetHighlight() {
    activeHighlightedSubjectId = null;
    if (centerSub) centerSub.textContent = 'Today';
    if (centerVal) centerVal.textContent = formattedTotal;
    if (centerPct) centerPct.classList.add('hidden');

    svg.querySelectorAll('.donut-slice').forEach(s => {
      s.classList.remove('active');
      s.style.opacity = '1';
      s.style.strokeWidth = '22';
    });
    legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
  }

  svgWrap?.addEventListener('mouseleave', () => {
    if (activeHighlightedSubjectId) resetHighlight();
  });

  entries.forEach(item => {
    const itemPct = (item.durationSec / totalSecToday) * 100;
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    const sliceLength = (itemPct / 100) * circumference;
    const offset = -((accumulatedPercent / 100) * circumference);

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', '100');
    circle.setAttribute('cy', '100');
    circle.setAttribute('r', radius.toString());
    circle.setAttribute('class', 'donut-slice');
    circle.setAttribute('data-sub-id', item.id);
    circle.setAttribute('stroke', item.color);
    circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
    circle.setAttribute('stroke-dashoffset', offset.toString());
    circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

    circle.addEventListener('mouseenter', () => highlightSubject(item));
    circle.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightSubject(item);
    });

    svg.appendChild(circle);
    accumulatedPercent += itemPct;

    const legendItem = document.createElement('div');
    legendItem.className = 'donut-legend-item';
    legendItem.setAttribute('data-sub-id', item.id);
    legendItem.innerHTML = `
      <div class="donut-legend-left">
        <span class="donut-legend-dot" style="background-color: ${item.color};"></span>
        <span class="donut-legend-name">${item.name}</span>
      </div>
      <div class="donut-legend-right">
        <span class="donut-legend-pct">${Math.round(itemPct)}%</span>
        <span class="donut-legend-time">${itemTimeStr}</span>
      </div>
    `;

    legendItem.addEventListener('mouseenter', () => highlightSubject(item));
    legendItem.addEventListener('mouseleave', () => resetHighlight());
    legendItem.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightSubject(item);
    });

    legendList.appendChild(legendItem);
  });
}

// 52-Week Activity Heatmap (HeatmapView.kt)
function renderActivityHeatmap() {
  const container = document.getElementById('heatmapGrid');
  const monthsRow = document.getElementById('heatmapMonthsRow');
  const yearLabel = document.getElementById('heatmapYearLabel');
  if (!container) return;

  const dateMap = {};
  let totalActiveDays = 0;
  let totalStudiedSec = 0;

  const allSessions = getAllValidatedSessions();

  allSessions.forEach(sess => {
    if (sess && sess.durationSec > 0) {
      const d = new Date(sess.timestamp || sess.startTime);
      const dateKey = d.toISOString().split('T')[0];
      dateMap[dateKey] = (dateMap[dateKey] || 0) + sess.durationSec;
    }
  });

  Object.values(dateMap).forEach(sec => {
    if (sec > 0) {
      totalActiveDays++;
      totalStudiedSec += sec;
    }
  });

  if (yearLabel) {
    const totalHrs = Math.floor(totalStudiedSec / 3600);
    const totalMins = Math.round((totalStudiedSec % 3600) / 60);
    const formattedTotal = totalHrs > 0 ? `${totalHrs}h ${totalMins}m` : `${totalMins}m`;
    yearLabel.textContent = `${totalActiveDays} Active Day${totalActiveDays === 1 ? '' : 's'}${totalStudiedSec > 0 ? ' • ' + formattedTotal : ''}`;
  }

  const now = new Date();
  const currentDayOfWeek = (now.getDay() + 6) % 7; // Monday = 0, Sunday = 6
  const totalDays = (51 * 7) + (currentDayOfWeek + 1); // 52 weeks total

  container.innerHTML = '';
  if (monthsRow) monthsRow.innerHTML = '';

  const startDate = new Date();
  startDate.setDate(now.getDate() - totalDays + 1);

  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  let lastMonthIndex = -1;
  let currentWeekCol = null;
  let colIndex = 0;

  for (let i = 0; i < totalDays; i++) {
    const curDate = new Date(startDate);
    curDate.setDate(startDate.getDate() + i);

    const dayOfWeek = (curDate.getDay() + 6) % 7; // 0 = Mon, 6 = Sun
    if (dayOfWeek === 0 || !currentWeekCol) {
      currentWeekCol = document.createElement('div');
      currentWeekCol.className = 'heatmap-col-week';
      container.appendChild(currentWeekCol);

      const curMonth = curDate.getMonth();
      if (curMonth !== lastMonthIndex && monthsRow) {
        lastMonthIndex = curMonth;
        const monthLabel = document.createElement('span');
        monthLabel.className = 'heatmap-month-label';
        monthLabel.style.left = `${colIndex * 13}px`;
        monthLabel.textContent = monthNames[curMonth];
        monthsRow.appendChild(monthLabel);
      }

      colIndex++;
    }

    const dateKey = curDate.toISOString().split('T')[0];
    const secStudied = dateMap[dateKey] || 0;
    const minStudied = Math.round(secStudied / 60);

    let level = 0;
    if (minStudied > 0 && minStudied < 30) level = 1;
    else if (minStudied >= 30 && minStudied < 60) level = 2;
    else if (minStudied >= 60 && minStudied < 120) level = 3;
    else if (minStudied >= 120) level = 4;

    const cell = document.createElement('div');
    cell.className = `heatmap-cell lvl-${level}`;
    const formattedDate = curDate.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    const formattedTime = minStudied >= 60 ? `${Math.floor(minStudied / 60)}h ${minStudied % 60}m` : `${minStudied}m`;
    cell.title = `${minStudied > 0 ? formattedTime : 'No study'} on ${formattedDate}`;

    cell.addEventListener('click', () => {
      switchInsightsTab('calendar');
      selectCalendarDate(dateKey);
    });

    currentWeekCol.appendChild(cell);
  }

  const scrollWrapper = document.querySelector('.heatmap-scroll-wrapper');
  if (scrollWrapper) {
    scrollWrapper.scrollLeft = scrollWrapper.scrollWidth;
  }
}

// ----------------------------------------------------------------------------
// TAB 2: CALENDAR & DAY TIMELINE (CalendarTimeline.kt)
// ----------------------------------------------------------------------------

let activeCalendarMonth = new Date().getMonth();
let activeCalendarYear = new Date().getFullYear();
let selectedCalendarDateStr = new Date().toISOString().split('T')[0];

function renderMonthlyCalendar() {
  const monthTitle = document.getElementById('calMonthTitle');
  const daysGrid = document.getElementById('calendarDaysGrid');
  const summaryRow = document.getElementById('calMonthSummaryRow');
  if (!monthTitle || !daysGrid) return;

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  monthTitle.textContent = `${monthNames[activeCalendarMonth]} ${activeCalendarYear}`;
  daysGrid.innerHTML = '';

  const firstDayOfMonth = new Date(activeCalendarYear, activeCalendarMonth, 1);
  const lastDayOfMonth = new Date(activeCalendarYear, activeCalendarMonth + 1, 0);
  const daysInMonth = lastDayOfMonth.getDate();

  const startDayIndex = (firstDayOfMonth.getDay() + 6) % 7;
  const prevMonthLastDay = new Date(activeCalendarYear, activeCalendarMonth, 0).getDate();
  for (let i = startDayIndex - 1; i >= 0; i--) {
    const cell = document.createElement('div');
    cell.className = 'cal-day-cell other-month';
    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <span class="cal-day-num">${prevMonthLastDay - i}</span>
      </div>
    `;
    daysGrid.appendChild(cell);
  }

  const todayStr = new Date().toISOString().split('T')[0];

  const dateSessionsMap = {};
  const dateDurationMap = {};

  const allSessions = getAllValidatedSessions();
  allSessions.forEach(s => {
    if (s && s.durationSec > 0) {
      const d = new Date(s.timestamp || s.startTime);
      const dateKey = d.toISOString().split('T')[0];
      if (!dateSessionsMap[dateKey]) dateSessionsMap[dateKey] = [];
      dateSessionsMap[dateKey].push(s.subject?.color || '#3b82f6');
      dateDurationMap[dateKey] = (dateDurationMap[dateKey] || 0) + s.durationSec;
    }
  });

  const dailyGoalSec = (timerConfig.dailyGoalMinutes || 120) * 60;
  let monthGoalsMet = 0;
  let monthTotalSecs = 0;

  for (let day = 1; day <= daysInMonth; day++) {
    const dateKey = `${activeCalendarYear}-${String(activeCalendarMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    
    const isToday = dateKey === todayStr;
    const isSelected = dateKey === selectedCalendarDateStr;
    const subjectColors = [...new Set(dateSessionsMap[dateKey] || [])];
    const focusSecs = dateDurationMap[dateKey] || 0;

    monthTotalSecs += focusSecs;
    const goalReached = dailyGoalSec > 0 && focusSecs >= dailyGoalSec;
    const goalIncomplete = focusSecs > 0 && !goalReached;
    if (goalReached) monthGoalsMet++;

    let goalStatusClass = '';
    if (goalReached) {
      goalStatusClass = 'goal-met';
    } else if (goalIncomplete) {
      goalStatusClass = 'goal-incomplete';
    }

    const pct = dailyGoalSec > 0 ? Math.min(1, focusSecs / dailyGoalSec) : 0;
    const ringCircumference = 75.4; // 2 * PI * 12
    const ringOffset = ringCircumference * (1 - pct);

    const cell = document.createElement('div');
    cell.className = `cal-day-cell ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''} ${goalStatusClass}`;
    
    const minStudied = Math.round(focusSecs / 60);
    const goalMin = Math.round(dailyGoalSec / 60);
    const leftMin = Math.max(0, goalMin - minStudied);
    const formattedDate = new Date(dateKey + 'T00:00:00').toLocaleDateString([], { month: 'short', day: 'numeric' });
    cell.title = focusSecs > 0 
      ? `${formattedDate}: ${minStudied}m / ${goalMin}m goal (${Math.round(pct * 100)}%${leftMin > 0 ? ' • ' + leftMin + 'm left' : ' • Goal Reached!'})` 
      : `${formattedDate}: No study sessions`;

    let dotsHtml = '';
    if (subjectColors.length > 0) {
      dotsHtml = `<div class="cal-study-dots-wrap">` + 
        subjectColors.slice(0, 3).map(color => `<span class="cal-dot" style="background-color: ${color};"></span>`).join('') +
        `</div>`;
    }

    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <svg class="cal-day-svg-ring" viewBox="0 0 30 30">
          <circle class="cal-ring-bg" cx="15" cy="15" r="12"></circle>
          ${pct > 0 ? `<circle class="cal-ring-fg" cx="15" cy="15" r="12" stroke-dasharray="${ringCircumference}" stroke-dashoffset="${ringOffset}"></circle>` : ''}
        </svg>
        <span class="cal-day-num">${day}</span>
      </div>
      ${dotsHtml}
    `;

    cell.addEventListener('click', () => {
      selectCalendarDate(dateKey);
    });

    daysGrid.appendChild(cell);
  }

  // Next month padding to complete grid
  const totalCellsSoFar = startDayIndex + daysInMonth;
  const remainingCells = (totalCellsSoFar <= 35 ? 35 : 42) - totalCellsSoFar;
  for (let d = 1; d <= remainingCells; d++) {
    const cell = document.createElement('div');
    cell.className = 'cal-day-cell other-month';
    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <span class="cal-day-num">${d}</span>
      </div>
    `;
    daysGrid.appendChild(cell);
  }

  if (summaryRow) {
    const totalHrs = Math.floor(monthTotalSecs / 3600);
    const totalMins = Math.round((monthTotalSecs % 3600) / 60);
    const formattedMonthStudy = totalHrs > 0 ? `${totalHrs}h ${totalMins}m` : `${totalMins}m`;

    summaryRow.innerHTML = `
      <span class="cal-summary-chip goals-met">✓ ${monthGoalsMet} ${monthGoalsMet === 1 ? 'Goal' : 'Goals'} Met</span>
      <span class="cal-summary-chip total-study">⏱️ ${formattedMonthStudy} Total Study</span>
    `;
  }

  renderSelectedDateTimeline(selectedCalendarDateStr);
}

function changeCalendarMonth(delta) {
  activeCalendarMonth += delta;
  if (activeCalendarMonth > 11) {
    activeCalendarMonth = 0;
    activeCalendarYear++;
  } else if (activeCalendarMonth < 0) {
    activeCalendarMonth = 11;
    activeCalendarYear--;
  }
  renderMonthlyCalendar();
}

function selectCalendarDate(dateStr) {
  selectedCalendarDateStr = dateStr;
  renderMonthlyCalendar();
}

// Precise Session Timeline with exact Start & End Time
function renderSelectedDateTimeline(dateStr) {
  const title = document.getElementById('selectedDateTimelineTitle');
  const countBadge = document.getElementById('selectedDateSessionCount');
  const container = document.getElementById('timelineList');
  if (!container) return;

  const todayStr = new Date().toISOString().split('T')[0];
  const isToday = dateStr === todayStr;

  const formattedDate = new Date(dateStr + 'T00:00:00').toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? "Today's Sessions" : `Sessions (${formattedDate})`;
  }

  const allSessions = getAllValidatedSessions();
  const sessions = allSessions.filter(s => {
    if (!s || !s.durationSec) return false;
    const dStr = new Date(s.timestamp || s.startTime).toISOString().split('T')[0];
    return dStr === dateStr;
  });

  if (countBadge) countBadge.textContent = `${sessions.length} session${sessions.length === 1 ? '' : 's'}`;

  if (sessions.length === 0) {
    container.innerHTML = `
      <div class="timeline-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p>No study sessions on ${formattedDate}.</p>
        <span>${isToday ? 'Click Start Focus to log your first session!' : 'Select a date with activity dots to view history.'}</span>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  sessions.forEach(sess => {
    const mins = Math.max(1, Math.round(sess.durationSec / 60));
    const startTimeFormatted = new Date(sess.startTime || sess.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const endTimeFormatted = sess.endTime 
      ? new Date(sess.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : new Date((sess.startTime || sess.timestamp) + (sess.durationSec * 1000)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const item = document.createElement('div');
    item.className = 'timeline-item';
    item.innerHTML = `
      <div class="timeline-item-left">
        <span class="subject-color-dot" style="background-color: ${sess.subject?.color || '#3b82f6'};"></span>
        <div>
          <div class="timeline-subject-name">${sess.subject?.name || 'Focus Study'}</div>
          <span class="timeline-mode-pill">${sess.mode || 'Focus Study'}</span>
        </div>
      </div>
      <div class="timeline-item-right timeline-time-meta">
        <span class="timeline-duration">+${mins} min</span>
        <div class="timeline-time-range">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          <span>${startTimeFormatted} – ${endTimeFormatted}</span>
        </div>
      </div>
    `;
    container.appendChild(item);
  });
}

// ----------------------------------------------------------------------------
// TAB 3: PLANNER & GOALS (PlannerHistoryManager.kt)
// ----------------------------------------------------------------------------

function renderPlannerGoals() {
  const container = document.getElementById('plannerGoalsContainer');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  if (!container) return;

  if (subjectSelect) {
    subjectSelect.innerHTML = appState.subjects.map(s => 
      `<option value="${s.id}">${s.name}</option>`
    ).join('');
  }

  const goals = appState.plannerGoals || [];
  if (goals.length === 0) {
    container.innerHTML = `
      <div class="planner-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <p>No dedicated subject targets set yet.</p>
        <span>Click <strong>+ New Target</strong> above to track daily goals per subject!</span>
      </div>
    `;
    return;
  }

  const studiedSecMap = {};
  (appState.todaySessions || []).forEach(s => {
    if (s && s.subject && typeof s.durationSec === 'number' && s.durationSec > 0) {
      const subId = s.subject.id || s.subject.name;
      studiedSecMap[subId] = (studiedSecMap[subId] || 0) + s.durationSec;
    }
  });

  container.innerHTML = '';
  goals.forEach(goal => {
    const subject = appState.subjects.find(s => s.id === goal.subjectId) || {
      name: goal.subjectId,
      color: '#3b82f6'
    };

    const studiedSec = studiedSecMap[goal.subjectId] || 0;
    const studiedMin = Math.round(studiedSec / 60);
    const targetMin = Math.max(1, goal.dailyMinutes || 60);
    const targetSec = targetMin * 60;
    const percent = Math.min(100, Math.round((studiedSec / targetSec) * 100));
    const isCompleted = studiedSec >= targetSec;
    const leftSec = Math.max(0, targetSec - studiedSec);
    const leftMin = Math.ceil(leftSec / 60);

    let studiedFormatted = '0m';
    if (studiedSec > 0) {
      if (studiedMin >= 60) {
        studiedFormatted = `${Math.floor(studiedMin / 60)}h ${studiedMin % 60 > 0 ? (studiedMin % 60) + 'm' : ''}`;
      } else if (studiedMin === 0) {
        studiedFormatted = `${studiedSec}s`;
      } else {
        studiedFormatted = `${studiedMin}m`;
      }
    }

    const targetFormatted = targetMin >= 60 
      ? `${Math.floor(targetMin / 60)}h ${targetMin % 60 > 0 ? (targetMin % 60) + 'm' : ''}` 
      : `${targetMin}m`;

    const remainingFormatted = isCompleted
      ? '✓ Target reached'
      : (leftMin >= 60 ? `${Math.floor(leftMin / 60)}h ${leftMin % 60 > 0 ? (leftMin % 60) + 'm' : ''} left` : `${leftMin}m left`);

    const card = document.createElement('div');
    card.className = 'planner-goal-card';
    card.innerHTML = `
      <div class="goal-card-header">
        <div class="goal-subject-meta">
          <span class="goal-dot" style="background-color: ${subject.color};"></span>
          <span class="goal-title">${subject.name}</span>
        </div>
        <div class="goal-actions">
          <span class="goal-pct-badge ${isCompleted ? 'completed' : ''}">${isCompleted ? '✓ Completed' : percent + '%'}</span>
          <button class="goal-btn-delete" data-goal-id="${goal.id}" title="Remove Target">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>
      </div>
      <div class="goal-stat-row">
        <div class="goal-numbers-wrap">
          <span class="goal-current-val">${studiedFormatted}</span>
          <span class="goal-target-val">/ ${targetFormatted}</span>
        </div>
        <span class="goal-remaining-val ${isCompleted ? 'completed' : ''}">${remainingFormatted}</span>
      </div>
      <div class="goal-progress-track">
        <div class="goal-progress-bar" style="width: ${percent}%; background: linear-gradient(90deg, ${subject.color}, #10b981);"></div>
      </div>
    `;

    card.querySelector('.goal-btn-delete')?.addEventListener('click', (e) => {
      e.stopPropagation();
      openDeleteGoalModal(goal.id);
    });

    container.appendChild(card);
  });
}

function openDeleteGoalModal(goalId) {
  pendingGoalIdToDelete = goalId;
  const goal = appState.plannerGoals?.find(g => g.id === goalId);
  const subject = appState.subjects.find(s => s.id === goal?.subjectId);
  const subtitle = document.getElementById('deleteGoalModalSubtitle');
  if (subtitle) {
    subtitle.textContent = `Are you sure you want to remove the daily target for "${subject ? subject.name : 'this subject'}"?`;
  }
  document.getElementById('deleteGoalModalOverlay')?.classList.remove('hidden');
}

function closeDeleteGoalModal() {
  pendingGoalIdToDelete = null;
  document.getElementById('deleteGoalModalOverlay')?.classList.add('hidden');
}

function confirmDeletePlannerGoal() {
  if (!pendingGoalIdToDelete || !appState.plannerGoals) return;
  appState.plannerGoals = appState.plannerGoals.filter(g => g.id !== pendingGoalIdToDelete);
  closeDeleteGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast('Subject target goal removed.', 'info');
}

function openAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  if (subjectSelect) {
    subjectSelect.innerHTML = appState.subjects.map(s => 
      `<option value="${s.id}">${s.name}</option>`
    ).join('');
  }
  if (modal) modal.classList.remove('hidden');
}

function closeAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  if (modal) modal.classList.add('hidden');
}

function handleAddGoal(e) {
  e.preventDefault();
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const minutesInput = document.getElementById('goalMinutesInput');

  const subjectId = subjectSelect?.value;
  const dailyMinutes = parseInt(minutesInput?.value, 10) || 60;

  if (!subjectId) return;

  if (!appState.plannerGoals) appState.plannerGoals = [];

  const existing = appState.plannerGoals.find(g => g.subjectId === subjectId);
  if (existing) {
    existing.dailyMinutes = dailyMinutes;
  } else {
    appState.plannerGoals.push({
      id: 'goal_' + Date.now(),
      subjectId,
      dailyMinutes
    });
  }

  closeAddGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast('🎯 Subject target goal set!', 'success');
}

// ----------------------------------------------------------------------------
// TAB 4: LIVE LEADERBOARD & PROFILE CUSTOMIZATION
// ----------------------------------------------------------------------------

let presenceHeartbeatInterval = null;
let leaderboardCache = { data: null, timestamp: 0 };
const LEADERBOARD_CACHE_TTL_MS = 30000; // 30-second client cache
let resetCountdownInterval = null;
let selectedAvatarPreset = '🐱';

function formatLeaderboardTime(totalSec) {
  if (!totalSec || totalSec <= 0) return '0m';
  const totalMin = Math.round(totalSec / 60);
  if (totalMin >= 60) {
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    return `${h}h ${m > 0 ? m + 'm' : ''}`;
  } else if (totalMin === 0 && totalSec > 0) {
    return `${totalSec}s`;
  }
  return `${totalMin}m`;
}

function getAvatarElementHtml(avatarVal, userName, className = 'row-avatar-img') {
  if (!avatarVal || avatarVal.trim() === '') {
    avatarVal = '🐱';
  }
  const isUrl = /^(http|https|data:|assets\/|\/)/i.test(avatarVal.trim());
  if (isUrl) {
    return `<img src="${avatarVal}" alt="${userName || 'Student'}" class="${className}" onerror="this.outerHTML='<span class=\\'avatar-sticker ${className}\\'>🐱</span>'">`;
  } else {
    return `<span class="avatar-sticker ${className}">${avatarVal}</span>`;
  }
}

// ----------------------------------------------------------------------------
// PROFILE CUSTOMIZATION SYSTEM
// ----------------------------------------------------------------------------

function openProfileModal() {
  const modal = document.getElementById('profileModalOverlay');
  if (!modal) return;

  const profile = appState.userProfile || {
    displayName: 'Student',
    avatarPreset: '🐱',
    motto: '🎯 Deep focus & daily consistency',
    primarySubjectId: 'math',
    isPublicLeaderboard: true
  };

  selectedAvatarPreset = profile.avatarPreset || '🐱';

  const nameInput = document.getElementById('inputProfileDisplayName');
  const mottoInput = document.getElementById('inputProfileMotto');
  const subjectSelect = document.getElementById('selectProfilePrimarySubject');
  const publicToggle = document.getElementById('checkLeaderboardPublic');

  if (nameInput) {
    nameInput.value = profile.displayName || (appState.currentUser?.user_metadata?.full_name || 'Student');
  }
  if (mottoInput) {
    mottoInput.value = profile.motto || '';
  }
  if (subjectSelect) {
    subjectSelect.innerHTML = appState.subjects.map(s => 
      `<option value="${s.id}" ${s.id === profile.primarySubjectId ? 'selected' : ''}>${s.name}</option>`
    ).join('');
  }
  if (publicToggle) {
    publicToggle.checked = profile.isPublicLeaderboard !== false;
  }

  // Highlight active preset button
  document.querySelectorAll('.avatar-preset-btn').forEach(btn => {
    if (btn.dataset.avatar === selectedAvatarPreset) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  updateProfileLivePreview();
  modal.classList.remove('hidden');
}

function closeProfileModal() {
  document.getElementById('profileModalOverlay')?.classList.add('hidden');
}

function updateProfileLivePreview() {
  const nameInput = document.getElementById('inputProfileDisplayName');
  const mottoInput = document.getElementById('inputProfileMotto');
  const previewAvatarIcon = document.getElementById('previewAvatarIcon');
  const previewDisplayName = document.getElementById('previewDisplayName');
  const previewMottoText = document.getElementById('previewMottoText');

  const nameVal = (nameInput?.value.trim() || 'Student').slice(0, 24);
  const mottoVal = (mottoInput?.value.trim() || '🎯 Deep focus & daily consistency').slice(0, 60);

  if (previewAvatarIcon) previewAvatarIcon.textContent = selectedAvatarPreset;
  if (previewDisplayName) previewDisplayName.textContent = nameVal;
  if (previewMottoText) previewMottoText.textContent = mottoVal;
}

function handleSaveProfile(e) {
  e.preventDefault();
  const nameInput = document.getElementById('inputProfileDisplayName');
  const mottoInput = document.getElementById('inputProfileMotto');
  const subjectSelect = document.getElementById('selectProfilePrimarySubject');
  const publicToggle = document.getElementById('checkLeaderboardPublic');

  const displayName = (nameInput?.value.trim() || 'Student').slice(0, 24);
  const motto = (mottoInput?.value.trim() || '').slice(0, 60);
  const primarySubjectId = subjectSelect?.value || (appState.subjects[0]?.id || 'math');
  const isPublicLeaderboard = publicToggle ? publicToggle.checked : true;

  appState.userProfile = {
    displayName,
    avatarPreset: selectedAvatarPreset,
    motto,
    primarySubjectId,
    isPublicLeaderboard
  };

  saveLocalState();
  renderUserProfileUI();
  pushDataToCloud();
  closeProfileModal();

  if (timerStatus === 'RUNNING' && currentMode !== 'break') {
    updateStudyPresence(true);
  }

  leaderboardCache.timestamp = 0;
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }

  showToast('Profile customizations saved! ✨', 'success');
}

function renderUserProfileUI() {
  const profile = appState.userProfile || {
    displayName: 'Student',
    avatarPreset: '🐱',
    motto: '🎯 Deep focus & daily consistency',
    primarySubjectId: 'math',
    isPublicLeaderboard: true
  };

  const name = profile.displayName || (appState.currentUser?.user_metadata?.full_name || 'Student');
  const avatar = profile.avatarPreset || '🐱';

  const userDisplayName = document.getElementById('userDisplayName');
  const dropdownUserName = document.getElementById('dropdownUserName');
  const navUserAvatarWrap = document.getElementById('navUserAvatarWrap');

  if (userDisplayName) userDisplayName.textContent = name;
  if (dropdownUserName) dropdownUserName.textContent = name;
  
  if (navUserAvatarWrap) {
    navUserAvatarWrap.innerHTML = getAvatarElementHtml(avatar, name, 'user-avatar');
  }
}

// ----------------------------------------------------------------------------
// REAL-TIME STUDY PRESENCE & LEADERBOARD DATA SYNC
// ----------------------------------------------------------------------------

async function updateStudyPresence(isStudying) {
  if (!supabaseClient || !appState.currentUser) return;

  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) {
    // If user opts out of public leaderboard, don't broadcast live presence
    return;
  }

  const currentSub = appState.selectedSubject || { name: 'Focus Study', color: '#3b82f6' };
  const userName = profile.displayName || 
                   appState.currentUser.user_metadata?.full_name || 
                   appState.currentUser.user_metadata?.name || 
                   appState.currentUser.email?.split('@')[0] || 
                   'Student';
  const avatarUrl = profile.avatarPreset || 
                    appState.currentUser.user_metadata?.avatar_url || 
                    appState.currentUser.user_metadata?.picture || 
                    '🐱';

  const shouldBeStudying = isStudying && currentMode !== 'break' && timerStatus === 'RUNNING';

  try {
    await supabaseClient.rpc('update_study_presence', {
      p_user_id: appState.currentUser.id,
      p_user_name: userName,
      p_avatar_url: avatarUrl,
      p_is_studying: shouldBeStudying,
      p_current_subject: shouldBeStudying ? (currentSub.name || 'Focus Study') : '',
      p_subject_color: shouldBeStudying ? (currentSub.color || '#3b82f6') : '#3b82f6'
    });
  } catch (err) {
    console.warn('Presence update error:', err);
  }
}

function startPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
  }
  updateStudyPresence(true);
  // Send active heartbeat every 60 seconds while timer is actively running
  presenceHeartbeatInterval = setInterval(() => {
    if (timerStatus === 'RUNNING' && currentMode !== 'break') {
      updateStudyPresence(true);
    }
  }, 60000);
}

function stopPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
    presenceHeartbeatInterval = null;
  }
  updateStudyPresence(false);
}

async function logSessionToLeaderboard(durationSec, subject) {
  if (!supabaseClient || !appState.currentUser || durationSec < 10) return;

  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) {
    return;
  }

  const userName = profile.displayName || 
                   appState.currentUser.user_metadata?.full_name || 
                   appState.currentUser.user_metadata?.name || 
                   appState.currentUser.email?.split('@')[0] || 
                   'Student';
  const avatarUrl = profile.avatarPreset || 
                    appState.currentUser.user_metadata?.avatar_url || 
                    appState.currentUser.user_metadata?.picture || 
                    '🐱';

  try {
    await supabaseClient.rpc('record_study_session_leaderboard', {
      p_user_id: appState.currentUser.id,
      p_user_name: userName,
      p_avatar_url: avatarUrl,
      p_duration_seconds: durationSec,
      p_subject: subject?.name || 'Focus Study',
      p_subject_color: subject?.color || '#3b82f6'
    });
    // Invalidate local leaderboard cache so fresh scores render immediately
    leaderboardCache.timestamp = 0;
    const modal = document.getElementById('leaderboardModalOverlay');
    if (modal && !modal.classList.contains('hidden')) {
      fetchLeaderboard(true);
    }
  } catch (err) {
    console.warn('Leaderboard session log error:', err);
  }
}

function openLeaderboardModal() {
  const modal = document.getElementById('leaderboardModalOverlay');
  if (modal) {
    modal.classList.remove('hidden');
    fetchLeaderboard(true);
  }
}

function closeLeaderboardModal() {
  document.getElementById('leaderboardModalOverlay')?.classList.add('hidden');
}

async function fetchLeaderboard(forceRefresh = false) {
  const now = Date.now();
  startResetCountdownTimer();

  if (!forceRefresh && leaderboardCache.data && (now - leaderboardCache.timestamp < LEADERBOARD_CACHE_TTL_MS)) {
    renderLeaderboard(leaderboardCache.data);
    return;
  }

  const refreshBtn = document.getElementById('btnRefreshLeaderboard');
  refreshBtn?.classList.add('spinning');

  try {
    if (supabaseClient) {
      const todayStr = new Date().toISOString().split('T')[0];
      const { data, error } = await supabaseClient.rpc('get_daily_leaderboard', {
        p_date: todayStr,
        p_limit: 25
      });

      if (error) {
        console.warn('Supabase get_daily_leaderboard error:', error);
        fallbackLocalLeaderboard();
      } else if (data) {
        leaderboardCache = { data, timestamp: now };
        renderLeaderboard(data);
      }
    } else {
      fallbackLocalLeaderboard();
    }
  } catch (err) {
    console.warn('Leaderboard fetch exception:', err);
    fallbackLocalLeaderboard();
  } finally {
    setTimeout(() => refreshBtn?.classList.remove('spinning'), 500);
  }
}

function fallbackLocalLeaderboard() {
  let totalSecToday = 0;
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number') totalSecToday += s.durationSec;
  });

  const isUserStudying = timerStatus === 'RUNNING' && currentMode !== 'break';
  const dummyRanks = [];

  const profile = appState.userProfile || {};
  const userName = profile.displayName || appState.currentUser?.user_metadata?.full_name || 'You';
  const avatar = profile.avatarPreset || appState.currentUser?.user_metadata?.avatar_url || '🐱';

  if (totalSecToday > 0 || isUserStudying) {
    dummyRanks.push({
      rank: 1,
      user_id: appState.currentUser ? appState.currentUser.id : 'guest',
      user_name: userName,
      avatar_url: avatar,
      total_seconds: totalSecToday,
      is_studying: isUserStudying,
      current_subject: isUserStudying ? (appState.selectedSubject?.name || 'Mathematics') : '',
      subject_color: appState.selectedSubject?.color || '#3b82f6'
    });
  }

  renderLeaderboard(dummyRanks);
}

function renderLeaderboard(rankings) {
  const podiumContainer = document.getElementById('leaderboardPodium');
  const listContainer = document.getElementById('leaderboardListItems');
  const emptyState = document.getElementById('leaderboardEmptyState');
  const activeStudyingText = document.getElementById('leaderboardActiveStudyingText');

  if (!podiumContainer || !listContainer) return;

  const currentUserId = appState.currentUser?.id;

  // 1. Calculate Active Studiers Count
  const activeStudyingCount = rankings.filter(r => r.is_studying).length;
  if (activeStudyingText) {
    activeStudyingText.textContent = activeStudyingCount > 0 
      ? `${activeStudyingCount} Studying Now` 
      : 'Live Focus Hub';
  }

  const headerLiveDot = document.getElementById('headerLiveDot');
  const mobileLiveDot = document.getElementById('mobileLiveDot');
  const isAnyActive = activeStudyingCount > 0 || (timerStatus === 'RUNNING' && currentMode !== 'break');
  if (headerLiveDot) headerLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';
  if (mobileLiveDot) mobileLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';

  const top1 = rankings.find(r => Number(r.rank) === 1);
  const top2 = rankings.find(r => Number(r.rank) === 2);
  const top3 = rankings.find(r => Number(r.rank) === 3);

  const renderPodiumCard = (entry, rankNum) => {
    if (!entry) {
      return `
        <div class="podium-card rank-${rankNum} empty-podium">
          <div class="podium-avatar-wrap">
            <div class="podium-avatar-img empty-avatar"></div>
            <span class="podium-rank-pill">${rankNum}</span>
          </div>
          <span class="podium-name text-muted">Open Spot</span>
          <span class="podium-time text-muted">--</span>
        </div>
      `;
    }

    const isCurrent = currentUserId && entry.user_id === currentUserId;
    const crown = rankNum === 1 ? '<span class="podium-crown-badge">👑</span>' : '';
    const timeFormatted = formatLeaderboardTime(entry.total_seconds);
    const avatarHtml = getAvatarElementHtml(entry.avatar_url, entry.user_name, 'podium-avatar-img');

    let statusChip = '';
    if (entry.is_studying) {
      statusChip = `
        <span class="live-status-chip studying" title="Actively studying now">
          <span class="status-dot"></span>
          <span>${entry.current_subject ? entry.current_subject : 'Studying'}</span>
        </span>
      `;
    } else {
      statusChip = `
        <span class="live-status-chip resting" title="Resting">
          <span class="status-dot"></span>
          <span>Resting</span>
        </span>
      `;
    }

    return `
      <div class="podium-card rank-${rankNum} ${isCurrent ? 'is-current-user' : ''}">
        ${crown}
        <div class="podium-avatar-wrap">
          ${avatarHtml}
          <span class="podium-rank-pill">${rankNum}</span>
        </div>
        <span class="podium-name" title="${entry.user_name}">${isCurrent ? 'You' : entry.user_name}</span>
        <span class="podium-time">${timeFormatted}</span>
        ${statusChip}
      </div>
    `;
  };

  // Always show podium top 3 showcase
  podiumContainer.innerHTML = `
    ${renderPodiumCard(top2, 2)}
    ${renderPodiumCard(top1, 1)}
    ${renderPodiumCard(top3, 3)}
  `;

  if (!rankings || rankings.length === 0) {
    listContainer.innerHTML = `
      <div class="leaderboard-empty-state" style="padding: 24px 16px;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="8" r="6"></circle>
          <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"></path>
        </svg>
        <p>No study sessions recorded today yet.</p>
        <span>Start a timer session to claim the #1 spot on the leaderboard!</span>
      </div>
    `;
    updatePersonalUserBar(null, 0);
    return;
  }

  // 3. Render Ranks 4 to 25 List
  const remainingRanks = rankings.filter(r => Number(r.rank) > 3);
  if (remainingRanks.length === 0) {
    listContainer.innerHTML = `
      <div class="leaderboard-empty-state" style="padding: 24px 16px;">
        <p style="font-size: 0.85rem;">Only ${rankings.length} on the board today!</p>
        <span>Complete a session to join the top rankings.</span>
      </div>
    `;
  } else {
    listContainer.innerHTML = remainingRanks.map(r => {
      const isCurrent = currentUserId && r.user_id === currentUserId;
      const timeFormatted = formatLeaderboardTime(r.total_seconds);
      const avatarHtml = getAvatarElementHtml(r.avatar_url, r.user_name, 'row-avatar-img');

      const statusHtml = r.is_studying
        ? `<span class="live-status-chip studying"><span class="status-dot"></span><span>${r.current_subject || 'Studying'}</span></span>`
        : `<span class="live-status-chip resting"><span class="status-dot"></span><span>Resting</span></span>`;

      return `
        <div class="leaderboard-row ${isCurrent ? 'is-current-user' : ''}">
          <span class="row-rank-num">#${r.rank}</span>
          <div class="row-user-col">
            ${avatarHtml}
            <span class="row-user-name" title="${r.user_name}">${isCurrent ? 'You' : r.user_name}</span>
          </div>
          <div>${statusHtml}</div>
          <span class="row-time">${timeFormatted}</span>
        </div>
      `;
    }).join('');
  }

  // 4. Update Personal User Bar
  const myEntry = rankings.find(r => currentUserId && r.user_id === currentUserId);
  let localTotalSec = 0;
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number') localTotalSec += s.durationSec;
  });
  updatePersonalUserBar(myEntry, localTotalSec);
}

function updatePersonalUserBar(myEntry, localTotalSec) {
  const userBarRank = document.getElementById('userBarRank');
  const userBarAvatarWrap = document.getElementById('userBarAvatarWrap');
  const userBarName = document.getElementById('userBarName');
  const userBarStatus = document.getElementById('userBarStatus');
  const userBarTime = document.getElementById('userBarTime');

  const isStudyingNow = timerStatus === 'RUNNING' && currentMode !== 'break';
  const subName = appState.selectedSubject?.name || 'Focus';
  const profile = appState.userProfile || {};
  const currentName = profile.displayName || (appState.currentUser ? (appState.currentUser.user_metadata?.full_name || 'You') : 'You (Guest)');
  const avatar = profile.avatarPreset || appState.currentUser?.user_metadata?.avatar_url || '🐱';

  if (userBarName) {
    userBarName.textContent = currentName;
  }

  if (userBarAvatarWrap) {
    userBarAvatarWrap.innerHTML = getAvatarElementHtml(avatar, currentName, 'user-bar-avatar');
  }

  if (userBarStatus) {
    userBarStatus.innerHTML = isStudyingNow 
      ? `<span style="color: #10b981; font-weight: 600;">🟢 Studying ${subName}</span>` 
      : `<span>⚪ Resting</span>`;
  }

  if (myEntry) {
    if (userBarRank) userBarRank.textContent = `#${myEntry.rank}`;
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(myEntry.total_seconds);
  } else {
    if (userBarRank) userBarRank.textContent = '#--';
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(localTotalSec);
  }
}

function startResetCountdownTimer() {
  if (resetCountdownInterval) return;

  const updateCountdown = () => {
    const countdownEl = document.getElementById('leaderboardResetCountdown');
    if (!countdownEl) return;

    const now = new Date();
    const midnight = new Date(now);
    midnight.setHours(24, 0, 0, 0);

    const diffMs = midnight - now;
    if (diffMs <= 0) {
      countdownEl.textContent = 'Resets at 00:00';
      return;
    }

    const hours = Math.floor(diffMs / 3600000);
    const mins = Math.floor((diffMs % 3600000) / 60000);
    const secs = Math.floor((diffMs % 60000) / 1000);

    countdownEl.textContent = `Resets in ${hours}h ${mins}m`;
  };

  updateCountdown();
  resetCountdownInterval = setInterval(updateCountdown, 60000);
}

// Window Unload Presence Cleanup
window.addEventListener('beforeunload', () => {
  if (timerStatus === 'RUNNING') {
    stopPresenceHeartbeat();
  }
});

window.addEventListener('pagehide', () => {
  if (timerStatus === 'RUNNING') {
    stopPresenceHeartbeat();
  }
});

// ============================================================================
// 8. TIMER SETTINGS MODAL
// ============================================================================
function openTimerSettingsModal() {
  const modal = document.getElementById('timerSettingsModalOverlay');
  if (!modal) return;

  const customMinInput = document.getElementById('customMinutesInput');
  const pomoFocusInput = document.getElementById('pomoFocusInput');
  const pomoBreakInput = document.getElementById('pomoBreakInput');
  const pomoLongBreakInput = document.getElementById('pomoLongBreakInput');
  const pomoTotalCyclesInput = document.getElementById('pomoTotalCyclesInput');
  const pomoAutoSwitchBreak = document.getElementById('pomoAutoSwitchBreak');
  const pomoAutoSwitchFocus = document.getElementById('pomoAutoSwitchFocus');
  const dailyGoalInput = document.getElementById('dailyTargetGoalInput');

  if (customMinInput) customMinInput.value = timerConfig.customTimerMinutes || 25;
  if (pomoFocusInput) pomoFocusInput.value = timerConfig.pomoFocusMinutes || 25;
  if (pomoBreakInput) pomoBreakInput.value = timerConfig.pomoBreakMinutes || 5;
  if (pomoLongBreakInput) pomoLongBreakInput.value = timerConfig.pomoLongBreakMinutes || 15;
  if (pomoTotalCyclesInput) pomoTotalCyclesInput.value = timerConfig.pomoTotalCycles || 4;
  if (pomoAutoSwitchBreak) pomoAutoSwitchBreak.checked = timerConfig.pomoAutoSwitchBreak !== false;
  if (pomoAutoSwitchFocus) pomoAutoSwitchFocus.checked = timerConfig.pomoAutoSwitchFocus !== false;
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

async function handleSaveTimerSettings(e) {
  e.preventDefault();
  if (timerStatus === 'RUNNING' || (timerStatus === 'PAUSED' && accumulatedElapsedSec > 0)) {
    const confirmed = await showCustomConfirmDialog({
      title: 'Apply Timer Settings?',
      subtitle: 'Active session warning',
      message: 'Applying new settings will reset your active timer and unsaved progress. Continue?',
      confirmText: 'Apply & Reset',
      cancelText: 'Cancel',
      isDanger: true
    });
    if (!confirmed) return;
  }

  const customMin = parseInt(document.getElementById('customMinutesInput')?.value, 10) || 25;
  const pomoFocus = parseInt(document.getElementById('pomoFocusInput')?.value, 10) || 25;
  const pomoBreak = parseInt(document.getElementById('pomoBreakInput')?.value, 10) || 5;
  const pomoLongBreak = parseInt(document.getElementById('pomoLongBreakInput')?.value, 10) || 15;
  const pomoTotalCycles = parseInt(document.getElementById('pomoTotalCyclesInput')?.value, 10) || 4;
  const pomoAutoSwitchBreak = document.getElementById('pomoAutoSwitchBreak')?.checked !== false;
  const pomoAutoSwitchFocus = document.getElementById('pomoAutoSwitchFocus')?.checked !== false;
  const dailyGoal = parseInt(document.getElementById('dailyTargetGoalInput')?.value, 10) || 120;

  timerConfig.customTimerMinutes = Math.max(1, Math.min(720, customMin));
  timerConfig.pomoFocusMinutes = Math.max(1, Math.min(180, pomoFocus));
  timerConfig.pomoBreakMinutes = Math.max(1, Math.min(60, pomoBreak));
  timerConfig.pomoLongBreakMinutes = Math.max(1, Math.min(120, pomoLongBreak));
  timerConfig.pomoTotalCycles = Math.max(1, Math.min(12, pomoTotalCycles));
  timerConfig.pomoAutoSwitchBreak = pomoAutoSwitchBreak;
  timerConfig.pomoAutoSwitchFocus = pomoAutoSwitchFocus;
  timerConfig.dailyGoalMinutes = Math.max(15, Math.min(1440, dailyGoal));

  closeTimerSettingsModal();
  resetTimer();
  updateProgressAndStreak();
  pushDataToCloud();
  showToast('Timer preferences applied! ⚙️', 'success');
}

// Post-Session Subject Switch Modal
function openSessionCompleteModal(subject, mins) {
  const modal = document.getElementById('sessionCompleteModalOverlay');
  const title = document.getElementById('sessionCompleteTitle');
  const subtitle = document.getElementById('sessionCompleteSubtitle');
  const select = document.getElementById('nextSessionSubjectSelect');
  if (!modal || !select) return;

  if (title) title.textContent = 'Session Completed! 🎉';
  if (subtitle) subtitle.textContent = `+${mins || 1}m added to ${subject.name}`;

  select.innerHTML = appState.subjects.map(s => 
    `<option value="${s.id}" ${s.id === subject.id ? 'selected' : ''}>${s.name}</option>`
  ).join('');

  modal.classList.remove('hidden');
}

function closeSessionCompleteModal() {
  document.getElementById('sessionCompleteModalOverlay')?.classList.add('hidden');
}

function applyNextSessionSubject() {
  const select = document.getElementById('nextSessionSubjectSelect');
  if (select && select.value) {
    const chosen = appState.subjects.find(s => s.id === select.value);
    if (chosen) {
      appState.selectedSubject = chosen;
      saveLocalState();
      renderSubjects();
      updateSelectedSubjectUI();
      showToast(`Next session subject set to: ${chosen.name}`, 'info');
    }
  }
  closeSessionCompleteModal();
}

// Delete All Local Data Modal Handlers
function openDeleteAllDataModal() {
  document.getElementById('deleteAllDataModalOverlay')?.classList.remove('hidden');
}

function closeDeleteAllDataModal() {
  document.getElementById('deleteAllDataModalOverlay')?.classList.add('hidden');
}

function confirmDeleteAllData() {
  try {
    localStorage.removeItem('studytimer_demo_state');
  } catch (e) {}

  appState.streakCount = 1;
  appState.lastStudyDate = '';
  appState.subjects = [...DEFAULT_SUBJECTS];
  appState.selectedSubject = DEFAULT_SUBJECTS[0];
  appState.plannerGoals = [
    { id: 'goal_1', subjectId: 'math', dailyMinutes: 60 },
    { id: 'goal_2', subjectId: 'coding', dailyMinutes: 90 }
  ];
  appState.timelineEntries = [];
  appState.todaySessions = [];

  timerConfig = {
    customTimerMinutes: 25,
    pomoFocusMinutes: 25,
    pomoBreakMinutes: 5,
    pomoLongBreakMinutes: 15,
    pomoTotalCycles: 4,
    pomoAutoSwitchBreak: true,
    pomoAutoSwitchFocus: true,
    dailyGoalMinutes: 120
  };

  pomoCurrentCycle = 1;
  isLongBreakActive = false;

  closeDeleteAllDataModal();
  renderSubjects();
  resetTimer();
  updateProgressAndStreak();
  renderSubjectDonutChart();
  renderActivityHeatmap();
  renderMonthlyCalendar();
  renderPlannerGoals();

  showToast('✨ All local study records and custom data erased.', 'info');
}

// Custom Reusable Confirmation Dialog System
let customConfirmResolve = null;

function showCustomConfirmDialog(options = {}) {
  const {
    title = 'Confirm Action',
    subtitle = 'Confirmation required',
    message = 'Are you sure you want to proceed?',
    confirmText = 'Confirm',
    cancelText = 'Cancel',
    isDanger = true
  } = options;

  const modal = document.getElementById('customConfirmModalOverlay');
  const titleEl = document.getElementById('customConfirmTitle');
  const subtitleEl = document.getElementById('customConfirmSubtitle');
  const msgEl = document.getElementById('customConfirmMessage');
  const btnOk = document.getElementById('btnOkCustomConfirm');
  const btnCancel = document.getElementById('btnCancelCustomConfirm');
  const iconWrap = document.getElementById('customConfirmIconWrap');

  if (!modal) {
    return Promise.resolve(window.confirm(message));
  }

  if (titleEl) titleEl.textContent = title;
  if (subtitleEl) subtitleEl.textContent = subtitle;
  if (msgEl) msgEl.textContent = message;
  if (btnOk) {
    btnOk.textContent = confirmText;
    btnOk.className = isDanger ? 'btn btn-danger flex-1' : 'btn btn-primary flex-1';
  }
  if (btnCancel) btnCancel.textContent = cancelText;

  if (iconWrap) {
    if (isDanger) {
      iconWrap.style.background = 'rgba(239, 68, 68, 0.15)';
      iconWrap.style.color = 'var(--accent-red)';
    } else {
      iconWrap.style.background = 'rgba(245, 158, 11, 0.15)';
      iconWrap.style.color = '#f59e0b';
    }
  }

  modal.classList.remove('hidden');

  return new Promise((resolve) => {
    customConfirmResolve = resolve;
  });
}

function closeCustomConfirmDialog(result = false) {
  const modal = document.getElementById('customConfirmModalOverlay');
  if (modal) modal.classList.add('hidden');
  if (customConfirmResolve) {
    const resolve = customConfirmResolve;
    customConfirmResolve = null;
    resolve(result);
  }
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
    const redirectUrl = window.location.origin + window.location.pathname;
    const { error } = await supabaseClient.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: redirectUrl
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

    const redirectUrl = window.location.origin + window.location.pathname;
    const { error } = await supabaseClient.auth.signInWithOtp({
      email,
      options: {
        emailRedirectTo: redirectUrl
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
  renderPlannerGoals();
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
