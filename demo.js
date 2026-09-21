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
// 1. SUPABASE CLIENT CONFIGURATION & DATE HELPERS
// ============================================================================
const SUPABASE_URL = 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';

/**
 * Standard Local Calendar Date Formatter (yyyy-MM-dd)
 * Matches Android's SimpleDateFormat("yyyy-MM-dd", Locale.US) exactly.
 */
function getLocalDateStr(d = new Date()) {
  if (!d) d = new Date();
  const dateObj = (d instanceof Date && !isNaN(d)) ? d : new Date(d);
  if (isNaN(dateObj.getTime())) {
    const fallback = new Date();
    const year = fallback.getFullYear();
    const month = String(fallback.getMonth() + 1).padStart(2, '0');
    const day = String(fallback.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  const year = dateObj.getFullYear();
  const month = String(dateObj.getMonth() + 1).padStart(2, '0');
  const day = String(dateObj.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getIsoDateStr(d = new Date()) {
  return getLocalDateStr(d);
}

let supabaseClient = null;
function getSupabase() {
  if (!supabaseClient && typeof window !== 'undefined') {
    const sb = window.supabase || (typeof supabase !== 'undefined' ? supabase : null);
    if (sb && sb.createClient) {
      supabaseClient = sb.createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: true
        }
      });
    }
  }
  return supabaseClient;
}
getSupabase();

// ============================================================================
// 2. STATE MANAGEMENT & PREFERENCES
// ============================================================================
const DEFAULT_SUBJECTS = [
  { id: 'general', name: 'General', iconEmoji: '📖', color: '#6366f1' },
  { id: 'math', name: 'Math', iconEmoji: '📐', color: '#10b981' },
  { id: 'coding', name: 'Coding', iconEmoji: '💻', color: '#3b82f6' },
  { id: 'physics', name: 'Physics', iconEmoji: '⚛️', color: '#8b5cf6' },
  { id: 'history', name: 'History', iconEmoji: '📜', color: '#f59e0b' },
  { id: 'science', name: 'Science', iconEmoji: '🧪', color: '#ec4899' }
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

// User Data & History Helpers
function getCleanInitialState(user = null) {
  const name = user?.user_metadata?.full_name || 
               user?.user_metadata?.name || 
               user?.email?.split('@')[0] || 
               'Student';
  const avatar = user?.user_metadata?.avatar_url || 
                 user?.user_metadata?.picture || 
                 'assets/logo.png';

  return {
    currentUser: user || null,
    streakCount: 0,
    lastStudyDate: '',
    userProfile: {
      displayName: name,
      avatarPreset: avatar,
      motto: '🎯 Deep focus & daily consistency',
      primarySubjectId: 'general',
      isPublicLeaderboard: true
    },
    subjects: JSON.parse(JSON.stringify(DEFAULT_SUBJECTS)),
    selectedSubject: DEFAULT_SUBJECTS[0],
    plannerGoals: [],
    timelineEntries: [],
    todaySessions: [],
    dailyFocusTotals: {},
    subjectDurations: {},
    dailySubjectDurations: {}
  };
}

let appState = getCleanInitialState(null);

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
  if (!restoreActiveSessionIfAny()) {
    resetTimer();
  }
  updateProgressAndStreak();
  renderSubjectDonutChart();
  renderActivityHeatmap();
  renderMonthlyCalendar();
  renderPlannerGoals();

  await initAuth();
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

// Modal Scroll Lock Helpers (Prevents background page scrolling while modal is active)
function lockBodyScroll() {
  document.body.classList.add('modal-open');
  document.body.style.overflow = 'hidden';
}

function unlockBodyScroll() {
  const openModals = document.querySelectorAll('.modal-overlay:not(.hidden)');
  if (openModals.length === 0) {
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
  }
}

// Supabase Realtime Leaderboard Listener
let leaderboardRealtimeChannel = null;
function initLeaderboardRealtime() {
  if (!supabaseClient || leaderboardRealtimeChannel) return;
  try {
    leaderboardRealtimeChannel = supabaseClient
      .channel('realtime_daily_leaderboard')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'daily_leaderboard' }, () => {
        leaderboardCache.timestamp = 0;
        const modal = document.getElementById('leaderboardModalOverlay');
        if (modal && !modal.classList.contains('hidden')) {
          fetchLeaderboard(true);
        }
      })
      .subscribe();
  } catch (err) {
    console.warn('Leaderboard realtime subscription error:', err);
  }
}

// Supabase Realtime Live User Sync Listener (App <-> Web instant synchronization)
let userSyncRealtimeChannel = null;
let lastPulledCloudUpdatedAt = 0;

function initUserSyncRealtime() {
  if (!supabaseClient || userSyncRealtimeChannel || !appState.currentUser) return;
  try {
    const user = appState.currentUser;
    const currentUserId = (user.id || '').trim().toLowerCase();
    const currentUserEmail = (user.email || user.user_metadata?.email || '').trim().toLowerCase();

    userSyncRealtimeChannel = supabaseClient
      .channel(`realtime_user_sync_${currentUserId.replace(/[^a-zA-Z0-9_-]/g, '_')}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'user_sync_data'
        },
        (payload) => {
          const newRecord = payload?.new;
          if (!newRecord) return;

          const rowUserId = (newRecord.user_id || '').trim().toLowerCase();
          // Strict user ID matching (RLS compliant)
          const isUserMatch = currentUserId && rowUserId === currentUserId;
          if (!isUserMatch) return;

          const remoteUpdatedAt = Number(newRecord.updated_at) || 0;
          // Guard against echo-loops: if the web pushed this change within 4 seconds or timestamp is older/same, skip
          if (Date.now() - lastCloudPushTime < 4000) return;
          if (remoteUpdatedAt > 0 && remoteUpdatedAt <= lastPulledCloudUpdatedAt) return;

          lastPulledCloudUpdatedAt = remoteUpdatedAt;
          console.log('⚡ Live sync event received from mobile app. Updating Web Studio data in real-time...');
          pullDataFromCloud(false);
        }
      )
      .subscribe();
  } catch (err) {
    console.warn('Live user sync realtime subscription error:', err);
  }
}

function teardownUserSyncRealtime() {
  if (userSyncRealtimeChannel && supabaseClient) {
    try {
      supabaseClient.removeChannel(userSyncRealtimeChannel);
    } catch (_) {}
    userSyncRealtimeChannel = null;
  }
}

// Supabase Authentication
async function initAuth() {
  try {
    getSupabase();
    if (!supabaseClient) {
      setTimeout(() => {
        if (getSupabase()) initAuth();
      }, 200);
      return;
    }

    initLeaderboardRealtime();

    // 1. Attach Auth State Change Listener FIRST
    supabaseClient.auth.onAuthStateChange(async (event, session) => {
      console.log(`🔐 Supabase Auth Event: ${event}`, session?.user?.email);
      if (session && session.user) {
        handleUserSignedIn(session.user);
      } else if (event === 'SIGNED_OUT') {
        handleUserSignedOut();
      }
    });

    // 2. Check for OAuth Hash Errors (#error=...&error_description=...)
    if (window.location.hash && window.location.hash.includes('error=')) {
      try {
        const hash = window.location.hash.substring(1);
        const params = new URLSearchParams(hash);
        const errorMsg = params.get('error_description') || params.get('error') || 'Sign-in error';
        showToast('Google Sign-In: ' + decodeURIComponent(errorMsg), 'error');
        window.history.replaceState(null, '', window.location.pathname + window.location.search);
      } catch (_) {}
    }

    // 3. Check for OAuth Hash Tokens (#access_token=...&refresh_token=...)
    if (window.location.hash && window.location.hash.includes('access_token=')) {
      try {
        const hash = window.location.hash.substring(1);
        const params = new URLSearchParams(hash);
        const access_token = params.get('access_token');
        const refresh_token = params.get('refresh_token');
        if (access_token) {
          const { data, error } = await supabaseClient.auth.setSession({
            access_token,
            refresh_token: refresh_token || ''
          });
          if (!error && data?.session?.user) {
            handleUserSignedIn(data.session.user);
            window.history.replaceState(null, '', window.location.pathname + window.location.search);
            return;
          }
        }
      } catch (hashErr) {
        console.warn('OAuth hash parse error:', hashErr);
      }
    }

    // 4. Check for OAuth PKCE Code (?code=...)
    if (window.location.search && window.location.search.includes('code=')) {
      try {
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        if (code) {
          const { data, error } = await supabaseClient.auth.exchangeCodeForSession(code);
          if (!error && data?.session?.user) {
            handleUserSignedIn(data.session.user);
            window.history.replaceState(null, '', window.location.pathname);
            return;
          }
        }
      } catch (codeErr) {
        console.warn('OAuth code exchange error:', codeErr);
      }
    }

    // 5. Existing Session Check (from localStorage / Supabase auto-detect)
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (session && session.user) {
      handleUserSignedIn(session.user);
      if (window.location.hash.includes('access_token=') || window.location.search.includes('code=')) {
        window.history.replaceState(null, '', window.location.pathname);
      }
    }
  } catch (err) {
    console.error('Supabase auth initialization error:', err);
  }
}

function handleUserSignedIn(user) {
  if (!user) return;
  const isNewlySignedIn = !appState.currentUser || appState.currentUser.id !== user.id;
  
  appState.currentUser = user;

  if (isNewlySignedIn) {
    // Cleanly isolate state for this specific user account
    const cleanState = getCleanInitialState(user);
    Object.assign(appState, cleanState);
    loadLocalState(user.id);
  }
  
  closeAuthModal();

  const btnOpenAuth = document.getElementById('btnOpenAuth');
  const userDropdownContainer = document.getElementById('userDropdownContainer');
  const userDisplayName = document.getElementById('userDisplayName');
  const dropdownUserName = document.getElementById('dropdownUserName');
  const dropdownUserEmail = document.getElementById('dropdownUserEmail');
  const userAvatarImg = document.getElementById('userAvatarImg');
  const guestBanner = document.getElementById('guestBanner');
  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');

  const name = appState.userProfile?.displayName || 
               user.user_metadata?.full_name || 
               user.user_metadata?.name || 
               user.email?.split('@')[0] || 
               'Student';
  const avatar = appState.userProfile?.avatarPreset || 
                 user.user_metadata?.avatar_url || 
                 user.user_metadata?.picture || 
                 'assets/logo.png';

  if (btnOpenAuth) btnOpenAuth.classList.add('hidden');
  if (userDropdownContainer) userDropdownContainer.classList.remove('hidden');
  if (userDisplayName) userDisplayName.textContent = name;
  if (dropdownUserName) dropdownUserName.textContent = name;
  if (dropdownUserEmail) dropdownUserEmail.textContent = user.email || '';
  if (userAvatarImg) userAvatarImg.src = avatar;
  if (guestBanner) guestBanner.classList.add('hidden');

  renderUserProfileUI();

  if (syncStatusPill) {
    syncStatusPill.classList.add('synced');
    syncStatusText.textContent = 'Synced';
  }

  // Immediately render all views for the authenticated user
  if (typeof renderSubjects === 'function') renderSubjects();
  if (typeof updateProgressAndStreak === 'function') updateProgressAndStreak();
  if (typeof renderSubjectDonutChart === 'function') renderSubjectDonutChart();
  if (typeof renderActivityHeatmap === 'function') renderActivityHeatmap();
  if (typeof renderMonthlyCalendar === 'function') renderMonthlyCalendar();
  if (typeof renderPlannerGoals === 'function') renderPlannerGoals();
  if (typeof renderTimeline === 'function') renderTimeline();
  if (typeof updateStatsDisplay === 'function') updateStatsDisplay();

  // Refresh presence & leaderboard on login
  if (timerStatus === 'RUNNING' && currentMode !== 'break') {
    startPresenceHeartbeat();
  }
  leaderboardCache.timestamp = 0;
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }

  initUserSyncRealtime();
  pullDataFromCloud();
  
  if (isNewlySignedIn) {
    showToast(`Welcome, ${name}! Live Cloud Sync active. ☁️`, 'success');
  }
}

function handleUserSignedOut() {
  teardownUserSyncRealtime();
  stopPresenceHeartbeat();
  
  const cleanGuest = getCleanInitialState(null);
  Object.assign(appState, cleanGuest);

  saveLocalState();
  if (typeof renderSubjects === 'function') renderSubjects();
  if (typeof renderPlannerGoals === 'function') renderPlannerGoals();
  if (typeof renderTimeline === 'function') renderTimeline();
  if (typeof updateStatsDisplay === 'function') updateStatsDisplay();
  if (typeof renderSubjectDonutChart === 'function') renderSubjectDonutChart();
  if (typeof renderActivityHeatmap === 'function') renderActivityHeatmap();
  if (typeof renderMonthlyCalendar === 'function') renderMonthlyCalendar();
  if (typeof renderUserProfileUI === 'function') renderUserProfileUI();

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
// 4. TWO-WAY CLOUD SYNC LOGIC (100% Android App Compatible)
// ============================================================================

function parseSafeStringSet(val) {
  const set = new Set();
  if (!val) return set;
  if (Array.isArray(val)) {
    val.forEach(item => { if (item) set.add(String(item).trim()); });
    return set;
  }
  if (typeof val === 'object' && val !== null) {
    Object.keys(val).forEach(k => set.add(k));
    return set;
  }
  if (typeof val === 'string') {
    const trimmed = val.trim();
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          parsed.forEach(item => { if (item) set.add(String(item).trim()); });
          return set;
        }
      } catch (_) {}
    }
    const cleaned = trimmed.replace(/^\[|\]$/g, '');
    cleaned.split(',').map(s => s.trim().replace(/^["']|["']$/g, '')).filter(Boolean).forEach(s => set.add(s));
  }
  return set;
}

async function pullDataFromCloud(isUserTriggered = false) {
  if (!supabaseClient || !appState.currentUser) {
    if (isUserTriggered) showToast('Please sign in with Google to sync with mobile app', 'info');
    return;
  }

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const user = appState.currentUser;
    const userId = user.id;

    // Strict user ID query
    const query = supabaseClient.from('user_sync_data').select('*').eq('user_id', userId);

    const { data: rows, error } = await query.order('updated_at', { ascending: false });

    if (error) {
      console.warn('Error fetching cloud record:', error);
    }

    let data = null;
    if (Array.isArray(rows) && rows.length > 0) {
      // Find record with actual timeline/subject data or the most recent one
      data = rows.find(r => {
        const hasTimeline = r.timeline_data && r.timeline_data !== '[]' && r.timeline_data.length > 10;
        const hasPrefs = r.prefs_data && r.prefs_data.length > 10;
        return hasTimeline || hasPrefs;
      }) || rows[0];
    }

    if (data) {
      if (data.updated_at) {
        lastPulledCloudUpdatedAt = Math.max(lastPulledCloudUpdatedAt, Number(data.updated_at) || 0);
      }
      let restoredSubjectCount = 0;
      let restoredTimelineCount = 0;

      // Cleanly clear state collections before applying this user's cloud data
      const dailyFocus = {};
      let restoredSubDur = {};
      let restoredDailySub = {};
      let restoredGoals = [];
      let restoredTimeline = [];

      if (data.prefs_data) {
        try {
          const prefs = typeof data.prefs_data === 'string' ? JSON.parse(data.prefs_data) : data.prefs_data;
          
          // 1. Daily Goal & Timer Durations (Android & Web interop)
          const goalMins = prefs.daily_goal_minutes || (prefs.daily_goal_secs ? Math.round(prefs.daily_goal_secs / 60) : null);
          if (goalMins) timerConfig.dailyGoalMinutes = Math.min(1440, Math.max(15, goalMins));

          appState.streakCount = Math.max(0, Number(prefs.current_streak || prefs.streak_count) || 0);
          appState.lastStudyDate = prefs.last_study_date || '';

          if (prefs.study_interval_minutes || prefs.pomo_focus_minutes) {
            timerConfig.pomoFocusMinutes = Number(prefs.study_interval_minutes || prefs.pomo_focus_minutes);
          }
          if (prefs.break_interval_minutes || prefs.pomo_break_minutes) {
            timerConfig.pomoBreakMinutes = Number(prefs.break_interval_minutes || prefs.pomo_break_minutes);
          }
          if (prefs.pomo_long_break_minutes) timerConfig.pomoLongBreakMinutes = prefs.pomo_long_break_minutes;
          if (prefs.pomo_total_cycles) timerConfig.pomoTotalCycles = prefs.pomo_total_cycles;
          if (typeof prefs.pomo_auto_switch_break === 'boolean') timerConfig.pomoAutoSwitchBreak = prefs.pomo_auto_switch_break;
          if (typeof prefs.pomo_auto_switch_focus === 'boolean') timerConfig.pomoAutoSwitchFocus = prefs.pomo_auto_switch_focus;
          if (prefs.custom_timer_minutes) timerConfig.customTimerMinutes = prefs.custom_timer_minutes;

          // 2. Extract all daily focus totals & subject durations from Android
          const todayKey = getLocalDateStr();

          Object.keys(prefs).forEach(k => {
            const match = k.match(/^(\d{4}-\d{2}-\d{2})_focus_total$/);
            if (match) {
              const dStr = match[1];
              const sec = Number(prefs[k]) || 0;
              if (sec > 0) dailyFocus[dStr] = sec;
            }
          });

          if (prefs.accumulatedStudy) {
            const acc = Number(prefs.accumulatedStudy) || 0;
            if (acc > 0) {
              dailyFocus[todayKey] = Math.max(dailyFocus[todayKey] || 0, acc);
            }
          }

          if (prefs.subject_durations_json) {
            try {
              const rawSubDur = typeof prefs.subject_durations_json === 'string'
                ? JSON.parse(prefs.subject_durations_json)
                : prefs.subject_durations_json;
              if (rawSubDur && typeof rawSubDur === 'object') restoredSubDur = rawSubDur;
            } catch (_) {}
          }

          if (prefs.daily_subject_durations_json) {
            try {
              const rawDailySub = typeof prefs.daily_subject_durations_json === 'string'
                ? JSON.parse(prefs.daily_subject_durations_json)
                : prefs.daily_subject_durations_json;
              if (rawDailySub && typeof rawDailySub === 'object') restoredDailySub = rawDailySub;
            } catch (_) {}
          }

          appState.dailyFocusTotals = dailyFocus;
          appState.subjectDurations = restoredSubDur;
          appState.dailySubjectDurations = restoredDailySub;

          // 3. Full Subjects Restoration (Android studytimer_subject_tags & Web custom_subjects)
          const rawTags = prefs.__subject_tags_data__ || data.subject_tags_data || prefs.custom_subjects_json;
          let subjectPrefs = null;
          if (rawTags) {
            try {
              subjectPrefs = typeof rawTags === 'string' ? JSON.parse(rawTags) : rawTags;
            } catch (_) {}
          }

          let customList = [];
          let hiddenSet = new Set();
          let selectedSubId = prefs.selected_subject_id || 'general';

          if (subjectPrefs) {
            if (subjectPrefs.custom_subjects_json) {
              try {
                customList = typeof subjectPrefs.custom_subjects_json === 'string'
                  ? JSON.parse(subjectPrefs.custom_subjects_json)
                  : subjectPrefs.custom_subjects_json;
              } catch (_) {}
            } else if (subjectPrefs.custom_subjects) {
              try {
                customList = typeof subjectPrefs.custom_subjects === 'string'
                  ? JSON.parse(subjectPrefs.custom_subjects)
                  : subjectPrefs.custom_subjects;
              } catch (_) {}
            } else if (Array.isArray(subjectPrefs)) {
              customList = subjectPrefs;
            }

            if (subjectPrefs.hidden_subjects_set) {
              hiddenSet = parseSafeStringSet(subjectPrefs.hidden_subjects_set);
            }
            if (subjectPrefs.selected_subject_id) {
              selectedSubId = subjectPrefs.selected_subject_id;
            }
          }

          if ((!customList || customList.length === 0) && prefs.custom_subjects_json) {
            try {
              customList = typeof prefs.custom_subjects_json === 'string'
                ? JSON.parse(prefs.custom_subjects_json)
                : prefs.custom_subjects_json;
            } catch (_) {}
          }

          const mergedSubjects = [];
          DEFAULT_SUBJECTS.forEach(d => {
            if (!hiddenSet.has(d.id)) {
              mergedSubjects.push({ ...d });
            }
          });

          if (Array.isArray(customList)) {
            customList.forEach(c => {
              if (c && c.id && !hiddenSet.has(c.id)) {
                const existingIdx = mergedSubjects.findIndex(s => s.id === c.id);
                const formatted = {
                  id: c.id,
                  name: c.name || 'Subject',
                  color: c.colorHex || c.color || '#3b82f6',
                  colorHex: c.colorHex || c.color || '#3b82f6',
                  iconEmoji: c.iconEmoji || '📚',
                  isCustom: true
                };
                if (existingIdx >= 0) {
                  mergedSubjects[existingIdx] = formatted;
                } else {
                  mergedSubjects.push(formatted);
                }
              }
            });
          }

          // Also check timeline_data for subjects that might have been studied in Android
          if (data.timeline_data) {
            try {
              const tl = typeof data.timeline_data === 'string' ? JSON.parse(data.timeline_data) : data.timeline_data;
              if (Array.isArray(tl)) {
                tl.forEach(e => {
                  if (e && e.subId && !hiddenSet.has(e.subId) && !mergedSubjects.some(s => s.id === e.subId)) {
                    mergedSubjects.push({
                      id: e.subId,
                      name: e.subName || e.subId,
                      color: e.subColor || '#3b82f6',
                      colorHex: e.subColor || '#3b82f6',
                      iconEmoji: '📚',
                      isCustom: true
                    });
                  }
                });
              }
            } catch (_) {}
          }

          if (mergedSubjects.length > 0) {
            appState.subjects = mergedSubjects;
            restoredSubjectCount = mergedSubjects.length;
            const foundSel = appState.subjects.find(s => s.id === selectedSubId);
            appState.selectedSubject = foundSel || appState.subjects[0];
          }

          // 4. Planner Goals Restoration (Android session_goals_json & Web __planner_goals_data__)
          let loadedGoals = null;
          if (prefs.session_goals_json) {
            try {
              loadedGoals = typeof prefs.session_goals_json === 'string'
                ? JSON.parse(prefs.session_goals_json)
                : prefs.session_goals_json;
            } catch (_) {}
          } else if (prefs.__planner_goals_data__) {
            try {
              loadedGoals = typeof prefs.__planner_goals_data__ === 'string'
                ? JSON.parse(prefs.__planner_goals_data__)
                : prefs.__planner_goals_data__;
            } catch (_) {}
          }

          if (Array.isArray(loadedGoals)) {
            restoredGoals = loadedGoals.map(g => ({
              id: g.id || ('goal_' + Date.now()),
              subjectId: g.subjectId || null,
              dailyMinutes: Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0),
              targetMinutes: Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0),
              title: g.title || '',
              note: g.note || '',
              completed: !!g.completed,
              checkedAt: g.checkedAt || null,
              createdAt: g.createdAt || Date.now()
            }));
          }
          appState.plannerGoals = restoredGoals;

          // 5. User Profile
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

      // 6. Timeline History Restoration
      if (data.timeline_data) {
        try {
          const parsedTimeline = typeof data.timeline_data === 'string' ? JSON.parse(data.timeline_data) : data.timeline_data;
          if (Array.isArray(parsedTimeline)) {
            restoredTimeline = parsedTimeline;
            restoredTimelineCount = parsedTimeline.length;
          }
        } catch (e) {
          console.error('Failed to parse remote timeline_data', e);
        }
      }
      appState.timelineEntries = restoredTimeline;

      reconstructTodaySessionsFromTimeline();

      // Auto-sync presence & leaderboard for mobile logged in user
      let totalSecToday = 0;
      (appState.todaySessions || []).forEach(s => {
        if (s && typeof s.durationSec === 'number') totalSecToday += s.durationSec;
      });
      // Sync live score to global leaderboard table
      await syncLeaderboardScore();

      renderSubjects();
      renderUserProfileUI();
      updateProgressAndStreak();
      renderSubjectDonutChart();
      renderActivityHeatmap();
      renderMonthlyCalendar();
      renderPlannerGoals();
      switchInsightsTab(currentInsightsTab);
      saveLocalState();
      if (timerStatus === 'IDLE') {
        resetTimer();
      }

      // Update leaderboard UI if open
      const lbModal = document.getElementById('leaderboardModalOverlay');
      if (lbModal && !lbModal.classList.contains('hidden')) {
        fetchLeaderboard(true);
      }

      if (isUserTriggered) {
        const validatedSessions = getAllValidatedSessions();
        const activeDaysCount = Object.keys(appState.dailyFocusTotals || {}).length || validatedSessions.length;
        showToast(`Sync complete! Loaded ${appState.subjects.length} subjects & ${appState.plannerGoals.length} goals. ☁️`, 'success');
      }
    } else {
      if (isUserTriggered) {
        showToast('No existing cloud backup found. Clean account initialized.', 'info');
      }
    }
  } catch (err) {
    console.error('Cloud pull exception:', err);
    if (isUserTriggered) showToast('Sync failed: Network error', 'error');
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

async function pushDataToCloud(silent = false) {
  saveLocalState();
  if (!supabaseClient || !appState.currentUser) return;

  // Rate Limiting & Cooldown Protection (Minimum 3 seconds between cloud API calls)
  const now = Date.now();
  if (now - lastCloudPushTime < 3000) {
    if (cloudPushDebounceTimer) clearTimeout(cloudPushDebounceTimer);
    cloudPushDebounceTimer = setTimeout(() => {
      pushDataToCloud(silent);
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
    const userEmail = (user.email || user.user_metadata?.email || '').trim().slice(0, 150);
    const profileImg = (user.user_metadata?.avatar_url || '').slice(0, 500);

    // Payload sanitization & safety caps
    const sanitizedSubjects = Array.isArray(appState.subjects) ? appState.subjects.slice(0, 50) : [];
    const sanitizedTimeline = Array.isArray(appState.timelineEntries) ? appState.timelineEntries.slice(-500) : [];
    const sanitizedGoals = Array.isArray(appState.plannerGoals) ? appState.plannerGoals.slice(0, 50) : [];

    // Format subjects specifically for Android's studytimer_subject_tags SharedPreferences
    const defaultIds = DEFAULT_SUBJECTS.map(d => d.id);
    const currentSubjectIds = new Set(sanitizedSubjects.map(s => s.id));
    const hiddenDefaultIds = defaultIds.filter(id => !currentSubjectIds.has(id));

    const customSubjectsForAndroid = sanitizedSubjects
      .filter(s => !defaultIds.includes(s.id))
      .map(s => ({
        id: s.id,
        name: s.name,
        iconEmoji: s.iconEmoji || '📚',
        colorHex: s.color || s.colorHex || '#3b82f6',
        isCustom: true
      }));

    const allSubjectsForWeb = sanitizedSubjects.map(s => ({
      id: s.id,
      name: s.name,
      color: s.color || s.colorHex || '#3b82f6',
      colorHex: s.colorHex || s.color || '#3b82f6',
      iconEmoji: s.iconEmoji || '📚'
    }));

    const subjectTagsObj = {
      custom_subjects_json: JSON.stringify(customSubjectsForAndroid),
      custom_subjects: JSON.stringify(allSubjectsForWeb),
      selected_subject_id: appState.selectedSubject?.id || 'general',
      hidden_subjects_set: hiddenDefaultIds
    };

    // Format planner goals for Android's session_goals_json (100% matches SessionGoal Kotlin class)
    const androidPlannerGoals = sanitizedGoals.map(g => {
      const matchingSub = sanitizedSubjects.find(s => s.id === g.subjectId);
      return {
        id: g.id || ('goal_' + Date.now()),
        title: g.title || (matchingSub ? `${matchingSub.name} Goal` : 'Daily Study Goal'),
        note: g.note || '',
        targetMinutes: Math.min(1440, Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0)),
        subjectId: (g.subjectId && g.subjectId !== 'all') ? g.subjectId : null,
        completed: !!g.completed,
        checkedAt: g.checkedAt || (g.completed ? Date.now() : 0),
        createdAt: g.createdAt || Date.now()
      };
    });

    const dailyGoalMin = Math.min(1440, Math.max(1, Number(timerConfig.dailyGoalMinutes) || 120));

    // Calculate today's study seconds
    const todayKey = getLocalDateStr();
    let totalSecToday = 0;
    (appState.todaySessions || []).forEach(s => {
      if (s && typeof s.durationSec === 'number') totalSecToday += s.durationSec;
    });

    const prefsObj = {
      daily_goal_minutes: dailyGoalMin,
      daily_goal_secs: dailyGoalMin * 60,
      study_interval_minutes: Math.min(180, Math.max(1, Number(timerConfig.pomoFocusMinutes) || 25)),
      break_interval_minutes: Math.min(60, Math.max(1, Number(timerConfig.pomoBreakMinutes) || 5)),
      custom_timer_minutes: Math.min(720, Math.max(1, Number(timerConfig.customTimerMinutes) || 45)),
      pomo_focus_minutes: Math.min(180, Math.max(1, Number(timerConfig.pomoFocusMinutes) || 25)),
      pomo_break_minutes: Math.min(60, Math.max(1, Number(timerConfig.pomoBreakMinutes) || 5)),
      pomo_long_break_minutes: Math.min(120, Math.max(1, Number(timerConfig.pomoLongBreakMinutes) || 15)),
      pomo_total_cycles: Math.min(12, Math.max(1, Number(timerConfig.pomoTotalCycles) || 4)),
      pomo_auto_switch_break: timerConfig.pomoAutoSwitchBreak !== false,
      pomo_auto_switch_focus: timerConfig.pomoAutoSwitchFocus !== false,
      current_streak: Math.max(0, Number(appState.streakCount) || 0),
      streak_count: Math.max(0, Number(appState.streakCount) || 0),
      last_study_date: appState.lastStudyDate || '',
      accumulatedStudy: totalSecToday,
      [`${todayKey}_focus_total`]: totalSecToday,
      session_goals_json: JSON.stringify(androidPlannerGoals),
      __subject_tags_data__: JSON.stringify(subjectTagsObj),
      custom_subjects_json: JSON.stringify(customSubjectsForAndroid),
      __planner_goals_data__: JSON.stringify(androidPlannerGoals),
      __user_profile__: JSON.stringify(appState.userProfile)
    };

    // Preserve previous daily focus records
    if (appState.dailyFocusTotals) {
      Object.keys(appState.dailyFocusTotals).forEach(d => {
        if (d !== todayKey && appState.dailyFocusTotals[d]) {
          prefsObj[`${d}_focus_total`] = appState.dailyFocusTotals[d];
        }
      });
    }

    if (appState.subjectDurations) {
      prefsObj.subject_durations_json = JSON.stringify(appState.subjectDurations);
    }
    if (appState.dailySubjectDurations) {
      prefsObj.daily_subject_durations_json = JSON.stringify(appState.dailySubjectDurations);
    }


    const nowMs = Date.now();
    lastCloudPushTime = nowMs;
    lastPulledCloudUpdatedAt = nowMs;
    const payload = {
      user_id: user.id,
      user_name: userName,
      user_email: userEmail,
      profile_image_uri: profileImg,
      prefs_data: JSON.stringify(prefsObj),
      timeline_data: JSON.stringify(sanitizedTimeline),
      updated_at: nowMs
    };

    const { error } = await supabaseClient
      .from('user_sync_data')
      .upsert(payload, { onConflict: 'user_id' });

    if (error) {
      console.warn('Cloud sync push warning:', error);
      if (!silent) showToast('Cloud sync failed to update.', 'error');
    } else {
      if (!silent) showToast('Session synced with Android app!', 'success');
    }
  } catch (err) {
    console.warn('pushDataToCloud exception:', err);
  } finally {
    if (syncStatusPill) {
      syncStatusPill.classList.remove('syncing');
      syncStatusPill.classList.add('synced');
      syncStatusText.textContent = 'Synced';
    }
  }
}

// Universal timeline session parser (Handles Android TimelineLogger & Web formats)
function parseAllTimelineSessions(rawEntries) {
  if (!Array.isArray(rawEntries) || rawEntries.length === 0) return [];

  // Filter valid entries & sort ascending by timestamp
  const entries = rawEntries
    .filter(e => e && typeof e.t === 'number')
    .sort((a, b) => a.t - b.t);

  const studyStates = new Set(['STUDYING', 'POMODORO', 'COUNT_UP', 'COUNTDOWN', 'MANUAL_FOCUS', 'FOCUS']);
  const nonStudyStates = new Set(['IDLE', 'PAUSED', 'STOPPED', 'BREAK', 'MANUAL_BREAK']);

  const parsedSessions = [];
  let openSession = null;

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];

    if (studyStates.has(entry.s)) {
      if (openSession) {
        const gapSec = Math.round((entry.t - openSession.t) / 1000);
        if (gapSec > 0 && gapSec <= 86400) {
          parsedSessions.push(buildSessionFromEntry(openSession, entry.t, gapSec));
        }
      }
      if (typeof entry.durationSec === 'number' && entry.durationSec > 0) {
        parsedSessions.push(buildSessionFromEntry(entry, entry.endT || (entry.t + entry.durationSec * 1000), entry.durationSec));
        openSession = null;
      } else {
        openSession = entry;
      }
    } else if (nonStudyStates.has(entry.s)) {
      if (openSession) {
        const gapSec = Math.round((entry.t - openSession.t) / 1000);
        if (gapSec > 0 && gapSec <= 86400) {
          parsedSessions.push(buildSessionFromEntry(openSession, entry.t, gapSec));
        }
        openSession = null;
      }
    }
  }

  // Handle open session if it had explicit duration
  if (openSession && typeof openSession.durationSec === 'number' && openSession.durationSec > 0) {
    parsedSessions.push(buildSessionFromEntry(openSession, openSession.endT || (openSession.t + openSession.durationSec * 1000), openSession.durationSec));
  }

  return parsedSessions;
}

function buildSessionFromEntry(entry, endMs, durationSec) {
  let modeLabel = 'Focus Study';
  if (entry.s === 'POMODORO' || entry.s === 'COUNTDOWN') modeLabel = 'Pomodoro';
  else if (entry.s === 'COUNT_UP') modeLabel = 'Stopwatch';
  else if (entry.s === 'MANUAL_FOCUS') modeLabel = 'Manual Focus';

  const subId = entry.subId || 'general';
  const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
    id: subId,
    name: entry.subName || 'General',
    color: entry.subColor || '#6366f1'
  };

  return {
    id: 'sess_' + entry.t,
    subject: {
      id: matchingSub.id,
      name: entry.subName || matchingSub.name,
      color: entry.subColor || matchingSub.color || matchingSub.colorHex || '#6366f1'
    },
    durationSec: Math.max(1, durationSec),
    startTime: entry.t,
    endTime: endMs || (entry.t + durationSec * 1000),
    timestamp: entry.t,
    mode: modeLabel
  };
}

function reconstructTodaySessionsFromTimeline() {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);
  const startOfDayMs = startOfDay.getTime();
  const endOfDayMs = startOfDayMs + 86400000;
  const todayKey = getLocalDateStr();

  const allParsed = parseAllTimelineSessions(appState.timelineEntries || []);
  let sessions = allParsed
    .filter(s => s.timestamp >= startOfDayMs && s.timestamp < endOfDayMs)
    .sort((a, b) => b.timestamp - a.timestamp);

  // If timeline entries had no sessions for today, but Android recorded today's focus total in prefs
  const todayFocusSec = (appState.dailyFocusTotals && Number(appState.dailyFocusTotals[todayKey])) || 0;
  const currentSum = sessions.reduce((acc, s) => acc + (s.durationSec || 0), 0);

  if (todayFocusSec > currentSum) {
    const missingSec = todayFocusSec - currentSum;
    const dayBreakdown = (appState.dailySubjectDurations && appState.dailySubjectDurations[todayKey]) || null;
    if (dayBreakdown && typeof dayBreakdown === 'object') {
      Object.keys(dayBreakdown).forEach((subId, idx) => {
        const subSec = Number(dayBreakdown[subId]) || 0;
        if (subSec > 0) {
          const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
            id: subId,
            name: subId.charAt(0).toUpperCase() + subId.slice(1),
            color: '#3b82f6'
          };
          sessions.push({
            id: `today_cloud_${todayKey}_${subId}_${idx}`,
            subject: {
              id: matchingSub.id,
              name: matchingSub.name,
              color: matchingSub.color || matchingSub.colorHex || '#3b82f6'
            },
            durationSec: subSec,
            startTime: startOfDayMs + 36000000 + idx * 1000,
            endTime: startOfDayMs + 36000000 + idx * 1000 + subSec * 1000,
            timestamp: startOfDayMs + 36000000 + idx * 1000,
            mode: 'Focus Study'
          });
        }
      });
    } else {
      const defaultSub = appState.selectedSubject || (appState.subjects && appState.subjects[0]) || DEFAULT_SUBJECTS[0];
      sessions.push({
        id: `today_cloud_${todayKey}_${Date.now()}`,
        subject: {
          id: defaultSub.id,
          name: defaultSub.name,
          color: defaultSub.color || defaultSub.colorHex || '#3b82f6'
        },
        durationSec: missingSec,
        startTime: startOfDayMs + 36000000,
        endTime: startOfDayMs + 36000000 + missingSec * 1000,
        timestamp: startOfDayMs + 36000000,
        mode: 'Focus Study'
      });
    }
  }

  appState.todaySessions = sessions;
}

// Universal extractor for all completed sessions with zero duplicate or hardcoded inflations
function getAllValidatedSessions() {
  const allParsed = parseAllTimelineSessions(appState.timelineEntries || []);
  const seenDates = {};
  const sessions = [];

  // 1. Add all parsed timeline sessions
  allParsed.forEach(s => {
    const dStr = getLocalDateStr(s.timestamp);
    seenDates[dStr] = (seenDates[dStr] || 0) + s.durationSec;
    sessions.push(s);
  });

  // 2. Add real-time today sessions if not already in timeline
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      const alreadyHas = sessions.some(existing => Math.abs(existing.timestamp - s.timestamp) < 2000);
      if (!alreadyHas) {
        const dStr = getLocalDateStr(s.timestamp);
        seenDates[dStr] = (seenDates[dStr] || 0) + s.durationSec;
        sessions.push(s);
      }
    }
  });

  // 3. For any date in dailyFocusTotals from Android that doesn't have complete sessions in timeline:
  const dailyTotals = appState.dailyFocusTotals || {};
  Object.keys(dailyTotals).forEach(dateStr => {
    const totalRecordedSec = Number(dailyTotals[dateStr]) || 0;
    const currentParsedSec = seenDates[dateStr] || 0;
    const missingSec = totalRecordedSec - currentParsedSec;

    if (missingSec > 0) {
      const parsedDate = new Date(dateStr + 'T12:00:00Z');
      const ts = !isNaN(parsedDate.getTime()) ? parsedDate.getTime() : Date.now();

      const dayBreakdown = (appState.dailySubjectDurations && appState.dailySubjectDurations[dateStr]) || null;
      if (dayBreakdown && typeof dayBreakdown === 'object') {
        Object.keys(dayBreakdown).forEach((subId, idx) => {
          const subSec = Number(dayBreakdown[subId]) || 0;
          if (subSec > 0) {
            const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
              id: subId,
              name: subId.charAt(0).toUpperCase() + subId.slice(1),
              color: '#3b82f6'
            };
            sessions.push({
              id: `cloud_day_${dateStr}_${subId}_${idx}`,
              subject: {
                id: matchingSub.id,
                name: matchingSub.name,
                color: matchingSub.color || matchingSub.colorHex || '#3b82f6'
              },
              durationSec: subSec,
              startTime: ts + idx * 1000,
              endTime: ts + idx * 1000 + subSec * 1000,
              timestamp: ts + idx * 1000,
              mode: 'Focus Study'
            });
          }
        });
      } else {
        const defaultSub = appState.selectedSubject || (appState.subjects && appState.subjects[0]) || DEFAULT_SUBJECTS[0];
        sessions.push({
          id: `cloud_day_${dateStr}`,
          subject: {
            id: defaultSub.id,
            name: defaultSub.name,
            color: defaultSub.color || defaultSub.colorHex || '#3b82f6'
          },
          durationSec: missingSec,
          startTime: ts,
          endTime: ts + missingSec * 1000,
          timestamp: ts,
          mode: 'Focus Study'
        });
      }
    }
  });

  return sessions.sort((a, b) => b.timestamp - a.timestamp);
}

// ============================================================================
// 5. LOCAL STORAGE PERSISTENCE (Strictly User-Scoped)
// ============================================================================
function loadLocalState(targetUserId = null) {
  try {
    const uid = targetUserId || appState.currentUser?.id;
    const storageKey = uid ? `studytimer_state_${uid}` : 'studytimer_guest_state';
    const local = localStorage.getItem(storageKey);
    if (local) {
      const parsed = JSON.parse(local);
      if (parsed.timerConfig) timerConfig = { ...timerConfig, ...parsed.timerConfig };
      if (typeof parsed.streakCount === 'number') appState.streakCount = parsed.streakCount;
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
      if (parsed.dailyFocusTotals && typeof parsed.dailyFocusTotals === 'object') {
        appState.dailyFocusTotals = parsed.dailyFocusTotals;
      }
      if (parsed.subjectDurations && typeof parsed.subjectDurations === 'object') {
        appState.subjectDurations = parsed.subjectDurations;
      }
      if (parsed.dailySubjectDurations && typeof parsed.dailySubjectDurations === 'object') {
        appState.dailySubjectDurations = parsed.dailySubjectDurations;
      }

      const startOfDay = new Date();
      startOfDay.setHours(0, 0, 0, 0);
      const startOfDayMs = startOfDay.getTime();
      const endOfDayMs = startOfDayMs + 86400000;

      if (Array.isArray(parsed.todaySessions) && parsed.todaySessions.length > 0) {
        appState.todaySessions = parsed.todaySessions.filter(s => 
          s && typeof s.durationSec === 'number' && s.durationSec > 0 &&
          s.timestamp >= startOfDayMs && s.timestamp < endOfDayMs
        );
      } else {
        reconstructTodaySessionsFromTimeline();
      }
    }
  } catch (e) {
    console.error('Failed to load local state:', e);
  }
}

function saveLocalState() {
  try {
    const uid = appState.currentUser?.id;
    const storageKey = uid ? `studytimer_state_${uid}` : 'studytimer_guest_state';
    const stateToSave = {
      timerConfig,
      streakCount: appState.streakCount,
      lastStudyDate: appState.lastStudyDate,
      userProfile: appState.userProfile,
      subjects: appState.subjects,
      plannerGoals: appState.plannerGoals || [],
      timelineEntries: appState.timelineEntries || [],
      todaySessions: appState.todaySessions || [],
      dailyFocusTotals: appState.dailyFocusTotals || {},
      subjectDurations: appState.subjectDurations || {},
      dailySubjectDurations: appState.dailySubjectDurations || {}
    };
    localStorage.setItem(storageKey, JSON.stringify(stateToSave));
  } catch (e) {
    console.error('Failed to save local state:', e);
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

  // Dedicated Desktop & Mobile Leaderboard Triggers
  document.getElementById('btnDesktopLeaderboard')?.addEventListener('click', openLeaderboardModal);
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

  // Leaderboard Period Timeframe Tabs (Daily / Weekly / Monthly)
  document.querySelectorAll('.lb-timeframe-tab').forEach(tabBtn => {
    tabBtn.addEventListener('click', (e) => {
      const period = e.currentTarget.dataset.period || 'daily';
      switchLeaderboardTimeframe(period);
    });
  });

  // Manual "Sync with App" Dropdown Trigger
  document.getElementById('btnManualSync')?.addEventListener('click', async () => {
    document.getElementById('userMenuDropdown')?.classList.add('hidden');
    if (!appState.currentUser) {
      openAuthModal();
      return;
    }
    showToast('Pulling latest data from Android app...', 'info');
    await pullDataFromCloud(true);
    showToast('All study sessions, subjects & goals synced with Android app!', 'success');
  });

  // Day-by-Day Subject Pie Chart & Timeline History Modal (Matches Android showPieChartDetailsModal)
  document.getElementById('btnOpenDayPieHistoryModal')?.addEventListener('click', () => openDayPieHistoryModal());
  document.getElementById('donutSvgWrap')?.addEventListener('click', () => openDayPieHistoryModal());
  document.getElementById('btnCloseDayPieHistoryModal')?.addEventListener('click', closeDayPieHistoryModal);
  document.getElementById('dayPieHistoryModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'dayPieHistoryModalOverlay') closeDayPieHistoryModal();
  });
  document.getElementById('btnHistoryPrevDay')?.addEventListener('click', () => changeHistoryModalDay(-1));
  document.getElementById('btnHistoryNextDay')?.addEventListener('click', () => changeHistoryModalDay(1));
  document.getElementById('btnHistoryJumpToday')?.addEventListener('click', () => {
    activeHistoryModalDateStr = getLocalDateStr();
    renderDayPieHistoryModal(activeHistoryModalDateStr);
  });
  const historyDatePicker = document.getElementById('historyNativeDatePicker');
  document.getElementById('btnHistoryDateDisplay')?.addEventListener('click', () => {
    if (historyDatePicker) {
      historyDatePicker.value = activeHistoryModalDateStr;
      historyDatePicker.max = getLocalDateStr();
      if (typeof historyDatePicker.showPicker === 'function') {
        historyDatePicker.showPicker();
      } else {
        historyDatePicker.click();
      }
    }
  });
  historyDatePicker?.addEventListener('change', (e) => {
    if (e.target.value) {
      activeHistoryModalDateStr = e.target.value;
      renderDayPieHistoryModal(activeHistoryModalDateStr);
    }
  });

  // Guest Banner Sign-in Trigger
  document.getElementById('btnBannerSignIn')?.addEventListener('click', () => {
    openAuthModal();
  });

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

  // Planner Goals Modal Triggers (Add / Edit / Checkbox)
  document.getElementById('btnOpenAddGoalModal')?.addEventListener('click', openAddGoalModal);
  document.getElementById('btnCloseGoalModal')?.addEventListener('click', closeAddGoalModal);
  document.getElementById('btnCancelGoalModal')?.addEventListener('click', closeAddGoalModal);
  document.getElementById('plannerGoalModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'plannerGoalModalOverlay') closeAddGoalModal();
  });
  document.getElementById('addGoalForm')?.addEventListener('submit', handleSaveGoal);

  // Duration Presets for Add/Edit Goal Modal
  document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const mins = btn.dataset.mins;
      const input = document.getElementById('goalMinutesInput');
      if (input && mins !== undefined) input.value = mins;
    });
  });

  // Edit Overall Daily Focus Target Modal Triggers (Overview Tab)
  document.getElementById('btnEditGoal')?.addEventListener('click', openEditDailyGoalModal);
  document.getElementById('btnCloseDailyGoalModal')?.addEventListener('click', closeDailyGoalModal);
  document.getElementById('btnCancelDailyGoalModal')?.addEventListener('click', closeDailyGoalModal);
  document.getElementById('dailyGoalModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'dailyGoalModalOverlay') closeDailyGoalModal();
  });
  document.getElementById('dailyGoalEditForm')?.addEventListener('submit', handleSaveDailyGoal);

  // Daily Target Presets in Edit Target Modal
  document.querySelectorAll('#dailyTargetPresetsRow .preset-chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#dailyTargetPresetsRow .preset-chip-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const mins = btn.dataset.mins;
      const input = document.getElementById('inputDailyGoalMinutes');
      if (input && mins !== undefined) input.value = mins;
    });
  });

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
  showToast('Entered Full Screen Focus Mode', 'info');
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
      showToast('Entered Fullscreen Focus Mode', 'info');
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
      modeBadge.textContent = 'Countdown Timer';
    } else if (modeKey === 'pomodoro') {
      const total = timerConfig.pomoTotalCycles || 4;
      modeBadge.textContent = `Pomodoro Focus (${pomoCurrentCycle}/${total})`;
    } else if (modeKey === 'stopwatch') {
      modeBadge.textContent = 'Stopwatch';
    } else if (modeKey === 'break') {
      const dur = isLongBreakActive ? (timerConfig.pomoLongBreakMinutes || 15) : (timerConfig.pomoBreakMinutes || 5);
      modeBadge.textContent = `${isLongBreakActive ? 'Long Break' : 'Short Break'} (${dur}m)`;
    }
  }

  // Synchronize top mode buttons
  document.querySelectorAll('.mode-btn').forEach(btn => {
    if (btn.dataset.mode === modeKey) {
      btn.classList.add('active');
      btn.setAttribute('aria-selected', 'true');
    } else {
      btn.classList.remove('active');
      btn.setAttribute('aria-selected', 'false');
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
let lastAutoSavedMinute = 0;
let defaultPageTitle = document.title || 'StudyTimer Web - Focus Timer & Habit Tracker for Students';

// Native Offline Web Audio Synthesizer (Zero external mp3 dependencies)
function playAlarmChime() {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    if (ctx.state === 'suspended') {
      ctx.resume();
    }
    const notes = [587.33, 880, 1174.66]; // D5, A5, D6 harmonic chime
    notes.forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.18);
      gain.gain.setValueAtTime(0.001, ctx.currentTime + idx * 0.18);
      gain.gain.exponentialRampToValueAtTime(0.28, ctx.currentTime + idx * 0.18 + 0.04);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + idx * 0.18 + 1.2);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime + idx * 0.18);
      osc.stop(ctx.currentTime + idx * 0.18 + 1.25);
    });
  } catch (err) {
    console.warn('Audio chime warning:', err);
  }
}

// System Web Notifications (Notifies when tab is minimized or switched to another app)
function sendSessionNotification(title, body) {
  try {
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification(title, {
          body,
          icon: 'assets/logo.png',
          badge: 'assets/logo.png',
          tag: 'studytimer-session-alert'
        });
      } else if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  } catch (_) {}
}

function initTimerWorker() {
  try {
    const workerCode = `
      let interval = null;
      self.onmessage = function(e) {
        if (e.data === 'start') {
          if (interval) clearInterval(interval);
          interval = setInterval(() => {
            self.postMessage('tick');
          }, 200);
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
    } else {
      if (timerStatus === 'RUNNING') {
        saveActiveSessionState();
        saveLocalState();
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

  window.addEventListener('online', () => {
    const syncStatusPill = document.getElementById('syncStatusPill');
    const syncStatusText = document.getElementById('syncStatusText');
    if (syncStatusPill) {
      syncStatusPill.classList.remove('offline');
      if (appState.currentUser) {
        syncStatusPill.classList.add('synced');
        syncStatusText.textContent = 'Synced';
        showToast('Online! Cloud sync reconnected ☁️', 'success');
        pullDataFromCloud();
      } else {
        syncStatusText.textContent = 'Local';
      }
    }
  });

  window.addEventListener('offline', () => {
    const syncStatusPill = document.getElementById('syncStatusPill');
    const syncStatusText = document.getElementById('syncStatusText');
    if (syncStatusPill) {
      syncStatusPill.classList.add('offline');
      syncStatusText.textContent = 'Offline (Local)';
      showToast('Offline Mode: All study data saved locally 📱', 'info');
    }
  });

  window.addEventListener('beforeunload', () => {
    if (timerStatus === 'RUNNING') {
      saveActiveSessionState();
      saveLocalState();
    }
  });

  window.addEventListener('pagehide', () => {
    if (timerStatus === 'RUNNING') {
      saveActiveSessionState();
      saveLocalState();
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

// Persist active running session to localStorage for crash & tab-switch resilience
function saveActiveSessionState() {
  if (timerStatus === 'RUNNING' || timerStatus === 'PAUSED') {
    const sessionSnapshot = {
      timerStatus,
      currentMode,
      pomoCurrentCycle,
      isLongBreakActive,
      timerStartTimestamp,
      accumulatedElapsedSec,
      selectedSubjectId: appState.selectedSubject?.id || 'general',
      savedAt: Date.now()
    };
    try {
      localStorage.setItem('studytimer_active_session', JSON.stringify(sessionSnapshot));
    } catch (_) {}
  } else {
    clearActiveSessionState();
  }
}

function clearActiveSessionState() {
  try {
    localStorage.removeItem('studytimer_active_session');
  } catch (_) {}
}

function restoreActiveSessionIfAny() {
  try {
    const raw = localStorage.getItem('studytimer_active_session');
    if (!raw) return false;
    const session = JSON.parse(raw);
    if (!session || !session.timerStatus) return false;

    if (session.currentMode) currentMode = session.currentMode;
    if (session.pomoCurrentCycle) pomoCurrentCycle = session.pomoCurrentCycle;
    if (typeof session.isLongBreakActive === 'boolean') isLongBreakActive = session.isLongBreakActive;

    if (session.selectedSubjectId) {
      const foundSub = (appState.subjects || []).find(s => s.id === session.selectedSubjectId);
      if (foundSub) appState.selectedSubject = foundSub;
    }

    if (session.timerStatus === 'PAUSED') {
      timerStatus = 'PAUSED';
      accumulatedElapsedSec = session.accumulatedElapsedSec || 0;
      timerStartTimestamp = null;
      if (currentMode === 'stopwatch') {
        stopwatchElapsed = accumulatedElapsedSec;
      } else {
        const totalSec = getModeDurationSec();
        timeRemaining = Math.max(0, totalSec - accumulatedElapsedSec);
      }
      updateTimerControlsUI();
      updateTimerDisplay();
      return true;
    } else if (session.timerStatus === 'RUNNING' && session.timerStartTimestamp) {
      const now = Date.now();
      const elapsedSinceStart = Math.floor((now - session.timerStartTimestamp) / 1000);
      const totalElapsed = (session.accumulatedElapsedSec || 0) + elapsedSinceStart;
      const totalSec = getModeDurationSec();

      if (currentMode !== 'stopwatch' && totalElapsed >= totalSec && totalSec > 0) {
        // Session finished while tab/browser was away
        accumulatedElapsedSec = totalSec;
        timeRemaining = 0;
        clearActiveSessionState();
        finishSession(true);
        return true;
      } else {
        timerStatus = 'RUNNING';
        accumulatedElapsedSec = session.accumulatedElapsedSec || 0;
        timerStartTimestamp = session.timerStartTimestamp;
        if (currentMode === 'stopwatch') {
          stopwatchElapsed = totalElapsed;
        } else {
          timeRemaining = Math.max(0, totalSec - totalElapsed);
        }
        updateTimerControlsUI();
        updateTimerDisplay();
        requestWakeLock();
        if (timerWorker) timerWorker.postMessage('start');
        if (timerInterval) clearInterval(timerInterval);
        timerInterval = setInterval(tickTimer, 200);
        return true;
      }
    }
  } catch (e) {
    console.warn('Failed to restore active session:', e);
    clearActiveSessionState();
  }
  return false;
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
  saveActiveSessionState();

  // Minute-by-Minute Auto-Save (Saves locally & syncs to cloud every 60s for logged-in user)
  const currentMinute = Math.floor(totalElapsedSec / 60);
  if (currentMinute > lastAutoSavedMinute && currentMinute > 0) {
    lastAutoSavedMinute = currentMinute;
    if (currentMode !== 'break') {
      const todayKey = getLocalDateStr();
      appState.dailyFocusTotals[todayKey] = (appState.dailyFocusTotals[todayKey] || 0) + 60;

      const subId = appState.selectedSubject?.id || 'general';
      appState.subjectDurations[subId] = (appState.subjectDurations[subId] || 0) + 60;

      if (!appState.dailySubjectDurations[todayKey]) {
        appState.dailySubjectDurations[todayKey] = {};
      }
      appState.dailySubjectDurations[todayKey][subId] = (appState.dailySubjectDurations[todayKey][subId] || 0) + 60;

      saveLocalState();

      // If user is logged in, auto-save to cloud
      if (appState.currentUser && supabaseClient && navigator.onLine) {
        pushDataToCloud(true);
        syncLeaderboardScore();

        const syncStatusPill = document.getElementById('syncStatusPill');
        const syncStatusText = document.getElementById('syncStatusText');
        if (syncStatusPill && syncStatusText) {
          syncStatusPill.classList.add('auto-saved');
          const originalText = syncStatusText.textContent;
          syncStatusText.textContent = `Auto-saved (${currentMinute}m)`;
          setTimeout(() => {
            syncStatusPill.classList.remove('auto-saved');
            if (syncStatusText.textContent.startsWith('Auto-saved')) {
              syncStatusText.textContent = originalText || 'Synced';
            }
          }, 2500);
        }
      }
    }
  }
}

function startTimer() {
  timerStatus = 'RUNNING';
  timerStartTimestamp = Date.now();
  lastAutoSavedMinute = Math.floor(accumulatedElapsedSec / 60);

  updateTimerControlsUI();
  requestWakeLock();
  saveActiveSessionState();

  if (currentMode !== 'break') {
    startPresenceHeartbeat();
  }

  if (timerWorker) {
    timerWorker.postMessage('start');
  }

  if (timerInterval) clearInterval(timerInterval);
  timerInterval = setInterval(tickTimer, 200);
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
  saveActiveSessionState();
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
  clearActiveSessionState();
  showToast('Timer reset', 'info');
}

function resetTimer() {
  stopPresenceHeartbeat();
  stopInterval();
  timerStatus = 'IDLE';
  timerStartTimestamp = null;
  accumulatedElapsedSec = 0;
  stopwatchElapsed = 0;
  lastAutoSavedMinute = 0;
  timeRemaining = getModeDurationSec();
  clearActiveSessionState();

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

  // Sound chime & trigger system notification
  playAlarmChime();
  if (stateKey === 'BREAK') {
    sendSessionNotification('Break Complete! ⚡', 'Ready for your next focus study session.');
  } else {
    const minCount = Math.max(1, Math.round(studiedDurationSec / 60));
    sendSessionNotification(`Focus Complete! 🎉 (+${minCount}m)`, `Great job studying ${subject?.name || 'Subject'}. Time for a break!`);
  }

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

  clearActiveSessionState();
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

  // Push to cloud database whenever session is at least 10s and not a break
  if (stateKey !== 'BREAK' && studiedDurationSec >= 10) {
    pushDataToCloud();
  }

  triggerSaveSuccessFeedback();

  if (stateKey === 'BREAK') {
    showToast('Break finished! Ready to focus.', 'info');

    // Automation: Auto-switch back to Pomodoro Focus after break
    if (timerConfig.pomoAutoSwitchFocus !== false) {
      isLongBreakActive = false;
      setTimeout(() => {
        switchMode('pomodoro');
        const totalCycles = timerConfig.pomoTotalCycles || 4;
        showToast(`Ready for Pomodoro Focus (Cycle ${pomoCurrentCycle}/${totalCycles})`, 'info');
      }, 500);
    }
  } else {
    const minStr = Math.max(1, Math.round(studiedDurationSec / 60));
    if (studiedDurationSec >= 60) {
      showToast(`Focus session saved! +${minStr}m added to ${subject.name}`, 'success');
    } else {
      showToast(`Focus session saved! +${studiedDurationSec}s added to ${subject.name}`, 'success');
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
  const todayStr = getLocalDateStr();
  if (appState.lastStudyDate !== todayStr) {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = getLocalDateStr(yesterday);

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

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

async function triggerDeleteSubject(sub) {
  if (timerStatus === 'RUNNING' || currentMode === 'break') {
    showToast('Cannot delete subjects while timer is running', 'warning');
    return;
  }
  if (!sub || sub.id === 'general') {
    showToast('General subject cannot be deleted', 'info');
    return;
  }

  // Close open dropdown menus
  document.getElementById('subjectDropdownMenu')?.classList.add('hidden');
  document.getElementById('zenSubjectDropdownMenu')?.classList.add('hidden');

  const confirmed = await showCustomConfirmDialog({
    title: `Delete '${sub.name}' Subject?`,
    subtitle: 'Subject deletion',
    message: `Are you sure you want to remove ${sub.iconEmoji || '📚'} ${sub.name}? Existing recorded stats for this subject will remain saved.`,
    confirmText: 'Delete Subject',
    cancelText: 'Cancel',
    isDanger: true
  });

  if (!confirmed) return;

  // 1. Remove subject from state
  appState.subjects = (appState.subjects || []).filter(s => s.id !== sub.id);
  if (appState.subjects.length === 0) {
    appState.subjects = [...DEFAULT_SUBJECTS];
  }

  // 2. If the deleted subject was currently selected, reset to General or first available
  if (appState.selectedSubject?.id === sub.id) {
    appState.selectedSubject = appState.subjects[0];
  }

  // 3. Save locally and sync to cloud
  saveLocalState();
  renderSubjects();
  updateSelectedSubjectUI();
  renderSubjectDonutChart();
  pushDataToCloud();

  showToast(`Subject "${sub.name}" deleted.`, 'success');
}

function renderSubjects() {
  const mainContainer = document.getElementById('subjectMenuItems');
  const zenContainer = document.getElementById('zenSubjectMenuItems');

  if (!appState.selectedSubject && appState.subjects.length > 0) {
    appState.selectedSubject = appState.subjects[0];
  }

  function populateList(container) {
    if (!container) return;
    container.innerHTML = '';

    appState.subjects.forEach(sub => {
      const isSelected = appState.selectedSubject && sub.id === appState.selectedSubject.id;
      const isDefaultGeneral = sub.id === 'general';
      const item = document.createElement('button');
      item.type = 'button';
      item.className = `subject-menu-item ${isSelected ? 'active' : ''}`;
      item.dataset.subjectId = sub.id;
      item.innerHTML = `
        <div class="subject-item-left">
          <span class="subject-menu-dot" style="background-color: ${sub.color || sub.colorHex || '#6366f1'};"></span>
          <span class="subject-item-title">${escapeHtml(sub.name)}</span>
        </div>
        <div class="subject-item-right">
          ${!isDefaultGeneral ? `
            <span class="subject-item-delete-btn" title="Delete Subject" data-action="delete" aria-label="Delete subject">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
            </span>
          ` : ''}
          ${isSelected ? '<svg class="subject-check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>' : ''}
        </div>
      `;

      // Hold to delete state machine (mobile long-press + desktop hold)
      let holdTimer = null;
      let isHoldTriggered = false;

      const startHold = (e) => {
        if (e.target.closest('.subject-item-delete-btn')) return;
        isHoldTriggered = false;
        item.classList.add('holding');
        holdTimer = setTimeout(() => {
          isHoldTriggered = true;
          item.classList.remove('holding');
          triggerDeleteSubject(sub);
        }, 550);
      };

      const cancelHold = () => {
        if (holdTimer) {
          clearTimeout(holdTimer);
          holdTimer = null;
        }
        item.classList.remove('holding');
      };

      item.addEventListener('pointerdown', startHold);
      item.addEventListener('pointerup', cancelHold);
      item.addEventListener('pointerleave', cancelHold);
      item.addEventListener('pointercancel', cancelHold);

      // Desktop right-click contextmenu support
      item.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        cancelHold();
        triggerDeleteSubject(sub);
      });

      // Click handler
      item.addEventListener('click', (e) => {
        const deleteBtn = e.target.closest('.subject-item-delete-btn');
        if (deleteBtn) {
          e.stopPropagation();
          cancelHold();
          triggerDeleteSubject(sub);
          return;
        }

        if (isHoldTriggered) {
          isHoldTriggered = false;
          return;
        }

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

    // Helper footer hint
    const hint = document.createElement('div');
    hint.className = 'subject-dropdown-hint';
    hint.textContent = '💡 Press & hold or right-click any subject to delete';
    container.appendChild(hint);
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
// ========================================================
// 1. OVERVIEW TODAY SUBJECT DONUT CHART (Clean Original Design)
// ========================================================
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

  const todayStr = getLocalDateStr();
  const subjectTotals = getSubjectDistributionForDate(todayStr);

  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) {
      totalSec += item.durationSec;
    }
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (totalBadge) totalBadge.textContent = `${formattedTotal} total`;
  if (centerSub) centerSub.textContent = 'Today';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  activeHighlightedSubjectId = null;

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    // Render dashed placeholder circle
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state">
        <span>No study sessions recorded today yet.</span>
      </div>
    `;
    return;
  }

  // Chart Geometry (Clean Donut Ring)
  const radius = 70;
  const strokeW = 22;
  const circumference = 2 * Math.PI * radius;
  let accumulatedPercent = 0;

  // Background track
  const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
  bgTrack.setAttribute('cx', '100');
  bgTrack.setAttribute('cy', '100');
  bgTrack.setAttribute('r', radius.toString());
  bgTrack.setAttribute('class', 'donut-bg-track');
  bgTrack.setAttribute('stroke-width', strokeW.toString());
  svg.appendChild(bgTrack);

  entries.sort((a, b) => b.durationSec - a.durationSec);

  function highlightSubject(item) {
    activeHighlightedSubjectId = item.id;
    const itemPct = Math.round((item.durationSec / totalSec) * 100);
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
        s.style.strokeWidth = (strokeW + 5).toString();
      } else {
        s.classList.remove('active');
        s.style.opacity = '0.35';
        s.style.strokeWidth = strokeW.toString();
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
      s.style.strokeWidth = strokeW.toString();
    });
    legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
  }

  svgWrap?.addEventListener('mouseleave', () => {
    if (activeHighlightedSubjectId) resetHighlight();
  });

  entries.forEach(item => {
    const itemPct = (item.durationSec / totalSec) * 100;
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
    circle.setAttribute('stroke-width', strokeW.toString());
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

// ========================================================
// 2. HISTORICAL PAST DAY SUBJECT DISTRIBUTION & PIE CHART
// (Matches Android CalendarTimeline.kt & Day Details Modal)
// ========================================================
function getSubjectDistributionForDate(dateStr) {
  const subjectTotals = {};
  const todayStr = getLocalDateStr();
  const allSessions = getAllValidatedSessions();

  // 1. Sessions for this specific date
  allSessions.forEach(s => {
    if (!s || !s.durationSec || s.durationSec <= 0) return;
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
    if (dStr === dateStr) {
      const subId = s.subject?.id || s.subject?.name || 'general';
      if (!subjectTotals[subId]) {
        const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
          id: subId,
          name: s.subject?.name || 'Focus Study',
          color: s.subject?.color || '#3b82f6'
        };
        subjectTotals[subId] = {
          id: subId,
          name: matching.name,
          color: matching.color || matching.colorHex || '#3b82f6',
          durationSec: 0
        };
      }
      subjectTotals[subId].durationSec += s.durationSec;
    }
  });

  // 2. If it is today and todaySessions has data, merge
  if (dateStr === todayStr && appState.todaySessions && appState.todaySessions.length > 0) {
    appState.todaySessions.forEach(s => {
      if (s && s.durationSec > 0) {
        const subId = s.subject?.id || s.subject?.name || 'general';
        if (!subjectTotals[subId]) {
          const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
            id: subId,
            name: s.subject?.name || 'Focus Study',
            color: s.subject?.color || '#3b82f6'
          };
          subjectTotals[subId] = {
            id: subId,
            name: matching.name,
            color: matching.color || matching.colorHex || '#3b82f6',
            durationSec: 0
          };
        }
        if (!allSessions.some(as => as.id === s.id)) {
          subjectTotals[subId].durationSec += s.durationSec;
        }
      }
    });
  }

  // 3. Check dailySubjectDurations for this date from Android Cloud sync
  if (appState.dailySubjectDurations && appState.dailySubjectDurations[dateStr]) {
    const dayBreakdown = appState.dailySubjectDurations[dateStr];
    if (dayBreakdown && typeof dayBreakdown === 'object') {
      Object.keys(dayBreakdown).forEach(subId => {
        const sec = Number(dayBreakdown[subId]) || 0;
        if (sec > 0) {
          if (!subjectTotals[subId]) {
            const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
              id: subId,
              name: subId.startsWith('custom_') ? 'Subject' : subId.charAt(0).toUpperCase() + subId.slice(1),
              color: '#3b82f6'
            };
            subjectTotals[subId] = {
              id: subId,
              name: matching.name,
              color: matching.color || matching.colorHex || '#3b82f6',
              durationSec: 0
            };
          }
          subjectTotals[subId].durationSec = Math.max(subjectTotals[subId].durationSec, sec);
        }
      });
    }
  }

  // Harmonize names & colors with active user registered subjects
  Object.values(subjectTotals).forEach(item => {
    const matching = (appState.subjects || []).find(sub => sub.id === item.id || sub.name?.toLowerCase() === item.name?.toLowerCase());
    if (matching) {
      item.name = matching.name;
      item.color = matching.color || matching.colorHex || item.color;
    }
  });

  return subjectTotals;
}

function renderCalendarDayPieChart(dateStr) {
  const svg = document.getElementById('calDaySubjectDonutSvg');
  const svgWrap = document.getElementById('calDayDonutSvgWrap');
  const totalBadge = document.getElementById('calDayPieTotalBadge');
  const title = document.getElementById('calDayPieTitle');
  const centerSub = document.getElementById('calDayDonutCenterSub');
  const centerVal = document.getElementById('calDayDonutCenterVal');
  const centerPct = document.getElementById('calDayDonutCenterPct');
  const legendList = document.getElementById('calDayDonutLegendList');
  if (!svg || !legendList) return;

  const todayStr = getLocalDateStr();
  const isToday = dateStr === todayStr;
  const formattedDate = new Date(dateStr + 'T00:00:00').toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? "Today's Subject Breakdown" : `Subject Breakdown (${formattedDate})`;
  }

  const subjectTotals = getSubjectDistributionForDate(dateStr);
  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) totalSec += item.durationSec;
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (totalBadge) totalBadge.textContent = `${formattedTotal} total`;
  if (centerSub) centerSub.textContent = 'Studied';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state">
        <span>No study sessions recorded on ${formattedDate}.</span>
      </div>
    `;
    return;
  }

  const radius = 70;
  const strokeW = 22;
  const circumference = 2 * Math.PI * radius;
  let accumulatedPercent = 0;

  const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
  bgTrack.setAttribute('cx', '100');
  bgTrack.setAttribute('cy', '100');
  bgTrack.setAttribute('r', radius.toString());
  bgTrack.setAttribute('class', 'donut-bg-track');
  bgTrack.setAttribute('stroke-width', strokeW.toString());
  svg.appendChild(bgTrack);

  entries.sort((a, b) => b.durationSec - a.durationSec);

  function highlightDaySubject(item) {
    const itemPct = Math.round((item.durationSec / totalSec) * 100);
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    if (centerSub) centerSub.textContent = item.name;
    if (centerVal) centerVal.textContent = itemTimeStr;
    if (centerPct) {
      centerPct.textContent = `${itemPct}% of day`;
      centerPct.classList.remove('hidden');
    }

    svg.querySelectorAll('.donut-slice').forEach(s => {
      if (s.getAttribute('data-sub-id') === item.id) {
        s.classList.add('active');
        s.style.opacity = '1';
        s.style.strokeWidth = (strokeW + 5).toString();
      } else {
        s.classList.remove('active');
        s.style.opacity = '0.35';
        s.style.strokeWidth = strokeW.toString();
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

  function resetDayHighlight() {
    if (centerSub) centerSub.textContent = 'Studied';
    if (centerVal) centerVal.textContent = formattedTotal;
    if (centerPct) centerPct.classList.add('hidden');

    svg.querySelectorAll('.donut-slice').forEach(s => {
      s.classList.remove('active');
      s.style.opacity = '1';
      s.style.strokeWidth = strokeW.toString();
    });
    legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
  }

  svgWrap?.addEventListener('mouseleave', resetDayHighlight);

  entries.forEach(item => {
    const itemPct = (item.durationSec / totalSec) * 100;
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
    circle.setAttribute('stroke-width', strokeW.toString());
    circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
    circle.setAttribute('stroke-dashoffset', offset.toString());
    circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

    circle.addEventListener('mouseenter', () => highlightDaySubject(item));
    circle.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightDaySubject(item);
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

    legendItem.addEventListener('mouseenter', () => highlightDaySubject(item));
    legendItem.addEventListener('mouseleave', resetDayHighlight);
    legendItem.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightDaySubject(item);
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
      const dateKey = getLocalDateStr(d);
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

    const dateKey = getLocalDateStr(curDate);
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
let selectedCalendarDateStr = getLocalDateStr();

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

  const todayStr = getLocalDateStr();

  const dateSessionsMap = {};
  const dateDurationMap = {};

  const allSessions = getAllValidatedSessions();
  allSessions.forEach(s => {
    if (s && s.durationSec > 0) {
      const d = new Date(s.timestamp || s.startTime);
      const dateKey = getLocalDateStr(d);
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

  renderCalendarDayPieChart(selectedCalendarDateStr);
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

  const todayStr = getLocalDateStr();
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
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
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
  if (!container) return;

  const goals = appState.plannerGoals || [];

  // Update Planner Quick Stats Bar
  const totalGoalsEl = document.getElementById('plannerTotalGoalsCount');
  const completedGoalsEl = document.getElementById('plannerCompletedGoalsCount');
  const totalPlannedTimeEl = document.getElementById('plannerTotalPlannedTime');

  let totalPlannedMins = 0;
  let completedCount = 0;

  goals.forEach(g => {
    totalPlannedMins += (g.targetMinutes || g.dailyMinutes || 0);
    if (g.completed) completedCount++;
  });

  if (totalGoalsEl) totalGoalsEl.textContent = String(goals.length);
  if (completedGoalsEl) completedGoalsEl.textContent = String(completedCount);
  if (totalPlannedTimeEl) {
    const h = Math.floor(totalPlannedMins / 60);
    const m = totalPlannedMins % 60;
    totalPlannedTimeEl.textContent = h > 0 ? `${h}h${m > 0 ? ` ${m}m` : ''}` : `${m}m`;
  }

  if (goals.length === 0) {
    container.innerHTML = `
      <div class="planner-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <p>No study goals set for today yet.</p>
        <span>Click <strong>+ New Goal</strong> above to set tasks, time targets &amp; daily habits!</span>
      </div>
    `;
    return;
  }

  // Calculate today's studied seconds per subject
  const studiedSecMap = {};
  (appState.todaySessions || []).forEach(s => {
    if (s && s.subject && typeof s.durationSec === 'number' && s.durationSec > 0) {
      const subId = s.subject.id || s.subject.name;
      studiedSecMap[subId] = (studiedSecMap[subId] || 0) + s.durationSec;
    }
  });

  container.innerHTML = '';
  goals.forEach(goal => {
    const subId = goal.subjectId;
    const subject = (appState.subjects || []).find(s => s.id === subId) || {
      id: 'all',
      name: 'All Subjects',
      color: '#3b82f6',
      iconEmoji: '📚'
    };

    const targetMin = Math.max(0, Number(goal.targetMinutes ?? goal.dailyMinutes) || 0);
    const targetSec = targetMin * 60;
    const studiedSec = subId ? (studiedSecMap[subId] || 0) : 0;
    const studiedMin = Math.round(studiedSec / 60);

    const isTimeGoal = targetMin > 0;
    const percent = isTimeGoal ? Math.min(100, Math.round((studiedSec / targetSec) * 100)) : (goal.completed ? 100 : 0);
    const isCompleted = goal.completed || (isTimeGoal && studiedSec >= targetSec);

    let progressChipText = '';
    if (isTimeGoal) {
      progressChipText = `${studiedMin}m / ${targetMin}m`;
    } else {
      progressChipText = 'Daily Habit';
    }

    const card = document.createElement('div');
    card.className = `planner-goal-card ${goal.completed ? 'is-completed' : ''}`;
    card.setAttribute('data-goal-id', goal.id);

    card.innerHTML = `
      <div class="goal-card-main-row">
        <!-- Animated Circle Checkbox -->
        <button class="goal-checkbox-btn ${goal.completed ? 'checked' : ''}" data-action="toggle-check" title="${goal.completed ? 'Mark as incomplete' : 'Mark as complete'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><polyline points="20 6 9 17 4 12"></polyline></svg>
        </button>

        <!-- Goal Content Column -->
        <div class="goal-content-col">
          <div class="goal-title-wrap">
            <h4 class="goal-title ${goal.completed ? 'is-completed' : ''}">${escapeHtml(goal.title || (subject ? `${subject.name} Goal` : 'Study Goal'))}</h4>
          </div>
          ${goal.note ? `<p class="goal-note">${escapeHtml(goal.note)}</p>` : ''}
          <div class="goal-badges-row">
            <span class="goal-subject-chip" style="background-color: rgba(59, 130, 246, 0.08); color: ${subject.color || '#3b82f6'}; border-color: ${subject.color ? subject.color + '40' : 'rgba(59, 130, 246, 0.2)'};">
              <span class="subject-color-dot" style="background-color: ${subject.color || '#3b82f6'}; width: 7px; height: 7px;"></span>
              <span>${escapeHtml(subject.name)}</span>
            </span>
            <span class="goal-progress-chip ${isCompleted ? 'completed' : ''}">${isCompleted ? '✓ Done' : progressChipText}</span>
          </div>
        </div>

        <!-- Action Buttons (Edit & Delete) -->
        <div class="goal-card-actions">
          <button class="goal-btn-action btn-edit" data-action="edit-goal" title="Edit Goal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
          <button class="goal-btn-action btn-delete" data-action="delete-goal" title="Remove Goal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>

      ${isTimeGoal ? `
        <div class="goal-progress-track">
          <div class="goal-progress-bar" style="width: ${percent}%; background: linear-gradient(90deg, ${subject.color || '#3b82f6'}, #10b981);"></div>
        </div>
      ` : ''}
    `;

    // Event Handlers for the card
    card.querySelector('[data-action="toggle-check"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleGoalCompleted(goal.id);
    });

    card.querySelector('[data-action="edit-goal"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      openEditGoalModal(goal.id);
    });

    card.querySelector('[data-action="delete-goal"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      openDeleteGoalModal(goal.id);
    });

    container.appendChild(card);
  });
}

function toggleGoalCompleted(goalId) {
  const goal = (appState.plannerGoals || []).find(g => g.id === goalId);
  if (!goal) return;

  goal.completed = !goal.completed;
  goal.checkedAt = goal.completed ? Date.now() : 0;

  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast(goal.completed ? `Completed: "${goal.title}"` : `Marked incomplete: "${goal.title}"`, 'info');
}

function openAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  const titleEl = document.getElementById('plannerGoalModalTitle');
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const minutesInput = document.getElementById('goalMinutesInput');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const btnSave = document.getElementById('btnSaveGoalModal');

  if (titleEl) titleEl.textContent = 'Add Study Goal';
  if (btnSave) btnSave.querySelector('span').textContent = 'Save Goal';
  if (idInput) idInput.value = '';
  if (titleInput) titleInput.value = '';
  if (noteInput) noteInput.value = '';
  if (minutesInput) minutesInput.value = '60';

  if (subjectSelect) {
    subjectSelect.innerHTML = `
      <option value="all">All Subjects / General</option>
      ${(appState.subjects || []).map(s => `<option value="${s.id}" ${s.id === appState.selectedSubject?.id ? 'selected' : ''}>${s.name}</option>`).join('')}
    `;
  }

  // Set 60m active in presets
  document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === '60');
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    setTimeout(() => titleInput?.focus(), 50);
  }
}

function openEditGoalModal(goalId) {
  const goal = (appState.plannerGoals || []).find(g => g.id === goalId);
  if (!goal) return;

  const modal = document.getElementById('plannerGoalModalOverlay');
  const titleEl = document.getElementById('plannerGoalModalTitle');
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const minutesInput = document.getElementById('goalMinutesInput');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const btnSave = document.getElementById('btnSaveGoalModal');

  if (titleEl) titleEl.textContent = 'Edit Study Goal';
  if (btnSave) btnSave.querySelector('span').textContent = 'Update Goal';
  if (idInput) idInput.value = goal.id;
  if (titleInput) titleInput.value = goal.title || '';
  if (noteInput) noteInput.value = goal.note || '';
  
  const targetMins = Number(goal.targetMinutes ?? goal.dailyMinutes) || 0;
  if (minutesInput) minutesInput.value = String(targetMins);

  if (subjectSelect) {
    subjectSelect.innerHTML = `
      <option value="all" ${!goal.subjectId || goal.subjectId === 'all' ? 'selected' : ''}>All Subjects / General</option>
      ${(appState.subjects || []).map(s => `<option value="${s.id}" ${s.id === goal.subjectId ? 'selected' : ''}>${s.name}</option>`).join('')}
    `;
  }

  // Highlight matching preset if any
  document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === String(targetMins));
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    setTimeout(() => titleInput?.focus(), 50);
  }
}

function closeAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function handleSaveGoal(e) {
  e.preventDefault();
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const minutesInput = document.getElementById('goalMinutesInput');

  const editingId = idInput?.value?.trim();
  const title = titleInput?.value?.trim() || 'Daily Goal';
  const note = noteInput?.value?.trim() || '';
  const subjectId = (subjectSelect?.value && subjectSelect.value !== 'all') ? subjectSelect.value : null;
  const targetMinutes = Math.min(1440, Math.max(0, parseInt(minutesInput?.value, 10) || 0));

  if (!appState.plannerGoals) appState.plannerGoals = [];

  if (editingId) {
    const existing = appState.plannerGoals.find(g => g.id === editingId);
    if (existing) {
      existing.title = title;
      existing.note = note;
      existing.subjectId = subjectId;
      existing.targetMinutes = targetMinutes;
      existing.dailyMinutes = targetMinutes;
    }
  } else {
    appState.plannerGoals.push({
      id: 'goal_' + Date.now(),
      title,
      note,
      subjectId,
      targetMinutes,
      dailyMinutes: targetMinutes,
      completed: false,
      checkedAt: 0,
      createdAt: Date.now()
    });
  }

  closeAddGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast(editingId ? 'Goal updated & synced with app!' : 'New study goal created & synced!', 'success');
}

function openDeleteGoalModal(goalId) {
  pendingGoalIdToDelete = goalId;
  const goal = appState.plannerGoals?.find(g => g.id === goalId);
  const subtitle = document.getElementById('deleteGoalModalSubtitle');
  if (subtitle) {
    subtitle.textContent = `Are you sure you want to remove "${goal ? (goal.title || 'this goal') : 'this goal'}"?`;
  }
  lockBodyScroll();
  document.getElementById('deleteGoalModalOverlay')?.classList.remove('hidden');
}

function closeDeleteGoalModal() {
  pendingGoalIdToDelete = null;
  document.getElementById('deleteGoalModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
}

function confirmDeletePlannerGoal() {
  if (!pendingGoalIdToDelete || !appState.plannerGoals) return;
  appState.plannerGoals = appState.plannerGoals.filter(g => g.id !== pendingGoalIdToDelete);
  closeDeleteGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast('Study goal removed and synced.', 'info');
}

// ----------------------------------------------------------------------------
// EDIT OVERALL DAILY FOCUS GOAL MODAL
// ----------------------------------------------------------------------------

function openEditDailyGoalModal() {
  const modal = document.getElementById('dailyGoalModalOverlay');
  const input = document.getElementById('inputDailyGoalMinutes');
  const currentMins = timerConfig.dailyGoalMinutes || 120;

  if (input) input.value = String(currentMins);

  document.querySelectorAll('#dailyTargetPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === String(currentMins));
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeDailyGoalModal() {
  const modal = document.getElementById('dailyGoalModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function handleSaveDailyGoal(e) {
  e.preventDefault();
  const input = document.getElementById('inputDailyGoalMinutes');
  const mins = Math.min(1440, Math.max(15, parseInt(input?.value, 10) || 120));

  timerConfig.dailyGoalMinutes = mins;
  closeDailyGoalModal();
  updateProgressAndStreak();
  saveLocalState();
  pushDataToCloud();
  showToast(`Daily study target updated to ${mins >= 60 ? `${Math.floor(mins / 60)}h ${mins % 60 > 0 ? (mins % 60) + 'm' : ''}` : `${mins}m`}!`, 'success');
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
  lockBodyScroll();
  modal.classList.remove('hidden');
}

function closeProfileModal() {
  document.getElementById('profileModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
}

function updateProfileLivePreview() {
  const nameInput = document.getElementById('inputProfileDisplayName');
  const mottoInput = document.getElementById('inputProfileMotto');
  const previewAvatarIcon = document.getElementById('previewAvatarIcon');
  const previewDisplayName = document.getElementById('previewDisplayName');
  const previewMottoText = document.getElementById('previewMottoText');

  const nameVal = (nameInput?.value.trim() || 'Student').slice(0, 24);
  const mottoVal = (mottoInput?.value.trim() || 'Deep focus & daily consistency').slice(0, 60);

  if (previewAvatarIcon) previewAvatarIcon.textContent = selectedAvatarPreset;
  if (previewDisplayName) previewDisplayName.textContent = nameVal;
  if (previewMottoText) previewMottoText.textContent = mottoVal;
}

async function handleSaveProfile(e) {
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
  closeProfileModal();

  showToast('Profile customizations saved!', 'success');

  // Immediately sync to Leaderboard and Cloud
  await syncLeaderboardScore();

  if (timerStatus === 'RUNNING' && currentMode !== 'break') {
    updateStudyPresence(true);
  }

  pushDataToCloud(true);

  leaderboardCache.timestamp = 0;
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }
}

// ============================================================================
// DAY-BY-DAY SUBJECT PIE CHART & TIMELINE HISTORY MODAL
// (Matches Android showPieChartDetailsModal in MainActivity.kt)
// ============================================================================
let activeHistoryModalDateStr = getLocalDateStr();

function openDayPieHistoryModal(dateStr) {
  const modal = document.getElementById('dayPieHistoryModalOverlay');
  if (!modal) return;
  activeHistoryModalDateStr = dateStr || selectedCalendarDateStr || getLocalDateStr();
  lockBodyScroll();
  modal.classList.remove('hidden');
  renderDayPieHistoryModal(activeHistoryModalDateStr);
}

function closeDayPieHistoryModal() {
  const modal = document.getElementById('dayPieHistoryModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function changeHistoryModalDay(delta) {
  const current = new Date(activeHistoryModalDateStr + 'T00:00:00');
  current.setDate(current.getDate() + delta);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Do not allow navigating into the future
  if (current.getTime() > today.getTime()) return;

  const y = current.getFullYear();
  const m = String(current.getMonth() + 1).padStart(2, '0');
  const d = String(current.getDate()).padStart(2, '0');
  activeHistoryModalDateStr = `${y}-${m}-${d}`;
  renderDayPieHistoryModal(activeHistoryModalDateStr);
}

function renderDayPieHistoryModal(dateStr) {
  const title = document.getElementById('historyModalDateTitle');
  const nextBtn = document.getElementById('btnHistoryNextDay');
  const svg = document.getElementById('historySubjectDonutSvg');
  const svgWrap = document.getElementById('historyDonutSvgWrap');
  const centerSub = document.getElementById('historyDonutCenterSub');
  const centerVal = document.getElementById('historyDonutCenterVal');
  const centerPct = document.getElementById('historyDonutCenterPct');
  const legendList = document.getElementById('historyDonutLegendList');
  const sessionsTitle = document.getElementById('historySessionsSectionTitle');
  const sessionsCountBadge = document.getElementById('historySessionsCountBadge');
  const timelineList = document.getElementById('historyTimelineList');

  if (!svg || !legendList || !timelineList) return;

  const todayStr = getLocalDateStr();
  const isToday = dateStr === todayStr;

  const parsedDate = new Date(dateStr + 'T00:00:00');
  const formattedDisplayDate = parsedDate.toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? `Today (${formattedDisplayDate})` : `${formattedDisplayDate}`;
  }

  if (nextBtn) {
    nextBtn.disabled = isToday;
  }

  if (sessionsTitle) {
    sessionsTitle.textContent = isToday ? "Today's Sessions" : `Sessions (${formattedDisplayDate})`;
  }

  // 1. Render Pie Chart
  const subjectTotals = getSubjectDistributionForDate(dateStr);
  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) totalSec += item.durationSec;
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (centerSub) centerSub.textContent = 'Studied';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state" style="padding: 16px 8px;">
        <span>No study sessions recorded for ${formattedDisplayDate}.</span>
      </div>
    `;
  } else {
    const radius = 70;
    const strokeW = 22;
    const circumference = 2 * Math.PI * radius;
    let accumulatedPercent = 0;

    const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    bgTrack.setAttribute('cx', '100');
    bgTrack.setAttribute('cy', '100');
    bgTrack.setAttribute('r', radius.toString());
    bgTrack.setAttribute('class', 'donut-bg-track');
    bgTrack.setAttribute('stroke-width', strokeW.toString());
    svg.appendChild(bgTrack);

    entries.sort((a, b) => b.durationSec - a.durationSec);

    function highlightModalSubject(item) {
      const itemPct = Math.round((item.durationSec / totalSec) * 100);
      const itemMin = Math.round(item.durationSec / 60);
      const itemTimeStr = itemMin >= 60 
        ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
        : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

      if (centerSub) centerSub.textContent = item.name;
      if (centerVal) centerVal.textContent = itemTimeStr;
      if (centerPct) {
        centerPct.textContent = `${itemPct}% of day`;
        centerPct.classList.remove('hidden');
      }

      svg.querySelectorAll('.donut-slice').forEach(s => {
        if (s.getAttribute('data-sub-id') === item.id) {
          s.classList.add('active');
          s.style.opacity = '1';
          s.style.strokeWidth = (strokeW + 5).toString();
        } else {
          s.classList.remove('active');
          s.style.opacity = '0.35';
          s.style.strokeWidth = strokeW.toString();
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

    function resetModalHighlight() {
      if (centerSub) centerSub.textContent = 'Studied';
      if (centerVal) centerVal.textContent = formattedTotal;
      if (centerPct) centerPct.classList.add('hidden');

      svg.querySelectorAll('.donut-slice').forEach(s => {
        s.classList.remove('active');
        s.style.opacity = '1';
        s.style.strokeWidth = strokeW.toString();
      });
      legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
    }

    svgWrap?.addEventListener('mouseleave', resetModalHighlight);

    entries.forEach(item => {
      const itemPct = (item.durationSec / totalSec) * 100;
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
      circle.setAttribute('stroke-width', strokeW.toString());
      circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
      circle.setAttribute('stroke-dashoffset', offset.toString());
      circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

      circle.addEventListener('mouseenter', () => highlightModalSubject(item));
      circle.addEventListener('click', (e) => {
        e.stopPropagation();
        highlightModalSubject(item);
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

      legendItem.addEventListener('mouseenter', () => highlightModalSubject(item));
      legendItem.addEventListener('mouseleave', resetModalHighlight);
      legendItem.addEventListener('click', (e) => {
        e.stopPropagation();
        highlightModalSubject(item);
      });

      legendList.appendChild(legendItem);
    });
  }

  // 2. Render Timeline Sessions for this Date
  const allSessions = getAllValidatedSessions();
  const dateSessions = allSessions.filter(s => {
    if (!s || !s.durationSec) return false;
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
    return dStr === dateStr;
  });

  if (sessionsCountBadge) {
    sessionsCountBadge.textContent = `${dateSessions.length} session${dateSessions.length === 1 ? '' : 's'}`;
  }

  if (dateSessions.length === 0) {
    timelineList.innerHTML = `
      <div class="timeline-empty-state" style="padding: 20px 10px;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p>No study sessions logged on this date.</p>
      </div>
    `;
    return;
  }

  timelineList.innerHTML = '';
  dateSessions.forEach(sess => {
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
          <div class="timeline-meta-text">${startTimeFormatted} - ${endTimeFormatted}</div>
        </div>
      </div>
      <div class="timeline-item-right">
        <span class="timeline-duration-badge">${mins}m</span>
      </div>
    `;
    timelineList.appendChild(item);
  });
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

async function updateStudyPresence(isStudying = false) {
  if (!supabaseClient || !appState.currentUser) return;

  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) {
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
      p_subject_color: shouldBeStudying ? (currentSub.color || '#3b82f6') : '#3b82f6',
      p_study_date: getLocalDateStr()
    });
  } catch (err) {
    console.warn('Presence update error:', err);
  }
}

async function syncLeaderboardScore() {
  if (!supabaseClient || !appState.currentUser) return;
  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) return;

  const todayKey = getLocalDateStr();
  let totalSecToday = 0;
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      totalSecToday += s.durationSec;
    }
  });

  if (appState.dailyFocusTotals && appState.dailyFocusTotals[todayKey]) {
    totalSecToday = Math.max(totalSecToday, Number(appState.dailyFocusTotals[todayKey]) || 0);
  }

  const isStudyingNow = timerStatus === 'RUNNING' && currentMode !== 'break';
  const currentSub = appState.selectedSubject || { name: 'Focus Study', color: '#3b82f6' };
  const userName = (profile.displayName || 
                    appState.currentUser.user_metadata?.full_name || 
                    appState.currentUser.user_metadata?.name || 
                    appState.currentUser.email?.split('@')[0] || 
                    'Student').slice(0, 100);
  const avatarUrl = profile.avatarPreset || 
                    appState.currentUser.user_metadata?.avatar_url || 
                    appState.currentUser.user_metadata?.picture || 
                    '🐱';

  const canonicalUserId = appState.currentUser?.id;
  if (!canonicalUserId) return;

  const payload = {
    user_id: canonicalUserId,
    user_name: userName,
    avatar_url: avatarUrl,
    study_date: todayKey,
    total_seconds: totalSecToday,
    is_studying: isStudyingNow,
    current_subject: isStudyingNow ? (currentSub.name || 'Focus Study') : '',
    subject_color: isStudyingNow ? (currentSub.color || '#3b82f6') : '#3b82f6',
    last_active_at: new Date().toISOString(),
    updated_at: new Date().toISOString()
  };

  try {
    await supabaseClient.from('daily_leaderboard').upsert(payload, { onConflict: 'user_id,study_date' });
  } catch (e) {
    console.warn('Leaderboard score sync error:', e);
  }

  leaderboardCache.timestamp = 0;
}

function startPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
  }
  updateStudyPresence(true);
  syncLeaderboardScore();
  // Send active heartbeat every 60 seconds while timer is actively running
  presenceHeartbeatInterval = setInterval(() => {
    if (timerStatus === 'RUNNING' && currentMode !== 'break') {
      updateStudyPresence(true);
      syncLeaderboardScore();
    }
  }, 60000);
}

function stopPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
    presenceHeartbeatInterval = null;
  }
  updateStudyPresence(false);
  syncLeaderboardScore();
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
      p_subject_color: subject?.color || '#3b82f6',
      p_study_date: getLocalDateStr()
    });
    await syncLeaderboardScore();
    const modal = document.getElementById('leaderboardModalOverlay');
    if (modal && !modal.classList.contains('hidden')) {
      fetchLeaderboard(true);
    }
  } catch (err) {
    console.warn('Leaderboard session log error:', err);
  }
}

let currentLeaderboardPeriod = 'daily'; // 'daily' | 'weekly' | 'monthly'
let leaderboardTimeframeCache = {
  daily: { data: null, timestamp: 0 },
  weekly: { data: null, timestamp: 0 },
  monthly: { data: null, timestamp: 0 }
};

function switchLeaderboardTimeframe(period) {
  if (!['daily', 'weekly', 'monthly'].includes(period)) period = 'daily';
  currentLeaderboardPeriod = period;

  // Update tabs UI
  document.querySelectorAll('.lb-timeframe-tab').forEach(btn => {
    const isTarget = btn.dataset.period === period;
    btn.classList.toggle('active', isTarget);
    btn.setAttribute('aria-selected', isTarget ? 'true' : 'false');
  });

  // Update title heading
  const heading = document.getElementById('leaderboardModalHeading');
  if (heading) {
    if (period === 'daily') heading.textContent = 'Daily Leaderboard';
    else if (period === 'weekly') heading.textContent = 'Weekly Leaderboard';
    else if (period === 'monthly') heading.textContent = 'Monthly Leaderboard';
  }

  // Update countdown timer
  updateLeaderboardCountdownDisplay();

  // Fetch rankings for the selected period
  fetchLeaderboard(false);
}

async function openLeaderboardModal() {
  const modal = document.getElementById('leaderboardModalOverlay');
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    switchLeaderboardTimeframe(currentLeaderboardPeriod || 'daily');
    await syncLeaderboardScore();
    fetchLeaderboard(true);
  }
}

function closeLeaderboardModal() {
  document.getElementById('leaderboardModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
}

// Helpers for dates
function getStartOfWeekDateStr() {
  const d = new Date();
  const day = d.getDay(); // 0 is Sunday
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Monday
  const monday = new Date(d.setDate(diff));
  return getLocalDateStr(monday);
}

function getStartOfMonthDateStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

async function fetchLeaderboard(forceRefresh = false) {
  const now = Date.now();
  const period = currentLeaderboardPeriod || 'daily';
  startResetCountdownTimer();

  if (forceRefresh) {
    await syncLeaderboardScore();
  }

  const cached = leaderboardTimeframeCache[period];
  if (!forceRefresh && cached && cached.data && (now - cached.timestamp < LEADERBOARD_CACHE_TTL_MS)) {
    renderLeaderboard(cached.data, period);
    return;
  }

  const refreshBtn = document.getElementById('btnRefreshLeaderboard');
  refreshBtn?.classList.add('spinning');

  try {
    if (supabaseClient) {
      const todayStr = getLocalDateStr();
      let rankings = null;

      if (period === 'daily') {
        const { data, error } = await supabaseClient.rpc('get_daily_leaderboard', {
          p_date: todayStr,
          p_limit: 25
        });
        if (!error && data) rankings = data;
      } else if (period === 'weekly') {
        const startWeekStr = getStartOfWeekDateStr();
        // Try RPC first
        const { data: rpcData, error: rpcErr } = await supabaseClient.rpc('get_weekly_leaderboard', {
          p_start_date: startWeekStr,
          p_end_date: todayStr,
          p_limit: 25
        });

        if (!rpcErr && rpcData) {
          rankings = rpcData;
        } else {
          // Client-side fallback aggregation from daily_leaderboard table
          const { data: tableData, error: tableErr } = await supabaseClient
            .from('daily_leaderboard')
            .select('*')
            .gte('study_date', startWeekStr)
            .lte('study_date', todayStr);

          if (!tableErr && tableData) {
            rankings = aggregateLeaderboardEntries(tableData);
          }
        }
      } else if (period === 'monthly') {
        const startMonthStr = getStartOfMonthDateStr();
        // Try RPC first
        const { data: rpcData, error: rpcErr } = await supabaseClient.rpc('get_monthly_leaderboard', {
          p_start_date: startMonthStr,
          p_end_date: todayStr,
          p_limit: 25
        });

        if (!rpcErr && rpcData) {
          rankings = rpcData;
        } else {
          // Client-side fallback aggregation from daily_leaderboard table
          const { data: tableData, error: tableErr } = await supabaseClient
            .from('daily_leaderboard')
            .select('*')
            .gte('study_date', startMonthStr)
            .lte('study_date', todayStr);

          if (!tableErr && tableData) {
            rankings = aggregateLeaderboardEntries(tableData);
          }
        }
      }

      if (rankings && Array.isArray(rankings)) {
        leaderboardTimeframeCache[period] = { data: rankings, timestamp: now };
        renderLeaderboard(rankings, period);
      } else {
        fallbackLocalLeaderboard(period);
      }
    } else {
      fallbackLocalLeaderboard(period);
    }
  } catch (err) {
    console.warn('Leaderboard fetch exception:', err);
    fallbackLocalLeaderboard(period);
  } finally {
    setTimeout(() => refreshBtn?.classList.remove('spinning'), 400);
  }
}

// Client-side aggregator for table rows
function aggregateLeaderboardEntries(rows) {
  const userMap = new Map();
  const threeMinsAgo = Date.now() - 3 * 60 * 1000;

  (rows || []).forEach(row => {
    if (!row || !row.user_id) return;
    const uid = row.user_id;
    const isRecentActive = row.last_active_at ? new Date(row.last_active_at).getTime() > threeMinsAgo : false;
    const isStudyingNow = Boolean(row.is_studying && isRecentActive);

    if (!userMap.has(uid)) {
      userMap.set(uid, {
        user_id: uid,
        user_name: row.user_name || 'Student',
        avatar_url: row.avatar_url || '🐱',
        total_seconds: Number(row.total_seconds) || 0,
        is_studying: isStudyingNow,
        current_subject: row.current_subject || '',
        subject_color: row.subject_color || '#3b82f6',
        last_active_at: row.last_active_at || new Date().toISOString()
      });
    } else {
      const existing = userMap.get(uid);
      existing.total_seconds += (Number(row.total_seconds) || 0);
      if (row.user_name && row.user_name !== 'Student') existing.user_name = row.user_name;
      if (row.avatar_url) existing.avatar_url = row.avatar_url;
      if (isStudyingNow) {
        existing.is_studying = true;
        existing.current_subject = row.current_subject || existing.current_subject;
        existing.subject_color = row.subject_color || existing.subject_color;
      }
      if (new Date(row.last_active_at) > new Date(existing.last_active_at)) {
        existing.last_active_at = row.last_active_at;
      }
    }
  });

  const sorted = Array.from(userMap.values()).sort((a, b) => b.total_seconds - a.total_seconds);
  return sorted.slice(0, 25);
}

function isCurrentUserEntry(entry) {
  if (!entry) return false;
  const currentUserId = appState.currentUser?.id;
  const currentUserEmail = appState.currentUser?.email;

  if (currentUserId && entry.user_id === currentUserId) return true;
  if (currentUserEmail && (entry.user_id === currentUserEmail || entry.user_id === currentUserEmail.split('@')[0])) return true;
  return false;
}

function calculateLocalFocusSecondsForPeriod(period) {
  const todayKey = getIsoDateStr();
  let totalSec = 0;

  if (period === 'daily') {
    (appState.todaySessions || []).forEach(s => {
      if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
    });
    if (appState.dailyFocusTotals && appState.dailyFocusTotals[todayKey]) {
      totalSec = Math.max(totalSec, Number(appState.dailyFocusTotals[todayKey]) || 0);
    }
  } else if (period === 'weekly') {
    const startWeekStr = getStartOfWeekDateStr();
    const totals = appState.dailyFocusTotals || {};
    Object.keys(totals).forEach(k => {
      if (k >= startWeekStr && k <= todayKey) {
        totalSec += Number(totals[k]) || 0;
      }
    });
    if (totalSec === 0) {
      (appState.todaySessions || []).forEach(s => {
        if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
      });
    }
  } else if (period === 'monthly') {
    const startMonthStr = getStartOfMonthDateStr();
    const totals = appState.dailyFocusTotals || {};
    Object.keys(totals).forEach(k => {
      if (k >= startMonthStr && k <= todayKey) {
        totalSec += Number(totals[k]) || 0;
      }
    });
    if (totalSec === 0) {
      (appState.todaySessions || []).forEach(s => {
        if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
      });
    }
  }

  return totalSec;
}

function fallbackLocalLeaderboard(period = 'daily') {
  const totalSec = calculateLocalFocusSecondsForPeriod(period);
  const isUserStudying = timerStatus === 'RUNNING' && currentMode !== 'break';
  const dummyRanks = [];

  const profile = appState.userProfile || {};
  const userName = profile.displayName || appState.currentUser?.user_metadata?.full_name || 'You';
  const avatar = profile.avatarPreset || appState.currentUser?.user_metadata?.avatar_url || '🐱';

  if (totalSec > 0 || isUserStudying) {
    dummyRanks.push({
      rank: 1,
      user_id: appState.currentUser ? appState.currentUser.id : 'guest',
      user_name: userName,
      avatar_url: avatar,
      total_seconds: totalSec,
      is_studying: isUserStudying,
      current_subject: isUserStudying ? (appState.selectedSubject?.name || 'Mathematics') : '',
      subject_color: appState.selectedSubject?.color || '#3b82f6'
    });
  }

  renderLeaderboard(dummyRanks, period);
}

function renderLeaderboard(rankings, period = currentLeaderboardPeriod) {
  const podiumContainer = document.getElementById('leaderboardPodium');
  const listContainer = document.getElementById('leaderboardListItems');
  const activeStudyingText = document.getElementById('leaderboardActiveStudyingText');

  if (!podiumContainer || !listContainer) return;

  // Deduplicate rankings by canonical key (user_name or user_id)
  const seenUsers = new Set();
  const uniqueRankings = [];
  (rankings || []).forEach(r => {
    if (!r) return;
    const key = (r.user_name || '').trim().toLowerCase() || r.user_id;
    if (!seenUsers.has(key)) {
      seenUsers.add(key);
      uniqueRankings.push({ ...r });
    }
  });

  // Re-index ranks 1..N
  uniqueRankings.forEach((r, idx) => {
    r.rank = idx + 1;
  });

  // 1. Calculate Active Studiers Count
  const activeStudyingCount = uniqueRankings.filter(r => r.is_studying).length;
  if (activeStudyingText) {
    activeStudyingText.textContent = activeStudyingCount > 0 
      ? `${activeStudyingCount} Studying Now` 
      : 'Live Leaderboard';
  }

  const headerLiveDot = document.getElementById('headerLiveDot');
  const mobileLiveDot = document.getElementById('mobileLiveDot');
  const isAnyActive = activeStudyingCount > 0 || (timerStatus === 'RUNNING' && currentMode !== 'break');
  if (headerLiveDot) headerLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';
  if (mobileLiveDot) mobileLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';

  const top1 = uniqueRankings.find(r => Number(r.rank) === 1);
  const top2 = uniqueRankings.find(r => Number(r.rank) === 2);
  const top3 = uniqueRankings.find(r => Number(r.rank) === 3);

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

    const isCurrent = isCurrentUserEntry(entry);
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

  const periodLabel = period === 'daily' ? 'today' : (period === 'weekly' ? 'this week' : 'this month');

  if (uniqueRankings.length === 0) {
    listContainer.innerHTML = `
      <div class="leaderboard-empty-state" style="padding: 24px 16px;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="8" r="6"></circle>
          <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"></path>
        </svg>
        <p>No study sessions recorded ${periodLabel} yet.</p>
        <span>Start a timer session to claim the #1 spot on the leaderboard!</span>
      </div>
    `;
    const localSec = calculateLocalFocusSecondsForPeriod(period);
    updatePersonalUserBar(null, localSec);
    return;
  }

  // 3. Render Ranks 4 to 25 List
  const remainingRanks = uniqueRankings.filter(r => Number(r.rank) > 3);
  if (remainingRanks.length === 0) {
    listContainer.innerHTML = `
      <div class="leaderboard-empty-state" style="padding: 24px 16px;">
        <p style="font-size: 0.85rem;">Only ${uniqueRankings.length} on the board ${periodLabel}!</p>
        <span>Complete a session to join the top rankings.</span>
      </div>
    `;
  } else {
    listContainer.innerHTML = remainingRanks.map(r => {
      const isCurrent = isCurrentUserEntry(r);
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
  const localTotalSec = calculateLocalFocusSecondsForPeriod(period);
  const myEntry = uniqueRankings.find(r => isCurrentUserEntry(r));
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
      ? `<span class="user-bar-status-studying"><span class="status-dot"></span>Studying ${subName}</span>` 
      : `<span class="user-bar-status-resting"><span class="status-dot"></span>Resting</span>`;
  }

  if (myEntry) {
    if (userBarRank) userBarRank.textContent = `#${myEntry.rank}`;
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(myEntry.total_seconds);
  } else {
    if (userBarRank) userBarRank.textContent = '#--';
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(localTotalSec);
  }
}

function updateLeaderboardCountdownDisplay() {
  const countdownEl = document.getElementById('leaderboardResetCountdown');
  if (!countdownEl) return;

  const now = new Date();
  const period = currentLeaderboardPeriod || 'daily';

  if (period === 'daily') {
    const midnight = new Date(now);
    midnight.setHours(24, 0, 0, 0);
    const diffMs = midnight - now;
    if (diffMs <= 0) {
      countdownEl.textContent = 'Resets at 00:00';
    } else {
      const hours = Math.floor(diffMs / 3600000);
      const mins = Math.floor((diffMs % 3600000) / 60000);
      countdownEl.textContent = `Resets in ${hours}h ${mins}m`;
    }
  } else if (period === 'weekly') {
    // Next Sunday 23:59:59
    const day = now.getDay();
    const daysUntilSunday = (7 - day) % 7;
    const sundayNight = new Date(now);
    sundayNight.setDate(now.getDate() + daysUntilSunday);
    sundayNight.setHours(23, 59, 59, 999);
    const diffMs = sundayNight - now;
    const days = Math.floor(diffMs / (24 * 3600000));
    const hours = Math.floor((diffMs % (24 * 3600000)) / 3600000);
    countdownEl.textContent = `Resets in ${days}d ${hours}h`;
  } else if (period === 'monthly') {
    // End of current month
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
    const diffMs = endOfMonth - now;
    const days = Math.floor(diffMs / (24 * 3600000));
    const hours = Math.floor((diffMs % (24 * 3600000)) / 3600000);
    countdownEl.textContent = `Resets in ${days}d ${hours}h`;
  }
}

function startResetCountdownTimer() {
  if (resetCountdownInterval) clearInterval(resetCountdownInterval);
  updateLeaderboardCountdownDisplay();
  resetCountdownInterval = setInterval(updateLeaderboardCountdownDisplay, 60000);
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

  lockBodyScroll();
  modal.classList.remove('hidden');
}

function closeTimerSettingsModal() {
  const modal = document.getElementById('timerSettingsModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
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
  showToast('Timer preferences applied!', 'success');
}

// Post-Session Subject Switch Modal
function openSessionCompleteModal(subject, mins) {
  const modal = document.getElementById('sessionCompleteModalOverlay');
  const title = document.getElementById('sessionCompleteTitle');
  const subtitle = document.getElementById('sessionCompleteSubtitle');
  const select = document.getElementById('nextSessionSubjectSelect');
  if (!modal || !select) return;

  if (title) title.textContent = 'Session Completed!';
  if (subtitle) subtitle.textContent = `+${mins || 1}m added to ${subject.name}`;

  select.innerHTML = appState.subjects.map(s => 
    `<option value="${s.id}" ${s.id === subject.id ? 'selected' : ''}>${s.name}</option>`
  ).join('');

  lockBodyScroll();
  modal.classList.remove('hidden');
}

function closeSessionCompleteModal() {
  document.getElementById('sessionCompleteModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
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
  lockBodyScroll();
  document.getElementById('deleteAllDataModalOverlay')?.classList.remove('hidden');
}

function closeDeleteAllDataModal() {
  document.getElementById('deleteAllDataModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
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

  showToast('All local study records and custom data erased.', 'info');
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

  lockBodyScroll();
  modal.classList.remove('hidden');

  return new Promise((resolve) => {
    customConfirmResolve = resolve;
  });
}

function closeCustomConfirmDialog(result = false) {
  const modal = document.getElementById('customConfirmModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
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
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeAuthModal() {
  const modal = document.getElementById('authModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

async function signInWithGoogle() {
  const btn = document.getElementById('btnGoogleSignIn');
  const originalHtml = btn ? btn.innerHTML : '';
  if (btn) {
    btn.disabled = true;
    btn.style.opacity = '0.7';
    btn.innerHTML = '<span>Connecting to Google...</span>';
  }

  try {
    const redirectUrl = window.location.origin + window.location.pathname;
    const sb = getSupabase();

    if (sb && sb.auth && typeof sb.auth.signInWithOAuth === 'function') {
      const { data, error } = await sb.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: redirectUrl
        }
      });
      if (error) throw error;
      if (data && data.url) {
        window.location.assign(data.url);
        return;
      }
    }

    // Direct endpoint fallback
    const oauthUrl = `${SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(redirectUrl)}`;
    window.location.assign(oauthUrl);
  } catch (err) {
    console.error('Google Sign-In Error:', err);
    try {
      const redirectUrl = window.location.origin + window.location.pathname;
      const oauthUrl = `${SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(redirectUrl)}`;
      window.location.assign(oauthUrl);
    } catch (fallbackErr) {
      showToast('Google Sign-In failed: ' + err.message, 'error');
      if (btn) {
        btn.disabled = false;
        btn.style.opacity = '';
        btn.innerHTML = originalHtml;
      }
    }
  }
}

async function signInWithEmail(e) {
  e.preventDefault();

  const emailInput = document.getElementById('authEmailInput');
  const statusMsg = document.getElementById('authStatusMessage');
  const email = emailInput?.value.trim();

  if (!email) return;

  try {
    statusMsg.textContent = 'Sending sign-in link...';
    statusMsg.classList.remove('hidden');

    const redirectUrl = window.location.origin + window.location.pathname;
    const sb = getSupabase();

    if (sb) {
      const { error } = await sb.auth.signInWithOtp({
        email,
        options: {
          emailRedirectTo: redirectUrl
        }
      });
      if (error) throw error;
    } else {
      const res = await fetch(`${SUPABASE_URL}/auth/v1/otp`, {
        method: 'POST',
        headers: {
          'apikey': SUPABASE_ANON_KEY,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          email,
          create_user: true,
          options: {
            email_redirect_to: redirectUrl
          }
        })
      });
      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.msg || errJson.message || `HTTP ${res.status}`);
      }
    }

    statusMsg.textContent = 'Magic sign-in link sent! Please check your email inbox.';
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
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeSubjectModal() {
  const modal = document.getElementById('subjectModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
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
