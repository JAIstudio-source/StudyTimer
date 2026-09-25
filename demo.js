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

function getCleanUniqueSubjects(subjectsList) {
  const source = Array.isArray(subjectsList) && subjectsList.length > 0 ? subjectsList : DEFAULT_SUBJECTS;
  const seenIds = new Set();
  const seenNames = new Set();
  const uniqueList = [];

  for (const s of source) {
    if (!s || !s.name) continue;
    const normId = String(s.id || '').trim().toLowerCase();
    const normName = String(s.name || '').trim().toLowerCase();

    if (!normName) continue;
    // Disallow duplicates by name or ID
    if (seenNames.has(normName) || (normId && seenIds.has(normId))) {
      continue;
    }

    if (normId) seenIds.add(normId);
    seenNames.add(normName);
    uniqueList.push({
      id: s.id || `sub_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      name: s.name.trim(),
      iconEmoji: s.iconEmoji || '📚',
      color: s.color || s.colorHex || '#3b82f6',
      colorHex: s.colorHex || s.color || '#3b82f6',
      isCustom: Boolean(s.isCustom)
    });
  }

  return uniqueList.length > 0 ? uniqueList : JSON.parse(JSON.stringify(DEFAULT_SUBJECTS));
}

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
let lastFocusMode = 'timer'; // 'timer', 'pomodoro'
let timerStatus = 'IDLE';  // 'IDLE', 'RUNNING', 'PAUSED'

// Millisecond-Accurate Tracking Variables
let timerStartTimestamp = null;
let accumulatedElapsedSec = 0;
let timeRemaining = 25 * 60;
let stopwatchElapsed = 0;
let timerInterval = null;

// Anti-Cheat & Continuous Study Tracking
const CONTINUOUS_STUDY_LIMIT_SEC = 12600; // 3.5 hours uninterrupted limit
const INACTIVITY_CHECK_WINDOW_SEC = 300; // 5 minutes confirmation window
const MAX_DAILY_LEADERBOARD_SECONDS = 57600; // 16 hours daily hard cap
let continuousStudyElapsedSec = 0;
let inactivityCheckPending = false;
let inactivityPromptTimestamp = 0;
let inactivityCountdownInterval = null;

// User Data & History Helpers
function getCleanInitialState(user = null) {
  const name = user?.user_metadata?.full_name || 
               user?.user_metadata?.name || 
               user?.email?.split('@')[0] || 
               'Student';
  const avatar = user?.user_metadata?.avatar_url || 
                 user?.user_metadata?.picture || 
                 'assets/logo.png';

  // Auto-approve Google OAuth avatars — they come from trusted Google servers
  const isGoogleAvatar = avatar && (
    avatar.startsWith('https://lh3.googleusercontent.com') ||
    avatar.startsWith('https://lh4.googleusercontent.com') ||
    avatar.startsWith('https://googleusercontent.com')
  );

  return {
    currentUser: user || null,
    streakCount: 0,
    lastStudyDate: '',
    approvedDisplayName: name,
    userProfile: {
      displayName: name,
      avatarPreset: avatar,
      fallbackSticker: isGoogleAvatar ? avatar : '',
      photoApproved: isGoogleAvatar, // Auto-approve trusted Google profile photos
      motto: '🎯 Deep focus & daily consistency',
      primarySubjectId: 'general',
      isPublicLeaderboard: true,
      profileStatus: 'approved'
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
async function initApp() {
  initTheme();
  loadLocalState();
  initSidebarState();
  initDomElements();
  setupEventListeners();
  initTimerWorker();
  initBackgroundSyncListeners();
  initAutoSyncEngine();
  initQuoteManager();
  initFocusAudio();
  initBackgroundManager();
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
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

// Theme Management
function initTheme() {
  const savedTheme = localStorage.getItem('studytimer-theme') || 'dark';
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

// Supabase Realtime Leaderboard Listener (On-Demand to conserve monthly quota)
let leaderboardRealtimeChannel = null;
let leaderboardRealtimeDebounce = null;

function initLeaderboardRealtime() {
  if (!supabaseClient || leaderboardRealtimeChannel) return;
  try {
    leaderboardRealtimeChannel = supabaseClient
      .channel('realtime_daily_leaderboard')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'daily_leaderboard' }, () => {
        if (leaderboardRealtimeDebounce) clearTimeout(leaderboardRealtimeDebounce);
        leaderboardRealtimeDebounce = setTimeout(() => {
          leaderboardTimeframeCache.daily = { data: null, timestamp: 0 };
          leaderboardTimeframeCache.weekly = { data: null, timestamp: 0 };
          leaderboardTimeframeCache.monthly = { data: null, timestamp: 0 };
          const modal = document.getElementById('leaderboardModalOverlay');
          if (modal && !modal.classList.contains('hidden')) {
            fetchLeaderboard(false, false);
          }
        }, 1500);
      })
      .subscribe();
  } catch (err) {
    console.warn('Leaderboard realtime subscription error:', err);
  }
}

function teardownLeaderboardRealtime() {
  if (leaderboardRealtimeChannel && supabaseClient) {
    try {
      supabaseClient.removeChannel(leaderboardRealtimeChannel);
    } catch (_) {}
    leaderboardRealtimeChannel = null;
  }
  if (leaderboardRealtimeDebounce) {
    clearTimeout(leaderboardRealtimeDebounce);
    leaderboardRealtimeDebounce = null;
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
          const rowUserEmail = (newRecord.user_email || '').trim().toLowerCase();
          // Strict user ID / email matching (RLS compliant)
          const isUserMatch = (currentUserId && (rowUserId === currentUserId || (currentUserEmail && rowUserId === currentUserEmail))) ||
                              (currentUserEmail && rowUserEmail === currentUserEmail);
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

// JWT Payload Decoder (Safe client-side extractor)
function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
    const jsonPayload = decodeURIComponent(
      atob(padded)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    try {
      const parts = token.split('.');
      if (parts.length >= 2) {
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(base64));
      }
    } catch (_) {}
    return null;
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


    // 1. Check for pending auth payload stashed by instant head sanitizer
    let authPayload = window.__STUDYTIMER_PENDING_AUTH__ || null;
    if (!authPayload) {
      try {
        const cached = sessionStorage.getItem('studytimer_pending_auth');
        if (cached) {
          authPayload = JSON.parse(cached);
          sessionStorage.removeItem('studytimer_pending_auth');
        }
      } catch (_) {}
    }

    // Also check current URL if sanitizer hadn't run yet
    if (!authPayload) {
      const hash = window.location.hash ? window.location.hash.substring(1) : '';
      const search = window.location.search ? window.location.search.substring(1) : '';
      if (hash.includes('access_token=') || search.includes('code=') || hash.includes('error=') || search.includes('error=')) {
        const hashParams = new URLSearchParams(hash);
        const searchParams = new URLSearchParams(search);
        authPayload = {
          accessToken: hashParams.get('access_token') || searchParams.get('access_token'),
          refreshToken: hashParams.get('refresh_token') || searchParams.get('refresh_token'),
          code: searchParams.get('code') || hashParams.get('code'),
          error: hashParams.get('error') || searchParams.get('error'),
          errorDescription: hashParams.get('error_description') || searchParams.get('error_description')
        };
        cleanAuthParamsFromUrl();
      }
    }

    // 2. Handle OAuth error if any
    if (authPayload?.error) {
      const msg = authPayload.errorDescription || authPayload.error || 'Sign-in error';
      showToast('Google Sign-In: ' + decodeURIComponent(msg), 'error');
      cleanAuthParamsFromUrl();
      return;
    }

    // 3. Attach Auth State Change Listener
    // Guard flag prevents double-fire when both onAuthStateChange AND getSession return a user
    let _authSignInHandled = false;
    supabaseClient.auth.onAuthStateChange(async (event, session) => {
      console.log(`🔐 Supabase Auth Event: ${event}`, session?.user?.email);
      if (session && session.user && !_authSignInHandled) {
        _authSignInHandled = true;
        handleUserSignedIn(session.user);
        cleanAuthParamsFromUrl();
      } else if (event === 'SIGNED_OUT') {
        _authSignInHandled = false;
        handleUserSignedOut();
      }
    });

    // 4. Check existing session from Supabase Client
    const { data: { session }, error: sessionError } = await supabaseClient.auth.getSession();
    if (session && session.user && !_authSignInHandled) {
      _authSignInHandled = true;
      handleUserSignedIn(session.user);
      cleanAuthParamsFromUrl();
      return;
    }

    // 5. Handle Access Token from Google OAuth redirect
    if (authPayload?.accessToken) {
      const token = authPayload.accessToken;
      const refreshToken = authPayload.refreshToken || '';

      // Decode user data directly from JWT payload
      const jwtData = parseJwtPayload(token);
      let userObj = null;

      if (jwtData && jwtData.sub) {
        userObj = {
          id: jwtData.sub,
          email: jwtData.email || '',
          user_metadata: jwtData.user_metadata || {
            full_name: jwtData.name || '',
            avatar_url: jwtData.picture || ''
          },
          app_metadata: jwtData.app_metadata || {},
          role: jwtData.role || 'authenticated'
        };
      }

      // Try setting session in Supabase Auth
      try {
        const { data, error } = await supabaseClient.auth.setSession({
          access_token: token,
          refresh_token: refreshToken
        });
        if (!error && data?.session?.user) {
          handleUserSignedIn(data.session.user);
          cleanAuthParamsFromUrl();
          return;
        }
      } catch (setErr) {
        console.warn('Supabase setSession error:', setErr);
      }

      // If setSession failed (e.g. invalid refresh token), verify access token directly via getUser
      try {
        const { data: userData, error: userError } = await supabaseClient.auth.getUser(token);
        if (!userError && userData?.user) {
          handleUserSignedIn(userData.user);
          cleanAuthParamsFromUrl();
          return;
        }
      } catch (getErr) {
        console.warn('Supabase getUser error:', getErr);
      }

      // If token is valid and not expired, log the user in with the verified JWT payload
      if (userObj) {
        const nowSec = Math.floor(Date.now() / 1000);
        if (!jwtData.exp || jwtData.exp > nowSec) {
          console.log('✅ Authenticated user from verified OAuth JWT payload:', userObj.email);
          handleUserSignedIn(userObj);
          cleanAuthParamsFromUrl();
          return;
        }
      }
    }

    // 6. Handle PKCE Code if present
    if (authPayload?.code) {
      try {
        const { data, error } = await supabaseClient.auth.exchangeCodeForSession(authPayload.code);
        if (!error && data?.session?.user) {
          handleUserSignedIn(data.session.user);
          cleanAuthParamsFromUrl();
          return;
        }
      } catch (codeErr) {
        console.warn('OAuth code exchange error:', codeErr);
      }
    }
  } catch (err) {
    console.error('Supabase auth initialization error:', err);
  }
}

function cleanAuthParamsFromUrl() {
  if (typeof window !== 'undefined' && window.history && window.history.replaceState) {
    const hasHashTokens = window.location.hash && (window.location.hash.includes('access_token=') || window.location.hash.includes('error='));
    const hasSearchTokens = window.location.search && (window.location.search.includes('code=') || window.location.search.includes('error='));
    if (hasHashTokens || hasSearchTokens) {
      const cleanUrl = window.location.pathname;
      window.history.replaceState(null, '', cleanUrl);
    }
  }
}

function handleUserSignedIn(user) {
  if (!user) return;
  const isNewlySignedIn = !appState.currentUser || appState.currentUser.id !== user.id;
  
  appState.currentUser = user;

  if (isNewlySignedIn) {
    // Strictly isolate state for this specific user account - NO cross-account bleeding
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
    syncStatusPill.classList.remove('syncing');
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
  leaderboardTimeframeCache.daily = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.weekly = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.monthly = { data: null, timestamp: 0 };
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

  loadLocalState(null);
  if (typeof renderSubjects === 'function') renderSubjects();
  if (typeof renderPlannerGoals === 'function') renderPlannerGoals();
  if (typeof renderTimeline === 'function') renderTimeline();
  if (typeof updateStatsDisplay === 'function') updateStatsDisplay();
  if (typeof renderSubjectDonutChart === 'function') renderSubjectDonutChart();
  if (typeof renderActivityHeatmap === 'function') renderActivityHeatmap();
  if (typeof renderMonthlyCalendar === 'function') renderMonthlyCalendar();
  if (typeof renderUserProfileUI === 'function') renderUserProfileUI();

  leaderboardTimeframeCache.daily = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.weekly = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.monthly = { data: null, timestamp: 0 };
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
    syncStatusPill.classList.remove('synced', 'syncing');
    syncStatusText.textContent = 'Local';
  }
}

// ============================================================================

// ============================================================================
// MODULAR MODULES:
// 1. js/demo-cloud-sync.js  - Two-way Supabase Sync & Auto-Sync Engine
// 2. js/demo-analytics.js   - Overview Charts, Donut, Heatmap, Calendar, Planner
// 3. js/demo-profanity.js   - Multi-Tier Community Safety & Filter
// 4. js/demo-leaderboard.js - Study Presence, Profile Modal & Live Leaderboard
// 5. js/demo-quotes.js      - Daily Quotes Repository & Rotation
// 6. js/demo-audio.js       - Procedural WebAudio & YouTube Ambience Player
// ============================================================================

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

  window.addEventListener('beforeunload', (e) => {
    if (timerStatus === 'RUNNING' || timerStatus === 'PAUSED' || accumulatedElapsedSec > 0) {
      saveActiveSessionState();
      saveLocalState();
      e.preventDefault();
      e.returnValue = '';
      return '';
    }
  });

  window.addEventListener('pagehide', () => {
    if (timerStatus === 'RUNNING' || timerStatus === 'PAUSED') {
      saveActiveSessionState();
      saveLocalState();
    }
  });

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

  // Anti-Cheat: Track continuous uninterrupted study & trigger 3.5h check-in prompt
  if (currentMode !== 'break') {
    continuousStudyElapsedSec = (continuousStudyElapsedSec || 0) + 1;
    if (!inactivityCheckPending && continuousStudyElapsedSec >= CONTINUOUS_STUDY_LIMIT_SEC) {
      triggerInactivityCheck();
    }
    if (inactivityCheckPending && (Date.now() - inactivityPromptTimestamp >= INACTIVITY_CHECK_WINDOW_SEC * 1000)) {
      pauseTimer();
      dismissInactivityModal();
      showToast('Timer auto-paused after 3.5h continuous session without check-in.', 'warning');
      return;
    }
  }

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

      // If user is logged in, auto-save to cloud & leaderboard progress
      if (appState.currentUser && supabaseClient && navigator.onLine) {
        pushDataToCloud(true);
        syncStudyProgressToLeaderboard(60);

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

function triggerInactivityCheck() {
  if (inactivityCheckPending) return;
  inactivityCheckPending = true;
  inactivityPromptTimestamp = Date.now();

  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(587.33, audioCtx.currentTime);
    osc.frequency.setValueAtTime(880, audioCtx.currentTime + 0.15);
    gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.5);
  } catch (e) {}

  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('Study Check-in Required', {
      body: "You've been studying for 3.5 hours continuously. Click to confirm you are active!",
      icon: 'assets/logo.png'
    });
  }

  showInactivityModal();
}

function showInactivityModal() {
  let modal = document.getElementById('inactivityCheckModal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'inactivityCheckModal';
    modal.className = 'modal-backdrop active';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.75);backdrop-filter:blur(8px);z-index:99999;display:flex;align-items:center;justify-content:center;animation:fadeIn 0.3s ease;';
    modal.innerHTML = `
      <div style="background:var(--bg-surface, #1e293b);border:1px solid rgba(255,255,255,0.15);border-radius:24px;padding:32px;max-width:440px;width:90%;box-shadow:0 25px 50px -12px rgba(0,0,0,0.5);text-align:center;color:var(--text-main, #fff);">
        <div style="font-size:42px;margin-bottom:12px;">🔥</div>
        <h3 style="font-size:22px;font-weight:700;margin-bottom:8px;color:var(--primary, #a78bfa);">Are you still studying?</h3>
        <p style="font-size:14px;color:rgba(255,255,255,0.75);line-height:1.5;margin-bottom:16px;">
          You have been studying continuously for <strong>3.5 hours</strong>! Please confirm you are active so we keep your timer running.
        </p>
        <div id="inactivityCountdownDisplay" style="display:inline-block;padding:8px 18px;background:rgba(167,139,250,0.15);border:1px solid rgba(167,139,250,0.3);border-radius:20px;font-size:15px;font-weight:600;color:var(--primary, #c4b5fd);margin-bottom:24px;">
          Auto-pausing in 5:00
        </div>
        <div style="display:flex;gap:12px;justify-content:center;">
          <button id="inactivityBreakBtn" style="padding:12px 20px;border-radius:14px;border:1px solid rgba(255,255,255,0.2);background:transparent;color:rgba(255,255,255,0.8);font-size:14px;font-weight:600;cursor:pointer;transition:all 0.2s;">
            Take a Break
          </button>
          <button id="inactivityConfirmBtn" style="padding:12px 24px;border-radius:14px;border:none;background:var(--primary-gradient, linear-gradient(135deg,#8b5cf6,#6366f1));color:#fff;font-size:14px;font-weight:700;cursor:pointer;box-shadow:0 4px 15px rgba(139,92,246,0.4);transition:all 0.2s;">
            ✓ Still Studying
          </button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    document.getElementById('inactivityConfirmBtn').onclick = confirmContinuousStudy;
    document.getElementById('inactivityBreakBtn').onclick = () => {
      dismissInactivityModal();
      switchMode('break');
    };
  } else {
    modal.style.display = 'flex';
  }

  if (inactivityCountdownInterval) clearInterval(inactivityCountdownInterval);
  inactivityCountdownInterval = setInterval(() => {
    if (!inactivityCheckPending) {
      clearInterval(inactivityCountdownInterval);
      return;
    }
    const elapsedSec = Math.floor((Date.now() - inactivityPromptTimestamp) / 1000);
    const remainingSec = Math.max(0, INACTIVITY_CHECK_WINDOW_SEC - elapsedSec);
    const m = Math.floor(remainingSec / 60);
    const s = remainingSec % 60;
    const disp = document.getElementById('inactivityCountdownDisplay');
    if (disp) {
      disp.textContent = `Auto-pausing in ${m}:${s.toString().padStart(2, '0')}`;
    }
    if (remainingSec <= 0) {
      clearInterval(inactivityCountdownInterval);
      pauseTimer();
      dismissInactivityModal();
      showToast('Timer auto-paused after 3.5h uninterrupted session.', 'warning');
    }
  }, 1000);
}

function dismissInactivityModal() {
  inactivityCheckPending = false;
  if (inactivityCountdownInterval) {
    clearInterval(inactivityCountdownInterval);
    inactivityCountdownInterval = null;
  }
  const modal = document.getElementById('inactivityCheckModal');
  if (modal) {
    modal.style.display = 'none';
  }
}

function confirmContinuousStudy() {
  continuousStudyElapsedSec = 0;
  inactivityCheckPending = false;
  dismissInactivityModal();
  showToast('Focus session confirmed! Keep going! 🚀', 'success');
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
  timerInterval = setInterval(tickTimer, 500);
}

function pauseTimer() {
  if (timerStatus === 'RUNNING' && timerStartTimestamp) {
    const elapsedSinceResume = Math.floor((Date.now() - timerStartTimestamp) / 1000);
    accumulatedElapsedSec += elapsedSinceResume;
  }
  timerStatus = 'PAUSED';
  timerStartTimestamp = null;
  continuousStudyElapsedSec = 0;
  dismissInactivityModal();
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
  continuousStudyElapsedSec = 0;
  dismissInactivityModal();
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
  const subject = appState.selectedSubject || DEFAULT_SUBJECTS[0];
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
    const todayKey = getLocalDateStr();
    // Add any remaining seconds that were not auto-saved on the minute tick
    const autoSavedSec = (lastAutoSavedMinute || 0) * 60;
    const remainderSec = Math.max(0, studiedDurationSec - autoSavedSec);
    if (remainderSec > 0) {
      appState.dailyFocusTotals[todayKey] = (appState.dailyFocusTotals[todayKey] || 0) + remainderSec;
      const subId = subject.id || 'general';
      appState.subjectDurations[subId] = (appState.subjectDurations[subId] || 0) + remainderSec;
      if (!appState.dailySubjectDurations[todayKey]) {
        appState.dailySubjectDurations[todayKey] = {};
      }
      appState.dailySubjectDurations[todayKey][subId] = (appState.dailySubjectDurations[todayKey][subId] || 0) + remainderSec;
    }

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

    // Automation: Auto-switch back to previous focus mode (Countdown or Pomodoro) after break
    if (timerConfig.pomoAutoSwitchFocus !== false) {
      isLongBreakActive = false;
      setTimeout(() => {
        const nextMode = lastFocusMode || 'pomodoro';
        switchMode(nextMode);
        if (nextMode === 'pomodoro') {
          const totalCycles = timerConfig.pomoTotalCycles || 4;
          showToast(`Ready for Pomodoro Focus (Cycle ${pomoCurrentCycle}/${totalCycles})`, 'info');
        } else {
          showToast(`Ready for Countdown Focus (${timerConfig.customTimerMinutes || 25}m)`, 'info');
        }
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

    // Pomodoro Automation: Auto-switch to break ONLY when in Pomodoro mode
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

  appState.subjects = getCleanUniqueSubjects(appState.subjects);
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
    const metricSubjectVal = document.getElementById('metricSubjectVal');
    if (metricSubjectVal && appState.selectedSubject) {
      metricSubjectVal.textContent = appState.selectedSubject.name;
    }
  }
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
    const redirectUrl = window.location.origin + (window.location.pathname.includes('demo') ? '/demo.html' : window.location.pathname);
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

    const redirectUrl = window.location.origin + (window.location.pathname.includes('demo') ? '/demo.html' : window.location.pathname);
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

// ============================================================================

// 10. STUDIO BACKGROUND & ATMOSPHERE MANAGER
// ============================================================================
const BG_PRESETS = {
  default: '',
  lofi: 'https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=80&w=1920&auto=format&fit=crop',
  rain: 'https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?q=80&w=1920&auto=format&fit=crop',
  library: 'https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=1920&auto=format&fit=crop',
  space: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?q=80&w=1920&auto=format&fit=crop',
  forest: 'https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=1920&auto=format&fit=crop',
  tokyo: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?q=80&w=1920&auto=format&fit=crop',
  sunset: 'https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?q=80&w=1920&auto=format&fit=crop',
  cafe: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=1920&auto=format&fit=crop',
  amoled: '#000000'
};

let currentBgKey = localStorage.getItem('studytimer_bg_preset') || 'sunset';
let customBgDataUrl = localStorage.getItem('studytimer_custom_bg') || '';
let bgDimmerVal = parseInt(localStorage.getItem('studytimer_bg_dimmer') || '30', 10);
let bgBlurVal = parseInt(localStorage.getItem('studytimer_bg_blur') || '0', 10);

function initBackgroundManager() {
  applyStudioBackground();

  // Dimmer & Blur slider handlers
  const dimmerSlider = document.getElementById('bgDimmerSlider');
  const dimmerText = document.getElementById('bgDimmerValText');
  const blurSlider = document.getElementById('bgBlurSlider');
  const blurText = document.getElementById('bgBlurValText');

  if (dimmerSlider) {
    dimmerSlider.value = bgDimmerVal;
    if (dimmerText) dimmerText.textContent = `${bgDimmerVal}%`;
    dimmerSlider.addEventListener('input', (e) => {
      bgDimmerVal = parseInt(e.target.value, 10);
      if (dimmerText) dimmerText.textContent = `${bgDimmerVal}%`;
      localStorage.setItem('studytimer_bg_dimmer', bgDimmerVal);
      applyStudioBackground();
    });
  }

  if (blurSlider) {
    blurSlider.value = bgBlurVal;
    if (blurText) blurText.textContent = `${bgBlurVal}px`;
    blurSlider.addEventListener('input', (e) => {
      bgBlurVal = parseInt(e.target.value, 10);
      if (blurText) blurText.textContent = `${bgBlurVal}px`;
      localStorage.setItem('studytimer_bg_blur', bgBlurVal);
      applyStudioBackground();
    });
  }

  // Preset Card click handlers
  document.querySelectorAll('.bg-preset-card').forEach(card => {
    card.addEventListener('click', () => {
      const bgKey = card.dataset.bg;
      setStudioBackgroundPreset(bgKey);
    });
  });

  // Custom upload zone
  const uploadInput = document.getElementById('bgImageFileInput');
  const triggerBtn = document.getElementById('btnTriggerBgUpload');
  const resetBtn = document.getElementById('btnResetCustomBg');
  const bgUrlInput = document.getElementById('bgUrlInput');
  const btnApplyBgUrl = document.getElementById('btnApplyBgUrl');

  let previewDebounceTimer = null;

  function testAndPreviewImageUrl(url, callback) {
    const card = document.getElementById('bgUrlPreviewCard');
    const img = document.getElementById('bgUrlPreviewImg');
    const placeholder = document.getElementById('bgUrlPreviewPlaceholder');
    const status = document.getElementById('bgUrlPreviewStatus');
    const msg = document.getElementById('bgUrlPreviewMsg');

    if (!card || !status || !msg) {
      if (typeof callback === 'function') callback(false);
      return;
    }

    const trimmed = (url || '').trim();
    if (!trimmed) {
      card.classList.add('hidden');
      card.classList.remove('is-valid', 'is-error');
      if (img) { img.src = ''; img.classList.add('hidden'); }
      if (placeholder) placeholder.classList.remove('hidden');
      if (typeof callback === 'function') callback(false);
      return;
    }

    card.classList.remove('hidden', 'is-valid', 'is-error');
    status.textContent = 'Verifying image/GIF format...';
    msg.textContent = trimmed.length > 55 ? trimmed.substring(0, 52) + '...' : trimmed;

    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://') && !trimmed.startsWith('data:image/')) {
      card.classList.add('is-error');
      status.textContent = '❌ Not supported format';
      msg.textContent = 'Link must start with https:// or http://';
      if (img) { img.src = ''; img.classList.add('hidden'); }
      if (placeholder) placeholder.classList.remove('hidden');
      if (typeof callback === 'function') callback(false);
      return;
    }

    const testImg = new Image();
    testImg.onload = () => {
      card.classList.remove('is-error');
      card.classList.add('is-valid');
      status.textContent = '✓ Supported Image/GIF Preview';
      const isGif = trimmed.toLowerCase().includes('.gif') || (testImg.src && testImg.src.toLowerCase().includes('.gif'));
      msg.textContent = `${isGif ? 'Animated GIF' : 'Image format'} (${testImg.naturalWidth || 0}×${testImg.naturalHeight || 0}px)`;
      if (img) {
        img.src = trimmed;
        img.classList.remove('hidden');
      }
      if (placeholder) placeholder.classList.add('hidden');
      if (typeof callback === 'function') callback(true);
    };

    testImg.onerror = () => {
      card.classList.remove('is-valid');
      card.classList.add('is-error');
      status.textContent = '❌ Not supported format';
      msg.textContent = 'Could not load image or GIF. Link is broken or format unsupported.';
      if (img) {
        img.src = '';
        img.classList.add('hidden');
      }
      if (placeholder) placeholder.classList.remove('hidden');
      if (typeof callback === 'function') callback(false);
    };

    testImg.src = trimmed;
  }

  bgUrlInput?.addEventListener('input', (e) => {
    clearTimeout(previewDebounceTimer);
    const val = e.target.value;
    if (!val || (!val.startsWith('http://') && !val.startsWith('https://') && !val.startsWith('data:image/'))) {
      testAndPreviewImageUrl(val);
    } else {
      previewDebounceTimer = setTimeout(() => {
        testAndPreviewImageUrl(val);
      }, 200);
    }
  });

  bgUrlInput?.addEventListener('paste', () => {
    setTimeout(() => {
      testAndPreviewImageUrl(bgUrlInput.value);
    }, 50);
  });

  triggerBtn?.addEventListener('click', () => uploadInput?.click());

  uploadInput?.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      showToast('Image is too large (max 5MB).', 'warning');
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const result = event.target?.result;
      if (typeof result === 'string') {
        try {
          localStorage.setItem('studytimer_custom_bg', result);
          customBgDataUrl = result;
          currentBgKey = 'custom';
          localStorage.setItem('studytimer_bg_preset', 'custom');
          applyStudioBackground();
          testAndPreviewImageUrl(result);
          if (resetBtn) resetBtn.style.display = 'inline-block';
          showToast('Custom wallpaper applied!', 'success');
        } catch (err) {
          showToast('Failed to save wallpaper: browser storage full.', 'danger');
        }
      }
    };
    reader.readAsDataURL(file);
  });

  // Apply custom URL wallpaper
  btnApplyBgUrl?.addEventListener('click', () => {
    const url = bgUrlInput?.value.trim();
    if (!url) {
      showToast('Please enter an image or GIF URL.', 'warning');
      return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://') && !url.startsWith('data:image/')) {
      testAndPreviewImageUrl(url);
      showToast('Please enter a valid URL starting with https://', 'warning');
      return;
    }

    testAndPreviewImageUrl(url, (isValid) => {
      if (isValid) {
        try {
          localStorage.setItem('studytimer_custom_bg', url);
          customBgDataUrl = url;
          currentBgKey = 'custom';
          localStorage.setItem('studytimer_bg_preset', 'custom');
          applyStudioBackground();
          if (resetBtn) resetBtn.style.display = 'inline-block';
          showToast('Custom wallpaper URL applied! ✨', 'success');
        } catch (err) {
          showToast('Failed to apply wallpaper URL.', 'danger');
        }
      } else {
        showToast('Not supported format or unable to load image/GIF.', 'danger');
      }
    });
  });

  resetBtn?.addEventListener('click', () => {
    localStorage.removeItem('studytimer_custom_bg');
    customBgDataUrl = '';
    if (bgUrlInput) bgUrlInput.value = '';
    testAndPreviewImageUrl('');
    setStudioBackgroundPreset('sunset');
    if (resetBtn) resetBtn.style.display = 'none';
    showToast('Reset to default Sunset Clouds wallpaper.', 'info');
  });

  if (customBgDataUrl && resetBtn) {
    resetBtn.style.display = 'inline-block';
  }

  // Modal open / close
  document.getElementById('btnTopbarBackground')?.addEventListener('click', openStudioBgModal);
  document.getElementById('btnOpenBackgroundModal')?.addEventListener('click', openStudioBgModal);
  document.getElementById('btnCloseStudioBgModal')?.addEventListener('click', closeStudioBgModal);
  document.getElementById('studioBgModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'studioBgModalOverlay') closeStudioBgModal();
  });
}

function setStudioBackgroundPreset(bgKey) {
  currentBgKey = bgKey;
  localStorage.setItem('studytimer_bg_preset', bgKey);
  
  document.querySelectorAll('.bg-preset-card').forEach(c => {
    if (c.dataset.bg === bgKey) {
      c.classList.add('active');
    } else {
      c.classList.remove('active');
    }
  });

  applyStudioBackground();
}

function applyStudioBackground() {
  const bgLayer = document.getElementById('studioBgLayer');
  const bgDimmer = document.getElementById('studioBgDimmer');

  if (bgDimmer) {
    bgDimmer.style.backgroundColor = `rgba(0, 0, 0, ${bgDimmerVal / 100})`;
  }

  if (bgLayer) {
    bgLayer.style.filter = bgBlurVal > 0 ? `blur(${bgBlurVal}px)` : 'none';

    if (currentBgKey === 'custom' && customBgDataUrl) {
      bgLayer.style.backgroundImage = `url("${customBgDataUrl}")`;
      bgLayer.style.backgroundColor = 'transparent';
      document.body.classList.add('has-custom-bg');
    } else if (currentBgKey === 'amoled') {
      bgLayer.style.backgroundImage = 'none';
      bgLayer.style.backgroundColor = '#000000';
      document.body.classList.add('has-custom-bg');
    } else if (BG_PRESETS[currentBgKey]) {
      bgLayer.style.backgroundImage = `url("${BG_PRESETS[currentBgKey]}")`;
      bgLayer.style.backgroundColor = 'transparent';
      document.body.classList.add('has-custom-bg');
    } else {
      bgLayer.style.backgroundImage = 'none';
      bgLayer.style.backgroundColor = 'transparent';
      document.body.classList.remove('has-custom-bg');
    }
  }

  // Update active preset UI cards
  document.querySelectorAll('.bg-preset-card').forEach(c => {
    if (c.dataset.bg === currentBgKey) {
      c.classList.add('active');
    } else {
      c.classList.remove('active');
    }
  });
}

function openStudioBgModal() {
  const modal = document.getElementById('studioBgModalOverlay');
  const bgUrlInput = document.getElementById('bgUrlInput');
  if (customBgDataUrl && bgUrlInput && (!bgUrlInput.value || bgUrlInput.value === customBgDataUrl)) {
    if (customBgDataUrl.startsWith('http://') || customBgDataUrl.startsWith('https://')) {
      bgUrlInput.value = customBgDataUrl;
    }
    const card = document.getElementById('bgUrlPreviewCard');
    const img = document.getElementById('bgUrlPreviewImg');
    const placeholder = document.getElementById('bgUrlPreviewPlaceholder');
    const status = document.getElementById('bgUrlPreviewStatus');
    const msg = document.getElementById('bgUrlPreviewMsg');
    if (card && status) {
      card.classList.remove('hidden', 'is-error');
      card.classList.add('is-valid');
      status.textContent = '✓ Active Custom Wallpaper';
      if (msg) msg.textContent = customBgDataUrl.startsWith('data:') ? 'Custom local uploaded image / GIF' : customBgDataUrl;
      if (img) {
        img.src = customBgDataUrl;
        img.classList.remove('hidden');
      }
      if (placeholder) placeholder.classList.add('hidden');
    }
  }
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeStudioBgModal() {
  const modal = document.getElementById('studioBgModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}








